package com.devbuild.renko.config;

import com.devbuild.renko.entities.CharityAction;
import com.devbuild.renko.entities.Organization;
import com.devbuild.renko.entities.Role;
import com.devbuild.renko.entities.User;
import com.devbuild.renko.repos.CharityActionRepository;
import com.devbuild.renko.repos.OrganizationRepository;
import com.devbuild.renko.repos.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final CharityActionRepository charityActionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            // Create Admin
            User admin = User.builder()
                    .firstName("Super")
                    .lastName("Admin")
                    .email("admin@renko.dev")
                    .password(passwordEncoder.encode("admin"))
                    .role(Role.ROLE_SUPER_ADMIN)
                    .balance(1000.0)
                    .build();
            if (admin != null) {
                userRepository.save(admin);
            }

            // Create Org Admin
            User orgAdmin = User.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("org@renko.dev")
                    .password(passwordEncoder.encode("org"))
                    .role(Role.ROLE_ORG_ADMIN)
                    .balance(500.0)
                    .build();
            if (orgAdmin != null) {
                userRepository.save(orgAdmin);
            }

            // Create Organization
            Organization org = Organization.builder()
                    .name("Hope Foundation")
                    .legalAddress("123 Charity St, Paris")
                    .taxId("FR123456789")
                    .mainContact("contact@hope.org")
                    .description("Providing education and health support.")
                    .approved(true)
                    .adminUser(orgAdmin)
                    .build();
            if (org != null) {
                organizationRepository.save(org);

                // Create Charity Actions (MongoDB)
                if (charityActionRepository.count() == 0) {
                    Long orgId = org.getId();
                    if (orgId != null) {
                        List<CharityAction> actions = java.util.List.of(
                                CharityAction.builder()
                                        .organizationId(orgId)
                                        .title("Construction d'une école au Sénégal")
                                        .category("Education")
                                        .description("Aidez-nous à construire une école primaire pour 200 enfants.")
                                        .date(LocalDate.now().plusMonths(2))
                                        .location("Dakar, Sénégal")
                                        .targetAmount(50000.0)
                                        .currentAmount(15000.0)
                                        .mediaUrls(java.util.List.of("https://images.unsplash.com/photo-1509062522246-3755977927d7?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"))
                                        .isArchived(false)
                                        .build(),
                                CharityAction.builder()
                                        .organizationId(orgId)
                                        .title("Aide médicale d'urgence")
                                        .category("Santé")
                                        .description("Fourniture de kits médicaux pour les zones touchées par la crise.")
                                        .date(LocalDate.now().plusWeeks(1))
                                        .location("Gaza")
                                        .targetAmount(20000.0)
                                        .currentAmount(18500.0)
                                        .mediaUrls(java.util.List.of("https://images.unsplash.com/photo-1532938911079-1b06ac7ceec7?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"))
                                        .isArchived(false)
                                        .build(),
                                CharityAction.builder()
                                        .organizationId(orgId)
                                        .title("Plantation de 10 000 arbres")
                                        .category("Environnement")
                                        .description("Initiative de reforestation pour lutter contre le changement climatique.")
                                        .date(LocalDate.now().plusMonths(6))
                                        .location("Amazonie, Brésil")
                                        .targetAmount(10000.0)
                                        .currentAmount(2500.0)
                                        .mediaUrls(java.util.List.of("https://images.unsplash.com/photo-1542601906990-b4d3fb778b09?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"))
                                        .isArchived(false)
                                        .build()
                        );

                        if (actions != null) {
                            charityActionRepository.saveAll(actions);
                        }
                    }
                }
            }
        } else {
            // Ensure Admin has correct role if DB already seeded
            userRepository.findByEmail("admin@renko.dev").ifPresent(user -> {
                if (user.getRole() != Role.ROLE_SUPER_ADMIN) {
                    user.setRole(Role.ROLE_SUPER_ADMIN);
                    userRepository.save(user);
                }
            });
        }
    }
}
