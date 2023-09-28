package com.antgroup.tugraph.model;

import java.util.ArrayList;
import java.util.List;

public class GraphQueryConstant {

    public final static String CREATE = "create ";

    public final static String SET = "set ";

    public final static String DELETE = "delete ";

    public final static String REMOVE = "remove ";

    public final static String MERGE = "merge ";

    public final static String INSERT = "insert ";

    public final static String DROP = "drop ";

    public final static String CALL = "CALL ";

    public List<String> getAllCypherType() {
        List<String> re = new ArrayList<>();
        re.add(CREATE);
        re.add(SET);
        re.add(DELETE);
        re.add(REMOVE);
        re.add(MERGE);
        return re;
    }

    public List<String> getAllGqlType() {
        List<String> re = new ArrayList<>();
        re.add(CREATE);
        re.add(INSERT);
        re.add(DROP);
        re.add(SET);
        re.add(REMOVE);
        re.add(DELETE);
        return re;
    }
}