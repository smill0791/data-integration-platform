package com.dataplatform.graphql;

import com.dataplatform.model.SyncJob;
import com.dataplatform.service.SyncJobService;
import com.dataplatform.service.SyncMessageProducer;
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

    private final SyncJobService syncJobService;
    private final SyncMessageProducer syncMessageProducer;

    @MutationMapping
    public SyncJob triggerSync(@Argument Map<String, Object> input) {
        String sourceName = (String) input.getOrDefault("sourceName", "CRM");
        log.info("GraphQL triggerSync requested for source={}", sourceName);

        SyncJob job = syncJobService.createQueuedJob(sourceName, "FULL");
        syncMessageProducer.sendSyncRequest(job.getId(), job.getSourceName(), job.getSyncType());
        return job;
    }

    @MutationMapping
    public SyncJob cancelSync(@Argument Long jobId) {
        log.info("GraphQL cancelSync requested for job={}", jobId);
        SyncJob job = syncJobService.getJobEntity(jobId);
        if (!"RUNNING".equals(job.getStatus()) && !"QUEUED".equals(job.getStatus())) {
            return job;
        }
        return syncJobService.failJob(job, "Cancelled via GraphQL");
    }
}
