
package com.example.payflow.service;

import com.example.payflow.dto.CreatePaymentRequest;
import com.example.payflow.dto.VerifyRazorpayPaymentRequest;
import com.example.payflow.model.PaymentStatus;
import com.example.payflow.model.PaymentTransaction;
import com.example.payflow.repository.PaymentTransactionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RazorpayPaymentService {

    private final PaymentTransactionRepository repository;
    private final String keyId;
    private final String keySecret;

    public RazorpayPaymentService(
            PaymentTransactionRepository repository,
            @Value("${razorpay.key.id}") String keyId,
            @Value("${razorpay.key.secret}") String keySecret) {
        this.repository = repository;
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    public Map<String, Object> createOrder(CreatePaymentRequest request) {
        try {
            BigDecimal amount = request.amount()
                    .setScale(2, RoundingMode.UNNECESSARY);

            int amountInPaise = amount
                    .movePointRight(2)
                    .intValueExact();

            RazorpayClient client =
                    new RazorpayClient(keyId, keySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", request.currency());
            orderRequest.put("receipt",
                    "payflow_" + System.currentTimeMillis());

            Order order = client.orders.create(orderRequest);

            PaymentTransaction transaction = new PaymentTransaction(
                    amount, request.currency(), PaymentStatus.PENDING);

            transaction.setRazorpayOrderId(order.get("id"));
            repository.save(transaction);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("keyId", keyId);
            response.put("orderId", order.get("id"));
            response.put("amount", amountInPaise);
            response.put("currency", request.currency());

            return response;

        } catch (ArithmeticException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Amount is too large or has invalid decimal places.");
        } catch (Exception e) {
    throw new ResponseStatusException(
        HttpStatus.BAD_GATEWAY,
        "Unable to create Razorpay order."
    );
}
    }

    @Transactional
    public PaymentTransaction verifyPayment(
            VerifyRazorpayPaymentRequest request) {
        try {
            if (request.razorpayOrderId() == null
                    || request.razorpayPaymentId() == null
                    || request.razorpaySignature() == null
                    || request.razorpayOrderId().isBlank()
                    || request.razorpayPaymentId().isBlank()
                    || request.razorpaySignature().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Payment verification details are required.");
            }

            PaymentTransaction transaction = repository
                    .findByRazorpayOrderId(request.razorpayOrderId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Payment order was not found."));

            if (transaction.getStatus() == PaymentStatus.SUCCESS) {
                if (request.razorpayPaymentId().equals(
                        transaction.getRazorpayPaymentId())) {
                    return transaction;
                }

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This order has already been paid.");
            }

            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id",
                    request.razorpayOrderId());
            attributes.put("razorpay_payment_id",
                    request.razorpayPaymentId());
            attributes.put("razorpay_signature",
                    request.razorpaySignature());

            boolean validSignature = Utils.verifyPaymentSignature(
                    attributes, keySecret);

            if (!validSignature) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid payment signature.");
            }

            RazorpayClient client =
                    new RazorpayClient(keyId, keySecret);

            com.razorpay.Payment razorpayPayment =
                    client.payments.fetch(request.razorpayPaymentId());

            String actualOrderId =
                    razorpayPayment.get("order_id");
            String actualStatus =
                    razorpayPayment.get("status");
            String actualCurrency =
                    razorpayPayment.get("currency");

            int actualAmount =
                    ((Number) razorpayPayment.get("amount")).intValue();

            int expectedAmount = transaction.getAmount()
                    .movePointRight(2)
                    .intValueExact();

            if (!request.razorpayOrderId().equals(actualOrderId)
                    || actualAmount != expectedAmount
                    || !transaction.getCurrency().equalsIgnoreCase(
                            actualCurrency)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Payment details do not match the order.");
            }

            if (!"captured".equalsIgnoreCase(actualStatus)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Payment is not captured yet.");
            }

            transaction.setRazorpayPaymentId(
                    request.razorpayPaymentId());
            transaction.setStatus(PaymentStatus.SUCCESS);

            return repository.save(transaction);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to verify Razorpay payment.");
        }
    }
}
