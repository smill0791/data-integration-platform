package com.dataplatform.service;

import com.dataplatform.dto.SyncErrorDTO;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.exception.ResourceNotFoundException;
import com.dataplatform.graphql.SyncJobEventPublisher;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.SyncErrorRepository;
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
    private final SyncErrorRepository syncErrorRepository;
    private final SyncJobEventPublisher eventPublisher;

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
        eventPublisher.publish(saved);
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
        eventPublisher.publish(saved);
        return saved;
    }

    @Transactional
    public SyncJob failJob(SyncJob job, String errorMessage) {
        job.setEndTime(LocalDateTime.now());
        job.setStatus("FAILED");
        job.setErrorMessage(errorMessage);
        SyncJob saved = syncJobRepository.save(job);
        log.error("Failed sync job {}: {}", saved.getId(), errorMessage);
        eventPublisher.publish(saved);
        return saved;
    }

    @Transactional
    public SyncJob createQueuedJob(String sourceName, String syncType) {
        SyncJob job = SyncJob.builder()
                .sourceName(sourceName)
                .syncType(syncType)
                .status("QUEUED")
                .startTime(LocalDateTime.now())
                .build();
        SyncJob saved = syncJobRepository.save(job);
        log.info("Created queued sync job {} for source={} type={}", saved.getId(), sourceName, syncType);
        eventPublisher.publish(saved);
        return saved;
    }

    @Transactional
    public SyncJob startJob(Long jobId) {
        SyncJob job = syncJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Sync job not found: " + jobId));
        job.setStatus("RUNNING");
        job.setStartTime(LocalDateTime.now());
        SyncJob saved = syncJobRepository.save(job);
        log.info("Started sync job {}", saved.getId());
        eventPublisher.publish(saved);
        return saved;
    }

    public SyncJobDTO getJobById(Long id) {
        SyncJob job = syncJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sync job not found: " + id));
        return SyncJobDTO.fromEntity(job);
    }

    public SyncJob getJobEntity(Long id) {
        return syncJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sync job not found: " + id));
    }

    public List<SyncJobDTO> getRecentJobs() {
        return syncJobRepository.findTop20ByOrderByStartTimeDesc()
                .stream()
                .map(SyncJobDTO::fromEntity)
                .toList();
    }

    public List<SyncErrorDTO> getErrorsForJob(Long jobId) {
        syncJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Sync job not found: " + jobId));
        return syncErrorRepository.findBySyncJobIdOrderByOccurredAtDesc(jobId)
                .stream()
                .map(SyncErrorDTO::fromEntity)
                .toList();
    }
}
