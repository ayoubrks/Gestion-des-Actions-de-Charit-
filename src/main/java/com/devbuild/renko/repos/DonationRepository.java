package com.devbuild.renko.repos;

import com.devbuild.renko.entities.Donation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationRepository extends MongoRepository<Donation, String> {
    List<Donation> findByUserId(Long userId);
    List<Donation> findByCharityActionId(String charityActionId);
}
