package com.cheongchun.backend.repository;

import com.cheongchun.backend.entity.SocialAccount;
import com.cheongchun.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    
    Optional<SocialAccount> findByProviderAndProviderId(SocialAccount.Provider provider, String providerId);
    
    Optional<SocialAccount> findByUserAndProvider(User user, SocialAccount.Provider provider);
}