
package com.example.payflow.controller;

import com.example.payflow.dto.CreatePaymentRequest;
import com.example.payflow.dto.VerifyRazorpayPaymentRequest;
import com.example.payflow.model.PaymentTransaction;
import com.example.payflow.service.RazorpayPaymentService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class RazorpayPaymentController {

    private final RazorpayPaymentService paymentService;

    public RazorpayPaymentController(
            RazorpayPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.ok(
                paymentService.createOrder(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentTransaction> verifyPayment(
            @RequestBody VerifyRazorpayPaymentRequest request) {
        return ResponseEntity.ok(
                paymentService.verifyPayment(request));
    }
}
