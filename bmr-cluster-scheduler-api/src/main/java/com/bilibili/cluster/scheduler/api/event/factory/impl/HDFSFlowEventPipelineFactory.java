
package com.bilibili.cluster.scheduler.api.event.factory.impl;


import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.event.factory.AbstractPipelineFactory;

import java.util.List;

public class HDFSFlowEventPipelineFactory extends AbstractPipelineFactory {

    public static final String IDENTIFIER = "HDFS";

    public HDFSFlowEventPipelineFactory() {
    }

    @Override
    public String identifier() {
        return IDENTIFIER;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {
        return null;
    }

}
