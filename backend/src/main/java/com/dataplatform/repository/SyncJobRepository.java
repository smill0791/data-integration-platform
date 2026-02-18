package com.dataplatform.repository;

import com.dataplatform.model.SyncJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncJobRepository extends JpaRepository<SyncJob, Long> {

    List<SyncJob> findByStatusOrderByStartTimeDesc(String status);

    List<SyncJob> findTop20ByOrderByStartTimeDesc();

    List<SyncJob> findBySourceNameOrderByStartTimeDesc(String sourceName);

    List<SyncJob> findAllByOrderByStartTimeDesc();
}
