package com.dataplatform.repository;

import com.dataplatform.model.SyncError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncErrorRepository extends JpaRepository<SyncError, Long> {

    List<SyncError> findBySyncJobIdOrderByOccurredAtDesc(Long syncJobId);

    @Query("SELECT DISTINCT e.failedRecord FROM SyncError e " +
            "WHERE e.syncJob.id = :jobId AND e.failedRecord IS NOT NULL")
    List<String> findDistinctFailedRecordsByJobId(@Param("jobId") Long jobId);
}
