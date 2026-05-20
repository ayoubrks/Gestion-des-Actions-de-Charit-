package com.devbuild.renko.controller;

import com.devbuild.renko.entities.User;
import com.devbuild.renko.repos.UserRepository;
import com.devbuild.renko.services.StripeService;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final StripeService stripeService;
    private final UserRepository userRepository;

    @Value("${app.baseUrl:http://localhost:8087}")
    private String baseUrl;

    @PostMapping("/topup")
    public String topUp(@RequestParam Double amount, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            User user = userRepository.findByEmail(authentication.getName()).orElseThrow();

            String successUrl = baseUrl + "/wallet/topup/success?sessionId={CHECKOUT_SESSION_ID}";
            String cancelUrl = baseUrl + "/profile?topup=cancel";

            Map<String, String> metadata = new HashMap<>();
            metadata.put("type", "TOPUP");
            metadata.put("userId", user.getId().toString());

            Session session = stripeService.createPaymentSession(
                    amount,
                    "eur",
                    successUrl,
                    cancelUrl,
                    "Recharge Portefeuille Renko",
                    metadata
            );

            return "redirect:" + session.getUrl();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/profile?error=stripe_error";
        }
    }

    @org.springframework.web.bind.annotation.GetMapping("/topup/success")
    public String topUpSuccess(@RequestParam String sessionId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            Session session = stripeService.retrieveSession(sessionId);
            if ("paid".equals(session.getPaymentStatus())) {
                if (stripeService.markSessionAsProcessed(sessionId)) {
                    User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
                    Double amount = session.getAmountTotal() / 100.0;
                    
                    user.setBalance(user.getBalance() + amount);
                    userRepository.save(user);
                }
                return "redirect:/profile?topup=success";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/profile?error=session_verification_failed";
    }
}
