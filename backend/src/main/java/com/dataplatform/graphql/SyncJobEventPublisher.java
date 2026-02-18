package com.dataplatform.graphql;

import com.dataplatform.model.SyncJob;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class SyncJobEventPublisher {

    private final Sinks.Many<SyncJob> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void publish(SyncJob job) {
        sink.tryEmitNext(job);
    }

    public Flux<SyncJob> getJobUpdates(Long jobId) {
        return sink.asFlux()
                .filter(job -> job.getId().equals(jobId));
    }
}
