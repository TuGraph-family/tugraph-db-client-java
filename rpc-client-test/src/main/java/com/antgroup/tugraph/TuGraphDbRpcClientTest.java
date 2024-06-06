package com.antgroup.tugraph;

import com.alibaba.fastjson.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TuGraphDbRpcClientTest {
    static Logger log = LoggerFactory.getLogger(TuGraphDbRpcClientTest.class);


    public static void deleteProcedure(TuGraphDbRpcClient client) {
        log.info("----------------testDeleteProcedure--------------------");
        try {
            boolean result = client.deleteProcedure("CPP", "sortstr", "default");
            log.info("deleteProcedure sortstr : " + result);
            assert (result);
            // should throw TuGraphDbRpcException
            result = client.deleteProcedure("CPP", "scan_graph", "default");
            log.info("deleteProcedure scan_graph : " + result);
            result = client.deleteProcedure("CPP", "multi_file", "default");
            log.info("deleteProcedure multi_file : " + result);
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

            String[] multi_files = {
                    "../../test/test_procedures/multi_files.cpp",
                    "../../test/test_procedures/multi_files.h",
                    "../../test/test_procedures/multi_files_core.cpp"
            };
            result = client.loadProcedure(multi_files, "CPP", "multi_file", "CPP", "test sortstr", true,
                    "v1", "default");
            log.info("loadProcedure : " + result);
            assert (result);
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
            assert (jsonArray.size() == 3);
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
        TuGraphDbRpcClientUtil.importSchemaFromContent(log, client, false);
    }

    public static void importDataFromContent(TuGraphDbRpcClient client) throws Exception {
        TuGraphDbRpcClientUtil.importDataFromContent(log, client, false);
    }

    public static void importSchemaFromFile(TuGraphDbRpcClient client) throws Exception {
        TuGraphDbRpcClientUtil.importSchemaFromFile(log, client, false);
    }

    public static void importDataFromFile(TuGraphDbRpcClient client) throws Exception {
        TuGraphDbRpcClientUtil.importDataFromFile(log, client, false);
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
            log.info("java -jar -ea tugraph-db-java-rpc-client-test-1.4.1.jar [host:port] [user] "
                    + "[password]");
            log.info("java -jar -ea tugraph-db-java-rpc-client-test-1.4.1.jar");
            return;
        }
        if (args.length == 0) {
            TuGraphDbHaRpcClientTestCase.haClientTest();
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
                throw e;
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
