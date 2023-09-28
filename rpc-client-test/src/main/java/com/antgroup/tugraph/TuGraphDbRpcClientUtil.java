package com.antgroup.tugraph;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;

public interface TuGraphDbRpcClientUtil {

    String IMPORT_SCHEMA = "{\"schema\" :" +
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

    String IMPORT_DATA_PERSON_DESC = "{\"files\": [" +
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

    String IMPORT_DATA_PERSON = "Rachel Kempson,1910,10086\n" +
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

    static void importSchemaFromContent(Logger log, TuGraphDbRpcClient client, boolean isHa) throws Exception {
        log.info("----------------testImportSchemaFromContent--------------------");
        client.callCypher("CALL db.dropDB()", "default", 10);

        try {
            boolean ret = client.importSchemaFromContent(TuGraphDbRpcClientUtil.IMPORT_SCHEMA, "default", 1000);
            log.info("importSchemaFromContent : " + ret);
            assert (ret);
        } catch (Exception e) {
            log.info("catch exception : " + e.getMessage());
        }

        // 高可用模式的测例sleep 5s，以保证写请求apply到每一个node
        if (isHa) {
            Thread.sleep(1000 * 5);
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

    static void importDataFromContent(Logger log, TuGraphDbRpcClient client, boolean isHa) throws Exception {
        log.info("----------------testImportDataFromContent--------------------");
        try {
            boolean ret = client.importDataFromContent(TuGraphDbRpcClientUtil.IMPORT_DATA_PERSON_DESC, TuGraphDbRpcClientUtil.IMPORT_DATA_PERSON, ",", true, 16, "default", 1000);
            log.info("importDataFromContent : " + ret);
            assert (ret);
        } catch (Exception e) {
            log.info("catch exception : " + e.getMessage());
        }

        // 高可用模式的测例sleep 5s，以保证写请求apply到每一个node
        if (isHa) {
            Thread.sleep(1000 * 5);
        }

        String res = client.callCypher("MATCH (n) RETURN COUNT(n)", "default", 10);
        log.info("MATCH (n) RETURN COUNT(n) : " + res);
        JSONArray jsonArray = JSONArray.parseArray(res);
        assert (jsonArray.size() == 1);
        JSONObject jsonObject = jsonArray.getJSONObject(0);
        assert (jsonObject.containsKey("COUNT(n)"));
        assert (jsonObject.getIntValue("COUNT(n)") == 13);
    }

    static void importSchemaFromFile(Logger log, TuGraphDbRpcClient client, boolean isHa) throws Exception {
        log.info("----------------testImportSchemaFromFile--------------------");
        client.callCypher("CALL db.dropDB()", "default", 10);
        try {
            boolean ret = client.importSchemaFromFile("./data/yago/yago.conf", "default", 1000);
            log.info("importSchemaFromFile : " + ret);
            assert (ret);
        } catch (IOException e) {
            log.info("catch exception : " + e.getMessage());
        }

        // 高可用模式的测例sleep 5s，以保证写请求apply到每一个node
        if (isHa) {
            Thread.sleep(1000 * 5);
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

    static void importDataFromFile(Logger log, TuGraphDbRpcClient client, boolean isHa) throws Exception {
        log.info("----------------testImportDataFromFile--------------------");
        try {
            boolean ret = client.importDataFromFile("./data/yago/yago.conf", ",", true, 16, 0, "default", 1000);
            log.info("importDataFromFile : " + ret);
            assert (ret);
        } catch (Exception e) {
            log.info("catch exception : " + e.getMessage());
        }

        // 高可用模式的测例sleep 5s，以保证写请求apply到每一个node
        if (isHa) {
            Thread.sleep(1000 * 5);
        }

        String res = client.callCypher("MATCH (n:Person) RETURN COUNT(n)", "default", 1000);
        log.info("MATCH (n:Person) RETURN COUNT(n) : " + res);
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
}
