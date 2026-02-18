package com.dataplatform.graphql;

import com.dataplatform.model.SyncJob;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
@RequiredArgsConstructor
public class SyncJobSubscriptionResolver {

    private final SyncJobEventPublisher eventPublisher;

    @SubscriptionMapping
    public Flux<SyncJob> syncJobUpdated(@Argument Long id) {
        return eventPublisher.getJobUpdates(id);
    }
}
