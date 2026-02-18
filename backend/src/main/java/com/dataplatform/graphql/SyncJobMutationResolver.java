package com.dataplatform.graphql;

import com.dataplatform.model.SyncJob;
import com.dataplatform.service.CustomerPipelineService;
import com.dataplatform.service.SyncJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SyncJobMutationResolver {

    private final CustomerPipelineService customerPipelineService;
    private final SyncJobService syncJobService;

    @MutationMapping
    public SyncJob triggerSync(@Argument Map<String, Object> input) {
        String sourceName = (String) input.getOrDefault("sourceName", "CRM");
        log.info("GraphQL triggerSync requested for source={}", sourceName);

        var result = customerPipelineService.runFullPipeline();
        return syncJobService.getJobEntity(result.getId());
    }

    @MutationMapping
    public SyncJob cancelSync(@Argument Long jobId) {
        log.info("GraphQL cancelSync requested for job={}", jobId);
        SyncJob job = syncJobService.getJobEntity(jobId);
        if (!"RUNNING".equals(job.getStatus())) {
            return job;
        }
        return syncJobService.failJob(job, "Cancelled via GraphQL");
    }
}
