package com.example.payment_event_processor.repository;

import com.example.payment_event_processor.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.util.UUID;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findByUserId(UUID userId);
}