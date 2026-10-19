
package com.example.payflow.dto;

public record VerifyRazorpayPaymentRequest(
        String razorpayOrderId,
        String razorpayPaymentId,
        String razorpaySignature
) {
}
