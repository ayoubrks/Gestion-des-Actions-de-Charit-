package com.devbuild.renko.controller;

import com.devbuild.renko.repos.OrganizationRepository;
import com.devbuild.renko.repos.UserRepository;
import com.devbuild.renko.entities.Organization;
import com.devbuild.renko.entities.User;
import com.devbuild.renko.entities.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    @GetMapping("/organizations")
    public String listOrganizations(Model model) {
        model.addAttribute("organizations", organizationRepository.findByApproved(true));
        return "organizations";
    }

    @GetMapping("/organizations/new")
    public String registerForm(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user != null && user.getOrganization() != null) {
                return "redirect:/profile?error=already_has_organization";
            }
        }
        return "organization-form";
    }

    @PostMapping("/organizations/new")
    public String registerSubmit(@ModelAttribute Organization organization, 
                                @RequestParam("logoFile") MultipartFile logoFile,
                                Authentication authentication) throws IOException {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        organization.setAdminUser(user);
        organization.setApproved(false);

        if (!logoFile.isEmpty()) {
            String originalFilename = logoFile.getOriginalFilename();
            String fileName = StringUtils.cleanPath(originalFilename != null ? originalFilename : "logo_" + System.currentTimeMillis());
            
            // Temporary ID if not saved yet, or use name-based folder
            String folderName = organization.getName().replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
            String uploadDir = "uploads/organizations/" + folderName;
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            try (java.io.InputStream inputStream = logoFile.getInputStream()) {
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                organization.setLogoUrl("/" + uploadDir + "/" + fileName);
            }
        }

        try {
            if (organization != null) {
                organizationRepository.save(organization);
            }
            
            user.setRole(Role.ROLE_ORG_ADMIN);
            if (user != null) {
                userRepository.save(user);
            }

            // Update Security Context to reflect new role immediately
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    authentication.getPrincipal(),
                    authentication.getCredentials(),
                    List.of(new SimpleGrantedAuthority(user.getRole().name()))
                )
            );
            
            return "redirect:/organizations?success=registration";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // This happens if the user already has an organization or if taxId is not unique
            return "redirect:/organizations/new?error=duplicate_or_invalid";
        }
    }
}
