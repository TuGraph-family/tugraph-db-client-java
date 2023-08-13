package com.antgroup.tugraph;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class TuGraphDbRpcClientTest {
    static Logger log = LoggerFactory.getLogger(TuGraphDbRpcClientTest.class);


    public static void deleteProcedure(TuGraphDbRpcClient client) {
        log.info("----------------testDeleteProcedure--------------------");
        try {
            boolean result = client.deleteProcedure("CPP", "sortstr", "default");
            log.info("deleteProcedure : " + result);
            assert (result);
            // should throw TuGraphDbRpcException
            result = client.deleteProcedure("CPP", "scan_graph", "default");
            log.info("loadProcedure : " + result);
        } catch (Exception e) {
            log.info("catch Exception : " + e.getMessage());
        }
    }

    public static void loadProcedure(TuGraphDbRpcClient client) {
        log.info("----------------testLoadProcedure--------------------");
        try {
            boolean result = client.loadProcedure("./sortstr.so", "CPP", "sortstr", "SO", "test sortstr", true,
                    "v1", "default");
            log.info("loadProcedure : " + result);
            assert (result);
            // should throw TuGraphDbRpcException
            result = client.loadProcedure("./scan_graph.so", "CPP", "scan_graph", "SO", "test scan_graph", true,
                    "v1", "default");
            log.info("loadProcedure : " + result);
        } catch (IOException e) {
            log.info("catch IOException : " + e.getMessage());
        } catch (Exception e) {
            log.info("catch Exception : " + e.getMessage());
        }
    }

    public static void listProcedures(TuGraphDbRpcClient client) throws Exception {
        try {
            log.info("----------------testListProcedures--------------------");
            String result = client.listProcedures("CPP", "any", "default");
            log.info("testListProcedure: " + result);
            JSONArray jsonArray = JSONArray.parseArray(result);
            assert (jsonArray.size() == 2);
        } catch (TuGraphDbRpcException e) {
            log.info("catch TuGraphDbRpcException : " + e.getMessage());
        }
    }

    public static void callProcedure(TuGraphDbRpcClient client) throws Exception {
        log.info("----------------testCallProcedure--------------------");

        String result = client.callProcedure("CPP", "sortstr", "gecfb", 1000, false, "default");
        log.info("testCallProcedure: " + result);
        assert ("bcefg".equals(result));
    }

    public static void importSchemaFromContent(TuGraphDbRpcClient client) throws Exception {
        log.info("----------------testImportSchemaFromContent--------------------");
        client.callCypher("CALL db.dropDB()", "default", 10);
        String schema = "{\"schema\" :" +
                "    [" +
                "         {" +
                "             \"label\" : \"Person\"," +
                "             \"type\" : \"VERTEX\"," +
                "             \"primary\" : \"name\"," +
                "             \"properties\" : [" +
                "                 {\"name\" : \"name\", \"type\":\"STRING\"}," +
                "                 {\"name\" : \"birthyear\", \"type\":\"INT16\", \"optional\":true}," +
                "                 {\"name\" : \"phone\", \"type\":\"INT16\",\"unique\":true, \"index\":true}" +
                "             ]" +
                "         }," +
                "        {" +
                "            \"label\" : \"Film\"," +
                "            \"type\" : \"VERTEX\"," +
                "            \"primary\" : \"title\"," +
                "            \"properties\" : [" +
                "                {\"name\" : \"title\", \"type\":\"STRING\"} " +
                "            ]" +
                "        }," +
                "       {" +
                "	        \"label\": \"PLAY_IN\"," +
                "	        \"type\": \"EDGE\"," +
                "	        \"properties\": [{" +
                "		        \"name\": \"role\"," +
                "		        \"type\": \"STRING\", " +
                "		        \"optional\": true " +
                "	        }]," +
                "	        \"constraints\": [" +
                "		        [\"Person\", \"Film\"]" +
                "	        ]" +
                "       }" +
                "    ]" +
                "}";

        try {
            boolean ret = client.importSchemaFromContent(schema, "default", 1000);
            log.info("importSchemaFromContent : " + ret);
            assert (ret);
        } catch (UnsupportedEncodingException e) {
            log.info("catch exception : " + e.getMessage());
        }

        String res = client.callCypher("CALL db.vertexLabels()", "default", 10);
        log.info("db.vertexLabels() : " + res);
        JSONArray jsonArray = JSONArray.parseArray(res);
        assert (jsonArray.size() == 2);
        for (Object o : jsonArray) {
            JSONObject obj = (JSONObject) o;
            assert ("Person".equals(obj.getString("label")) || "Film".equals(obj.getString("label")));
        }
        res = client.callCypher("CALL db.edgeLabels()", "default", 10);
        log.info("db.edgeLabels() : " + res);
        jsonArray = JSONArray.parseArray(res);
        assert (jsonArray.size() == 1);
        JSONObject jsonObject = jsonArray.getJSONObject(0);
        assert (jsonObject.containsKey("label"));
        assert ("PLAY_IN".equals(jsonObject.getString("label")));
    }

    public static void importDataFromContent(TuGraphDbRpcClient client) throws Exception {
        log.info("----------------testImportDataFromContent--------------------");
        String personDesc = "{\"files\": [" +
                "    {" +
                "        \"columns\": [" +
                "            \"name\"," +
                "            \"birthyear\"," +
                "            \"phone\"]," +
                "        \"format\": \"CSV\"," +
                "        \"header\": 0," +
                "        \"label\": \"Person\" " +
                "        }" +
                "    ]" +
                "}";

        String person = "Rachel Kempson,1910,10086\n" +
                "Michael Redgrave,1908,10087\n" +
                "Vanessa Redgrave,1937,10088\n" +
                "Corin Redgrave,1939,10089\n" +
                "Liam Neeson,1952,10090\n" +
                "Natasha Richardson,1963,10091\n" +
                "Richard Harris,1930,10092\n" +
                "Dennis Quaid,1954,10093\n" +
                "Lindsay Lohan,1986,10094\n" +
                "Jemma Redgrave,1965,10095\n" +
                "Roy Redgrave,1873,10096\n" +
                "John Williams,1932,10097\n" +
                "Christopher Nolan,1970,10098\n";


        try {
            boolean ret = client.importDataFromContent(personDesc, person, ",", true, 16, "default", 1000);
            log.info("importDataFromContent : " + ret);
            assert (ret);
        } catch (Exception e) {
            log.info("catch exception : " + e.getMessage());
        }
        String res = client.callCypher("MATCH (n) RETURN COUNT(n)", "default", 10);
        log.info("MATCH (n) RETURN COUNT(n) : " + res);
        JSONArray jsonArray = JSONArray.parseArray(res);
        assert (jsonArray.size() == 1);
        JSONObject jsonObject = jsonArray.getJSONObject(0);
        assert (jsonObject.containsKey("COUNT(n)"));
        assert (jsonObject.getIntValue("COUNT(n)") == 13);
    }

    public static void importSchemaFromFile(TuGraphDbRpcClient client) throws Exception {
        log.info("----------------testImportSchemaFromFile--------------------");
        client.callCypher("CALL db.dropDB()", "default", 10);
        try {
            boolean ret = client.importSchemaFromFile("./data/yago/yago.conf", "default", 1000);
            log.info("importSchemaFromFile : " + ret);
            assert (ret);
        } catch (IOException e) {
            log.info("catch exception : " + e.getMessage());
        }

        String res = client.callCypher("CALL db.vertexLabels()", "default", 10);
        log.info("db.vertexLabels() : " + res);
        JSONArray array = JSONArray.parseArray(res);
        assert (array.size() == 3);
        for (Object o : array) {
            JSONObject obj = (JSONObject) o;
            assert ("Person".equals(obj.getString("label")) || "Film".equals(obj.getString("label"))
                    || "City".equals(obj.getString("label")));
        }

        res = client.callCypher("CALL db.edgeLabels()", "default", 10);
        log.info("db.edgeLabels() : " + res);
        array = JSONArray.parseArray(res);
        assert (array.size() == 6);
        for (Object o : array) {
            JSONObject obj = (JSONObject) o;
            assert ("HAS_CHILD".equals(obj.getString("label")) || "MARRIED".equals(obj.getString("label"))
                    || "BORN_IN".equals(obj.getString("label")) || "DIRECTED".equals(obj.getString("label"))
                    || "WROTE_MUSIC_FOR".equals(obj.getString("label"))
                    || "ACTED_IN".equals(obj.getString("label")));
        }

    }

    public static void importDataFromFile(TuGraphDbRpcClient client) throws Exception {
        log.info("----------------testImportDataFromFile--------------------");
        try {
            boolean ret = client.importDataFromFile("./data/yago/yago.conf", ",", true, 16, 0, "default", 1000);
            log.info("importDataFromFile : " + ret);
            assert (ret);
        } catch (Exception e) {
            log.info("catch exception : " + e.getMessage());
        }
        String res = client.callCypher("MATCH (n:Person) RETURN COUNT(n)", "default", 1000);
        log.info("MATCH (n) RETURN COUNT(n) : " + res);
        JSONArray jsonArray = JSONArray.parseArray(res);
        assert (jsonArray.size() == 1);
        JSONObject jsonObject = jsonArray.getJSONObject(0);
        assert (jsonObject.containsKey("COUNT(n)"));
        assert (jsonObject.getIntValue("COUNT(n)") == 13);

        res = client.callCypher("match(n) -[r]->(m) return count(r)", "default", 1000);
        log.info("match(n) -[r]->(m) return count(r) : " + res);
        jsonArray = JSONArray.parseArray(res);
        assert (jsonArray.size() == 1);
        jsonObject = jsonArray.getJSONObject(0);
        assert (jsonObject.containsKey("count(r)"));
        assert (jsonObject.getIntValue("count(r)") == 28);
    }

    public static TuGraphDbRpcClient startClient(String[] args) throws Exception {
        log.info("----------------startClient--------------------");
        String hostPort = args[0];
        String user = args[1];
        String password = args[2];
        log.info("----------------new_Client--------------------");
        return new TuGraphDbRpcClient(hostPort, user, password);
    }

    public static void main(String[] args) throws Exception {
        log.info("----------------startMain--------------------");
        if (args.length != 3 && args.length != 0) {
            log.info("java -jar -ea tugraph-db-java-rpc-client-test-1.3.0.jar [host:port] [user] "
                    + "[password]");
            log.info("java -jar -ea tugraph-db-java-rpc-client-test-1.3.0.jar");
            return;
        }
        if (args.length == 0) {
            TuGraphHaRpcClientTestCase.haClientTest();
        } else {
            TuGraphDbRpcClient client = startClient(args);
            try {
                loadProcedure(client);
                listProcedures(client);
                deleteProcedure(client);
                loadProcedure(client);
                callProcedure(client);
                importSchemaFromContent(client);
                importDataFromContent(client);
                importSchemaFromFile(client);
                importDataFromFile(client);
            } catch (TuGraphDbRpcException e) {
                log.info("Exception at " + e.GetErrorMethod() + " with errorCodeName: " + e.GetErrorCodeName() + " and "
                        + "error: " + e.GetError());
            }
            log.info("----------------testRpcClientLogout--------------------");
            client.logout();
            try {
                loadProcedure(client);
            } catch (TuGraphDbRpcException e) {
                log.info("rpc client has logout !! Catch Exception in " + e.getMessage());
            }
        }
    }
}
