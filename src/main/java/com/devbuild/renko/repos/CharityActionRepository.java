package com.devbuild.renko.repos;

import com.devbuild.renko.entities.CharityAction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharityActionRepository extends MongoRepository<CharityAction, String> {
    List<CharityAction> findByOrganizationId(Long organizationId);
    List<CharityAction> findByCategoryAndIsArchivedFalse(String category);
    List<CharityAction> findByIsArchivedFalse();
    List<CharityAction> findByOrganizationIdIsNullAndIsArchivedFalse();
    List<CharityAction> findByCategoryAndOrganizationIdIsNullAndIsArchivedFalse(String category);
}
