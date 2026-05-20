package com.devbuild.renko.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${stripe.secret.key}")
    private String secretKey;

    private final java.util.Set<String> processedSessions = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public boolean markSessionAsProcessed(String sessionId) {
        // add returns true if it wasn't there, so we return true if we successfully marked it
        return processedSessions.add(sessionId);
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public Session createPaymentSession(Double amount, String currency, String successUrl, String cancelUrl, String productName, java.util.Map<String, String> metadata) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putAllMetadata(metadata)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency)
                                .setUnitAmount((long) (amount * 100))
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(productName)
                                        .build())
                                .build())
                        .build())
                .build();

        return Session.create(params);
    }

    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }
}
