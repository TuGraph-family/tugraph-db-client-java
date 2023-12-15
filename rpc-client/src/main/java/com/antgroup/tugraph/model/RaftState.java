package com.antgroup.tugraph.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class RaftState {

    @JSONField(name = "rest_address")
    private String restAddress;
    @JSONField(name = "rpc_address")
    private String rpcAddress;
    @JSONField(name = "state")
    private String state;

    public static class StateConstant {

        public final static String REST_ADDRESS = "rest_address";

        public final static String RPC_ADDRESS = "rpc_address";

        public final static String STATE = "state";

        public final static String MASTER = "MASTER";

        public final static String FOLLOW = "FOLLOW";

        public final static String WITNESS = "WITNESS";
    }

}
