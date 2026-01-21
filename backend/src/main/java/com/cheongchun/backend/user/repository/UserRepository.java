package com.cheongchun.backend.user.repository;

import com.cheongchun.backend.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderTypeAndProviderId(User.ProviderType providerType, String providerId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
