
package com.bilibili.cluster.scheduler.common.dto.scheduler.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DagData {

    private ProcessDefinition processDefinition;
    private List<ProcessTaskRelation> processTaskRelationList;
    private List<TaskDefinition> taskDefinitionList;

}