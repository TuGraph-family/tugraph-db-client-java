package com.antgroup.tugraph.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class BuiltInProcedure {
    @JSONField(name = "name")
    private String name;

    @JSONField(name = "signature")
    private String signature;

    @JSONField(name = "read_only")
    private boolean readOnly;
}
