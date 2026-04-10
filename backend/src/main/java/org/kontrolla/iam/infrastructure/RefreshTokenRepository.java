package org.kontrolla.iam.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.kontrolla.iam.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for refresh token persistence and lookup. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  /**
   * Returns all refresh tokens owned by a user.
   *
   * @param userId the user identifier
   * @return the user's refresh tokens
   */
  List<RefreshToken> findAllByUser_Id(UUID userId);

  /**
   * Finds a refresh token by its hashed token value.
   *
   * @param tokenHash the hashed token
   * @return the matching refresh token, if present
   */
  Optional<RefreshToken> findByTokenHash(String tokenHash);
}
