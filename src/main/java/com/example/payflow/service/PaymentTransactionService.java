
package com.example.payflow.service;

import com.example.payflow.dto.CreatePaymentRequest;
import com.example.payflow.model.PaymentStatus;
import com.example.payflow.model.PaymentTransaction;
import com.example.payflow.repository.PaymentTransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentTransactionService {

    private final PaymentTransactionRepository repository;

    public PaymentTransactionService(
            PaymentTransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PaymentTransaction createPayment(
            CreatePaymentRequest request) {

        PaymentTransaction payment = new PaymentTransaction(
                request.amount(),
                request.currency(),
                PaymentStatus.PENDING
        );

        return repository.save(payment);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentTransaction> getPayment(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<PaymentTransaction> getAllPayments() {
        return repository.findAll();
    }
}
