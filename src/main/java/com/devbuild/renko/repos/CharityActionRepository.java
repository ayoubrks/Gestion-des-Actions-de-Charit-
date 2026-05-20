package com.devbuild.renko.repos;

import com.devbuild.renko.entities.CharityAction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharityActionRepository extends MongoRepository<CharityAction, String> {
    List<CharityAction> findByOrganizationId(Long organizationId);
    List<CharityAction> findByCategoryAndIsArchivedFalse(String category);
    List<CharityAction> findByIsArchivedFalse();
    List<CharityAction> findByOrganizationIdIsNullAndIsArchivedFalse();
    List<CharityAction> findByCategoryAndOrganizationIdIsNullAndIsArchivedFalse(String category);

    @Query("{ 'isArchived': false, '$or': [ { 'title': { '$regex': ?0, '$options': 'i' } }, { 'description': { '$regex': ?0, '$options': 'i' } }, { 'location': { '$regex': ?0, '$options': 'i' } } ] }")
    List<CharityAction> searchWithoutCategory(String search);

    @Query("{ 'isArchived': false, 'category': ?0, '$or': [ { 'title': { '$regex': ?1, '$options': 'i' } }, { 'description': { '$regex': ?1, '$options': 'i' } }, { 'location': { '$regex': ?1, '$options': 'i' } } ] }")
    List<CharityAction> searchWithCategory(String category, String search);
}
