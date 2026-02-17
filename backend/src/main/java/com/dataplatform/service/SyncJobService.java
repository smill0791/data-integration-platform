package com.dataplatform.service;

import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.exception.ResourceNotFoundException;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.SyncJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncJobService {

    private final SyncJobRepository syncJobRepository;

    @Transactional
    public SyncJob createJob(String sourceName, String syncType) {
        SyncJob job = SyncJob.builder()
                .sourceName(sourceName)
                .syncType(syncType)
                .status("RUNNING")
                .startTime(LocalDateTime.now())
                .build();
        SyncJob saved = syncJobRepository.save(job);
        log.info("Created sync job {} for source={} type={}", saved.getId(), sourceName, syncType);
        return saved;
    }

    @Transactional
    public SyncJob completeJob(SyncJob job, int recordsProcessed, int recordsFailed) {
        job.setEndTime(LocalDateTime.now());
        job.setRecordsProcessed(recordsProcessed);
        job.setRecordsFailed(recordsFailed);
        job.setStatus(recordsFailed > 0 && recordsProcessed == 0 ? "FAILED" : "COMPLETED");
        SyncJob saved = syncJobRepository.save(job);
        log.info("Completed sync job {}: processed={}, failed={}, status={}",
                saved.getId(), recordsProcessed, recordsFailed, saved.getStatus());
        return saved;
    }

    @Transactional
    public SyncJob failJob(SyncJob job, String errorMessage) {
        job.setEndTime(LocalDateTime.now());
        job.setStatus("FAILED");
        job.setErrorMessage(errorMessage);
        SyncJob saved = syncJobRepository.save(job);
        log.error("Failed sync job {}: {}", saved.getId(), errorMessage);
        return saved;
    }

    public SyncJobDTO getJobById(Long id) {
        SyncJob job = syncJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sync job not found: " + id));
        return SyncJobDTO.fromEntity(job);
    }

    public List<SyncJobDTO> getRecentJobs() {
        return syncJobRepository.findTop20ByOrderByStartTimeDesc()
                .stream()
                .map(SyncJobDTO::fromEntity)
                .toList();
    }
}
