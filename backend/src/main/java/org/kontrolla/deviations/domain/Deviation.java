package org.kontrolla.deviations.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.iam.domain.User;

@Getter
@Entity
@Table(name = "deviations")
public class Deviation extends AbstractAuditableUuidEntity {

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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedToUser;

    @Setter
    @Column(nullable = false, length = 255)
    private String title;

    @Setter
    @Column(nullable = false, length = 2000)
    private String description;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeviationStatus status;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeviationSeverity severity;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeviationCategory category;

    protected Deviation() {
    }

    public Deviation(
        Organization organization,
        Establishment establishment,
        User createdByUser,
        User assignedToUser,
        String title,
        String description,
        DeviationSeverity severity,
        DeviationCategory category
    ) {
        this.organization = organization;
        this.establishment = establishment;
        this.createdByUser = createdByUser;
        this.assignedToUser = assignedToUser;
        this.title = title;
        this.description = description;
        this.status = DeviationStatus.OPEN;
        this.severity = severity;
        this.category = category;
    }



}

