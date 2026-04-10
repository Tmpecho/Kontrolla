package org.kontrolla.iam.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.kontrolla.iam.domain.UserInvite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for user invite tokens. */
public interface UserInviteRepository extends JpaRepository<UserInvite, UUID> {

  /**
   * Finds an invite by hashed token with related user and organization loaded.
   *
   * @param tokenHash the hashed token
   * @return the matching invite, if present
   */
  @EntityGraph(attributePaths = {"user", "organization"})
  Optional<UserInvite> findByTokenHash(String tokenHash);
}
