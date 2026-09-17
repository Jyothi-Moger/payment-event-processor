package com.example.payment_event_processor.controller;

import com.example.payment_event_processor.dto.TransactionRequest;
import com.example.payment_event_processor.dto.TransactionResponse;
import com.example.payment_event_processor.entity.Transaction;
import com.example.payment_event_processor.entity.Wallet;
import com.example.payment_event_processor.repository.WalletRepository;
import com.example.payment_event_processor.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final WalletRepository walletRepository;

    public TransactionController(
            TransactionService transactionService,
            WalletRepository walletRepository) {
        this.transactionService = transactionService;
        this.walletRepository = walletRepository;
    }

    @PostMapping("/process")
    public ResponseEntity<TransactionResponse> processTransaction(
            @Valid @RequestBody TransactionRequest request) {

        Transaction transaction = transactionService.processTransaction(request);

        Wallet wallet = walletRepository.findById(request.getUserId())
                .orElseThrow();

        TransactionResponse response = new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getUserId(),
                transaction.getAmount(),
                transaction.getType(),
                "SUCCESS",
                wallet.getBalance()
        );

        return ResponseEntity.ok(response);
    }
}