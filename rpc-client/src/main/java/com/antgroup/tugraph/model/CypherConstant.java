package com.antgroup.tugraph.model;

import java.util.ArrayList;
import java.util.List;

public class CypherConstant {

    public final static String CREATE = "create ";

    public final static String SET = "set ";

    public final static String DELETE = "delete ";

    public final static String REMOVE = "remove ";

    public final static String MERGE = "merge ";

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
}