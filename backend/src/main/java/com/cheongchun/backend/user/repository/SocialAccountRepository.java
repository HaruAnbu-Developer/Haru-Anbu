package com.cheongchun.backend.user.repository;

import com.cheongchun.backend.user.domain.SocialAccount;
import com.cheongchun.backend.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderId(SocialAccount.Provider provider, String providerId);

    Optional<SocialAccount> findByUserAndProvider(User user, SocialAccount.Provider provider);
}
