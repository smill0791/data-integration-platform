package com.dataplatform.controller;

import com.dataplatform.dto.SyncErrorDTO;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.model.SyncJob;
import com.dataplatform.service.CustomerPipelineService;
import com.dataplatform.service.SyncJobService;
import com.dataplatform.service.SyncMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final CustomerPipelineService customerPipelineService;
    private final SyncJobService syncJobService;
    private final SyncMessageProducer syncMessageProducer;

    @PostMapping("/sync/customers")
    public ResponseEntity<SyncJobDTO> syncCustomers() {
        log.info("Triggering async customer sync pipeline");
        SyncJob job = syncJobService.createQueuedJob("CRM", "FULL");
        syncMessageProducer.sendSyncRequest(job.getId(), job.getSourceName(), job.getSyncType());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(SyncJobDTO.fromEntity(job));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<SyncJobDTO>> getRecentJobs() {
        return ResponseEntity.ok(syncJobService.getRecentJobs());
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<SyncJobDTO> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(syncJobService.getJobById(id));
    }

    @GetMapping("/jobs/{id}/errors")
    public ResponseEntity<List<SyncErrorDTO>> getJobErrors(@PathVariable Long id) {
        return ResponseEntity.ok(syncJobService.getErrorsForJob(id));
    }
}
