
package com.bilibili.cluster.scheduler.api.event.factory;


import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;

import java.util.List;

public interface PipelineFactory {

    String identifier();

    List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter);

    List<ResolvedEvent> resolve(UnResolveEvent unResolveEvents, PipelineParameter pipelineParameter) throws Exception;

    List<ResolvedEvent> analyzerAndResolveEvents(PipelineParameter pipelineParameter) throws Exception;

}
