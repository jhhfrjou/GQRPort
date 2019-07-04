package gqr;

import uk.ac.ox.cs.chaseBench.model.*;
import uk.ac.ox.cs.chaseBench.model.Constant;
import uk.ac.ox.cs.chaseBench.parser.*;
import uk.ac.ox.cs.chaseBench.processors.InputCollector;
import uk.ac.soton.ecs.RelationalModel.*;
import uk.ac.soton.ecs.RelationalModel.DatabaseSchema;
import uk.ac.soton.ecs.RelationalModel.Predicate;
import uk.ac.soton.ecs.RelationalModel.parser.*;

import java.io.*;
import java.util.*;

public class GQRPort {

    public static void main(String[] args) {
        GQRPort port = new GQRPort();
        try {
            List<ConjunctiveQuery> conjunctiveQueries = port.getQueriesFromFile("resources/views_12.txt");
            ConjunctiveQuery query = conjunctiveQueries.get(0);
            System.out.println(query.getHead().size());
            System.out.println(conjunctiveQueries);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    public List<ConjunctiveQuery> getQueriesFromFile(String resourceName) throws Exception {
        Set<Rule> cbRules = new LinkedHashSet<>();
        List<ConjunctiveQuery> conjunctiveQueries = new ArrayList<>();
        InputStream resourceStream = new FileInputStream(resourceName);
        Reader input = new InputStreamReader(resourceStream);
        StringBuffer output = new StringBuffer();
        char[] buffer = new char[4096];
        int read;
        while ((read = input.read(buffer)) != -1)
            output.append(buffer, 0, read);
        InputCollector inputCollector = new InputCollector(null, cbRules, null);
        CommonParser parser = new CommonParser(new StringReader(output.toString()));
        parser.parse(inputCollector);
        input.close();

        uk.ac.ox.cs.chaseBench.model.DatabaseSchema dbSchema = new uk.ac.ox.cs.chaseBench.model.DatabaseSchema();
        for ( Rule rule: cbRules) {
            for(uk.ac.ox.cs.chaseBench.model.Atom atom : rule.getBodyAtoms()) {
                if (!dbSchema.getPredicates().contains(atom.getPredicate())) {
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
                    dbSchema.addPredicateSchema(atom.getPredicate(), false, colName, doms);
                }
            }
            for(uk.ac.ox.cs.chaseBench.model.Atom atom : rule.getHeadAtoms()) {
                String[] colName = new String[atom.getNumberOfArguments()];
                Domain[] doms = new Domain[atom.getNumberOfArguments()];
                for(int i = 0; i < atom.getNumberOfArguments(); i++) {
                    uk.ac.ox.cs.chaseBench.model.Term temp = atom.getArgument(i);
                    colName[i] = temp.toString();
                    doms[i] = Domain.INTEGER;
                }
                dbSchema.addPredicateSchema(atom.getPredicate(),true,colName,doms);
            }


            CBSchemaConverter schemaConverter = new CBSchemaConverter();
            uk.ac.soton.ecs.RelationalModel.DatabaseSchema[] rmSchema = schemaConverter.toRelationalModel(dbSchema);
            CBQueryConverter queryConverter = new CBQueryConverter();
            conjunctiveQueries.add(queryConverter.toRelationalModel(rule,dbSchema,combineSchemas(rmSchema)));
        }
        System.out.println(cbRules);
        System.out.println(conjunctiveQueries);
        return conjunctiveQueries;
    }

    private static DatabaseSchema combineSchemas(DatabaseSchema[] schemas) {
        if (schemas.length == 2) {
            Set<uk.ac.soton.ecs.RelationalModel.Predicate> otherPredicates = schemas[1].getPredicates();
            for(Predicate otherPredicate : otherPredicates) {
                schemas[0].add(otherPredicate);
            }
            return schemas[0];
        }
        return null;
    }
}
