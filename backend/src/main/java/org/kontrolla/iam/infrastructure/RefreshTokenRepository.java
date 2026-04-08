package org.kontrolla.iam.infrastructure;

import org.kontrolla.iam.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

	List<RefreshToken> findAllByUser_Id(UUID userId);

	Optional<RefreshToken> findByTokenHash(String tokenHash);
}
