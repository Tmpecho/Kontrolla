package org.kontrolla.iam.infrastructure;

import org.kontrolla.iam.domain.UserInvite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserInviteRepository extends JpaRepository<UserInvite, UUID> {

	@EntityGraph(attributePaths = {"user", "organization"})
	Optional<UserInvite> findByTokenHash(String tokenHash);
}
