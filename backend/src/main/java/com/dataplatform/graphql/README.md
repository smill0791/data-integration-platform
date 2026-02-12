# GraphQL Resolvers Package

This package contains GraphQL resolvers that implement the schema defined in `src/main/resources/graphql/schema.graphqls`.

## Structure

```
graphql/
├── SyncJobResolver.java      # Query, Mutation, and Subscription resolvers for SyncJob
├── SyncMetricsResolver.java  # Resolvers for metrics queries
└── scalar/                    # Custom scalar type implementations
    ├── DateTimeScalar.java
    └── DateScalar.java
```

## Resolver Pattern

GraphQL resolvers in Spring Boot use annotations to map schema fields to Java methods:

### Query Resolvers

```java
@QueryMapping
public SyncJob syncJob(@Argument Long id) {
    return syncJobService.findById(id);
}
```

### Mutation Resolvers

```java
@MutationMapping
public SyncJob triggerSync(@Argument TriggerSyncInput input) {
    return syncJobService.triggerSync(input.getSourceName(), input.getSyncType());
}
```

### Subscription Resolvers

```java
@SubscriptionMapping
public Flux<SyncJob> syncJobUpdated(@Argument Long id) {
    return syncJobService.watchJob(id);
}
```

### Field Resolvers (Computed Fields)

```java
@SchemaMapping(typeName = "SyncJob", field = "duration")
public Integer duration(SyncJob syncJob) {
    if (syncJob.getEndTime() == null) return null;
    return (int) Duration.between(syncJob.getStartTime(), syncJob.getEndTime()).toSeconds();
}
```

## Implementation Checklist

When implementing GraphQL support:

- [ ] Add Spring Boot GraphQL dependencies to `pom.xml`
- [ ] Configure GraphQL in `application.yml`
- [ ] Implement `SyncJobResolver` with all query, mutation, and subscription mappings
- [ ] Implement custom scalar types (`DateTime`, `Date`)
- [ ] Create DTOs for GraphQL inputs (`TriggerSyncInput`, `SyncJobFilter`, etc.)
- [ ] Add GraphQL integration tests using `@AutoConfigureGraphQlTester`
- [ ] Enable GraphiQL for testing at `/graphiql`

## Testing

Test GraphQL endpoints using GraphiQL at `http://localhost:8080/graphiql`:

```graphql
# Example Query
query {
  syncJobs(limit: 5) {
    id
    sourceName
    status
    recordsProcessed
  }
}

# Example Mutation
mutation {
  triggerSync(input: { sourceName: "CRM" }) {
    id
    status
    startTime
  }
}
```

## References

- [Spring for GraphQL Documentation](https://docs.spring.io/spring-graphql/reference/)
- [GraphQL Java Documentation](https://www.graphql-java.com/)
