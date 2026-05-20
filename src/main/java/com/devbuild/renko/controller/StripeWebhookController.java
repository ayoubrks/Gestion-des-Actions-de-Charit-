package com.devbuild.renko.controller;

import com.devbuild.renko.entities.CharityAction;
import com.devbuild.renko.entities.Donation;
import com.devbuild.renko.entities.User;
import com.devbuild.renko.repos.CharityActionRepository;
import com.devbuild.renko.repos.DonationRepository;
import com.devbuild.renko.repos.UserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/stripe/webhook")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final CharityActionRepository charityActionRepository;
    private final com.devbuild.renko.services.StripeService stripeService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session != null) {
                handleCheckoutSessionCompleted(session);
            }
        }

        return ResponseEntity.ok("Success");
    }

    private void handleCheckoutSessionCompleted(Session session) {
        if (!stripeService.markSessionAsProcessed(session.getId())) {
            // Already processed by the success controller
            return;
        }
        
        Map<String, String> metadata = session.getMetadata();
        String type = metadata.get("type");
        String userIdStr = metadata.get("userId");
        Long userId = userIdStr != null ? Long.parseLong(userIdStr) : null;
        Double amount = session.getAmountTotal() / 100.0;

        if ("DONATION".equals(type)) {
            String actionId = metadata.get("actionId");
            processDonation(userId, actionId, amount);
        } else if ("TOPUP".equals(type)) {
            processTopUp(userId, amount);
        }
    }

    private void processDonation(Long userId, String actionId, Double amount) {
        if (userId == null || actionId == null) {
            return;
        }
        User user = userRepository.findById(userId).orElse(null);
        CharityAction action = charityActionRepository.findById(actionId).orElse(null);

        if (user != null && action != null) {
            Donation donation = Donation.builder()
                    .userId(user.getId())
                    .charityActionId(actionId)
                    .amount(amount)
                    .date(LocalDateTime.now())
                    .paymentMethod("Stripe (Carte)")
                    .status("SUCCESS")
                    .build();
            donationRepository.save(donation);

            if (action.getCurrentAmount() == null) {
                action.setCurrentAmount(0.0);
            }
            action.setCurrentAmount(action.getCurrentAmount() + amount);
            charityActionRepository.save(action);
        }
    }

    private void processTopUp(Long userId, Double amount) {
        if (userId == null) return;
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setBalance(user.getBalance() + amount);
            userRepository.save(user);
        }
    }
}
