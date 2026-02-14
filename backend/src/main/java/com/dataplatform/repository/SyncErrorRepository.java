package com.dataplatform.repository;

import com.dataplatform.model.SyncError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncErrorRepository extends JpaRepository<SyncError, Long> {

    List<SyncError> findBySyncJobIdOrderByOccurredAtDesc(Long syncJobId);
}
