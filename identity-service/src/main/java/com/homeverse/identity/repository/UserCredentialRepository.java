package com.homeverse.identity.repository;

import com.homeverse.identity.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    Optional<UserCredential> findByEmail(String email);
    boolean existsByEmail(String email);
    List<UserCredential> findByKycStatus(String kycStatus);
}