# Payment Event Processor

A Spring Boot backend service for processing wallet payment transactions safely and reliably.

## Features

- Wallet debit and credit transactions
- Idempotent transaction processing
- Prevents duplicate transactions
- Prevents wallet balance from becoming negative
- Handles concurrent debit requests
- REST API for transaction processing
- Global exception handling
- Automated tests using JUnit

## Tech Stack

- Java
- Spring Boot
- Spring Data JPA
- Maven
- JUnit 5
- H2 Database

## Testing

The project includes tests for:

- Successful debit transaction
- Duplicate transaction handling
- Concurrent debit requests
- Insufficient wallet balance

Run tests using:

```bash
./mvnw test
