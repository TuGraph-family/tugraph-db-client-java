package com.antgroup.tugraph;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.protocol.Options;
import com.google.protobuf.ByteString;
import lgraph.Lgraph;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@Slf4j
public class TuGraphDbRpcClient {

    private static final int TIMEOUTINMS = 60 * 60 * 1000;
    private final RpcClient client;
    private final TuGraphDbService tuGraphService;
    private final String token;
    private final String url;
    private long serverVersion;

    public TuGraphDbRpcClient(String url, String user, String pass) {
        RpcClientOptions options = new RpcClientOptions();
        options.setProtocolType(Options.ProtocolType.PROTOCOL_BAIDU_STD_VALUE);
        options.setLoadBalanceType(LoadBalanceStrategy.LOAD_BALANCE_FAIR);
        options.setWriteTimeoutMillis(TIMEOUTINMS);
        options.setReadTimeoutMillis(TIMEOUTINMS);
        client = new RpcClient(url, options);
        tuGraphService = BrpcProxy.getProxy(client, TuGraphDbService.class);
        Lgraph.LoginRequest loginReq = Lgraph.LoginRequest.newBuilder().setUser(user).setPassword(pass).build();
        Lgraph.AuthRequest authReq = Lgraph.AuthRequest.newBuilder().setLogin(loginReq).build();
        Lgraph.AclRequest req = Lgraph.AclRequest.newBuilder().setAuthRequest(authReq).build();
        Lgraph.LGraphRequest request =
                Lgraph.LGraphRequest.newBuilder().setAclRequest(req).setToken("").setIsWriteOp(false).build();

        Lgraph.LGraphResponse response = tuGraphService.HandleRequest(request);
        if (response.getErrorCode().getNumber() != Lgraph.LGraphResponse.ErrorCode.SUCCESS_VALUE) {
            throw new TuGraphDbRpcException(response.getErrorCode(), response.getError(), "TuGraphRpcClient");
        }
        this.token = response.getAclResponse().getAuthResponse().getToken();
        this.serverVersion = response.getServerVersion();
        this.url = url;
    }

    public void stopClient() {
        client.stop();
    }

    private String handleCypherRequest(String query, String graph, double timeout) {
        Lgraph.CypherRequest cypherRequest =
                Lgraph.CypherRequest.newBuilder().setQuery(query).setResultInJsonFormat(true).setGraph(graph)
                                    .setTimeout(timeout).build();
        Lgraph.LGraphRequest request =
                Lgraph.LGraphRequest.newBuilder().setCypherRequest(cypherRequest).setToken(this.token)
                                    .setClientVersion(serverVersion).build();
        Lgraph.LGraphResponse response = tuGraphService.HandleRequest(request);
        if (response.getErrorCode().getNumber() != Lgraph.LGraphResponse.ErrorCode.SUCCESS_VALUE) {
            throw new TuGraphDbRpcException(response.getErrorCode(), response.getError(), "CallCypher");
        }
        serverVersion = Math.max(response.getServerVersion(), serverVersion);
        return response.getCypherResponse().getJsonResult();
    }

    // parse delimiter and process strings like \r \n \002 \0xA
    private String parseDelimiter(String delimiter) {
        int size = delimiter.length();
        StringBuilder sb = new StringBuilder();
        for (int idx = 0; idx < size; ++idx) {
            if (delimiter.charAt(idx) == '\\') {
                int begin = idx;
                ++idx;
                if (idx == size) {
                    throw new InputException("Illegal escape sequence, do you mean \\?");
                }
                char c = delimiter.charAt(idx);
                switch (c) {
                    case ('\\'): {
                        sb.append('\\');
                        break;
                    }
                    case ('a'): {
                        throw new InputException("unsupported delimiter in java language");
//                         sb.append('\a');
//                         break;
                    }
                    case ('f'): {
                        sb.append('\f');
                        break;
                    }
                    case ('n'): {
                        sb.append('\n');
                        break;
                    }
                    case ('r'): {
                        sb.append('\r');
                        break;
                    }
                    case ('t'): {
                        sb.append('\t');
                        break;
                    }
                    case ('v'): {
                        throw new InputException("unsupported delimiter in java language");
//                         sb.append('\v');
//                         break;
                    }
                    case ('x'): {
                        // \xnn hex numbers
                        ++idx;
                        int temp = 0;
                        for (int i = 0; i < 2; i++) {
                            if (idx == size) {
                                throw new InputException("Illegal escape sequence: " + delimiter.substring(begin, idx));
                            }
                            if (delimiter.charAt(idx) >= '0' && delimiter.charAt(idx) <= '9') {
                                temp = temp * 16 + delimiter.charAt(idx) - '0';
                            } else if (delimiter.charAt(idx) >= 'a' && delimiter.charAt(idx) <= 'f') {
                                temp = temp * 16 + (delimiter.charAt(idx) - 'a' + 10);
                            } else if (delimiter.charAt(idx) >= 'A' && delimiter.charAt(idx) <= 'F') {
                                temp = temp * 16 + (delimiter.charAt(idx) - 'A' + 10);
                            } else {
                                throw new InputException("Illegal escape sequence: " + delimiter.substring(begin,
                                                                                                           idx + 1));
                            }
                            ++idx;
                        }
                        sb.append((char) (temp + '0'));
                        break;
                    }
                    default: {
                        if (c >= '0' && c <= '9') {
                            // \nnn octal numbers
                            int temp = 0;
                            for (int i = 0; i < 3; i++) {
                                if (idx == size) {
                                    throw new InputException("Illegal escape sequence: " + delimiter.substring(begin,
                                                                                                               idx));
                                }
                                char sc = delimiter.charAt(idx);
                                if (sc < '0' || sc > '7') {
                                    throw new InputException("Illegal escape sequence: " + delimiter.substring(begin,
                                                                                                               idx + 1));
                                }
                                temp = temp * 8 + sc - '0';
                                ++idx;
                            }
                            if (temp >= 256) {
                                throw new InputException("Illegal escape sequence: " + delimiter.substring(begin, idx));
                            }
                            sb.append((char) (temp + '0'));
                            break;
                        } else {
                            throw new InputException("Illegal escape sequence: " + delimiter.substring(begin, idx + 1));
                        }
                    }
                }
            } else {
                sb.append(delimiter.charAt(idx));
                ++idx;
            }
        }
        return sb.toString();
    }

    private String textFileReader(String path) throws IOException {
        String result = null;
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            throw new InputException("Illegal file: " + path);
        }
        result = FileUtils.readFileToString(file, "utf-8");
        return result;
    }

    private byte[] binaryFileReader(String path) throws IOException {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            throw new InputException("Illegal file: " + path);
        }
        long fileSize = file.length();
        try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(file.toPath()), (int) fileSize)) {
            byte[] buf = new byte[(int) fileSize];
            int readBytes = in.read(buf, 0, (int) fileSize);
            if (readBytes != fileSize) {
                return null;
            }
            return buf;
        }
    }

    private List<CsvDesc> parseConfigurationFile(JSONObject conf, boolean hasPath) {
        if (!conf.containsKey("files")) {
            return null;
        }
        List<CsvDesc> list = new ArrayList<CsvDesc>();
        JSONArray array = conf.getJSONArray("files");
        for (Object value : array) {
            JSONObject obj = (JSONObject) value;
            if (!obj.containsKey("path") || !obj.containsKey("format") || !obj.containsKey("label")
                    || !obj.containsKey("columns")) {
                throw new InputException("Missing \"path\" or \"format\" or \"label\" or \"columns\" in json");
            }
            List<File> files = new ArrayList<File>();
            if (hasPath) {
                File path = new File(obj.getString("path"));
                if (!path.exists()) {
                    throw new InputException("Path " + obj.getString("path") + " does not exist in json {}");
                }
                if (path.isFile()) {
                    files.add(path);
                }
                if (path.isDirectory()) {
                    File[] temp = path.listFiles();
                    assert temp != null;
                    Collections.addAll(files, temp);
                }
            } else {
                files.add(new File(""));
            }
            for (File file : files) {
                CsvDesc csv = new CsvDesc();
                if (!StringUtils.isBlank(file.getPath())) {
                    csv.setPath(file.getPath());
                    csv.setSize(file.length());
                }
                String format = obj.getString("format");
                if (!"JSON".equals(format) && !"CSV".equals(format)) {
                    throw new InputException("\"format\" value error : " + format + "should be CSV or JSON");
                }
                csv.setDataFormat(format);
                csv.setLabel(obj.getString("label"));
                if (obj.containsKey("header")) {
                    csv.setHeaderLine(obj.getIntValue("header"));
                }
                boolean isVertexFile = !(obj.containsKey("SRC_ID") || obj.containsKey("DST_ID"));
                csv.setFileType(isVertexFile);
                if (!isVertexFile) {
                    if (!obj.containsKey("SRC_ID") || !obj.containsKey("DST_ID")) {
                        throw new InputException("Missing \"SRC_ID\" or \"DST_ID\"");
                    }
                    csv.setEdgeSrcLabel(obj.getString("SRC_ID"));
                    csv.setEdgeDstLabel(obj.getString("DST_ID"));
                }

                JSONArray columns = obj.getJSONArray("columns");
                for (Object o : columns) {
                    String column = (String) o;
                    if ("".equals(column)) {
                        throw new InputException("Found empty filed in json " + file.getPath());
                    }
                    csv.addColumn(column);
                }
                list.add(csv);
            }
        }
        return list;
    }

    public String getUrl() {
        return url;
    }

    public String callCypher(String cypher, String graph, double timeout) {
        return handleCypherRequest(cypher, graph, timeout);
    }

    public boolean loadPlugin(String sourceFile, String pluginType, String pluginName, String codeType,
                                 String pluginDescription, boolean readOnly, String graph, double timeout) throws IOException {
        byte[] content = binaryFileReader(sourceFile);
        if (content == null) {
            return false;
        }
        String content64 = Base64.getEncoder().encodeToString(content);
        String sb = "CALL db.plugin.loadPlugin('" +
                pluginType +
                "','" +
                pluginName +
                "','" +
                content64 +
                "','" +
                codeType +
                "','" +
                pluginDescription +
                "'," +
                readOnly +
                ")";
        callCypher(sb, graph, timeout);
        return true;
    }

    public String callPlugin(String pluginType, String pluginName, String param, double pluginTimeOut,
                                boolean inProcess, String graph, double timeout) {
        String sb = "CALL db.plugin.callPlugin('" +
                pluginType +
                "','" +
                pluginName +
                "','" +
                param +
                "'," +
                pluginTimeOut +
                "," +
                inProcess +
                ")";
        return callCypher(sb, graph, timeout);
    }

    public String callProcedure(String procedureType, String procedureName, String param, double procedureTimeOut,
                             boolean inProcess, String graph) {
        Lgraph.PluginRequest.PluginType type =
                "CPP".equals(procedureType) ? Lgraph.PluginRequest.PluginType.CPP : Lgraph.PluginRequest.PluginType.PYTHON;
        ByteString resp = callProcedure(type, procedureName, ByteString.copyFromUtf8(param), graph, procedureTimeOut, inProcess);
        return resp.toStringUtf8();
    }

    public String callProcedure(String procedureType, String procedureName, ByteString param,
                             String graph, double timeout, boolean inProcess) {
        Lgraph.PluginRequest.PluginType type =
                "CPP".equals(procedureType) ? Lgraph.PluginRequest.PluginType.CPP : Lgraph.PluginRequest.PluginType.PYTHON;
        ByteString resp = callProcedure(type, procedureName, param, graph, timeout, inProcess);
        return resp.toStringUtf8();
    }

    public ByteString callProcedure(Lgraph.PluginRequest.PluginType type, String name, ByteString param,
                                 String graph, double timeout, boolean inProcess) {
        Lgraph.CallPluginRequest vreq =
                Lgraph.CallPluginRequest.newBuilder().setName(name).setParam(param).setTimeout(timeout)
                                        .setInProcess(inProcess).build();
        Lgraph.PluginRequest req =
                Lgraph.PluginRequest.newBuilder().setType(type).setCallPluginRequest(vreq).setGraph(graph).build();
        Lgraph.LGraphRequest request =
                Lgraph.LGraphRequest.newBuilder().setPluginRequest(req).setToken(this.token).build();
        Lgraph.LGraphResponse response = tuGraphService.HandleRequest(request);
        if (response.getErrorCode().getNumber() != Lgraph.LGraphResponse.ErrorCode.SUCCESS_VALUE) {
            throw new TuGraphDbRpcException(response.getErrorCode(), response.getError(), "CallProcedure");
        }
        return response.getPluginResponse().getCallPluginResponse().getReply();
    }

    public String listProcedures(String procedureType, String graph) {
        Lgraph.PluginRequest.PluginType type =
                "CPP".equals(procedureType) ? Lgraph.PluginRequest.PluginType.CPP : Lgraph.PluginRequest.PluginType.PYTHON;
        Lgraph.ListPluginRequest vreq = Lgraph.ListPluginRequest.newBuilder().build();
        Lgraph.PluginRequest req =
                Lgraph.PluginRequest.newBuilder().setType(type).setListPluginRequest(vreq).setGraph(graph).build();
        Lgraph.LGraphRequest request =
                Lgraph.LGraphRequest.newBuilder().setIsWriteOp(false).setPluginRequest(req).setToken(this.token).build();
        Lgraph.LGraphResponse response = tuGraphService.HandleRequest(request);
        if (response.getErrorCode().getNumber() != Lgraph.LGraphResponse.ErrorCode.SUCCESS_VALUE) {
            throw new TuGraphDbRpcException(response.getErrorCode(), response.getError(), "listProcedures");
        }
        return response.getPluginResponse().getListPluginResponse().getReply();
    }

    public boolean loadProcedure(String sourceFile,
                              String procedureType, String procedureName,
                              String codeType,
                              String procedureDescription, boolean readOnly,
                              String graph) throws IOException {
        Lgraph.PluginRequest.PluginType type =
                "CPP".equals(procedureType) ? Lgraph.PluginRequest.PluginType.CPP : Lgraph.PluginRequest.PluginType.PYTHON;
        Lgraph.LoadPluginRequest.CodeType cType =
                "SO".equals(codeType) ? Lgraph.LoadPluginRequest.CodeType.SO :
                "PY".equals(codeType) ? Lgraph.LoadPluginRequest.CodeType.PY :
                "CPP".equals(codeType) ? Lgraph.LoadPluginRequest.CodeType.CPP :
                Lgraph.LoadPluginRequest.CodeType.ZIP;
        ByteString content = ByteString.copyFrom(Objects.requireNonNull(binaryFileReader(sourceFile)));
        Lgraph.LoadPluginRequest lpRequest =
                Lgraph.LoadPluginRequest.newBuilder().setName(procedureName).setDesc(procedureDescription)
                .setReadOnly(readOnly).setCode(content).setCodeType(cType).build();
        Lgraph.PluginRequest req =
                Lgraph.PluginRequest.newBuilder().setType(type).setLoadPluginRequest(lpRequest).setGraph(graph).build();
        Lgraph.LGraphRequest request =
                Lgraph.LGraphRequest.newBuilder().setIsWriteOp(true).setPluginRequest(req).setToken(this.token).build();
        Lgraph.LGraphResponse response = tuGraphService.HandleRequest(request);
        if (response.getErrorCode().getNumber() != Lgraph.LGraphResponse.ErrorCode.SUCCESS_VALUE) {
            throw new TuGraphDbRpcException(response.getErrorCode(), response.getError(), "loadProcedure");
        }
        return true;
    }

    public boolean deleteProcedure(String procedureType,
                                String procedureName, String graph) {
        Lgraph.PluginRequest.PluginType type =
                "CPP".equals(procedureType) ? Lgraph.PluginRequest.PluginType.CPP : Lgraph.PluginRequest.PluginType.PYTHON;
        Lgraph.DelPluginRequest dpRequest = Lgraph.DelPluginRequest.newBuilder().setName(procedureName).build();
        Lgraph.PluginRequest req =
                Lgraph.PluginRequest.newBuilder().setType(type).setDelPluginRequest(dpRequest).setGraph(graph).build();
        Lgraph.LGraphRequest request =
                Lgraph.LGraphRequest.newBuilder().setIsWriteOp(true).setPluginRequest(req).setToken(this.token).build();
        Lgraph.LGraphResponse response = tuGraphService.HandleRequest(request);
        if (response.getErrorCode().getNumber() != Lgraph.LGraphResponse.ErrorCode.SUCCESS_VALUE) {
            throw new TuGraphDbRpcException(response.getErrorCode(), response.getError(), "deleteProcedure");
        }
        return true;
    }

    public boolean importSchemaFromContent(String schema, String graph, double timeout) throws UnsupportedEncodingException {
        byte[] textByte = schema.getBytes(StandardCharsets.UTF_8);
        String schema64 = Base64.getEncoder().encodeToString(textByte);
        String sb = "CALL db.importor.schemaImportor('"
                + schema64
                + "')";
        String res = callCypher(sb, graph, timeout);
        // this built-in procedure always returns "[]" for null.
        if (JSONArray.parseArray(res).size() != 0) {
            throw new InputException(res);
        }
        return true;
    }

    public boolean importDataFromContent(String desc, String data, String delimiter,
                                         boolean continueOnError, int threadNums, String graph, double timeout) throws UnsupportedEncodingException {
        byte[] textByteDesc = desc.getBytes(StandardCharsets.UTF_8);
        byte[] textByteData = data.getBytes(StandardCharsets.UTF_8);
        String desc64 = Base64.getEncoder().encodeToString(textByteDesc);
        String data64 = Base64.getEncoder().encodeToString(textByteData);
        String sb = "CALL db.importor.dataImportor('"
                + desc64
                + "','"
                + data64
                + "',"
                + continueOnError
                + ","
                + threadNums
                + ",'"
                + parseDelimiter(delimiter)
                + "')";
        String res = callCypher(sb, graph, timeout);
        // this built-in procedure always returns "[]" for null.
        if (JSONArray.parseArray(res).size() != 0) {
            throw new InputException(res);
        }
        return true;
    }

    public boolean importSchemaFromFile(String schemaFile, String graph, double timeout) throws UnsupportedEncodingException, IOException {
        String content = textFileReader(schemaFile);
        if ("".equals(content)) {
            throw new InputException("Illegal schema_file : " + schemaFile);
        }
        JSONObject jsonObject = JSON.parseObject(content);
        if (jsonObject.isEmpty() || !jsonObject.containsKey("schema")) {
            throw new InputException("Illegal schema_file : " + schemaFile);
        }
        JSONObject schema = new JSONObject();
        schema.put("schema", jsonObject.getJSONArray("schema"));
        byte[] textByte = schema.toJSONString().getBytes(StandardCharsets.UTF_8);
        String schema64 = Base64.getEncoder().encodeToString(textByte);
        String sb = "CALL db.importor.schemaImportor('"
                + schema64
                + "')";
        String res = callCypher(sb, graph, timeout);
        // this built-in procedure always returns "[]" for null.
        if (JSONArray.parseArray(res).size() != 0) {
            throw new InputException(res);
        }
        return true;
    }

    public boolean importDataFromFile(String confFile, String delimiter, boolean continueOnError, int threadNums,
                                      int skipPackages, String graph, double timeout) throws IOException {
        String content = textFileReader(confFile);
        if ("".equals(content)) {
            throw new InputException("Illegal conf_file : " + confFile);
        }
        JSONObject jsonObject = JSON.parseObject(content);
        if (jsonObject.isEmpty()) {
            throw new InputException("Illegal conf_file : " + confFile);
        }
        List<CsvDesc> files = parseConfigurationFile(jsonObject, true);
        assert files != null;
        if (files.isEmpty()) {
            return true;
        }
        Collections.sort(files);

        for (CsvDesc cd : files) {
            byte[] desc = cd.dump(false);
            boolean isFirstPackage = true;
            FileCutter cutter = new FileCutter(cd.getPath());
            byte[] buf;
            for (buf = cutter.cut(); buf != null; isFirstPackage = false) {
                if (skipPackages > 0) {
                    --skipPackages;
                    continue;
                }
                if (isFirstPackage) {
                    int lines = cutter.lineCount(buf);
                    if (lines < cd.getHeaderLine()) {
                        throw new InputException("HEADER too large");
                    }
                } else {
                    cd.setHeaderLine(0);
                    desc = cd.dump(false);
                }
                String desc64 = Base64.getEncoder().encodeToString(desc);
                String content64 = Base64.getEncoder().encodeToString(buf);
                String sb = "CALL db.importor.dataImportor('"
                        + desc64
                        + "','"
                        + content64
                        + "',"
                        + continueOnError
                        + ","
                        + threadNums
                        + ",'"
                        + parseDelimiter(delimiter)
                        + "')";
                String res = callCypher(sb, graph, timeout);
                if (JSONArray.parseArray(res).size() != 0) {
                    throw new InputException(res);
                }
                // this built-in procedure always returns "[]" for null.
                if (JSONArray.parseArray(res).size() != 0) {
                    throw new InputException(res);
                }
                buf = cutter.cut();
            }
        }
        return true;
    }

    // When the scope of the client object ends, the token is automatically deleted, making the token invalid.
    public void logout() {
        Lgraph.LogoutRequest logoutReq = Lgraph.LogoutRequest.newBuilder().setToken(this.token).build();
        Lgraph.AuthRequest authReq = Lgraph.AuthRequest.newBuilder().setLogout(logoutReq).build();
        Lgraph.AclRequest req = Lgraph.AclRequest.newBuilder().setAuthRequest(authReq).build();
        Lgraph.LGraphRequest request =
                Lgraph.LGraphRequest.newBuilder().setAclRequest(req).setToken(this.token).setIsWriteOp(false).build();

        Lgraph.LGraphResponse response = tuGraphService.HandleRequest(request);
        if (response.getErrorCode().getNumber() != Lgraph.LGraphResponse.ErrorCode.SUCCESS_VALUE) {
            throw new TuGraphDbRpcException(response.getErrorCode(), response.getError(), "TuGraphRpcClient");
        }
    }

    protected void finalize() {
        try {
            logout();
        } catch (Exception e) {
            System.out.print(e.getClass().getName());
        }
    }
}
