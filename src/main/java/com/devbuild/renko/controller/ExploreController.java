package com.devbuild.renko.controller;

import com.devbuild.renko.entities.CharityAction;
import com.devbuild.renko.repos.CharityActionRepository;
import com.devbuild.renko.repos.OrganizationRepository;
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
    private final OrganizationRepository organizationRepository;

    @GetMapping("/explore")
    public String explore(@RequestParam(required = false) String category, 
                          @RequestParam(required = false) String search, 
                          Model model) {
        try {
            List<CharityAction> actions;
            boolean hasCategory = category != null && !category.isEmpty();
            boolean hasSearch = search != null && !search.isEmpty();

            if (hasCategory && hasSearch) {
                actions = charityActionRepository.searchWithCategory(category, search);
            } else if (hasCategory) {
                actions = charityActionRepository.findByCategoryAndIsArchivedFalse(category);
            } else if (hasSearch) {
                actions = charityActionRepository.searchWithoutCategory(search);
            } else {
                actions = charityActionRepository.findByIsArchivedFalse();
            }
            model.addAttribute("actions", actions);
        } catch (Exception e) {
            model.addAttribute("actions", java.util.List.of());
            model.addAttribute("error", "MongoDB est actuellement indisponible. Impossible de charger les initiatives.");
        }
        
        model.addAttribute("selectedCategory", category);
        model.addAttribute("search", search);
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
        
        if (action.getOrganizationId() != null) {
            organizationRepository.findById(action.getOrganizationId())
                    .ifPresent(org -> model.addAttribute("organization", org));
        }
        
        return "action-details";
    }
}
