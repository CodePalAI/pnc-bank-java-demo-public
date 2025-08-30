# Concurrency Handling in PNC Bank Demo

## Overview

This document describes the concurrency control mechanisms implemented in the PNC Bank Demo application to prevent race conditions and ensure data consistency during concurrent financial transactions.

## Problem Statement

The original transfer method in `TransactionServiceImpl` had a potential race condition when processing transfers between accounts. Multiple concurrent transfers involving the same accounts could lead to:

- Inconsistent account balances
- Lost updates
- Data corruption
- Race conditions in balance calculations

## Solution Implementation

### 1. Optimistic Locking

**Implementation**: Added `@Version` annotation to the `Account` entity.

```java
@Version
@Column(name = "version")
private Long version;
```

**Benefits**:
- Prevents lost updates by detecting concurrent modifications
- Lightweight compared to pessimistic locking
- Better performance under low contention scenarios
- Automatic version incrementing by JPA

### 2. Database Migration

Created `V2__Add_Version_Column_For_Optimistic_Locking.sql` to:
- Add version column to existing accounts table
- Initialize existing records with version 0
- Set NOT NULL constraint after initialization

### 3. Transaction Isolation Levels

**Transfer Operations**: `SERIALIZABLE` isolation level
- Highest level of isolation
- Prevents phantom reads, non-repeatable reads, and dirty reads
- Ensures complete data consistency for critical transfer operations

**Deposit/Withdrawal Operations**: `READ_COMMITTED` isolation level
- Prevents dirty reads
- Balances performance with consistency requirements
- Appropriate for single-account operations

### 4. Deadlock Prevention

**Account Loading Order**: Implemented consistent ordering when loading multiple accounts:

```java
if (transferRequest.getFromAccountNumber().compareTo(transferRequest.getToAccountNumber()) < 0) {
    sourceAccount = findAccountByAccountNumber(transferRequest.getFromAccountNumber());
    destinationAccount = findAccountByAccountNumber(transferRequest.getToAccountNumber());
} else {
    destinationAccount = findAccountByAccountNumber(transferRequest.getToAccountNumber());
    sourceAccount = findAccountByAccountNumber(transferRequest.getFromAccountNumber());
}
```

**Benefits**:
- Prevents circular dependencies
- Eliminates deadlock scenarios
- Ensures deterministic lock acquisition order

### 5. Exception Handling

**Custom Exception**: `ConcurrentModificationException`
- Wraps `OptimisticLockException` with user-friendly messages
- Provides clear guidance for retry logic
- Maintains transaction boundaries

**Global Exception Handler**: Enhanced to handle concurrent modification scenarios:
- Returns HTTP 409 (Conflict) for concurrency issues
- Provides consistent error responses
- Logs detailed information for debugging

### 6. Comprehensive Testing

**Test Coverage**:
- Concurrent transfer scenarios with multiple threads
- Optimistic locking verification
- Deadlock prevention validation
- Data consistency verification
- Money conservation checks

**Test Scenarios**:
- Multiple concurrent transfers on same accounts
- Bi-directional transfers to test deadlock prevention
- Single-threaded baseline verification
- Balance consistency validation

## Error Handling Strategy

### Client-Side Handling
When receiving a 409 Conflict response:
1. Display user-friendly message: "Transaction failed due to concurrent activity"
2. Implement exponential backoff retry logic
3. Provide manual retry option for users
4. Log incidents for monitoring

### Monitoring and Alerting
- Monitor `ConcurrentModificationException` frequency
- Alert on unusual patterns of concurrent failures
- Track retry success rates
- Monitor transaction completion times

## Performance Considerations

### Optimistic vs Pessimistic Locking
- **Chosen**: Optimistic locking for better throughput
- **Trade-off**: Occasional retries vs. blocking operations
- **Rationale**: Banking operations typically have low contention

### Isolation Level Impact
- **SERIALIZABLE**: Higher consistency, potential performance impact
- **Monitoring**: Track transaction durations and deadlock occurrences
- **Tuning**: Consider READ_COMMITTED for high-volume scenarios if acceptable

## Best Practices

### Development Guidelines
1. Always wrap account updates in try-catch for `OptimisticLockException`
2. Use consistent account ordering for multi-account operations
3. Implement proper retry logic in client applications
4. Add comprehensive logging for debugging concurrent issues

### Deployment Considerations
1. Monitor database lock wait times
2. Configure appropriate connection pool sizes
3. Set realistic transaction timeouts
4. Implement circuit breakers for external service calls

## Future Enhancements

### Potential Improvements
1. **Retry Logic**: Implement automatic retry with exponential backoff in service layer
2. **Read Replicas**: Use read replicas for read-only operations to reduce contention
3. **Event Sourcing**: Consider event sourcing for audit trail and replay capabilities
4. **Distributed Locking**: Implement distributed locking for microservices architecture

### Monitoring Enhancements
1. Custom metrics for concurrency patterns
2. Real-time alerting on excessive retry rates
3. Performance dashboards for transaction latencies
4. Automated testing of concurrent scenarios in CI/CD

## Conclusion

The implemented concurrency control mechanism provides:
- **Data Consistency**: Ensures accurate account balances
- **Race Condition Prevention**: Eliminates concurrent modification issues
- **Performance Balance**: Optimizes for both consistency and throughput
- **Robust Testing**: Comprehensive validation of concurrent scenarios
- **Clear Error Handling**: User-friendly responses with proper HTTP status codes

This solution addresses the original race condition while maintaining system performance and providing a foundation for future scalability improvements.