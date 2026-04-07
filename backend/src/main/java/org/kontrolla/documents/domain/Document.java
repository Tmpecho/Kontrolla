package org.kontrolla.documents.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.iam.domain.User;
import org.kontrolla.organizations.domain.Organization;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "documents")
public class Document extends AbstractAuditableUuidEntity {

	public static final int DEFAULT_EXPIRY_WARNING_DAYS = 30;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "establishment_id", nullable = false)
	private Establishment establishment;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_user_id", nullable = false)
	private User createdByUser;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "service_area", nullable = false, length = 32)
	private DocumentServiceArea serviceArea;

	@Setter
	@Column(nullable = false, length = 255)
	private String title;

	@Setter
	@Column(name = "holder_name", nullable = false, length = 255)
	private String holderName;

	@Setter
	@Column(name = "issue_date", nullable = false)
	private LocalDate issueDate;

	@Setter
	@Column(name = "renewal_date", nullable = false)
	private LocalDate renewalDate;

	protected Document() {
	}

	public Document(
			Organization organization,
			Establishment establishment,
			User createdByUser,
			DocumentServiceArea serviceArea,
			String title,
			String holderName,
			LocalDate issueDate,
			LocalDate renewalDate
	) {
		this.organization = organization;
		this.establishment = establishment;
		this.createdByUser = createdByUser;
		this.serviceArea = serviceArea;
		this.title = title;
		this.holderName = holderName;
		this.issueDate = issueDate;
		this.renewalDate = renewalDate;
	}

	public DocumentStatus getStatus(LocalDate today) {
		return getStatus(today, DEFAULT_EXPIRY_WARNING_DAYS);
	}

	public DocumentStatus getStatus(LocalDate today, int warningDays) {
		if (warningDays < 0) {
			throw new IllegalArgumentException("warningDays must be non-negative");
		}

		if (renewalDate.isBefore(today)) {
			return DocumentStatus.EXPIRED;
		}

		if (!renewalDate.isAfter(today.plusDays(warningDays))) {
			return DocumentStatus.EXPIRING;
		}

		return DocumentStatus.VALID;
	}
}
