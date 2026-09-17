package com.example.payment_event_processor;

import com.example.payment_event_processor.dto.TransactionRequest;
import com.example.payment_event_processor.entity.Wallet;
import com.example.payment_event_processor.repository.TransactionRepository;
import com.example.payment_event_processor.repository.WalletRepository;
import com.example.payment_event_processor.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PaymentEventProcessorApplicationTests {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionService transactionService;

    @Test
    @DisplayName("Processes a single valid debit transaction successfully.")
    void processesSingleValidDebitTransactionSuccessfully() {

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

        System.out.println();
        System.out.println("=== HAPPY PATH TEST ===");
        System.out.println("Processing single ₹100 debit transaction...");

        transactionService.processTransaction(request);

        BigDecimal finalBalance = walletRepository.findById(userId)
                .orElseThrow()
                .getBalance();

        assertEquals(
                new BigDecimal("400.00"),
                finalBalance
        );

        assertTrue(
                transactionRepository
                        .findByTransactionId(transactionId)
                        .isPresent()
        );

        System.out.println("Transaction processed successfully.");
        System.out.println("Final balance: ₹" + finalBalance);
        System.out.println("RESULT: PASS");
    }

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

        System.out.println();
        System.out.println("=== IDEMPOTENCY TEST ===");
        System.out.println("Sending 3 identical transactionIDs simultaneously...");

        ExecutorService executor = Executors.newFixedThreadPool(3);

        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            futures.add(
                    executor.submit(() -> {
                        try {
                            transactionService.processTransaction(request);
                            return true;
                        } catch (Exception exception) {
                            return false;
                        }
                    })
            );
        }

        int successfulRequests = 0;

        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successfulRequests++;
            }
        }

        executor.shutdown();

        long storedTransactions =
                transactionRepository
                        .findByTransactionId(transactionId)
                        .stream()
                        .count();

        BigDecimal finalBalance =
                walletRepository.findById(userId)
                        .orElseThrow()
                        .getBalance();

        assertEquals(1, successfulRequests);
        assertEquals(1, storedTransactions);
        assertEquals(
                new BigDecimal("400.00"),
                finalBalance
        );

        System.out.println("Successful requests: " + successfulRequests);
        System.out.println("Stored transactions: " + storedTransactions);
        System.out.println("Final balance: ₹" + finalBalance);
        System.out.println("RESULT: PASS");
    }

    @Test
    @DisplayName("Sends 10 concurrent debit requests of ₹100 for a wallet with a ₹500 balance. Ensures the final balance is exactly ₹0 and 5 requests fail with insufficient funds.")
    void concurrentDebitsPreventNegativeBalance() throws Exception {

        UUID userId = UUID.randomUUID();

        walletRepository.save(
                new Wallet(userId, new BigDecimal("500.00"))
        );

        System.out.println();
        System.out.println("=== RACE CONDITION TEST ===");
        System.out.println("Sending 10 concurrent ₹100 debit requests...");
        System.out.println("Starting balance: ₹500.00");

        ExecutorService executor = Executors.newFixedThreadPool(10);

        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {

            UUID transactionId = UUID.randomUUID();

            TransactionRequest request = new TransactionRequest();
            request.setTransactionId(transactionId);
            request.setUserId(userId);
            request.setAmount(new BigDecimal("100.00"));
            request.setType("DEBIT");

            futures.add(
                    executor.submit(() -> {
                        try {
                            transactionService.processTransaction(request);
                            return true;
                        } catch (Exception exception) {
                            return false;
                        }
                    })
            );
        }

        int successfulRequests = 0;
        int failedRequests = 0;

        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successfulRequests++;
            } else {
                failedRequests++;
            }
        }

        executor.shutdown();

        BigDecimal finalBalance =
                walletRepository.findById(userId)
                        .orElseThrow()
                        .getBalance();

        assertEquals(5, successfulRequests);
        assertEquals(5, failedRequests);
        assertEquals(
                new BigDecimal("0.00"),
                finalBalance
        );

        System.out.println("Successful requests: " + successfulRequests);
        System.out.println("Failed requests: " + failedRequests);
        System.out.println("Final balance: ₹" + finalBalance);
        System.out.println("RESULT: PASS");
    }
}