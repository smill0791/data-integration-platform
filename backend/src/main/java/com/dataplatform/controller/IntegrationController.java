package com.dataplatform.controller;

import com.dataplatform.dto.SyncErrorDTO;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.service.CustomerPipelineService;
import com.dataplatform.service.SyncJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @PostMapping("/sync/customers")
    public ResponseEntity<SyncJobDTO> syncCustomers() {
        log.info("Triggering customer sync pipeline");
        SyncJobDTO result = customerPipelineService.runFullPipeline();
        return ResponseEntity.ok(result);
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
