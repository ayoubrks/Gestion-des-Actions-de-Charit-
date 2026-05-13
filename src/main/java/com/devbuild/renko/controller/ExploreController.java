package com.devbuild.renko.controller;

import com.devbuild.renko.entities.CharityAction;
import com.devbuild.renko.repos.CharityActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ExploreController {

    private final CharityActionRepository charityActionRepository;

    @GetMapping("/explore")
    public String explore(@RequestParam(required = false) String category, Model model) {
        try {
            List<CharityAction> actions;
            if (category != null && !category.isEmpty()) {
                actions = charityActionRepository.findByCategoryAndOrganizationIdIsNullAndIsArchivedFalse(category);
            } else {
                actions = charityActionRepository.findByOrganizationIdIsNullAndIsArchivedFalse();
            }
            model.addAttribute("actions", actions);
        } catch (Exception e) {
            model.addAttribute("actions", java.util.List.of());
            model.addAttribute("error", "MongoDB est actuellement indisponible. Impossible de charger les initiatives.");
        }
        
        model.addAttribute("selectedCategory", category);
        return "explore";
    }

    @GetMapping("/action/{id}")
    public String getActionDetails(@org.springframework.web.bind.annotation.PathVariable String id, Model model) {
        if (id == null) {
            return "redirect:/explore";
        }
        CharityAction action = charityActionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid action ID"));
        model.addAttribute("action", action);
        return "action-details";
    }
}
