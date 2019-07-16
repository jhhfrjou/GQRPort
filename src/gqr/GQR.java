package gqr;

import gqr.Join.joinTypeInQuery;

import isi.mediator.SourceQuery;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import uk.ac.ox.cs.chaseBench.model.Constant;
import uk.ac.ox.cs.chaseBench.model.Domain;
import uk.ac.ox.cs.chaseBench.model.Rule;
import uk.ac.ox.cs.chaseBench.parser.CommonParser;
import uk.ac.ox.cs.chaseBench.processors.InputCollector;
import uk.ac.soton.ecs.RelationalModel.ConjunctiveQuery;
import uk.ac.soton.ecs.RelationalModel.DatabaseSchema;
import uk.ac.soton.ecs.RelationalModel.parser.CBQueryConverter;
import uk.ac.soton.ecs.RelationalModel.parser.CBSchemaConverter;

public class GQR {

    //the user query to be reformulated
    private Query query;
    //time that should be excluded from the measurements (measuring assertions etc)
    long dontCountTime = 0;

    //times recorded for profiling purposes
    private long postProcTime = 0;
    private long enforceEquatesandMergesTime = 0;
    private long restofPostProcTime = 0;
    private long enforceMergesTime = 0;
    private long enforceEquatesTime = 0;
    private long cloningTime = 0;
    private long subSymbolsTime = 0;
    private long subDontCaresTime = 0;
    private List<ConjunctiveQuery> views;

    //map that temporarily holds pairs of partial rewritings that are to be merged while combining their CPJs
    private List<Pair<AtomicRewriting, AtomicRewriting>> merges = new ArrayList<Pair<AtomicRewriting, AtomicRewriting>>();
    //exmerges temporarily holds at each combination step all the to-be-merged partial rewritings due to them being existential
    //we need this so we don't cross-product partial rewritings among different sets of exmerges when concluding the combination step.
    private HashSet<HashSet<AtomicRewriting>> exmerges = new HashSet<HashSet<AtomicRewriting>>();////TODO Wrap up AtomicRewritings in exmerges below, so I can use my own equals for efficient hashing (look at method addExistentialMerge)

    //map that temporarily holds pairs of variables that are to be equated while combining their CPJs
    private List<Pair<String,Pair<Pair<Integer,JoinInView>, Pair<Integer,JoinInView>>>> equates = new ArrayList<Pair<String,Pair<Pair<Integer,JoinInView>, Pair<Integer,JoinInView>>>>();

    //map to index all PJs that are keys of the next map, any pj A which maps to a key pj B of the next map
    //will be a kept in this map, and its value will be a table containing B
    Map <SourcePredicateJoin,List<SourcePredicateJoin>> indexSourcePJs;

    //map to ``globally'' hold all PJs, if repeated predicates exist in the same source, these are contained in the list for the spj
    Map<SourcePredicateJoin, List<SourcePredicateJoin>> sourcePJs;

    //holds all different query pj patterns asked by retrieveSourcePjs()
    Map<SourcePredicateJoin, List<SourcePredicateJoin>> pastReturned = new HashMap<SourcePredicateJoin,List<SourcePredicateJoin>>();

    //Number of the conjunctive rewritings in the answer
    public Integer reNo;
    private int timescloneddummycalled = 0;


    private enum Variables {
        Query, ViewNo, Time, RewNo, ExcludedTime
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Supported options:");
            System.out.println("-s-sch   <file> (optional)    | the file containing the source schema");
            System.out.println("-t-sch   <file> (optional)    | the file containing the target schema");
            System.out.println("-st-tgds <file>               | the file containing the source-to-target TGDs");
            System.out.println("-q       <file>               | the file containing the query");
        } else {
            String sSchemLoc = null;
            String tSchemLoc = null;
            String views = null;
            String query = null;

            for(int i = 0; i < args.length-1; i += 2) {
                String argument = args[i];
                switch(argument) {
                    case "-s-sch":
                        sSchemLoc = args[i+1];
                        break;
                    case "-t-sch":
                        tSchemLoc = args[i+1];
                        break;
                    case "-st-tgds":
                        views = args[i+1];
                        break;
                    case "-q":
                        query = args[i+1];
                        break;
                    default:
                        System.out.println("Unknown option '" + argument + "'.");

                }

            }
            GQR g;
            if(views != null && query != null) {
                if(sSchemLoc != null && tSchemLoc != null) {
                    g = new GQR(query,views,sSchemLoc,tSchemLoc);
                } else {
                    g = new GQR(query, views,-1);
                }
                long st = System.currentTimeMillis();


                List<CompRewriting> ans = new ArrayList<CompRewriting>();
                try {
                    ans = g.reformulate(g.getQuery());
                } catch (NonAnswerableQueryException e) {
                    //					throw new RuntimeException(e);
                    long end = System.currentTimeMillis();
                    System.out.println("NA Case:  Time:" + ((end-st)-g.dontCountTime));
                }
                long end = System.currentTimeMillis();
                System.out.println("Time:" + ((end-st)-g.dontCountTime) +" rewNo:"+g.reNo);
            } else if(views == null){
                System.out.println("Invalid argument setup: No source-to-target TGDs found");
            } else {
                System.out.println("Invalid argument setup: No query found");
            }

        }














    }

    public Query getQuery() {
        return query;
    }




    public GQR(String queryFile, String viewsFile, String viewSchema, String targetSchema) {
        long st = System.currentTimeMillis();
        try {
            List<Query> views = getQueriesWithSchema(viewsFile,viewSchema,targetSchema, true);
            Pair <Map <SourcePredicateJoin,List<SourcePredicateJoin>>,Map <SourcePredicateJoin,List<SourcePredicateJoin>>> pjs = Query.createSourcePredicateJoins(views);
            sourcePJs = pjs.getA();
            indexSourcePJs = pjs.getB();
            dontCountTime += System.currentTimeMillis() - st;
            query = getQueriesWithSchema(queryFile,viewSchema,targetSchema, false).get(0);
            query.computeQueryPJs();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("query: ");
        System.out.println("	"+query);
    }

    /**
     * Initializes a GQR object. Parses numberofsources of the views (source definitions) and the query
     * @param queryFile the file containing the query
     * @param viewsFile the file containing the sources
     */
    public GQR(String queryFile, String viewsFile, int numberOfSources) {

        long st = System.currentTimeMillis();
        try {
            List<Query> views = getQueriesNoSchema(viewsFile,numberOfSources, true);
            Pair <Map <SourcePredicateJoin,List<SourcePredicateJoin>>,Map <SourcePredicateJoin,List<SourcePredicateJoin>>> pjs = Query.createSourcePredicateJoins(views);
            sourcePJs = pjs.getA();
            indexSourcePJs = pjs.getB();
            dontCountTime += System.currentTimeMillis() - st;
            query = getQueriesNoSchema(queryFile,1, false).get(0);
            query.computeQueryPJs();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Query: "+query);

    }


    static boolean assertIDs(
            Map<SourcePredicateJoin, List<SourcePredicateJoin>> sourcePJs2) {
        for(List<SourcePredicateJoin> lsp: sourcePJs2.values())
            assert(assertRepeatedIds(lsp));

        return true;
    }


    public List<Query> getQueriesWithSchema(String queryFile, String sourceSchema, String targetSchema, boolean source) throws Exception {
        List<Rule>  rules = readRulesfromFile(queryFile);
        uk.ac.ox.cs.chaseBench.model.DatabaseSchema dbSchema = new uk.ac.ox.cs.chaseBench.model.DatabaseSchema();
        dbSchema.load(new File(sourceSchema),false);
        dbSchema.load(new File(targetSchema),true);
        CBSchemaConverter converter = new CBSchemaConverter();
        DatabaseSchema rmSchema = combineSchemas(converter.toRelationalModel(dbSchema));
        return createQueries(rules, dbSchema,rmSchema,-1, source);
    }

    public List<Query> getQueriesNoSchema(String queryFile, int numberOfSources, boolean source) throws Exception {
        List<Rule> rules = readRulesfromFile(queryFile);
        uk.ac.ox.cs.chaseBench.model.DatabaseSchema cbSchema = generateSchema(rules);
        CBSchemaConverter schemaConverter = new CBSchemaConverter();
        uk.ac.soton.ecs.RelationalModel.DatabaseSchema[] rmSchema = schemaConverter.toRelationalModel(cbSchema);
        return createQueries(rules,cbSchema,combineSchemas(rmSchema),numberOfSources, source);

    }

    public List<Query> createQueries(List<Rule> cbRules, uk.ac.ox.cs.chaseBench.model.DatabaseSchema cbSchema, DatabaseSchema schema, int numberOfSources, boolean source) throws Exception {
        List<Query> queries = new ArrayList<>();
        CBQueryConverter queryConverter = new CBQueryConverter();
        for (Rule rule: cbRules) {
            if(queries.size() == numberOfSources)
                return queries;
            ConjunctiveQuery cQ = queryConverter.toRelationalModel(rule,cbSchema,schema);
            queries.add(new Query(cQ,source));
        }
        return queries;
    }

    private static DatabaseSchema combineSchemas(DatabaseSchema[] schemas) {
        if (schemas.length == 2) {
            Set<uk.ac.soton.ecs.RelationalModel.Predicate> otherPredicates = schemas[1].getPredicates();
            for(uk.ac.soton.ecs.RelationalModel.Predicate otherPredicate : otherPredicates) {
                schemas[0].add(otherPredicate);
            }
            return schemas[0];
        }
        return null;
    }

    private List<Rule> readRulesfromFile(String ruleLocation) throws Exception {
        List<Rule> cbRules = new ArrayList<>();
        StringBuffer output = getStringBufferofFile(ruleLocation);
        InputCollector inputCollector = new InputCollector(null, cbRules, null);
        CommonParser parser = new CommonParser(new StringReader(output.toString()));
        parser.parse(inputCollector);
        return cbRules;
    }

    private uk.ac.ox.cs.chaseBench.model.DatabaseSchema readSchemafromFile(String schemaLocation) throws Exception {
        uk.ac.ox.cs.chaseBench.model.DatabaseSchema schema = new uk.ac.ox.cs.chaseBench.model.DatabaseSchema();
        StringBuffer output = getStringBufferofFile(schemaLocation);
        InputCollector inputCollector = new InputCollector(schema, null, null);
        CommonParser parser = new CommonParser(new StringReader(output.toString()));
        parser.parse(inputCollector);
        return schema;
    }

    private StringBuffer getStringBufferofFile(String schemaLocation) throws IOException {
        InputStream resourceStream = new FileInputStream(schemaLocation);
        Reader input = new InputStreamReader(resourceStream);
        StringBuffer output = new StringBuffer();
        char[] buffer = new char[4096];
        int read;
        while ((read = input.read(buffer)) != -1)
            output.append(buffer, 0, read);
        input.close();
        return output;
    }


    public uk.ac.ox.cs.chaseBench.model.DatabaseSchema generateSchema(List<Rule> rules) {
        uk.ac.ox.cs.chaseBench.model.DatabaseSchema schema = new uk.ac.ox.cs.chaseBench.model.DatabaseSchema();
        for ( Rule rule: rules) {
            for (uk.ac.ox.cs.chaseBench.model.Atom atom : rule.getBodyAtoms()) {
                if (!schema.getPredicates().contains(atom.getPredicate())) {
                    String[] colName = new String[atom.getNumberOfArguments()];
                    Domain[] doms = new Domain[atom.getNumberOfArguments()];
                    for (int i = 0; i < atom.getNumberOfArguments(); i++) {
                        uk.ac.ox.cs.chaseBench.model.Term temp = atom.getArgument(i);
                        colName[i] = temp.toString();
                        if (temp instanceof uk.ac.ox.cs.chaseBench.model.Constant)
                            doms[i] = ((Constant) temp).getDomain();
                        else
                            doms[i] = Domain.INTEGER;
                    }
                    schema.addPredicateSchema(atom.getPredicate(), false, colName, doms);
                }
            }
            for (uk.ac.ox.cs.chaseBench.model.Atom atom : rule.getHeadAtoms()) {
                if (!schema.getPredicates().contains(atom.getPredicate())) {
                    String[] colName = new String[atom.getNumberOfArguments()];
                    Domain[] doms = new Domain[atom.getNumberOfArguments()];
                    for (int i = 0; i < atom.getNumberOfArguments(); i++) {
                        uk.ac.ox.cs.chaseBench.model.Term temp = atom.getArgument(i);
                        colName[i] = temp.toString();
                        doms[i] = Domain.INTEGER;
                    }
                    schema.addPredicateSchema(atom.getPredicate(), true, colName, doms);
                }
            }
        }
        return schema;
    }

    /**
     * Input: A query Q on the mediation schema
     * Output: A list of re-writings for the query
     * @return a list of conjunctive rewritings (using only source/view relations), whose union is a maximally-contained rewriting of the query
     * @throws NonAnswerableQueryException if some part ofthe query cannot covered (or answered) by any source/view
     */
    public final List<CompRewriting> reformulate(Query query) throws NonAnswerableQueryException {

        Set<CPJCoverSet> currentCPJSets = new HashSet<CPJCoverSet>(); //(sets of) partial coverings of the query
        Set<CompositePredicateJoin> resultCPJs = new HashSet<CompositePredicateJoin>(); //complete coverings

        Set<PredicateJoin> guery_pjs = query.getQueryPJs();
        for(PredicateJoin pjq: guery_pjs)
        {
            CPJCoverSet source_pjs = retrieveSourcePJSet(pjq); //(a wrapper of) a set of CPJs for this predicate
            if(source_pjs.isEmpty())
                throw new NonAnswerableQueryException();
            else
                currentCPJSets.add(source_pjs);
        }
        while(!currentCPJSets.isEmpty()) { //iterates over partial sets of query coverings
            Pair<CPJCoverSet,CPJCoverSet>  p = null;
            try{
                p = select(currentCPJSets); //must always exist a pair until we reach a complete rewriting
            }catch(NotEnoughCPJsException e){

                long st = System.currentTimeMillis();
                assert(currentCPJSets.size() == 1);
                dontCountTime += System.currentTimeMillis() - st;

                CPJCoverSet C = currentCPJSets.iterator().next();
                if(C.getSerialNo() == 1 && query.sumOfPredicatesSerials() ==1)
                {
                    st = System.currentTimeMillis();
                    assert(query.getBody().size() ==1);
                    assert(AssertAllHaveSerialOne(C.getCPJs()));//Assertions only for debugging
                    dontCountTime += System.currentTimeMillis() - st;
                    resultCPJs.addAll(C.getCPJs());
                    //I change collect rewritings and I am additionally passing the original query (casted as ISI query)
                    //in order to check that the rewritings collected are contained in the query

                    return collectRewritings(resultCPJs,Util.castQueryAsISISourceQuery(query/*parseQueryIntoDatalog(cr.toString())*/));
                    //return;
                }


                throw new NonAnswerableQueryException();

            }

            CPJCoverSet A = p.getA();
            CPJCoverSet B = p.getB();

            //combine A,B remove them and put C in their place
            CPJCoverSet C = combineSets(A,B);


            if(C.isEmpty())
                throw new NonAnswerableQueryException();


            currentCPJSets.remove(A);
            currentCPJSets.remove(B);

            if(C.getSerialNo() == query.sumOfPredicatesSerials())//we reached a complete cover
                resultCPJs.addAll(C.getCPJs());
            else
                currentCPJSets.add(C);
        }

        //		reNo = countRewritings(resultCPJs);
        return collectRewritings(resultCPJs,Util.castQueryAsISISourceQuery(query));
    }


    private Integer countRewritings(Set<CompositePredicateJoin> resultCPJs) {
        int i =0;
        for(CompositePredicateJoin rc: resultCPJs)
            i += rc.getRewritings().size();
        return i;
    }

    private boolean AssertAllHaveSerialOne(List<CompositePredicateJoin> AltCpjs) {

        for(CompositePredicateJoin cpj: AltCpjs)
        {
            assert(cpj.getPjs().size() == 1);
            assert(cpj.getPjs().iterator().next().getQueryCPJ().getSerialNumber() == 1);
        }
        return true;
    }

    private List<CompRewriting> collectRewritings(Set<CompositePredicateJoin> resultCPJs, SourceQuery inputQuery) {
        List<CompRewriting> res = new ArrayList<CompRewriting>();
        reNo =0;

        for(CompositePredicateJoin rc: resultCPJs)
        {
            for(CompRewriting cr: rc.getRewritings())
            {
                reNo++;
                long st1 = System.currentTimeMillis();
                CompRewriting nc = renameRewrAndReturnIt(cr);
                postProcTime += System.currentTimeMillis() - st1;
                long st = System.currentTimeMillis();
                dontCountTime += System.currentTimeMillis() - st;
                res.add(nc);
                //				renameRewritings(cr);
            }
        }
        System.out.println("Post Proc Time:"+postProcTime);
        System.out.println("Enforce Equates and Merges Time:"+enforceEquatesandMergesTime);

        System.out.println("Enforce Equates Time:"+enforceEquatesTime);
        System.out.println("Enforce Merges Time:"+enforceMergesTime);
        System.out.println("Cloning Time:"+cloningTime);

        System.out.println("Rest of Post Proc Time:"+restofPostProcTime);
        System.out.println("Time for sub symbols:"+subSymbolsTime);
        System.out.println("Time for sub dontcares:"+subDontCaresTime);

        System.out.println("Times Atomicrewriting were cloned "+timescloneddummycalled);
        return res;
    }



    private boolean assertContainedInQuery(CompRewriting nc,
                                           SourceQuery inputQuery) {

        //		System.out.println("rewr: "+nc);
        String exp = nc.getExpansion();
        //		System.out.println(exp);
        SourceQuery expansion = Util.castQueryAsISISourceQuery(query);
        if(expansion.isContained(inputQuery))
            return true;
        else{

            //			expansion.

            throw new RuntimeException("rewriting "+nc+" not contained in the query \n"+" Rewriting expansion:"+expansion+"\n Query: "+inputQuery+" \n");

        }
        //		return false;
    }


    private CompRewriting renameRewrAndReturnIt(CompRewriting newcr) {


        long st1 = System.currentTimeMillis();
        newcr = enforceMergesAndEquates(newcr);
        enforceEquatesandMergesTime  += System.currentTimeMillis() - st1;
        ////
        //PoolOfNames pl = new PoolOfNames();
        //
        long st2 = System.currentTimeMillis();
        int count =0;

//		for(AtomicRewriting ar: newcr.getAtomicRewritings())
//		{
//			for(SourceHead sh: ar.getSourceHeads())
//				for(String var: sh.getSourceHeadVars())
////					/*if(var.startsWith("__")||*/if(  var.contains("AT"))
////					{
////						long st3 = System.currentTimeMillis();
////						substituteVar1byVar2TEMP(newcr,var,PoolOfNames.getName2(var));
////						subSymbolsTime   += ((long)(System.currentTimeMillis() - st3));
////					}else 
//					if(var.startsWith("DC"))
//					{
//						long st4 = System.currentTimeMillis();
//						count = substituteDontCares(newcr,var,count);
//						subDontCaresTime   += ((long)(System.currentTimeMillis() - st4));
//					}
//		}

        restofPostProcTime   += System.currentTimeMillis() - st2;
        //
        return newcr;
    }

//	private String renameRewritings(CompRewriting newcr) {
//
//		//		CompRewriting newcr;
//		//		
//		//		try {
//		//			newcr = cr.cloneDummy();
//		//		} catch (CloneNotSupportedException e) {
//		//			throw new RuntimeException(e);
//		//		}
//		enforceMergesAndEquates(newcr);
//		////		
//		PoolOfNames pl = new PoolOfNames();
//		//		
//		for(AtomicRewriting ar: newcr.getAtomicRewritings())
//		{
//			for(SourceHead sh: ar.getSourceHeads())
//				for(String var: sh.getSourceHeadVars())
//					if(var.startsWith("DC"))
//						substituteVar1byVar2TEMP(newcr,var,pl.getNameTemp(var));
//		}
//
//		//		
//		return newcr+"";
//	}

    private CompRewriting enforceMergesAndEquates(CompRewriting cr) {


        //System.out.println("----->In enforce merges:");
        //System.out.println("Rewriting: \n\t"+cr.toString());
        //System.out.println("merges:"+cr.getMerges());
        long st1 = System.currentTimeMillis();
        for(Set<AtomicRewriting> s: cr.getMerges() )
        {
            //System.out.println("Calling to enforce merge: ");
            //System.out.println(s);
            mergeSetViewsInRw(s,cr);
            //System.out.println("Affter merge:");
            //System.out.println(cr.toString());
        }

        enforceMergesTime   += System.currentTimeMillis() - st1;

        CompRewriting newr = cr;//null;

        long st3 = System.currentTimeMillis();
        try {
            newr = cr.cloneDummy();
            timescloneddummycalled += cr.getAtomicRewritings().size();
        } catch (CloneNotSupportedException e1) {
            throw new RuntimeException(e1);
        }
        cloningTime    += System.currentTimeMillis() - st3;
//		boolean thisIsANewRewriting = true;

        long st2 = System.currentTimeMillis();

        for(Entry<String,Set<String>> e: newr.getEquates().entrySet())
        {
            Set<String> vars = e.getValue();
            String var2 = e.getKey();

            substituteVarsbyOtherVar(newr, vars, var2);//, thisIsANewRewriting);
//			thisIsANewRewriting = false;
        }
        enforceEquatesTime   += System.currentTimeMillis() - st2;

        //		System.out.println("Affter merges and equates:");
        //		System.out.println(newr.toString());

        return newr;
    }

    private void mergeSetViewsInRw(Set<AtomicRewriting> s, CompRewriting cr) {


        AtomicRewriting at1 = s.iterator().next();
        assert(at1.getSourceHeads().size() == 1);
        SourceHead s2 = at1.getSourceHeads().iterator().next();

        SourceHead newsh = new SourceHead(s2.getSourceName());
        try {
            newsh.setQuery(s2.getQuery().clone());
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        boolean uninitialized = true;
        for(AtomicRewriting jv: s)
        {
            //			long st = System.currentTimeMillis();
            //			assert(jv.getRewritings().size() ==1);
            //			dontCountTime += ((long)(System.currentTimeMillis() - st));
            //
            //			System.out.println("Rewriting: "+cr);
            //			System.out.println("Looking to find At: "+jv+ " in cr");
            //			for(AtomicRewriting attemp:cr.getAtomicRewritings())
            //			{
            //				System.out.print("jv == atINCr? jv:"+jv+" cr:"+cr+((jv == attemp)?"yes":"no")+"\n");
            //			}
            AtomicRewriting at = (cr.getAtomicRewritings().contains(jv)?jv:null);


            if(at == null) //if this is null it must be because we already enforced the merges for this compRewriting so we went to the next statement and
            //removed the atomic rewriting involved in the merge
            {

                return;
                //				}
            }

            at = cr.removeAtomic(jv);

            long st = System.currentTimeMillis();
            assert(at.getSourceHeads().size() == 1);
            dontCountTime += System.currentTimeMillis() - st;

            SourceHead s1 = at.getSourceHeads().iterator().next();
            //			if(s1.getSourceHeadVars().size() != 10)
            //				System.out.println(s1);

            for(int i =0; i<s1.getSourceHeadVars().size(); i++)
            {
                if(uninitialized)//need to put something there
                {
                    //					System.out.println("I'm 1 and putting "+s1.getSourceHeadVars().get(i)+" in the "+i+" place");

                    //					if(!s1.getSourceHeadVars().get(i).startsWith("__"))
                    newsh.addSourceHeadVar(s1.getSourceHeadVars().get(i));
                    //					else
                    //						newsh.addSourceHeadVar("__");
                }
                else
                {
                    String varS1 = s1.getSourceHeadVars().get(i);
                    String already_in = newsh.getSourceHeadVars().get(i);
                    if(!already_in.startsWith("DC") && !varS1.startsWith("DC"))
                    {
                        //						System.out.println("I'm in 2nd branch and putting an equate between already_in:"+already_in+" and "+varS1+" in the "+i+"th place");
                        cr.addEquate(varS1, already_in, null);
                        //						newsh.setSourceHeadVar(i,varS1);
                    }
                    else if(!varS1.startsWith("DC"))
                    {
                        //						System.out.println("I'm in 3rd branch and putting "+varS1+" in the "+i+" place (it was:"+already_in+")");
                        st = System.currentTimeMillis();
                        assert(already_in.startsWith("DC"));
                        dontCountTime += System.currentTimeMillis() - st;
                        newsh.setSourceHeadVar(i, varS1);
                        //						substituteVar1byVar2(cr, already_in, varS1);
                        //						cr.addEquate(varS1, newsh.getSourceHeadVars().get(i), null);
                    }
                    else if(already_in.startsWith("DC"))
                    {
                        //						System.out.println("I'm in 4th branch and putting __ in the "+i+" place (it was:"+already_in+")");
                        newsh.setSourceHeadVar(i,"DC");
                    }
                }

                //				else if(!(varS2.startsWith("__")))
                //				{
                //					newsh.addSourceHeadVar(varS2);
                //					substituteVar1byVar2(compRew, s2.getSourceHeadVars().get(i), varS2);
                //				}
                //				else
                //					newsh.addSourceHeadVar(varS1);//arbitrarily chosen, is a don't care and we don't care ;)
            }
            uninitialized = false;
        }
        //		System.out.println("\t--->resutl"+ newsh);
        //		if(newsh.getSourceHeadVars().size() != 10)
        //			System.out.println(newsh);
        //		assert(newsh.getSourceHeadVars().size() == 10);

        AtomicRewriting at3 = new AtomicRewriting();
        at3.addSourceHead(newsh);
        //		System.out.println("Attttttttttttttttttttttttttt3"+at3);
        //		System.out.println("Before "+compRew);
        cr.add(at3);
        //		System.out.println("After "+compRew);

        //			List<AtomicRewriting> dummylist = new ArrayList<AtomicRewriting>();
        //			dummylist.add(at3);
        //			jv1.setRewritings(dummylist);
        //			List<AtomicRewriting> dummylist1 = new ArrayList<AtomicRewriting>();
        //			dummylist1.add(at3);
        //			jv2.setRewritings(dummylist1);
        //		System.out.println("--------------------------"+cr);
    }

    //
    private void substituteVarsbyOtherVarWithCloning(CompRewriting cr, Set<String> vars,
                                                     String var2, boolean thisIsANewRewriting) {

        ListIterator<AtomicRewriting> list_it = cr.getAtomicRewritings().listIterator();

        //for all atomic rewritings cr in this rewriting
        while(list_it.hasNext())
        {
            AtomicRewriting at =  list_it.next();

            if(thisIsANewRewriting)
                at.clonedForVariableRenamingDueToEquations = false;
            long st = System.currentTimeMillis();
            assert(at.getSourceHeads().size() == 1);
            assert(vars instanceof HashSet);
            dontCountTime += System.currentTimeMillis() - st;

            ListIterator<String> variablesinAtomicRewr = at.getSourceHeads().iterator().next().getSourceHeadVars().listIterator();
            List<String> newsourceheadvars = new ArrayList<String>();

            boolean needToclone = false;
            //get all variables in cr
            while(variablesinAtomicRewr.hasNext())
            {
                String nextvar = variablesinAtomicRewr.next();
                if(vars.contains(nextvar))
                {
                    needToclone = true;
                    newsourceheadvars.add(var2);
                    //variablesinAtomicRewr.set(var2);
                }
                else
                    newsourceheadvars.add(nextvar);
            }

            if(needToclone)
            {
                if(!at.clonedForVariableRenamingDueToEquations)
                {
                    timescloneddummycalled ++;
                    at = at.cloneDummyAndSetSourceHeadVars(newsourceheadvars);
                    at.clonedForVariableRenamingDueToEquations = true;
                }
                else
                {
                    at.getSourceHeads().iterator().next().setSourceHeadVars(newsourceheadvars);
                }

                list_it.set(at);
            }

        }


    }

    private void substituteVarsbyOtherVar(CompRewriting cr, Set<String> vars,
                                          String var2) {

        for(AtomicRewriting at:cr.getAtomicRewritings())
        {
            long st = System.currentTimeMillis();
            assert(at.getSourceHeads().size() == 1);
            dontCountTime += System.currentTimeMillis() - st;

            SourceHead s1 = at.getSourceHeads().iterator().next();
            ListIterator<String> l_it = s1.getSourceHeadVars().listIterator();
            while(l_it.hasNext())
            {
                if(vars.contains(l_it.next()))
                {
                    l_it.set(var2);
                }
            }
        }
    }

    /**
     *
     * @param setA
     * @param setB
     * @return a set of CPJs combinations of setA, setB
     */
    private CPJCoverSet combineSets(CPJCoverSet setA, CPJCoverSet setB) {

        CPJCoverSet r = new CPJCoverSet();
        r.setSerialNo(setA.getSerialNo()+setB.getSerialNo());

        for(CompositePredicateJoin sa:setA.getCPJs())
            for(CompositePredicateJoin sb:setB.getCPJs())
            {
                CompositePredicateJoin c = combineCPJs(sa,sb);
                if(c != null)//null means uncombinable
                    r.add(c);
            }
        return r;
    }

    /**
     *
     * @param a
     * @param b
     * @return a CPJ, combination of a,b or null if a,b uncombinable
     */
    private CompositePredicateJoin combineCPJs(CompositePredicateJoin a, CompositePredicateJoin b) {

        try {
            a = a.cloneShallow();
            b = b.cloneShallow();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        CompositePredicateJoin c = new CompositePredicateJoin();
        exmerges = new HashSet<HashSet<AtomicRewriting>>();

        //		JoinsForSPJs spj_joins = new JoinsForSPJs();

        for(Join j: lookupJoins(a,b))
        {
            //TODO change names in the Join object, node1, getA, getB etc.
            //also change name to variables here, e.g., p1, jv1 etc.
            Pair<SourcePredicateJoin,Integer> spjOfAandEdge = j.node1;
            GQRNode sourceVarNode1 = spjOfAandEdge.getA().getGqrNodes().get(spjOfAandEdge.getB());
            int edgeNo1 = spjOfAandEdge.getB();

            //join also returns which pairs spjs (of a and b) are joined only distinguished
            //			spj_joins = j.getDistinguishedJoinedSPJs(a,b);

            //if all pairs of joins can be minimized p.canMinimize will be true

            Pair<SourcePredicateJoin,Integer> spjOfBandEdge = j.node2;
            GQRNode sourceVarNode2 = spjOfBandEdge.getA().getGqrNodes().get(spjOfBandEdge.getB());
            int edgeNo2 = spjOfBandEdge.getB();

            if((sourceVarNode1.isExistential()!=sourceVarNode2.isExistential()) || (sourceVarNode1.isExistential() &&(j.jt == joinTypeInQuery.D)))
                return null;
            else if (sourceVarNode1.isExistential())
            {
                //		for all sources S in va's infobox do
                //		if S contains a join description for v2 then

                List<Boolean> sources_of_jv2_found_in_jv1 = new ArrayList<Boolean>(sourceVarNode2.getInfobox().getJoinInViews().size());
                int index_of_jv2_sources = 0;

                for(JoinInView jv1: sourceVarNode1.getInfobox().getJoinInViews())
                {
                    boolean same_view_found_in_jv2 = false;//after this for is over if this is false I'm dropping jv1

                    index_of_jv2_sources = 0;

                    for(JoinInView jv2: sourceVarNode2.getInfobox().getJoinInViews())
                    {
                        if(jv1.getSourceName().equals(jv2.getSourceName()))//we're talking about the smae view
                        {
                            sources_of_jv2_found_in_jv1.add(index_of_jv2_sources++,true); //I'm keeping a list with all jvs in infobox2
                            //if jv2 found exists also in infobox1 I set its pointer true;

                            same_view_found_in_jv2 = true; //I don't have to drop jv1 for sure since I found it in infobox2
                            // I still might drop it if next flag doesn't go true;
                            boolean once_found_in_jv2_preserves_join = false;

                            for(JoinDescription jdOfSourceNode1: jv1.getJoinDescriptions())
                            {
                                if(jdOfSourceNode1.equals(new JoinDescription(j.node2.getA().getPredicate(), edgeNo2)))
                                {
                                    long st = System. currentTimeMillis();
                                    //									if(jdOfSourceNode1.getPredicate() != j.node2.getA().getPredicate())
                                    //									{
                                    //										System.out.println("se ");
                                    //										System.out.println("nai");
                                    //										System.out.println("poly");
                                    //									}

                                    //TODO examine why these 2 assertions are breaking
                                    //assert(jdOfSourceNode1.getPredicate() == j.node2.getA().getPredicate());
                                    //assert(jv2.getJoinDescriptions().contains(new JoinDescritpion(j.node1.getA().getPredicate(), edgeNo1)));
                                    assert(jv1.getRewritings().size() == 1);
                                    assert(jv2.getRewritings().size() == 1);
                                    dontCountTime += System.currentTimeMillis() - st;
                                    addMerge(jv1.getRewritings().iterator().next(),jv2.getRewritings().iterator().next());

                                    //since the rewriting for jv1 is going to be merged with some other rewritings
                                    // (this means there are some views/jvs that "preserve" (existentially) the joins of jv1)
                                    // then the rewriting for jv1 should NOT be cross-producted with all the rest rewritings apart from those merged
                                    // "existential merges" records the merges done for exactly this reason.
                                    addExistentialMerge(jv2.getRewritings().iterator().next(),jv1.getRewritings().iterator().next());
                                    //TODO addMerge and addExistentialMerge can be optimized and merged to one function
                                    //									System.out.println(jv1);
                                    once_found_in_jv2_preserves_join = true;
                                    break;
                                }
                            }
                            //							if(found_in_jv2)
                            //								break;

                            //if not found in jv2 DROP
                            //		if va infobox is empy  then
                            //		return null;
                            if(!once_found_in_jv2_preserves_join)
                            {
                                if(a.emptyJoinInView(jv1)|| b.emptyJoinInView(jv2))//TODO check this!! This dropping the view from the pj across all coversets it belongs not just this one! is this what we want?
                                    return null; //null means at least one sourcepredicatejoin in a or b is left with no rewritings
                                //therefore they are uncombinable
                            }

                        }


                        long st = System.currentTimeMillis();
                        assert(jv1.getRewritings().size() == 1);
                        //						System.out.println(a.getRewritings()+"\n\n"+jv1.getRewritings());
                        assert(containsExactlyOncePerCompRew(a.getRewritings(),jv1.getRewritings().iterator().next()));
                        assert(jv2.getRewritings().size() == 1);
                        //						System.out.println(b.getRewritings()+"\n\n"+jv2.getRewritings());
                        assert(containsExactlyOncePerCompRew(b.getRewritings(),jv2.getRewritings().iterator().next()));
                        dontCountTime += System.currentTimeMillis() - st;
                    }

                    if(!same_view_found_in_jv2)
                        if(a.emptyJoinInView(jv1))
                            return null;
                }

                for(int i =0; i<sources_of_jv2_found_in_jv1.size(); i++)//for all sources of jv2
                    if(sources_of_jv2_found_in_jv1.get(i)!=true)//if the source is not common with jv1
                        if(b.emptyJoinInView(sourceVarNode2.getInfobox().getJoinInViews().get(i)))
                            return null;
            }
            else
            {
                long st = System.currentTimeMillis();
                assert(!sourceVarNode2.isExistential());
                dontCountTime += System.currentTimeMillis() - st;
                //		repeat
                //		for all pairs, get a pair sources (s1; s2) from infobox of va
                //		and vb respectively
                //		if s1 = s2 and we can "simplify" (-->WITNESS PROBLEM)
                //		addMerge(s1; va; vb)
                //		else
                //		addEquate(s1; va; s2; vb)
                //		until until all possible pairs of sources are chosen

                for(JoinInView jv1: sourceVarNode1.getInfobox().getJoinInViews())
                {
                    for(JoinInView jv2: sourceVarNode2.getInfobox().getJoinInViews())
                    {
                        //HERE UNCOMMENT AND SOLVE THE WITNESS PROBLEM
                        //						if(jv1.getSourceName().equals(jv2.getSourceName()))
                        //						{
                        //							boolean found_in_jv2 = false;
                        //							//							for(JoinDescritpion jd2 :jv2.getJoinDescriptions())
                        //							//							{
                        //							for(JoinDescritpion jdOfSourceNode1: jv1.getJoinDescriptions())
                        //							{
                        //								if(jdOfSourceNode1.equals(new JoinDescritpion(j.node2.getA().getPredicate(), edgeNo2)))
                        //								{
                        //									assert(jdOfSourceNode1.getPredicate().getRepeatedId() == j.node2.getA().getPredicate().getRepeatedId());
                        //
                        //									//									if(!(jd1.getPredicate() == j.node2.getA().getPredicate()))
                        //									//									{
                        //									//										System.out.println(true);
                        //									//									}
                        //									//									assert(jd1.getPredicate() == j.node2.getA().getPredicate());
                        //									assert(jv2.getJoinDescriptions().contains(new JoinDescritpion(j.node1.getA().getPredicate(), edgeNo1)));
                        //									//									assert(n1.getQueryVar() == null || n1.getQueryVar().equals(n2.getQueryVar()));
                        ////									addMerge(jv1, jv2);
                        //
                        //									addEquate(jv1, jv1.getHeadPosition(), jv2, jv2.getHeadPosition(),sourceVarNode1.getQueryVar());
                        //
                        //
                        //									found_in_jv2 = true;
                        //									break;
                        //								}
                        //							}
                        //
                        //							assert(jv1.getRewritings().size() == 1);
                        //							//							System.out.println(a.getRewritings()+"\n\n"+jv1.getRewritings());
                        //
                        //							assert(containsExactlyOncePerCompRew(a.getRewritings(),jv1.getRewritings().iterator().next()));
                        //							assert(jv2.getRewritings().size() == 1);
                        //							//							System.out.println(b.getRewritings()+"\n\n"+jv2.getRewritings());
                        //							assert(containsExactlyOncePerCompRew(b.getRewritings(),jv2.getRewritings().iterator().next()));
                        //
                        //
                        //							if(!found_in_jv2)
                        //							{
                        //								assert(sourceVarNode1.getQueryVar() == null || sourceVarNode1.getQueryVar().equals(sourceVarNode2.getQueryVar()));
                        //
                        //								addEquate(jv1, jv1.getHeadPosition(), jv2, jv2.getHeadPosition(),sourceVarNode1.getQueryVar());
                        //							}
                        //						}
                        //						else
                        //						{
                        //if we CANNOT MERGE p.setCanMinimize(false);
                        st = System.currentTimeMillis();
                        assert(jv1.getRewritings().size() == 1);
                        //							System.out.println(a.getRewritings()+"\n\n"+jv1.getRewritings());
                        assert(containsExactlyOncePerCompRew(a.getRewritings(),jv1.getRewritings().iterator().next()));
                        assert(jv2.getRewritings().size() == 1);
                        //							System.out.println(b.getRewritings()+"\n\n"+jv2.getRewritings());
                        assert(containsExactlyOncePerCompRew(b.getRewritings(),jv2.getRewritings().iterator().next()));
                        assert(sourceVarNode1.getQueryVar() == null || sourceVarNode1.getQueryVar().equals(sourceVarNode2.getQueryVar()));
                        dontCountTime += System.currentTimeMillis() - st;

                        addEquate(jv1, jv1.getHeadPosition(), jv2, jv2.getHeadPosition(),sourceVarNode1.getQueryVar());
                        //						}
                    }
                }
            }
        }

        //if p satisfies Def 4.2
        //		if(p.canMinimize())
        //		//then potential merges can be added
        //		for(Pair<AtomicRewriting,AtomicRewriting> pair_merges: p.potentialMerges())
        //			addMerge(pair_merges.getA(),pair_merges.getA());

        c.add(a);
        c.add(b);

        List<CompRewriting> compRew = crossProductRewritings(a,b);

        c.setRewritings(appendMergesAndEquates(compRew));

        return c;
    }


    private boolean containsExactlyOncePerCompRew(List<CompRewriting> rewritings,
                                                  AtomicRewriting atr) {
        for(CompRewriting cr : rewritings)
        {
            //			System.out.println(cr+"\n");
            int i = cr.getAtomicRewritings().indexOf(atr);
            if(i == -1)
                return true;
            //			System.out.println(atr);
            int j = cr.getAtomicRewritings().lastIndexOf(atr);
            if(j != i)
                return false;
        }
        return true;
    }

    private List<CompRewriting> appendMergesAndEquates(List<CompRewriting> compRew) {

        //		System.out.println("List of Rewritings: "+compRew+" ");


        for(Pair<String,Pair<Pair<Integer,JoinInView>,Pair<Integer,JoinInView>>> pwithq: equates)
        {
            String queryVar = pwithq.getA();
            Pair<Pair<Integer,JoinInView>,Pair<Integer,JoinInView>> p = pwithq.getB();
            Pair<Integer,JoinInView> p1 = p.getA();
            Pair<Integer,JoinInView> p2 = p.getB();

            for(CompRewriting cr: compRew)
            {
                //				System.out.println("Equates from memory transferred into rewritings: Jv1 "+p1.getB()+" -- arg1 "+p1.getA()+",\n\t Jv2 "+p2.getB()+" -- arg2 "+p2.getA());
                //				System.out.println("Rewriting: "+cr);
                addEquateViewsInRw(p1.getB(),p1.getA(),p2.getB(),p2.getA(),cr, queryVar);
            }
        }

        equates = new ArrayList<Pair<String, Pair<Pair<Integer, JoinInView>, Pair<Integer, JoinInView>>>>();

        //Imagine the set of cr.getMerges() contains {at1,at2} and this.merges contains two
        //pairs {at1,at3} and {at3,at4}.. Then all merges should go into the same set in cr
        //however if {at3,at4} goes in the loop first this will end up in an independent set of cr.getMerges
        //then
        //hence we need to "merge" the merges in this.Merge before adding them to the cr

        //TODO I keep pairs in this.merges If I had sets it would be better
        //mergeMergesNotConnectedThroughRewriting(merges);
        for(Pair<AtomicRewriting,AtomicRewriting> p: merges )
        {

            AtomicRewriting at1 = p.getA();
            AtomicRewriting at2 = p.getB();

            for(CompRewriting cr:  compRew)
            {
                //System.out.println("Merges from memory transferred into rewritings: "+at1+"--- "+at2+" cr:"+cr);
                addMergeViewsInRw(at1,at2,cr);
            }
        }
        merges = new ArrayList<Pair<AtomicRewriting,AtomicRewriting>>();



        return compRew;
    }


    //	private void equateViewsInRw(JoinInView jv1, Integer arg1, JoinInView jv2,
    //			Integer arg2, CompRewriting compRew) {
    //
    //		//		System.out.println("equate\t"+ jv1 +" on "+arg1+" with"+ jv2 +" on "+arg2);
    //		assert(jv1.getRewritings().size() ==1);
    //		assert(jv2.getRewritings().size() ==1);
    //
    //		//		AtomicRewriting at1 = jv1.getRewritings().iterator().next();
    //		//		AtomicRewriting at2 = jv2.getRewritings().iterator().next();
    //
    //
    //		//		int i1 = compRew.getAtomic(jv1.getRewritings().iterator().next());
    //		//		int i2 = compRew.getAtomic(jv2.getRewritings().iterator().next());
    //		//
    //		//		if((i1 == -1) || i2 == -1)
    //		//			return;
    //
    //		////TEMP-----
    //		AtomicRewriting at1 = (compRew.getAtomicRewritings().contains(jv1.getRewritings().iterator().next())?jv1.getRewritings().iterator().next():null);
    //		AtomicRewriting at2 = (compRew.getAtomicRewritings().contains(jv2.getRewritings().iterator().next())?jv2.getRewritings().iterator().next():null);
    //		////-----------
    //
    //		if((at1 == null) || at2 == null)
    //			return;
    //
    //
    //		assert(at1.getSourceHeads().size() == 1);
    //		assert(at2.getSourceHeads().size() == 1);
    //
    //		SourceHead s1 = at1.getSourceHeads().iterator().next();
    //		SourceHead s2 = at2.getSourceHeads().iterator().next();
    //		//		//TEMP----
    //		//		compRew.addEquate(s1,s1.getSourceHeadVars().get(arg1),s2,s2.getSourceHeadVars().get(arg2));
    //		//		//////-----
    //		////		System.out.println("I'm going to equate\n\t"+ s1 +" on "+arg1+"\n\t"+ s2+"\n\t on "+arg2);
    //		//
    //		substituteVar1byVar2(compRew, s1.getSourceHeadVars().get(arg1), s2.getSourceHeadVars().get(arg2));
    //		////		System.out.println("\n\t Result: "+compRew+"\n");
    //	}

    private void addEquateViewsInRw(JoinInView jv1, Integer arg1, JoinInView jv2,
                                    Integer arg2, CompRewriting compRew, String queryVar) {

        //		System.out.println("equate\t"+ jv1 +" on "+arg1+" with"+ jv2 +" on "+arg2);
        long st = System.currentTimeMillis();
        assert(jv1.getRewritings().size() ==1);
        assert(jv2.getRewritings().size() ==1);
        dontCountTime += System.currentTimeMillis() - st;

        AtomicRewriting at1 = (compRew.getAtomicRewritings().contains(jv1.getRewritings().iterator().next())?jv1.getRewritings().iterator().next():null);
        AtomicRewriting at2 = (compRew.getAtomicRewritings().contains(jv2.getRewritings().iterator().next())?jv2.getRewritings().iterator().next():null);

        if((at1 == null) || at2 == null)
        {
            //			System.out.println("returned");
            return;
        }

        st = System.currentTimeMillis();
        assert(at1.getSourceHeads().size() == 1);
        assert(at2.getSourceHeads().size() == 1);
        dontCountTime += System.currentTimeMillis() - st;

        SourceHead s1 = at1.getSourceHeads().iterator().next();
        SourceHead s2 = at2.getSourceHeads().iterator().next();

        //System.out.println("Equating "+at1+" with \n\t"+at2+" on "+s1.getSourceHeadVars().get(arg1)+","+s2.getSourceHeadVars().get(arg2));
        compRew.addEquate(s1.getSourceHeadVars().get(arg1),s2.getSourceHeadVars().get(arg2),queryVar);

    }

    //	private void mergeViewsInRw(JoinInView jv1, JoinInView jv2,
    //			CompRewriting compRew) {
    ////		System.out.println("Initially "+compRew);
    //		assert(jv1.getRewritings().size() ==1);
    //		assert(jv2.getRewritings().size() ==1);
    //
    //		AtomicRewriting at1 = (compRew.getAtomicRewritings().contains(jv1.getRewritings().iterator().next())?jv1.getRewritings().iterator().next():null);
    //		AtomicRewriting at2 = (compRew.getAtomicRewritings().contains(jv2.getRewritings().iterator().next())?jv2.getRewritings().iterator().next():null);
    //
    //
    //		if((at1 == null) || at2 == null)
    //			return;
    ////		System.out.println("Initially "+compRew);
    //
    ////		System.out.println("At some point "+compRew);
    //
    //		at1 = compRew.removeAtomic(jv1.getRewritings().iterator().next());
    //////		System.out.println("here "+compRew);
    ////
    //		at2 = compRew.removeAtomic(jv2.getRewritings().iterator().next());
    //
    ////		System.out.println("there "+compRew);
    //
    //
    ////		System.out.println(at1);
    ////		System.out.println(at2);
    //
    //		assert(at1.getSourceHeads().size() == 1);
    //		assert(at2.getSourceHeads().size() == 1);
    //
    //		SourceHead s1 = at1.getSourceHeads().iterator().next();
    //		SourceHead s2 = at2.getSourceHeads().iterator().next();
    ////		System.out.println("I'm going to merge\n\t"+ s1 +"\n\t"+ s2);
    //
    //		assert(s1.getSourceName().equals(s2.getSourceName()));
    //
    //		SourceHead newsh = new SourceHead(s1.getSourceName());
    //		for(int i =0; i<s1.getSourceHeadVars().size(); i++)
    //		{
    //			String varS1 = s1.getSourceHeadVars().get(i);
    //			String varS2 = s2.getSourceHeadVars().get(i);
    //
    //			if(!(varS1.startsWith("__")))
    //			{
    //				newsh.addSourceHeadVar(varS1);
    //				substituteVar1byVar2(compRew, s2.getSourceHeadVars().get(i), varS1);
    //			}
    //			else if(!(varS2.startsWith("__")))
    //			{
    //				newsh.addSourceHeadVar(varS2);
    //				substituteVar1byVar2(compRew, s2.getSourceHeadVars().get(i), varS2);
    //			}
    //			else
    //				newsh.addSourceHeadVar(varS1);//arbitrarily chosen, is a don't care and we don't care ;)
    //		}
    //
    ////		System.out.println("\t--->resutl"+ newsh);
    //
    //		AtomicRewriting at3 = new AtomicRewriting();
    //		at3.addSourceHead(newsh);
    ////		System.out.println("Attttttttttttttttttttttttttt3"+at3);
    ////		System.out.println("Before "+compRew);
    //		compRew.add(at3);
    ////		System.out.println("After "+compRew);
    //
    //		List<AtomicRewriting> dummylist = new ArrayList<AtomicRewriting>();
    //		dummylist.add(at3);
    //		jv1.setRewritings(dummylist);
    //		List<AtomicRewriting> dummylist1 = new ArrayList<AtomicRewriting>();
    //		dummylist1.add(at3);
    //		jv2.setRewritings(dummylist1);
    //
    //	}

    private void addMergeViewsInRw(AtomicRewriting jv1, AtomicRewriting jv2,
                                   CompRewriting compRew) {
        //		System.out.println("Initially "+compRew);

        AtomicRewriting at1 = (compRew.getAtomicRewritings().contains(jv1)?jv1:null);
        AtomicRewriting at2 = (compRew.getAtomicRewritings().contains(jv2)?jv2:null);

        if((at1 == null) || at2 == null)
            return;

        long st = System.currentTimeMillis();
        assert(at1.getSourceHeads().size() == 1);
        assert(at2.getSourceHeads().size() == 1);

        SourceHead s1 = at1.getSourceHeads().iterator().next();
        SourceHead s2 = at2.getSourceHeads().iterator().next();
        //		System.out.println("I'm going to merge\n\t"+ s1 +"\n\t"+ s2);

        st = System.currentTimeMillis();
        assert(s1.getSourceName().equals(s2.getSourceName()));
        dontCountTime += System.currentTimeMillis() - st;
        compRew.addMerge(jv1,jv2);
    }


    private void substituteVar1byVar2TEMP(CompRewriting compRew, String var1,
                                          String var2) {
        for(AtomicRewriting at:compRew.getAtomicRewritings())
        {
            long st = System.currentTimeMillis();
            assert(at.getSourceHeads().size() == 1);
            dontCountTime += System.currentTimeMillis() - st;

            SourceHead s1 = at.getSourceHeads().iterator().next();
            ListIterator<String> l_it = s1.getSourceHeadVars().listIterator();
            while(l_it.hasNext())
            {
                if(l_it.next().equals(var1))
                {
                    l_it.set(var2);
                }
            }
        }
    }

    private int substituteDontCares(CompRewriting compRew, String var1,
                                    int var2) {
        for(AtomicRewriting at:compRew.getAtomicRewritings())
        {
            long st = System.currentTimeMillis();
            assert(at.getSourceHeads().size() == 1);
            dontCountTime += System.currentTimeMillis() - st;

            SourceHead s1 = at.getSourceHeads().iterator().next();
            ListIterator<String> l_it = s1.getSourceHeadVars().listIterator();
            while(l_it.hasNext())
            {
                if(l_it.next().equals(var1))
                {
                    l_it.set("C"+var2++);
                }
            }
        }
        return var2;
    }

    //	private void substituteVar1byVar2(CompRewriting compRew, String var1,
    //			String var2) {
    //		for(AtomicRewriting at:compRew.getAtomicRewritings())
    //		{
    //			assert(at.getSourceHeads().size() == 1);
    //			SourceHead s1 = at.getSourceHeads().iterator().next();
    //			ListIterator<String> l_it = s1.getSourceHeadVars().listIterator();
    //			while(l_it.hasNext())
    //			{
    //				if(l_it.next().equals(var1))
    //				{
    //					l_it.set(var2);
    //				}
    //			}
    //		}

    //		ListIterator<Pair<String,String>> l_it = compRew.getEquates().listIterator();
    //		while(l_it.hasNext())
    //		{
    //			Pair<String,String> p = l_it.next();
    //
    //			if(p.getA().equals(var1))
    //			{
    //				if(p.getB().equals(var1))
    //				{
    //					l_it.set(new Pair<String,String>(var2,var2));
    //				}
    //				else
    //					l_it.set(new Pair<String,String>(var2,p.getB()));
    //			}else if(p.getB().equals(var1))
    //			{
    //				l_it.set(new Pair<String,String>(p.getA(),var2));
    //			}
    //		}
    //
    //
    //	}


    private List<CompRewriting> crossProductRewritings(CompositePredicateJoin a,
                                                       CompositePredicateJoin b) {
        List <CompRewriting> newR = new ArrayList<CompRewriting>();
        for(CompRewriting crA: a.getRewritings())
        {
            for(CompRewriting crB: b.getRewritings())
            {

                boolean existsMergeForCra = false;

                for(HashSet<AtomicRewriting> mergeSet: exmerges)
                {
                    boolean toBeMergedWithCrb = false;

                    for(AtomicRewriting at1 : mergeSet)
                    {
                        //						System.out.println(mergeSet);
                        if(crA.getAtomicRewritings().contains(at1)) //there is an existential merge for this rewriting
                        {
                            existsMergeForCra = true;
                            for(AtomicRewriting atMergedWithat1: mergeSet)
                            {
                                if(crB.getAtomicRewritings().contains(atMergedWithat1))// if crB does not contain any of the jvMergedWithjv1 there's no need to crossproduct it
                                {
                                    toBeMergedWithCrb = true;
                                    //if it does, we do crossproduct it
                                    newR.add(crA.merge(crB));// crB contains an atomic term (i.e., atomic rewriting) which is supposed to be merged with a term from crA
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    if(toBeMergedWithCrb)
                        break;
                }

                if(!existsMergeForCra)
                    newR.add(crA.merge(crB));

            }
        }
        return newR;
    }


    private void addEquate(JoinInView jv1, int arg1, JoinInView jv2,
                           int arg2, String queryVar) {
        //		System.out.println("------------>");
        //		System.out.println("We should equate: "+jv1);
        //		System.out.println("\t ON "+arg1+" WITH the next ON "+arg2);
        //		System.out.println("We should equate: "+jv2);
        //		System.out.println("<-----------------");

        //		if(queryVar != null)
        //		{
        //			equates.get(queryVar)
        //		}

        Pair<Integer,JoinInView> a = new Pair<Integer, JoinInView>(arg1, jv1);
        Pair<Integer,JoinInView> b = new Pair<Integer, JoinInView>(arg2, jv2);
        Pair<Pair<Integer,JoinInView>, Pair<Integer,JoinInView>> p = new Pair<Pair<Integer,JoinInView>, Pair<Integer,JoinInView>>(a, b);
        Pair<String,Pair<Pair<Integer,JoinInView>, Pair<Integer,JoinInView>>> pwithq = new Pair<String,Pair<Pair<Integer,JoinInView>, Pair<Integer,JoinInView>>>(queryVar, p);
        equates.add(pwithq);
    }


    private void addMerge(AtomicRewriting at1, AtomicRewriting at2) {
        Pair<AtomicRewriting, AtomicRewriting> p = new Pair<AtomicRewriting,AtomicRewriting>(at1,at2);
        merges.add(p);
    }

    private void addExistentialMerge(AtomicRewriting at1, AtomicRewriting at2) {

        boolean found = false;
        for(HashSet<AtomicRewriting> setMerges: exmerges)
        {
            if(setMerges.contains(at1) || setMerges.contains(at2))
            {
                found = true;
                setMerges.add(at1);
                setMerges.add(at2);
            }

        }

        if(!found)
        {
            HashSet<AtomicRewriting> s = new HashSet<AtomicRewriting>();
            s.add(at1);
            s.add(at2);
            exmerges.add(s);
        }
        //TODO old code delete this
        //			HashSet<AtomicRewriting> s = exmerges.get(at1);
        //		if(s == null)
        //		{
        //			s = new HashSet<AtomicRewriting>();
        //			s.add(at2);
        //			exmerges.put(at1, s);
        //		}
        //		else{
        //			s.add(at2);
        //		}
    }

    /**
     * Constructs the joins among pairs of pjs. Each member of each pair belongs to exactly to one of
     * the CPJs a or b.
     * Each Join object returned in the list, contains two source pjs (which cover query pjs) and
     * whose join covers a corresponding joined subgoal in the query.
     * The covered query pjs can be joined on an existential or a distinguished query variables; allowing the
     * corresponding variable types in the source pjs.
     * @param a
     * @param b
     * @return
     */
    private List<Join> lookupJoins(CompositePredicateJoin a, CompositePredicateJoin b) {

        //		System.out.println("Combining :"+a.getRewritings());
        //		System.out.println("with :"+b.getRewritings());

        List<Join> joins = new ArrayList<Join>();

        for(SourcePredicateJoin spj1: a.getPjs()) {
            PredicateJoin q1 = spj1.getQueryCPJ();

            for(SourcePredicateJoin spj2: b.getPjs()) {
                PredicateJoin q2 = spj2.getQueryCPJ();

                for(Entry<Integer,GQRNode> node1OfQ: q1.getGqrNodes().entrySet())
                {
                    for(Entry<Integer,GQRNode> node2OfQ: q2.getGqrNodes().entrySet())
                    {
                        //this is a dummy for - only one joininview will be retrieved
                        //describing the query joins of this variable
                        for(JoinInView joinsofnode1ofQ: node1OfQ.getValue().getInfobox().getJoinInViews())
                        {
                            long st=System.currentTimeMillis();
                            assert(node1OfQ.getValue().getInfobox().getJoinInViews().size() == 1);
                            dontCountTime += System.currentTimeMillis() - st;

                            //this is a dummy for - only one joininview will be retrieved
                            //describing the query joins of this variable

                            //TODO check this: have I really not thought about the following ever?
                            //							//or I missed somewhere? WAIT!!! joins are constructed even if infeasible
                            //they'll be dropped later!
                            //							//two source variables can be joined only if the are the same type
                            //							if(spj1.getGqrNodes().get(entry1.getKey()).isExistential()
                            //									!=
                            //										spj2.getGqrNodes().get(entry2.getKey()).isExistential())
                            //							{
                            //								//abort -- these two pjs cannot join on the specific edges
                            //								break;
                            //							}

                            for(JoinInView joinsofnode2ofQ: node2OfQ.getValue().getInfobox().getJoinInViews())
                            {
                                st = System.currentTimeMillis();
                                assert(node2OfQ.getValue().getInfobox().getJoinInViews().size() == 1);

                                assert(joinsofnode1ofQ.getSourceName().equals(joinsofnode2ofQ.getSourceName()));

                                dontCountTime += System.currentTimeMillis() - st;

                                //that is the name of the query

                                //all the joindescriptions that the query variable (covered by a source variable
                                //attached in a source pj in cpj b) participates in.
                                for(JoinDescription jd2 :joinsofnode2ofQ.getJoinDescriptions())
                                {
                                    //all the joindescriptions that the query variable (covered by a source variable
                                    //attached in a source pj in cpj a) participates in.
                                    for(JoinDescription jd1: joinsofnode1ofQ.getJoinDescriptions())
                                    {
                                        //TODO
                                        //maybe my intention here was to call equalsIgnoreRepeatedID I'm not sure -- however
                                        // q2.getPredicate() carries a repeatedID
                                        if(jd1.equalsWithSamePred(new JoinDescription(q2.getPredicate(),node2OfQ.getKey()))
                                                &&
                                                jd2.equalsWithSamePred(new JoinDescription(q1.getPredicate(),node1OfQ.getKey())))//second part of if might be redundant; TODO check this
                                        {
                                            Join j = new Join(new Pair<SourcePredicateJoin,Integer>(spj1,node1OfQ.getKey()),new Pair<SourcePredicateJoin,Integer>(spj2,node2OfQ.getKey()));
                                            j.jt = node1OfQ.getValue().isExistential()?joinTypeInQuery.E:joinTypeInQuery.D;
                                            st=System.currentTimeMillis();
                                            assert(j.jt == (node2OfQ.getValue().isExistential()?joinTypeInQuery.E:joinTypeInQuery.D));
                                            dontCountTime += System.currentTimeMillis() - st;
                                            //j.setPredicate1(q1.getPredicate());
                                            //											//j.setPredicate2(q2.getPredicate());
                                            joins.add(j);

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return joins;
    }


    private Pair<CPJCoverSet,CPJCoverSet> select (Set<CPJCoverSet> currentCPJSets) throws NotEnoughCPJsException{




        Iterator<CPJCoverSet> it = currentCPJSets.iterator();



        //		Iterator<CPJCoverSet> it = currentCPJSets.iterator();

        CPJCoverSet cpjCSet1;
        CPJCoverSet cpjCSet2;

        try{
            cpjCSet1 = it.next();
            //			PredicateJoin qpj1 = cpjCSet1.getCPJs().iterator().next().getPjs().iterator().next().getQueryCPJ();
            //			System.out.print("Combining cover set :"); for(CompositePredicateJoin cpj : cpjCSet1.getCPJs()){ System.out.print(cpj.getPjs()+" -- "); }
            //			System.out.println();
            cpjCSet2 = it.next();
            //			PredicateJoin qpj2 = cpjCSet2.getCPJs().iterator().next().getPjs().iterator().next().getQueryCPJ();
            ////			System.out.println("	with cover set for: "+qpj2.getPredicate()+qpj2.variablePatternStringSequence());
            //			System.out.print("	with cover set :"); for(CompositePredicateJoin cpj : cpjCSet2.getCPJs()){ System.out.print(cpj.getPjs()+" -- "); }
            //			System.out.println();

        }catch (NoSuchElementException e) {
            throw new NotEnoughCPJsException();
        }

        return new Pair<CPJCoverSet,CPJCoverSet>(cpjCSet1,cpjCSet2);
    }

    private CPJCoverSet retrieveSourcePJSet(PredicateJoin pjq) throws NonAnswerableQueryException {

        //indexSourcePJs:
        //map to index all PJs that are keys of the next map, any pj A which maps to a key pj B of the next map
        //will be a kept of this map, and its value will be a list containing B

        //sourcePJs:
        //map to ``globally'' hold all PJs, if repeated predicates exist in the same source, these are contained in the list for the spj

        SourcePredicateJoin askfor = new SourcePredicateJoin(pjq);
        CPJCoverSet cset = new CPJCoverSet();
        List<SourcePredicateJoin> indexes = indexSourcePJs.get(askfor); //take all distinct pjs that askfor maps onto
        if(indexes != null)
        {
            for(SourcePredicateJoin key: indexes)
            {
                List<SourcePredicateJoin> l = sourcePJs.get(key); //take all repeated pjs for key

                if(l != null)
                {
                    l = cloneIfReturnedInPast(key,l); //TODO need to check this! if breaking later do we still clone here? we shouldn't! (optimization)

                    long st = System.currentTimeMillis();
                    assert(assertRepeatedIds(l));
                    dontCountTime += System.currentTimeMillis() - st;
                    //TODO also need to check this for below
                    for(Entry<Integer,GQRNode> queryNodeEntry: pjq.getGqrNodes().entrySet())
                    {
                        Iterator<SourcePredicateJoin> l_it= l.iterator();
                        while(l_it.hasNext())
                        {
                            SourcePredicateJoin s_pj = l_it.next();
                            GQRNode sourceNode = s_pj.getGqrNodes().get(queryNodeEntry.getKey());

                            GQRNode qNode = queryNodeEntry.getValue();

                            if(sourceNode.isExistential())
                            {
                                if(qNode.isExistential())
                                {
                                    //TODO I disabled the optimization below. In order for this to work we need to "deep" clone the sourcePJs.get(key) above
                                    //and I don't know if it worths it -- UPDATE I enable it again as too many exceptions have risen.
                                    //Past this point my original implementation assumed this true

                                    //I'm cloning the infobox so I don't get a ConcurrentModificationException
                                    List<JoinInView> cloneListViews  = new ArrayList<JoinInView>(sourceNode.getInfobox().getJoinInViews());

                                    for(JoinInView jv:cloneListViews)
                                    {
                                        if(!jv.containsQueryDescriptions(qNode.getInfobox().getJoinInViews().iterator().next().getJoinDescriptions()))//should have exactly one joinInView since it's a query
                                            if(s_pj.emptyJoinInView(jv))//drop source(i.e, joininview) from source_pj's infoboxes, and if we dropped all return true
                                            {
                                                l_it.remove();//if infoboxes of s_pj became empty remove s_pj from l
                                            }
                                    }
                                }
                                else
                                {
                                    st = System.currentTimeMillis();
                                    assert(false); //should nver come here
                                    dontCountTime += System.currentTimeMillis() - st;

                                    l_it.remove();//remove s_pj from l
                                }
                            }
                            else if(!qNode.isExistential())
                            {
                                sourceNode.addQueryVar(qNode.getVariable().getName());
                                for(JoinInView jv: sourceNode.getInfobox().getJoinInViews())
                                {
                                    st = System.currentTimeMillis();
                                    assert(jv.getRewritings().size() == 1);
                                    assert(jv.getRewritings().iterator().next().getSourceHeads().size() == 1);
                                    dontCountTime += System.currentTimeMillis() - st;
                                    //									if( jv.getRewritings().iterator().next().getSourceHeads().iterator().next().getSourceHeadVars().get(jv.getHeadPosition()).startsWith("_"))
                                    //									{
                                    //										System.out.println("I'm in "+queryNodeEntry.getKey() +" node of predicate "+s_pj+ " which has var"+sourceNode.getVariable());
                                    //
                                    //										System.out.println("I'm in sourceNode "+sourceNode +" which is "+sourceNode.isExistential());
                                    //										System.out.println("I'm looking joinInView "+jv +" \n \t and getting the rewriting"+jv.getRewritings().iterator().next());
                                    //										System.out.println("The sourcehead is "+jv.getRewritings().iterator().next().getSourceHeads().iterator().next());
                                    //										System.out.println(" \t and I'm looking var number "+jv.getHeadPosition());
                                    //									}
                                    //
                                    //									System.out.println("I'm equating "+jv.getRewritings().iterator().next().getSourceHeads().iterator().next().getSourceHeadVars().get(jv.getHeadPosition())+ " with "+qNode.getVariable().name);
                                    //									System.out.println("head pos "+jv.getHeadPosition());
                                    s_pj.addEquate(qNode.getVariable().getName(), jv.getRewritings().iterator().next().getSourceHeads().iterator().next().getSourceHeadVars().get(jv.getHeadPosition()), qNode.getVariable().getName());
                                }
                            }

                        }
                    }

                    addPJQtoCPJCoverSets(l,pjq); //link the query PJ to these (alternatives) sourcePJs
                    addAllSourcePJsAsCPJs(cset,l,this.query.getHead().toString());
                }
            }
        }

        if(cset.isEmpty())
            throw new NonAnswerableQueryException();

        cset.setSerialNo(pjq.getSerialNumber());

        long st = System.currentTimeMillis();
        if(getQuery().getBody().size()==1)
        {
            assert(this.getQuery().sumOfPredicatesSerials() == 1);
            assert(pjq.getSerialNumber()==1);
        }
        dontCountTime += System.currentTimeMillis() - st;

        return cset;
    }

    static boolean assertRepeatedIds(List<SourcePredicateJoin> l) {
        for(SourcePredicateJoin spj: l)
            assert(spj.getPredicate().getRepeatedId() != -1);

        return true;
    }

    private void addPJQtoCPJCoverSets(List<SourcePredicateJoin> l,
                                      PredicateJoin pjq) {

        for(SourcePredicateJoin spj: l)
            spj.setQueryPJ(pjq);
    }


    private void addAllSourcePJsAsCPJs(CPJCoverSet cset,
                                       List<SourcePredicateJoin> l, String head) {
        for(SourcePredicateJoin spj: l)
        {
            CompositePredicateJoin cpj = new CompositePredicateJoin();
            long st = System.currentTimeMillis();
            assert(spj.getPredicate().getRepeatedId() != -1);
            dontCountTime += System.currentTimeMillis() - st;

            cpj.add(spj);
            for(AtomicRewriting ar: spj.getRewritings())
            {
                CompRewriting cr = new CompRewriting(head);
                cr.add(ar);
                for(Pair<String, Pair<String,String>> p : spj.getEquates())
                    cr.addEquate(p.getB().getA(), p.getB().getB(), p.getA());
                cpj.addRewritings(cr);

            }
            cset.add(cpj);
        }
    }

    //	private void transformAndLinkQPJToCPJ(CompositePredicateJoin cpj,
    //			PredicateJoin pjq) {
    //
    //		CompositePredicateJoin cpjq = new CompositePredicateJoin();
    //		cpjq.add(pjq);
    //		cpj.linkToQueryCPJ(cpjq);
    //	}


    //TODO disabled for a bug's sake this is the original cloneIfReturnedInPast
    //	private List<SourcePredicateJoin> cloneIfReturnedInPast(SourcePredicateJoin key, List<SourcePredicateJoin> l) {
    //
    //		List<SourcePredicateJoin> retlist = new ArrayList<SourcePredicateJoin>();
    //		// If key has been asked for in the past clone all list and return it
    //		if(pastReturned.contains(key))
    //		{
    //			for(SourcePredicateJoin sp: l)
    //			{
    //				try{
    //					retlist.add(sp.clone());}
    //				catch (Exception e) {
    //					throw new RuntimeException(e);
    //				}
    //			}
    //			return retlist;
    //		}
    //		pastReturned.add(key);
    //		return l;
    //	}


    private List<SourcePredicateJoin> cloneIfReturnedInPast(SourcePredicateJoin key, List<SourcePredicateJoin> l) {

        List<SourcePredicateJoin> retlist = new ArrayList<SourcePredicateJoin>();
        // If key has been asked for in the past clone all list and return it
        if(pastReturned.containsKey(key))
        {
            List<SourcePredicateJoin> retl = pastReturned.get(key);
            for(SourcePredicateJoin sp: retl)
            {
                try{
                    retlist.add(sp.clone());}
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            pastReturned.put(key, retlist);
            return retlist;
        }
        else
        {
            //dummyclone list and keep it for future cloning
            for(SourcePredicateJoin sp: l)
            {
                try{
                    retlist.add(sp.cloneDummy());}
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            pastReturned.put(key,retlist);
            return l;
        }
        //return l;
    }


}

