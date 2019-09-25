package gqr;

import uk.ac.soton.ecs.RelationalModel.*;

import java.util.*;

public class SQLCreator {

    public static String manyQueriestoSQL(List<CompRewriting> queries) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < queries.size(); i++) {
            ConjunctiveQuery q = queries.get(i);
            builder.append(queryToSQL(q));
            if(i != queries.size()-1)
                builder.append(" UNION ");
        }
        builder.append(';');
        return builder.toString();

    }

    public static String queryToSQL(ConjunctiveQuery query) {
        char aliasLetter= 'A';
        List<Variable> distVars = new ArrayList<>();
        Map<Variable, ArrayList<String>> columnMappings = new HashMap<Variable, ArrayList<String>>();
        Map <Constant, ArrayList<String>> columnConstraints = new HashMap<Constant, ArrayList<String>>();
        List<String> predicates = new ArrayList<>();
        query.getHead().forEach(atom -> atom.getTerms().stream().filter(term -> term instanceof Variable).forEach(term -> distVars.add((Variable) term)));

        for (Atom a : query.getBody()) {
            predicates.add(a.getPredicate().getName());
            for (int i = 0; i < a.getTerms().size(); i++) {
                if (a.getTerm(i) instanceof Variable) {
                    Variable var = (Variable) a.getTerm(i);
                    String predName = a.getPredicate().getName();
                    if (columnMappings.containsKey(var)) {
                        ArrayList<String> columns = columnMappings.get(var);
                        columns.add(aliasLetter+".\"c" + i + "\"");
                        columnMappings.put(var, columns);
                    } else {
                        ArrayList<String> columns = new ArrayList<>();
                        columns.add(aliasLetter+".\"c" + i + "\"");
                        columnMappings.put(var, columns);
                    }
                } else if (a.getTerm(i) instanceof Constant) {
                    Constant con = (Constant) a.getTerm(i);
                    String predName = a.getPredicate().getName();
                    if (columnConstraints.containsKey(con)) {
                        ArrayList<String> columns = columnConstraints.get(con);
                        columns.add(aliasLetter+".\"c" + i + "\"");
                        columnConstraints.put(con, columns);
                    } else {
                        ArrayList<String> columns = new ArrayList<String>();
                        columns.add(aliasLetter+".\"c" + i + "\"");
                        columnConstraints.put(con, columns);
                    }
                }
            }
            aliasLetter++;
        }
        aliasLetter='A';
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT DISTINCT ");
        String prefix = "";
        for(Variable v : distVars) {
            if(columnMappings.containsKey(v)) {
                builder.append(prefix);
                builder.append(columnMappings.get(v).get(0));
                prefix = ", ";
            }
        }
        builder.append(" FROM ");
        prefix = "";
        for (String s : predicates) {
            builder.append(prefix);
            builder.append("\"").append(s).append("\"");
            builder.append(" as ").append(aliasLetter++);
            prefix = ", ";
        }

        if(equalities(columnMappings,columnConstraints))
            builder.append(" WHERE ");

        prefix = "";
        for(Variable v : columnMappings.keySet()) {
            ArrayList<String> columns = columnMappings.get(v);
            if(columns.size() > 1) {
                for(int i = 1; i < columns.size(); i++) {
                    builder.append(prefix);
                    builder.append(columns.get(i-1)).append(" = ").append(columns.get(i));
                    prefix = " AND ";
                }
            }
        }

        for (Constant c : columnConstraints.keySet()) {
            ArrayList<String> columns = columnConstraints.get(c);
            for(int i = 0; i < columns.size(); i++) {
                builder.append(prefix);
                builder.append(columns.get(i) +" = "+ c.toString().replace("?",""));
            }
        }
        return builder.toString().replace("?","");

    }

    private static boolean equalities(Map<Variable, ArrayList<String>> columnMappings, Map<Constant, ArrayList<String>> columnConstraints) {
        for (Map.Entry<Variable,ArrayList<String>> entry: columnMappings.entrySet()) {
            if(entry.getValue().size() > 1) {
                return true;
            }
        }
        for (Map.Entry<Constant,ArrayList<String>> entry: columnConstraints.entrySet()) {
            if(entry.getValue().size() > 1) {
                return true;
            }
        }
        return false;

    }
}
