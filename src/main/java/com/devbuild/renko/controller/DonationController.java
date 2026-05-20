package com.devbuild.renko.controller;

import com.devbuild.renko.entities.CharityAction;
import com.devbuild.renko.entities.Donation;
import com.devbuild.renko.entities.User;
import com.devbuild.renko.repos.CharityActionRepository;
import com.devbuild.renko.repos.DonationRepository;
import com.devbuild.renko.repos.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/donate")
@RequiredArgsConstructor
public class DonationController {

    private final CharityActionRepository charityActionRepository;
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final com.devbuild.renko.services.StripeService stripeService;

    @Value("${app.baseUrl:http://localhost:8087}")
    private String baseUrl;

    @PostMapping("/checkout")
    public String checkout(@RequestParam String actionId, @RequestParam Double amount, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        if (actionId == null || amount == null) {
            return "redirect:/explore?error=invalid_params";
        }

        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        
        // Check if user has sufficient balance
        if (user.getBalance() < amount) {
            return "redirect:/action/" + actionId + "?error=insufficient_funds";
        }

        CharityAction action = charityActionRepository.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid action ID"));

        // Deduct from balance
        user.setBalance(user.getBalance() - amount);
        userRepository.save(user);

        Donation donation = Donation.builder()
                .userId(user.getId())
                .charityActionId(actionId)
                .amount(amount)
                .date(LocalDateTime.now())
                .paymentMethod("Portefeuille Renko")
                .status("SUCCESS")
                .build();
        
        if (donation != null) {
            donationRepository.save(donation);
        }

        // Update Charity Action current amount
        if (action.getCurrentAmount() == null) {
            action.setCurrentAmount(0.0);
        }
        action.setCurrentAmount(action.getCurrentAmount() + amount);
        charityActionRepository.save(action);

        return "redirect:/donate/success?actionId=" + actionId + "&amount=" + amount;
    }

    @PostMapping("/stripe")
    public String stripeCheckout(@RequestParam String actionId, @RequestParam Double amount, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        if (actionId == null || amount == null) {
            return "redirect:/explore";
        }

        try {
            User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
            CharityAction action = charityActionRepository.findById(actionId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid action ID"));

            String successUrl = baseUrl + "/donate/stripe/success?actionId=" + actionId + "&amount=" + amount + "&sessionId={CHECKOUT_SESSION_ID}";
            String cancelUrl = baseUrl + "/action/" + actionId;

            java.util.Map<String, String> metadata = new java.util.HashMap<>();
            metadata.put("type", "DONATION");
            metadata.put("userId", user.getId().toString());
            metadata.put("actionId", actionId);

            com.stripe.model.checkout.Session session = stripeService.createPaymentSession(
                    amount, 
                    "eur", 
                    successUrl, 
                    cancelUrl, 
                    "Don pour : " + action.getTitle(),
                    metadata
            );

            return "redirect:" + session.getUrl();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/action/" + actionId + "?error=stripe_error";
        }
    }

    @GetMapping("/stripe/success")
    public String stripeSuccess(@RequestParam String actionId, @RequestParam Double amount, @RequestParam String sessionId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            com.stripe.model.checkout.Session session = stripeService.retrieveSession(sessionId);
            if ("paid".equals(session.getPaymentStatus())) {
                if (stripeService.markSessionAsProcessed(sessionId)) {
                    User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
                    CharityAction action = charityActionRepository.findById(actionId).orElseThrow();

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
                return "redirect:/donate/success?actionId=" + actionId + "&amount=" + amount;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/action/" + actionId + "?error=session_verification_failed";
    }

    @GetMapping("/success")
    public String success(@RequestParam String actionId, @RequestParam Double amount, Model model) {
        if (actionId == null) {
            return "redirect:/explore";
        }
        CharityAction action = charityActionRepository.findById(actionId).orElseThrow();
        model.addAttribute("action", action);
        model.addAttribute("amount", amount);
        return "donation-success";
    }
}
