#!/bin/bash
echo "Creating SQS queues..."

# Create Dead Letter Queue
awslocal sqs create-queue --queue-name customer-sync-dlq

# Get DLQ ARN
DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/customer-sync-dlq \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)

# Create main queue with redrive policy
awslocal sqs create-queue \
  --queue-name customer-sync-queue \
  --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"${DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}"

echo "SQS queues created successfully"
awslocal sqs list-queues
