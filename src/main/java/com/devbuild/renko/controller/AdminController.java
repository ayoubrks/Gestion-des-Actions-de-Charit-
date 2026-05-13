package com.devbuild.renko.controller;

import com.devbuild.renko.entities.CharityAction;
import com.devbuild.renko.entities.Donation;
import com.devbuild.renko.entities.User;
import com.devbuild.renko.repos.CharityActionRepository;
import com.devbuild.renko.repos.DonationRepository;
import com.devbuild.renko.repos.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

@Controller
@RequestMapping("/admin")
@org.springframework.transaction.annotation.Transactional
public class AdminController {

    private final CharityActionRepository charityActionRepository;
    private final UserRepository userRepository;
    private final DonationRepository donationRepository;
    private final com.devbuild.renko.repos.OrganizationRepository organizationRepository;
    private final com.devbuild.renko.services.EmailService emailService;

    public AdminController(CharityActionRepository charityActionRepository,
                           UserRepository userRepository,
                           DonationRepository donationRepository,
                           com.devbuild.renko.repos.OrganizationRepository organizationRepository,
                           com.devbuild.renko.services.EmailService emailService) {
        this.charityActionRepository = charityActionRepository;
        this.userRepository = userRepository;
        this.donationRepository = donationRepository;
        this.organizationRepository = organizationRepository;
        this.emailService = emailService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Convert users to simple Maps to avoid LazyInitializationException
        // (User.organization is LAZY-loaded and would crash Thymeleaf after session closes)
        List<Map<String, Object>> userDTOs = new ArrayList<>();
        List<User> rawUsers = new ArrayList<>();
        try {
            rawUsers = userRepository.findAll();
            for (User u : rawUsers) {
                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("id", u.getId());
                dto.put("firstName", u.getFirstName() != null ? u.getFirstName() : "");
                dto.put("lastName", u.getLastName() != null ? u.getLastName() : "");
                dto.put("email", u.getEmail() != null ? u.getEmail() : "--");
                dto.put("role", u.getRole() != null ? u.getRole().toString() : "USER");
                dto.put("balance", u.getBalance() != null ? u.getBalance() : 0.0);
                userDTOs.add(dto);
            }
            model.addAttribute("totalUsers", rawUsers.size());
            model.addAttribute("users", userDTOs);
        } catch (Exception e) {
            System.err.println("[DASHBOARD ERROR - Users] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            model.addAttribute("totalUsers", 0);
            model.addAttribute("users", new ArrayList<>());
        }

        try {
            model.addAttribute("totalActions", charityActionRepository.count());
            model.addAttribute("actions", charityActionRepository.findAll());

            List<Donation> donations = donationRepository.findAll();
            List<Donation> sortedDonations = new ArrayList<>(donations);
            sortedDonations.sort((d1, d2) -> {
                if (d1.getDate() == null || d2.getDate() == null) return 0;
                return d2.getDate().compareTo(d1.getDate());
            });
            model.addAttribute("donations", sortedDonations);

            double totalDonationsAmount = donations.stream()
                    .mapToDouble(d -> d.getAmount() != null ? d.getAmount() : 0.0)
                    .sum();
            model.addAttribute("totalDonationsAmount", totalDonationsAmount);
 
            // Gestion des organisations
            List<com.devbuild.renko.entities.Organization> allOrgs = organizationRepository.findAll();
            model.addAttribute("totalOrgs", allOrgs.size());
            model.addAttribute("organizations", allOrgs);
            model.addAttribute("pendingOrgs", allOrgs.stream().filter(o -> !o.isApproved()).toList());

        } catch (Exception e) {
            System.err.println("[DASHBOARD ERROR - Actions/Donations] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            model.addAttribute("totalActions", 0);
            model.addAttribute("actions", new ArrayList<>());
            model.addAttribute("donations", new ArrayList<>());
            model.addAttribute("totalDonationsAmount", 0.0);
        }

        model.addAttribute("pageTitle", "Dashboard Admin");
        return "admin/dashboard";
    }

    @GetMapping("/actions/new")
    public String newActionForm(Model model) {
        model.addAttribute("action", new CharityAction());
        return "admin/action-form";
    }

    @GetMapping({"/actions/edit/{id}", "/actions/edit/"})
    public String editActionForm(@PathVariable(required = false) String id, Model model) {
        if (id == null || id.trim().isEmpty())
            return "redirect:/admin/dashboard?error=invalid_id";
        CharityAction action = charityActionRepository.findById(id).orElseThrow();
        model.addAttribute("action", action);
        return "admin/action-form";
    }

    @PostMapping("/actions/save")
    public String saveAction(@ModelAttribute CharityAction action,
            @RequestParam("imageFile") org.springframework.web.multipart.MultipartFile imageFile)
            throws java.io.IOException {

        if (action.getId() != null && action.getId().trim().isEmpty()) {
            action.setId(null);
        }

        if (!imageFile.isEmpty()) {
            String originalFilename = imageFile.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty())
                originalFilename = "initiative_" + System.currentTimeMillis();
            
            String fileName = org.springframework.util.StringUtils.cleanPath(originalFilename);
            String idForFolder = (action.getId() != null) ? action.getId() : "new_" + System.currentTimeMillis();
            String uploadDir = "uploads/actions/" + idForFolder;
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);

            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            try (java.io.InputStream inputStream = imageFile.getInputStream()) {
                java.nio.file.Path filePath = uploadPath.resolve(fileName);
                java.nio.file.Files.copy(inputStream, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                action.setMainImageUrl("/" + uploadDir + "/" + fileName);
            }
        } else if (action.getId() != null) {
            charityActionRepository.findById(action.getId()).ifPresent(old -> {
                action.setMainImageUrl(java.util.Objects.requireNonNullElse(old.getMainImageUrl(), ""));
            });
        }

        if (action.getCurrentAmount() == null) action.setCurrentAmount(0.0);
        action.setArchived(false);
        
        // Ensure organizationId is set (use first available if null)
        if (action.getOrganizationId() == null) {
            organizationRepository.findAll().stream().findFirst().ifPresent(org -> action.setOrganizationId(org.getId()));
        }

        charityActionRepository.save(action);
        
        String title = action.getTitle();
        emailService.broadcastInitiativeUpdate(title != null ? title : "Nouvelle Initiative", "Mise à jour de l'initiative.");
        return "redirect:/admin/dashboard?success=action_saved";
    }

    @GetMapping({"/actions/delete/{id}", "/actions/delete/"})
    public String deleteAction(@PathVariable(required = false) String id) {
        if (id != null && !id.trim().isEmpty()) {
            charityActionRepository.deleteById(id);
        }
        return "redirect:/admin/dashboard?success=action_deleted";
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        try {
            userRepository.deleteById(id);
        } catch (Exception e) {
            System.err.println("Error deleting user: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?success=user_deleted";
    }

    @GetMapping("/donations/delete/{id}")
    public String deleteDonation(@PathVariable String id) {
        Donation donation = donationRepository.findById(id).orElseThrow();
        Long userId = donation.getUserId();
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.setBalance(user.getBalance() + donation.getAmount());
                userRepository.save(user);
            }
        }
        String actionId = donation.getCharityActionId();
        if (actionId != null) {
            CharityAction action = charityActionRepository.findById(actionId).orElse(null);
            if (action != null) {
                action.setCurrentAmount(action.getCurrentAmount() - donation.getAmount());
                charityActionRepository.save(action);
            }
        }
        donationRepository.deleteById(id);
        return "redirect:/admin/dashboard?success=donation_deleted";
    }
    @PostMapping("/organizations/approve/{id}")
    public String approveOrganization(@PathVariable Long id) {
        organizationRepository.findById(id).ifPresent(org -> {
            org.setApproved(true);
            organizationRepository.save(org);
            
            // Notification Email
            if (org.getAdminUser() != null) {
                emailService.sendNotification(
                    org.getAdminUser().getEmail(),
                    "Félicitations ! Votre organisation a été approuvée",
                    "Bonjour " + org.getName() + ",\n\nVotre demande d'adhésion à Renko Charity a été acceptée. Vous pouvez maintenant créer des initiatives."
                );
            }
        });
        return "redirect:/admin/dashboard?success=org_approved";
    }

    @PostMapping("/organizations/reject/{id}")
    public String rejectOrganization(@PathVariable Long id) {
        organizationRepository.findById(id).ifPresent(org -> {
            String email = (org.getAdminUser() != null) ? org.getAdminUser().getEmail() : null;
            String orgName = org.getName();
            
            // On dissocie l'utilisateur pour éviter les problèmes de contrainte
            if (org.getAdminUser() != null) {
                org.setAdminUser(null);
                organizationRepository.save(org);
            }
            
            organizationRepository.deleteById(id);
            
            // Notification Email
            if (email != null) {
                emailService.sendNotification(
                    email,
                    "Mise à jour concernant votre demande d'organisation",
                    "Bonjour " + orgName + ",\n\nNous avons le regret de vous informer que votre demande n'a pas été retenue pour le moment."
                );
            }
        });
        return "redirect:/admin/dashboard?success=org_rejected";
    }

    @GetMapping("/organizations/delete/{id}")
    public String deleteOrganization(@PathVariable("id") Long id) {
        try {
            organizationRepository.findById(id).ifPresent(organization -> {
                // Dissocier l'utilisateur admin pour éviter les contraintes FK
                if (organization.getAdminUser() != null) {
                    organization.setAdminUser(null);
                    organizationRepository.save(organization);
                }
                organizationRepository.deleteById(id);
            });
            return "redirect:/admin/dashboard?success=org_deleted";
        } catch (Exception e) {
            System.err.println("ERREUR suppression org: " + e.getMessage());
            return "redirect:/admin/dashboard?error=delete_failed";
        }
    }

    @GetMapping("/organizations/edit/{id}")
    public String editOrganizationForm(@PathVariable("id") Long id, Model model) {
        organizationRepository.findById(id).ifPresentOrElse(
            organization -> model.addAttribute("organization", organization),
            () -> model.addAttribute("error", "Organization not found")
        );
        return "admin/organization-edit-form";
    }

    @PostMapping("/organizations/save")
    public String saveOrganization(@ModelAttribute com.devbuild.renko.entities.Organization organization,
                                   @RequestParam(value = "logoFile", required = false) org.springframework.web.multipart.MultipartFile logoFile) {
        
        try {
            organizationRepository.findById(organization.getId()).ifPresent(existing -> {
                organization.setAdminUser(existing.getAdminUser());
                organization.setApproved(existing.isApproved());
                
                if (logoFile != null && !logoFile.isEmpty()) {
                    try {
                        String fileName = org.springframework.util.StringUtils.cleanPath(logoFile.getOriginalFilename());
                        String uploadDir = "uploads/organizations/" + organization.getName().replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
                        java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
                        if (!java.nio.file.Files.exists(uploadPath)) java.nio.file.Files.createDirectories(uploadPath);
                        try (java.io.InputStream inputStream = logoFile.getInputStream()) {
                            java.nio.file.Path filePath = uploadPath.resolve(fileName);
                            java.nio.file.Files.copy(inputStream, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            organization.setLogoUrl("/" + uploadDir + "/" + fileName);
                        }
                    } catch (java.io.IOException e) { e.printStackTrace(); }
                } else {
                    organization.setLogoUrl(existing.getLogoUrl());
                }
            });
            
            organizationRepository.save(organization);
            return "redirect:/admin/dashboard?success=org_updated";
        } catch (Exception e) {
            System.err.println("Error saving organization: " + e.getMessage());
            return "redirect:/admin/dashboard?error=save_failed";
        }
    }
}
