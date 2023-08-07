package com.antgroup.tugraph.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class UserDefinedProcedure {

    @JSONField(name = "graph")
    private String graphName;

    @JSONField(name = "plugins")
    private Desc desc;

    @Getter
    @Setter
    public static class Desc {
        @JSONField(name = "description")
        private String description;

        @JSONField(name = "name")
        private String name;

        @JSONField(name = "version")
        private String version;

        @JSONField(name = "read_only")
        private boolean readOnly;
    }
}