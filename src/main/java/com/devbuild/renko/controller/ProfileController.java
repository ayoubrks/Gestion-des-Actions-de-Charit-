package com.devbuild.renko.controller;

import com.devbuild.renko.entities.User;
import com.devbuild.renko.repos.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        return "profile";
    }

    @GetMapping("/profile/settings")
    public String settings(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        return "settings";
    }



    @PostMapping("/profile/upload-image")
    public String uploadImage(@AuthenticationPrincipal UserDetails userDetails, 
                            @RequestParam("profileImage") org.springframework.web.multipart.MultipartFile multipartFile) throws java.io.IOException {
        if (userDetails == null) return "redirect:/login";
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (multipartFile.isEmpty()) {
            return "redirect:/profile/settings?error=empty_file";
        }

        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null) originalFilename = "profile_" + System.currentTimeMillis();
        String fileName = org.springframework.util.StringUtils.cleanPath(originalFilename);
        // Use a consistent naming or store relative path
        String relativePath = "/uploads/profiles/" + user.getId() + "/" + fileName;
        user.setProfileImageUrl(relativePath);
        userRepository.save(user);

        String uploadDir = "uploads/profiles/" + user.getId();
        java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);

        if (!java.nio.file.Files.exists(uploadPath)) {
            java.nio.file.Files.createDirectories(uploadPath);
        }

        try (java.io.InputStream inputStream = multipartFile.getInputStream()) {
            java.nio.file.Path filePath = uploadPath.resolve(fileName);
            java.nio.file.Files.copy(inputStream, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException ioe) {
            throw new java.io.IOException("Could not save image file: " + fileName, ioe);
        }

        return "redirect:/profile?success=upload";
    }
}
