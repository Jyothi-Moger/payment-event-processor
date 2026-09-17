# Decision Log

## 1. How did you handle the concurrency race condition?

The wallet balance is protected using database-level pessimistic locking.

When a debit request is processed, the wallet row is locked before checking and updating the balance. This ensures that concurrent debit requests for the same wallet are processed one at a time.

If the available balance is insufficient, the transaction is rejected with an `InsufficientFundsException` and the balance is not changed.

This prevents the wallet balance from becoming negative when multiple debit requests arrive simultaneously.

The concurrency test verifies this by sending 10 concurrent ₹100 debit requests to a wallet containing ₹500. Exactly 5 requests succeed, 5 fail due to insufficient funds, and the final balance remains ₹0.

## 2. Where did your AI assistant give you an incorrect or sub-optimal suggestion?

During development, an initial test class was accidentally created under `src/main/java` instead of `src/test/java`.

This caused compilation errors because JUnit test dependencies are available in the test scope and are not available while compiling the main application code.

The issue was identified from the Maven compilation errors and the test class was moved to the correct location:

`src/test/java/com/example/payment_event_processor/ConcurrentDebitTest.java`

After correcting the location, the Maven test suite compiled and executed successfully.

The final test suite uses JUnit 5 and Spring Boot's testing support with an in-memory H2 database.