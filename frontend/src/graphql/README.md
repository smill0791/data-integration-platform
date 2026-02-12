# GraphQL Operations

This directory contains all GraphQL queries, mutations, and subscriptions for the frontend application.

## Structure

```
graphql/
├── queries/           # Read operations
│   └── syncJobs.ts   # Sync job queries
├── mutations/         # Write operations
│   └── syncJobs.ts   # Sync job mutations
└── subscriptions/     # Real-time updates
    └── syncJobs.ts   # Sync job subscriptions
```

## Usage Patterns

### Queries (Read Data)

```typescript
import { useQuery } from '@apollo/client';
import { GET_SYNC_JOBS } from '@/graphql/queries/syncJobs';

function SyncJobList() {
  const { data, loading, error } = useQuery(GET_SYNC_JOBS, {
    variables: { limit: 10, orderBy: 'START_TIME_DESC' },
  });

  if (loading) return <Spinner />;
  if (error) return <Error message={error.message} />;

  return <Table data={data.syncJobs} />;
}
```

### Mutations (Write Data)

```typescript
import { useMutation } from '@apollo/client';
import { TRIGGER_SYNC } from '@/graphql/mutations/syncJobs';
import { GET_SYNC_JOBS } from '@/graphql/queries/syncJobs';

function TriggerButton() {
  const [triggerSync, { loading }] = useMutation(TRIGGER_SYNC, {
    // Refetch queries after mutation
    refetchQueries: [{ query: GET_SYNC_JOBS }],
    // Or update cache manually
    update(cache, { data }) {
      // Custom cache update logic
    },
  });

  const handleClick = async () => {
    try {
      await triggerSync({
        variables: {
          input: { sourceName: 'CRM', syncType: 'FULL' },
        },
      });
    } catch (error) {
      console.error('Sync failed:', error);
    }
  };

  return (
    <button onClick={handleClick} disabled={loading}>
      {loading ? 'Triggering...' : 'Trigger Sync'}
    </button>
  );
}
```

### Subscriptions (Real-time Updates)

```typescript
import { useSubscription } from '@apollo/client';
import { WATCH_SYNC_JOB } from '@/graphql/subscriptions/syncJobs';

function LiveSyncStatus({ jobId }: { jobId: string }) {
  const { data, loading } = useSubscription(WATCH_SYNC_JOB, {
    variables: { id: jobId },
    // Only subscribe if job is running
    skip: !jobId,
  });

  if (loading) return <Spinner />;

  return (
    <div>
      <p>Status: {data?.syncJobUpdated.status}</p>
      <p>Progress: {data?.syncJobUpdated.recordsProcessed} records</p>
    </div>
  );
}
```

## Fragment Pattern

Use fragments to avoid duplication and ensure consistency:

```typescript
// Define once
export const SYNC_JOB_CORE_FIELDS = gql`
  fragment SyncJobCoreFields on SyncJob {
    id
    sourceName
    status
    recordsProcessed
  }
`;

// Reuse everywhere
export const GET_SYNC_JOB = gql`
  ${SYNC_JOB_CORE_FIELDS}
  query GetSyncJob($id: ID!) {
    syncJob(id: $id) {
      ...SyncJobCoreFields
      errors {
        id
        errorMessage
      }
    }
  }
`;
```

## Best Practices

1. **Fragments for Consistency**: Use fragments for commonly queried fields
2. **Cache Management**: Configure cache policies in `apollo-client.ts`
3. **Error Handling**: Always handle loading and error states
4. **Type Safety**: Use TypeScript types generated from schema
5. **Refetch Strategies**: Choose between `refetchQueries`, `cache.update`, or polling
6. **Subscription Cleanup**: Subscriptions auto-cleanup when component unmounts

## Code Generation (Future)

Use GraphQL Code Generator to generate TypeScript types:

```bash
npm install -D @graphql-codegen/cli @graphql-codegen/typescript @graphql-codegen/typescript-operations

# Generate types
npm run codegen
```

## Testing

Mock Apollo Client in tests:

```typescript
import { MockedProvider } from '@apollo/client/testing';

const mocks = [
  {
    request: {
      query: GET_SYNC_JOBS,
      variables: { limit: 10 },
    },
    result: {
      data: {
        syncJobs: [
          { id: '1', sourceName: 'CRM', status: 'COMPLETED' },
        ],
      },
    },
  },
];

test('renders sync jobs', () => {
  render(
    <MockedProvider mocks={mocks}>
      <SyncJobList />
    </MockedProvider>
  );
});
```

## Resources

- [Apollo Client Docs](https://www.apollographql.com/docs/react/)
- [GraphQL Best Practices](https://graphql.org/learn/best-practices/)
