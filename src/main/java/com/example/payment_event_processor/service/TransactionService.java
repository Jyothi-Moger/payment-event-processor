package com.example.payment_event_processor.service;

import com.example.payment_event_processor.exception.InsufficientFundsException;
import com.example.payment_event_processor.dto.TransactionRequest;
import com.example.payment_event_processor.entity.Transaction;
import com.example.payment_event_processor.entity.Wallet;
import com.example.payment_event_processor.repository.TransactionRepository;
import com.example.payment_event_processor.repository.WalletRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransactionService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Transaction processTransaction(TransactionRequest request) {

        // Idempotency check
        if (transactionRepository.existsByTransactionId(request.getTransactionId())) {
            return transactionRepository
                    .findByTransactionId(request.getTransactionId())
                    .orElseThrow();
        }

        // Lock the wallet row before checking/updating balance
        Wallet wallet = walletRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if ("DEBIT".equalsIgnoreCase(request.getType())) {

            if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientFundsException("Insufficient funds");
            }

            wallet.setBalance(
                    wallet.getBalance().subtract(request.getAmount())
            );

            walletRepository.save(wallet);
        }

        Transaction transaction = new Transaction(
                request.getTransactionId(),
                request.getUserId(),
                request.getAmount(),
                request.getType()
        );

        try {
            return transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException e) {
            // Another concurrent request inserted the same transactionId.
            return transactionRepository
                    .findByTransactionId(request.getTransactionId())
                    .orElseThrow();
        }
    }
}