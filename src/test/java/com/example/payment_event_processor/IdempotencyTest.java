package com.example.payment_event_processor;

import com.example.payment_event_processor.dto.TransactionRequest;
import com.example.payment_event_processor.entity.Transaction;
import com.example.payment_event_processor.entity.Wallet;
import com.example.payment_event_processor.repository.TransactionRepository;
import com.example.payment_event_processor.repository.WalletRepository;
import com.example.payment_event_processor.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class IdempotencyTest {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionService transactionService;

    @Test
    @DisplayName("Sends 3 identical transactionIDs simultaneously. Ensures the balance is only deducted once.")
    void duplicateTransactionIsProcessedOnlyOnce() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        walletRepository.save(
                new Wallet(userId, new BigDecimal("500.00"))
        );

        TransactionRequest request = new TransactionRequest();
        request.setTransactionId(transactionId);
        request.setUserId(userId);
        request.setAmount(new BigDecimal("100.00"));
        request.setType("DEBIT");

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            Future<TransactionResult> result1 =
                    executor.submit(() -> processRequest(request, startLatch));

            Future<TransactionResult> result2 =
                    executor.submit(() -> processRequest(request, startLatch));

            Future<TransactionResult> result3 =
                    executor.submit(() -> processRequest(request, startLatch));

            System.out.println();
            System.out.println("=== IDEMPOTENCY TEST ===");
            System.out.println("Sending 3 identical transactionIDs simultaneously...");

            startLatch.countDown();

            TransactionResult r1 = result1.get();
            TransactionResult r2 = result2.get();
            TransactionResult r3 = result3.get();

            long successfulRequests =
                    (r1.success() ? 1 : 0)
                    + (r2.success() ? 1 : 0)
                    + (r3.success() ? 1 : 0);

            BigDecimal finalBalance = walletRepository
                    .findById(userId)
                    .orElseThrow()
                    .getBalance();

            long transactionCount = transactionRepository
                    .findAll()
                    .stream()
                    .filter(transaction ->
                            transaction.getTransactionId().equals(transactionId))
                    .count();

            assertEquals(1, transactionCount);
            assertEquals(new BigDecimal("400.00"), finalBalance);

            System.out.println("Successful requests: " + successfulRequests);
            System.out.println("Stored transactions: " + transactionCount);
            System.out.println("Final balance: ₹" + finalBalance);
            System.out.println("RESULT: PASS");

        } finally {
            executor.shutdown();
        }
    }

    private TransactionResult processRequest(
            TransactionRequest request,
            CountDownLatch startLatch) throws InterruptedException {

        startLatch.await();

        try {
            transactionService.processTransaction(request);
            return new TransactionResult(true);
        } catch (Exception exception) {
            return new TransactionResult(false);
        }
    }

    private record TransactionResult(boolean success) {
    }
}