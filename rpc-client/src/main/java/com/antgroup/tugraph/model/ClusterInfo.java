package com.antgroup.tugraph.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ClusterInfo {
    @JSONField(name = "cluster_info")
    private List<RaftState> clusterInfo;

    @JSONField(name = "is_master")
    private boolean isMaster;
}