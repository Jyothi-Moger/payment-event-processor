package com.example.payment_event_processor;

import com.example.payment_event_processor.dto.TransactionRequest;
import com.example.payment_event_processor.entity.Wallet;
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
class ConcurrentDebitTest {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionService transactionService;

    @Test
    @DisplayName("Sends 10 concurrent debit requests of ₹100 for a wallet with ₹500 balance. Ensures final balance is ₹0.")
    void concurrentDebitsPreventNegativeBalance() throws Exception {

        UUID userId = UUID.randomUUID();

        walletRepository.save(
                new Wallet(userId, new BigDecimal("500.00"))
        );

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            Future<Boolean>[] results = new Future[10];

            for (int i = 0; i < 10; i++) {

                UUID transactionId = UUID.randomUUID();

                TransactionRequest request = new TransactionRequest();
                request.setTransactionId(transactionId);
                request.setUserId(userId);
                request.setAmount(new BigDecimal("100.00"));
                request.setType("DEBIT");

                results[i] = executor.submit(() -> {

                    startLatch.await();

                    try {
                        transactionService.processTransaction(request);
                        return true;
                    } catch (Exception exception) {
                        return false;
                    }
                });
            }

            System.out.println();
            System.out.println("=== RACE CONDITION TEST ===");
            System.out.println("Sending 10 concurrent ₹100 debit requests...");
            System.out.println("Starting balance: ₹500.00");

            startLatch.countDown();

            int successfulRequests = 0;
            int failedRequests = 0;

            for (Future<Boolean> result : results) {
                if (result.get()) {
                    successfulRequests++;
                } else {
                    failedRequests++;
                }
            }

            BigDecimal finalBalance = walletRepository
                    .findById(userId)
                    .orElseThrow()
                    .getBalance();

            assertEquals(5, successfulRequests);
            assertEquals(5, failedRequests);
            assertEquals(new BigDecimal("0.00"), finalBalance);

            System.out.println("Successful requests: " + successfulRequests);
            System.out.println("Failed requests: " + failedRequests);
            System.out.println("Final balance: ₹" + finalBalance);
            System.out.println("RESULT: PASS");

        } finally {
            executor.shutdown();
        }
    }
}