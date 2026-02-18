package com.dataplatform.graphql;

import com.dataplatform.model.RawCustomer;
import com.dataplatform.model.SyncError;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.RawCustomerRepository;
import com.dataplatform.repository.SyncErrorRepository;
import com.dataplatform.repository.SyncJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class SyncJobQueryResolver {

    private final SyncJobRepository syncJobRepository;
    private final SyncErrorRepository syncErrorRepository;
    private final RawCustomerRepository rawCustomerRepository;

    @QueryMapping
    public SyncJob syncJob(@Argument Long id) {
        return syncJobRepository.findById(id).orElse(null);
    }

    @QueryMapping
    public List<SyncJob> syncJobs(@Argument Integer limit,
                                   @Argument Integer offset,
                                   @Argument Map<String, Object> filter,
                                   @Argument String orderBy) {
        List<SyncJob> jobs = syncJobRepository.findAllByOrderByStartTimeDesc();

        if (filter != null) {
            String status = (String) filter.get("status");
            if (status != null) {
                jobs = jobs.stream()
                        .filter(j -> status.equals(j.getStatus()))
                        .collect(Collectors.toList());
            }
            String sourceName = (String) filter.get("sourceName");
            if (sourceName != null) {
                jobs = jobs.stream()
                        .filter(j -> sourceName.equals(j.getSourceName()))
                        .collect(Collectors.toList());
            }
        }

        if (orderBy != null) {
            switch (orderBy) {
                case "START_TIME_ASC" -> jobs.sort(Comparator.comparing(SyncJob::getStartTime));
                case "RECORDS_PROCESSED_DESC" -> jobs.sort(
                        Comparator.comparing(SyncJob::getRecordsProcessed).reversed());
                // START_TIME_DESC is default from query
            }
        }

        int effectiveOffset = offset != null ? offset : 0;
        int effectiveLimit = limit != null ? limit : 20;

        return jobs.stream()
                .skip(effectiveOffset)
                .limit(effectiveLimit)
                .collect(Collectors.toList());
    }

    @QueryMapping
    public Map<String, Object> syncMetrics(@Argument String period) {
        List<SyncJob> allJobs = syncJobRepository.findAllByOrderByStartTimeDesc();
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> last24Hours = computeMetricsSummary(allJobs, now.minusHours(24), now);
        Map<String, Object> last30Days = computeMetricsSummary(allJobs, now.minusDays(30), now);

        return Map.of(
                "last24Hours", last24Hours,
                "last30Days", last30Days
        );
    }

    @SchemaMapping(typeName = "SyncJob", field = "duration")
    public Integer duration(SyncJob job) {
        if (job.getStartTime() == null || job.getEndTime() == null) return null;
        return (int) Duration.between(job.getStartTime(), job.getEndTime()).getSeconds();
    }

    @SchemaMapping(typeName = "SyncJob", field = "successRate")
    public Double successRate(SyncJob job) {
        int total = job.getRecordsProcessed() + job.getRecordsFailed();
        if (total == 0) return null;
        return (double) job.getRecordsProcessed() / total * 100.0;
    }

    @SchemaMapping(typeName = "SyncJob", field = "errors")
    public List<SyncError> errors(SyncJob job, @Argument Integer limit) {
        List<SyncError> errors = syncErrorRepository.findBySyncJobIdOrderByOccurredAtDesc(job.getId());
        int effectiveLimit = limit != null ? limit : 10;
        return errors.stream().limit(effectiveLimit).collect(Collectors.toList());
    }

    @SchemaMapping(typeName = "SyncJob", field = "stagingRecords")
    public List<RawCustomer> stagingRecords(SyncJob job, @Argument Integer limit) {
        List<RawCustomer> records = rawCustomerRepository.findBySyncJobId(job.getId());
        int effectiveLimit = limit != null ? limit : 10;
        return records.stream().limit(effectiveLimit).collect(Collectors.toList());
    }

    @SchemaMapping(typeName = "SyncJob", field = "validationStats")
    public Map<String, Object> validationStats(SyncJob job) {
        List<SyncError> errors = syncErrorRepository.findBySyncJobIdOrderByOccurredAtDesc(job.getId());
        int totalRecords = job.getRecordsProcessed() + job.getRecordsFailed();
        int failedValidation = job.getRecordsFailed();
        int passedValidation = job.getRecordsProcessed();

        Map<String, Long> errorCounts = errors.stream()
                .filter(e -> e.getErrorType() != null)
                .collect(Collectors.groupingBy(SyncError::getErrorType, Collectors.counting()));

        List<Map<String, Object>> topErrors = errorCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> Map.<String, Object>of(
                        "errorType", entry.getKey(),
                        "count", entry.getValue().intValue()))
                .collect(Collectors.toList());

        return Map.of(
                "totalRecords", totalRecords,
                "passedValidation", passedValidation,
                "failedValidation", failedValidation,
                "topErrors", topErrors
        );
    }

    private Map<String, Object> computeMetricsSummary(List<SyncJob> allJobs,
                                                       LocalDateTime from,
                                                       LocalDateTime to) {
        List<SyncJob> periodJobs = allJobs.stream()
                .filter(j -> j.getStartTime() != null
                        && !j.getStartTime().isBefore(from)
                        && !j.getStartTime().isAfter(to))
                .collect(Collectors.toList());

        int totalSyncs = periodJobs.size();
        long completedCount = periodJobs.stream()
                .filter(j -> "COMPLETED".equals(j.getStatus()))
                .count();
        double successRate = totalSyncs > 0 ? (double) completedCount / totalSyncs * 100.0 : 0.0;

        double avgDuration = periodJobs.stream()
                .filter(j -> j.getEndTime() != null)
                .mapToLong(j -> Duration.between(j.getStartTime(), j.getEndTime()).getSeconds())
                .average()
                .orElse(0.0);

        int totalRecords = periodJobs.stream()
                .mapToInt(j -> j.getRecordsProcessed() != null ? j.getRecordsProcessed() : 0)
                .sum();

        // Daily stats
        Map<LocalDate, int[]> dailyMap = new LinkedHashMap<>();
        for (SyncJob job : periodJobs) {
            LocalDate date = job.getStartTime().toLocalDate();
            int[] counts = dailyMap.computeIfAbsent(date, k -> new int[2]);
            if ("COMPLETED".equals(job.getStatus())) {
                counts[0]++;
            } else if ("FAILED".equals(job.getStatus())) {
                counts[1]++;
            }
        }

        List<Map<String, Object>> dailyStats = dailyMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> Map.<String, Object>of(
                        "date", entry.getKey(),
                        "syncsCompleted", entry.getValue()[0],
                        "syncsFailed", entry.getValue()[1]))
                .collect(Collectors.toList());

        return Map.of(
                "totalSyncs", totalSyncs,
                "successRate", successRate,
                "avgDuration", avgDuration,
                "totalRecords", totalRecords,
                "dailyStats", dailyStats
        );
    }
}
