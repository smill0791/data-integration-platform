/**
 * Apollo Client Configuration
 *
 * Sets up Apollo Client for GraphQL queries, mutations, and subscriptions.
 * Uses HTTP for queries/mutations and WebSocket for subscriptions.
 */

import { ApolloClient, InMemoryCache, split, HttpLink } from '@apollo/client';
import { GraphQLWsLink } from '@apollo/client/link/subscriptions';
import { getMainDefinition } from '@apollo/client/utilities';
import { createClient } from 'graphql-ws';

// HTTP Link for queries and mutations
const httpLink = new HttpLink({
  uri: process.env.NEXT_PUBLIC_GRAPHQL_URL || 'http://localhost:8080/graphql',
  credentials: 'include', // Include cookies for authentication
});

// WebSocket Link for subscriptions
const wsLink = typeof window !== 'undefined' ? new GraphQLWsLink(
  createClient({
    url: process.env.NEXT_PUBLIC_GRAPHQL_WS_URL || 'ws://localhost:8080/graphql',
    connectionParams: {
      // Add authentication headers if needed
      // authToken: getAuthToken(),
    },
  })
) : null;

// Split between HTTP and WebSocket based on operation type
const splitLink = typeof window !== 'undefined' && wsLink
  ? split(
      ({ query }) => {
        const definition = getMainDefinition(query);
        return (
          definition.kind === 'OperationDefinition' &&
          definition.operation === 'subscription'
        );
      },
      wsLink,
      httpLink,
    )
  : httpLink;

// Apollo Client instance
export const apolloClient = new ApolloClient({
  link: splitLink,
  cache: new InMemoryCache({
    typePolicies: {
      Query: {
        fields: {
          // Configure cache policies for specific fields
          syncJobs: {
            // Merge strategy for paginated queries
            keyArgs: ['filter', 'orderBy'],
            merge(existing = [], incoming) {
              return [...existing, ...incoming];
            },
          },
        },
      },
      SyncJob: {
        fields: {
          // Computed fields are calculated on the client
          duration: {
            read(_, { readField }) {
              const startTime = readField<string>('startTime');
              const endTime = readField<string>('endTime');

              if (!startTime || !endTime) return null;

              const start = new Date(startTime).getTime();
              const end = new Date(endTime).getTime();
              return Math.floor((end - start) / 1000); // Duration in seconds
            },
          },
        },
      },
    },
  }),
  defaultOptions: {
    watchQuery: {
      fetchPolicy: 'cache-and-network',
      errorPolicy: 'all',
    },
    query: {
      fetchPolicy: 'network-only',
      errorPolicy: 'all',
    },
    mutate: {
      errorPolicy: 'all',
    },
  },
});
