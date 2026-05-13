package com.devbuild.renko.scratch;

import com.devbuild.renko.repos.CharityActionRepository;
import com.devbuild.renko.repos.DonationRepository;
import com.devbuild.renko.repos.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInspector implements CommandLineRunner {
    private final CharityActionRepository actionRepo;
    private final DonationRepository donationRepo;
    private final UserRepository userRepo;

    public DataInspector(CharityActionRepository actionRepo, DonationRepository donationRepo, UserRepository userRepo) {
        this.actionRepo = actionRepo;
        this.donationRepo = donationRepo;
        this.userRepo = userRepo;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== DATA INSPECTION ===");
        try {
            System.out.println("Users: " + userRepo.count());
            userRepo.findAll().forEach(u -> System.out.println("User: ID=" + u.getId() + ", Email=" + u.getEmail() + ", Name=" + u.getFirstName()));
            
            System.out.println("Actions: " + actionRepo.count());
            actionRepo.findAll().forEach(a -> System.out.println("Action: ID=" + a.getId() + ", Title=" + a.getTitle()));
            
            System.out.println("Donations: " + donationRepo.count());
            donationRepo.findAll().forEach(d -> System.out.println("Donation: ID=" + d.getId() + ", UserID=" + d.getUserId() + ", ActionID=" + d.getCharityActionId()));
        } catch (Exception e) {
            System.err.println("Inspection Error: " + e.getMessage());
        }
        System.out.println("=======================");
    }
}
