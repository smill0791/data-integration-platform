package com.dataplatform.graphql;

import com.dataplatform.model.SyncJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SyncJobEventPublisherTest {

    private SyncJobEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SyncJobEventPublisher();
    }

    @Test
    void getJobUpdates_shouldReceiveMatchingJob() {
        var flux = publisher.getJobUpdates(1L);

        SyncJob job = SyncJob.builder().id(1L).sourceName("CRM").status("RUNNING")
                .startTime(LocalDateTime.now()).build();

        StepVerifier.create(flux.take(1))
                .then(() -> publisher.publish(job))
                .expectNext(job)
                .verifyComplete();
    }

    @Test
    void getJobUpdates_shouldFilterNonMatchingJobId() {
        var flux = publisher.getJobUpdates(1L);

        SyncJob job2 = SyncJob.builder().id(2L).sourceName("ERP").status("RUNNING")
                .startTime(LocalDateTime.now()).build();
        SyncJob job1 = SyncJob.builder().id(1L).sourceName("CRM").status("COMPLETED")
                .startTime(LocalDateTime.now()).build();

        StepVerifier.create(flux.take(1))
                .then(() -> {
                    publisher.publish(job2); // should be filtered
                    publisher.publish(job1); // should pass
                })
                .expectNext(job1)
                .verifyComplete();
    }

    @Test
    void getJobUpdates_multipleSubscribers_shouldBothReceive() {
        SyncJob job = SyncJob.builder().id(1L).sourceName("CRM").status("RUNNING")
                .startTime(LocalDateTime.now()).build();

        // Collect results from two independent subscribers
        var results1 = new java.util.ArrayList<SyncJob>();
        var results2 = new java.util.ArrayList<SyncJob>();

        publisher.getJobUpdates(1L).subscribe(results1::add);
        publisher.getJobUpdates(1L).subscribe(results2::add);

        publisher.publish(job);

        assertThat(results1).containsExactly(job);
        assertThat(results2).containsExactly(job);
    }
}
