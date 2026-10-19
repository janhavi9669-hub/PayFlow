
package com.example.payflow.controller;

import com.example.payflow.dto.CreatePaymentRequest;
import com.example.payflow.model.PaymentTransaction;
import com.example.payflow.service.PaymentTransactionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class PaymentTransactionController {

    private final PaymentTransactionService service;

    public PaymentTransactionController(
            PaymentTransactionService service) {
        this.service = service;
    }

    // Create a new transaction
    @PostMapping
    public ResponseEntity<PaymentTransaction> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {

        PaymentTransaction payment = service.createPayment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(payment);
    }

    // Get a transaction by ID
    @GetMapping("/{id}")
    public ResponseEntity<PaymentTransaction> getPayment(
            @PathVariable Long id) {

        return service.getPayment(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Get all transactions
    @GetMapping
    public ResponseEntity<List<PaymentTransaction>> getAllPayments() {

        return ResponseEntity.ok(service.getAllPayments());
    }

    
}
