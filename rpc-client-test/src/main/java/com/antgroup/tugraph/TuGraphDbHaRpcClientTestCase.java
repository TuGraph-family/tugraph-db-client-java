package com.antgroup.tugraph;

import java.io.*;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

@Slf4j
public class TuGraphDbHaRpcClientTestCase {

    public static void importSchemaFromContent(TuGraphDbRpcClient client) throws Exception {
        TuGraphDbRpcClientUtil.importSchemaFromContent(log, client, true);
    }

    public static void importDataFromContent(TuGraphDbRpcClient client) throws Exception {
        TuGraphDbRpcClientUtil.importDataFromContent(log, client, true);
    }

    public static void importSchemaFromFile(TuGraphDbRpcClient client) throws Exception {
        TuGraphDbRpcClientUtil.importSchemaFromFile(log, client, true);
    }

    public static void importDataFromFile(TuGraphDbRpcClient client) throws Exception {
        TuGraphDbRpcClientUtil.importDataFromFile(log, client, true);
    }

    public static void buildProcedure(String pluginName, String pluginPath) {
        log.info("----------------testBuildProcedure--------------------");
        String includeDir = "../../include";
        String libLgraph = "./liblgraph.so";
        String cmd = String.format("g++ -fno-gnu-unique -fPIC -g --std=c++17 -I %s -rdynamic -O3 -fopenmp -DNDEBUG -o %s %s %s -shared",
                includeDir, pluginName, pluginPath, libLgraph);
        executive(cmd);
    }

    public static void loadProcedure(TuGraphDbRpcClient client) {
        log.info("----------------testLoadProcedure--------------------");
        try {
            client.callCypher("CALL db.dropDB()", "default", 10);
            buildProcedure("./sortstr.so", "../../test/test_procedures/sortstr.cpp");
            boolean result = client.loadProcedure("./sortstr.so", "CPP", "sortstr", "SO", "test sortstr", true, "v1",  "default");
            log.info("loadProcedure : " + result);
            assert (result);
            // should throw TuGraphRpcException
            buildProcedure("./scan_graph.so", "../../test/test_procedures/scan_graph.cpp");
            result = client.loadProcedure("./scan_graph.so", "CPP", "scan_graph", "SO", "test scan_graph", true,  "v1", "default");
            log.info("loadProcedure : " + result);
        } catch (Exception e) {
            log.info("catch Exception : " + e.getMessage());
        }
    }

    public static void callProcedure(TuGraphDbRpcClient client) throws Exception {
        log.info("----------------testCallProcedure--------------------");
        String result = client.callProcedure("CPP", "sortstr", "gecfb", 1000, false, "default");
        log.info("testCallProcedure : " + result);
        assert ("bcefg".equals(result));
        try {
            result = client.callProcedure("CPP", "sortstr", "gecfb", 1000, false, "default", host+":29093");
        } catch (Exception e) {
            result = client.callProcedure("CPP", "sortstr", "gecfb", 1000, false, "default", host+":29094");
        }
        log.info("testCallProcedure : " + result);
        assert ("bcefg".equals(result));
    }

    public static void listProcedures(TuGraphDbRpcClient client) throws Exception {
        log.info("----------------testListProcedures--------------------");
        String result = client.listProcedures("CPP", "v1", "default");
        log.info("testListProcedures : " + result);
        JSONArray array = JSONObject.parseArray(result);
        assert array.size()==2;
        Thread.sleep(3000);
        String result2 = client.listProcedures("CPP", "v1", "default", host+":29093");
        String result3 = client.listProcedures("CPP", "v1", "default", host+":29094");
        JSONArray array2 = JSONObject.parseArray(result2), array3 = JSONObject.parseArray(result3);
        log.info("testListProcedures2 : " + result2);
        log.info("testListProcedures3 : " + result3);
        assert (array2.size()==2 || array3.size()==2);
    }

    public static void deleteProcedure(TuGraphDbRpcClient client) throws Exception {
        log.info("----------------testDeleteProcedure--------------------");
        boolean res = client.deleteProcedure("CPP","sortstr", "default");
        assert res;
        Thread.sleep(3000);
        String result = client.listProcedures("CPP", "v1",  "default");
        log.info("testListProcedures : " + result);
        JSONArray array = JSONObject.parseArray(result);
        assert array.size()==1;
    }

    public static void testQueryToLeader(TuGraphDbRpcClient client) throws Exception {
        log.info("----------------testQueryToLeader--------------------");
        client.callCypher("CALL db.dropDB()", "default", 10);
        boolean ret = client.importSchemaFromContent(TuGraphDbRpcClientUtil.IMPORT_SCHEMA, "default", 1000)
                && client.importDataFromContent(TuGraphDbRpcClientUtil.IMPORT_DATA_PERSON_DESC, TuGraphDbRpcClientUtil.IMPORT_DATA_PERSON, ",", true, 16, "default", 1000);
        assert (ret);
        String res = client.callCypherToLeader("MATCH (n) RETURN COUNT(n)", "default", 10);
        JSONArray jsonArray = JSONArray.parseArray(res);
        assert (jsonArray.size() == 1);
        JSONObject jsonObject = jsonArray.getJSONObject(0);
        assert (jsonObject.containsKey("COUNT(n)"));
        assert (jsonObject.getIntValue("COUNT(n)") == 13);

        ret = client.loadProcedure("./sortstr.so", "CPP", "sortstr", "SO", "test sortstr", true, "v1",  "default");
        assert ret;
        String result = client.callProcedureToLeader("CPP", "sortstr", "gecfb", 1000, false, "default");
        log.info("testCallProcedure : " + result);
        assert ("bcefg".equals(result));
    }

    public static void executive(String stmt) {
        Runtime runtime = Runtime.getRuntime();

        try {
            String[] command = {"/bin/sh", "-c", stmt};

            Process process = runtime.exec(command);
            String inStr = consumeInputStream(process.getInputStream());
            String errStr = consumeInputStream(process.getErrorStream());

            int proc = process.waitFor();
            if (proc == 0) {
                log.info("succ");
                log.info(inStr);
            } else {
                log.info("fail");
                log.info(errStr);
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }

    public static String executiveWithValue(String stmt) {
        Runtime runtime = Runtime.getRuntime();
        String inStr = "";

        try {
            String[] command = {"/bin/sh", "-c", stmt};

            Process process = runtime.exec(command);

            inStr = consumeInputStream(process.getInputStream());
            // String errStr = consumeInputStream(process.getErrorStream());

            int proc = process.waitFor();
            if (proc == 0) {
                log.info("succ");
            } else {
                log.info("fail");
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return inStr;
    }

    public static String consumeInputStream(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "GBK"));
        String s;
        StringBuilder sb = new StringBuilder();
        while ((s = br.readLine()) != null) {
            log.info(s);
            sb.append(s);
        }
        return sb.toString();
    }

    public static TuGraphDbRpcClient startHaClient(String port) throws Exception {
        log.info("----------------startClient--------------------");
        String hostPort = host + ":" + port;
        String user = "admin";
        String password = "73@TuGraph";
        return new TuGraphDbRpcClient(hostPort, user, password);
    }

    public static TuGraphDbRpcClient startHaClient(List<String> ports) throws Exception {
        log.info("----------------startClient--------------------");
        List<String> urls = new ArrayList<>();
        for (String port: ports) {
            urls.add(host + ":" + port);
        }
        String user = "admin";
        String password = "73@TuGraph";
        return new TuGraphDbRpcClient(urls, user, password);
    }

    public static String getRestPortByKey(String key, TuGraphDbRpcClient client) throws Exception {

        String s = "";

        String res = client.callCypher("CALL dbms.ha.clusterInfo()", "default", 10);
        log.info(res);
        JSONObject jsonObject = (JSONObject)JSONObject.parseArray(res).get(0);
        assert (jsonObject.containsKey("cluster_info"));
        JSONArray array = JSONArray.parseArray(jsonObject.getString("cluster_info"));
        for (Object o : array) {
            JSONObject obj = (JSONObject) o;
            if (obj.getString("state").equals(key)) {
                s = obj.getString("rest_address");
            }
        }
        return s.split(":")[1];
    }

    public static void getAllRestPorts(TuGraphDbRpcClient client) throws Exception {
        List<String> res = new ArrayList<>();
        String ss = client.callCypher("CALL dbms.ha.clusterInfo()", "default", 10);
        log.info(ss);
        JSONObject jsonObject = (JSONObject)JSONObject.parseArray(ss).get(0);
        JSONArray array = JSONArray.parseArray(jsonObject.getString("cluster_info"));
        for (Object o : array) {
            JSONObject obj = (JSONObject) o;
            res.add(obj.getString("rest_address").split(":")[1]);
        }
        log.info(res.toString());
    }

    private static String host;

    public static void executeCypherAndAssert(TuGraphDbRpcClient client, String cypher, int count) throws Exception {
        String res = client.callCypher(cypher, "default", 10);
        JSONObject jsonObject = (JSONObject)JSONObject.parseArray(res).get(0);
        assert (jsonObject.containsKey("n"));
        assert (JSONObject.parseObject(jsonObject.getString("n")).containsKey("identity"));
        assert (JSONObject.parseObject(jsonObject.getString("n")).getIntValue("identity") == count);
    }

    public static void haClientTest() throws Exception {
        host = executiveWithValue("hostname -I").trim();
        int len = host.indexOf(' ');
        if (len != -1) {
            host = host.substring(0, len);
        }

        // start HA group
        executive("mkdir ha1 && cp -r ../../src/server/lgraph_ha.json ./lgraph_server ./resource ha1 && cd ha1 && ./lgraph_server --host " + host + " --port 27072 --enable_rpc true --enable_ha true --ha_node_offline_ms 5000 --ha_node_remove_ms 10000 --rpc_port 29092 --directory ./db --log_dir ./log  --ha_conf " + host + ":29092," + host + ":29093," + host + ":29094 -c lgraph_ha.json -d start");
        Thread.sleep(3000);

        executive("mkdir ha2 && cp -r ../../src/server/lgraph_ha.json ./lgraph_server ./resource ha2 && cd ha2 && ./lgraph_server --host " + host + " --port 27073 --enable_rpc true --enable_ha true --ha_node_offline_ms 5000 --ha_node_remove_ms 10000 --rpc_port 29093 --directory ./db --log_dir ./log  --ha_conf " + host + ":29092," + host + ":29093," + host + ":29094 -c lgraph_ha.json -d start");
        Thread.sleep(3000);

        executive("mkdir ha3 && cp -r ../../src/server/lgraph_ha.json ./lgraph_server ./resource ha3 && cd ha3 && ./lgraph_server --host " + host + " --port 27074 --enable_rpc true --enable_ha true --ha_node_offline_ms 5000 --ha_node_remove_ms 10000 --rpc_port 29094 --directory ./db --log_dir ./log  --ha_conf " + host + ":29092," + host + ":29093," + host + ":29094 -c lgraph_ha.json -d start");
        Thread.sleep(3000);

        TuGraphDbRpcClient client = startHaClient("29092");
        try {

            log.info("---------client start success!--------");
            Thread.sleep(5000);
            log.info(getRestPortByKey("MASTER", client));


            String res1 = client.callCypher("MATCH (n) RETURN count(n)", "default", 10);
            JSONArray jsonArray = JSONObject.parseArray(res1);
            assert (jsonArray.size()==0);


            importSchemaFromContent(client);
            importDataFromContent(client);

            loadProcedure(client);
            callProcedure(client);
            listProcedures(client);
            deleteProcedure(client);
            testQueryToLeader(client);
            importSchemaFromFile(client);
            importDataFromFile(client);

            // query after importing data
            Thread.sleep(10000);
            String res2 = client.callCypher("MATCH (n:Person) RETURN count(n)", "default", 10);
            JSONObject jsonObject1 = (JSONObject)JSONObject.parseArray(res2).get(0);
            assert (jsonObject1.containsKey("count(n)"));
            assert (jsonObject1.getIntValue("count(n)") == 13);

            res2 = client.callCypher("MATCH (n:Person) RETURN count(n)", "default", 10, host+":29094");
            jsonObject1 = (JSONObject)JSONObject.parseArray(res2).get(0);
            assert (jsonObject1.containsKey("count(n)"));
            assert (jsonObject1.getIntValue("count(n)") == 13);

            try {
                client.callCypher("MATCH (n:Person) WHERE n.name=\"Test1\" RETURN n", "default", 10);
            } catch (TuGraphDbRpcException e) {
                log.info("Catch exception " + e.getMessage() + "in callCypher");
            }

            log.info(client.callCypher("CREATE (p:Person{name:\"Test1\",birthyear:1988,phone:10000})", "default", 10));

            Thread.sleep(1000 * 5);
            executeCypherAndAssert(client, "MATCH (n:Person) WHERE n.name=\"Test1\" RETURN n", 21);


            // test urlTable
            List<String> urls = new ArrayList<>();
            urls.add("29092");
            urls.add("29093");
            urls.add("29094");
            TuGraphDbRpcClient urlClient = startHaClient(urls);
            executeCypherAndAssert(urlClient, "MATCH (n:Person) WHERE n.name=\"Test1\" RETURN n", 21);
            urlClient.logout();
            // stop follower
            log.info("-------------------------stopping follower-------------------------");
            client.logout();
            Thread.sleep(1000 * 7);
            executive("kill -2 $(ps -ef | grep 27073 | grep -v grep | awk '{print $2}')");
            Thread.sleep(1000 * 13);
            client = startHaClient("29092");
            Thread.sleep(1000 * 7);

            getAllRestPorts(client);
            log.info("-------------------------stop follower successfully-------------------------");
            client.callCypher("CREATE (p:Person{name:\"Test2\",birthyear:1988,phone:20000})", "default", 10);

            // restart follower
            log.info("-------------------------starting follower-------------------------");
            client.logout();
            Thread.sleep(1000 * 7);
            executive("cd ha2 && ./lgraph_server --host " + host + " --port 27073 --enable_rpc true --enable_ha true --ha_node_offline_ms 5000 --ha_node_remove_ms 10000 --rpc_port 29093 --directory ./db --log_dir ./log  --ha_conf " + host + ":29092," + host + ":29093," + host + ":29094 -c lgraph_ha.json -d start");
            Thread.sleep(1000 * 13);
            client = startHaClient("29092");
            Thread.sleep(1000 * 7);

            getAllRestPorts(client);
            log.info("-------------------------start follower successfully-------------------------");
            executeCypherAndAssert(client, "MATCH (n:Person) WHERE n.name=\"Test2\" RETURN n", 22);

            // stop leader
            log.info("-------------------------stopping leader-------------------------");
            client.logout();
            Thread.sleep(1000 * 7);
            executive("kill -2 $(ps -ef | grep 27072 | grep -v grep | awk '{print $2}')");
            Thread.sleep(1000 * 13);
            client = startHaClient("29093");
            Thread.sleep(1000 * 7);

            getAllRestPorts(client);
            log.info("-------------------------stop leader successfully-------------------------");
            client.callCypher("CREATE (p:Person{name:\"Test3\",birthyear:1988,phone:30000})", "default", 10);

            // restart leader
            log.info("-------------------------starting leader-------------------------");
            client.logout();
            Thread.sleep(1000 * 7);
            executive("cd ha1 && ./lgraph_server --host " + host + " --port 27072 --enable_rpc true --enable_ha true --ha_node_offline_ms 5000 --ha_node_remove_ms 10000 --rpc_port 29092 --directory ./db --log_dir ./log  --ha_conf " + host + ":29092," + host + ":29093," + host + ":29094 -c lgraph_ha.json -d start");
            Thread.sleep(1000 * 13);
            client = startHaClient("29093");
            Thread.sleep(1000 * 7);

            getAllRestPorts(client);
            log.info("-------------------------start leader successfully-------------------------");
            executeCypherAndAssert(client, "MATCH (n:Person) WHERE n.name=\"Test3\" RETURN n", 23);
        } catch (TuGraphDbRpcException e) {
            log.info("Exception at " + e.GetErrorMethod() + " with errorCodeName: " + e.GetErrorCodeName() + " and error: " + e.GetError());
            log.info(e.getMessage());
            throw e;
        } catch (Exception e2) {
            log.info(e2.getMessage());
            throw e2;
        } finally {
            // stop leader and follower
            client.logout();
            for (int i = 27072; i <= 27074; i++) {
                executive("kill -2 $(ps -ef | grep " + i + " | grep -v grep | awk '{print $2}')");
            }
            for (int i = 1; i <= 3; i++) {
                executive("rm -rf ha" + i);
            }
            executive("rm -rf scan_graph.so sortstr.so");
        }
    }
}
