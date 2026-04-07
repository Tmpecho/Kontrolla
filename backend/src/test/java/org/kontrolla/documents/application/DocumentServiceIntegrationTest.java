package org.kontrolla.documents.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.common.exception.ApplicationException;
import org.kontrolla.common.exception.ForbiddenException;
import org.kontrolla.documents.domain.Document;
import org.kontrolla.documents.domain.DocumentServiceArea;
import org.kontrolla.documents.domain.DocumentStatus;
import org.kontrolla.documents.infrastructure.DocumentRepository;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.domain.OrganizationRole;
import org.kontrolla.organizations.domain.OrganizationStatus;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.kontrolla.organizations.infrastructure.OrganizationRepository;
import org.kontrolla.support.TestDataCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class DocumentServiceIntegrationTest {

	@Autowired
	private DocumentService documentService;

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private EstablishmentRepository establishmentRepository;

	@Autowired
	private OrganizationMembershipRepository organizationMembershipRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TestDataCleaner testDataCleaner;

	@Autowired
	private Clock clock;

	@BeforeEach
	void setUp() {
		testDataCleaner.clearAll();
	}

	@Test
	void managerCanCreateUpdateAndFilterDocumentsByServiceArea() {
		User manager = createUser("documents-manager@example.com", "Manager", "Documents", true);
		Organization organization = createOrganization("Kontrolla Documents");
		Establishment establishment = createEstablishment(organization, "Main Bar");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);
		LocalDate today = LocalDate.now(clock);

		Document createdDocument = documentService.createDocument(
				organization.getId(),
				establishment.getId(),
				DocumentServiceArea.IK_ALKOHOL,
				"  Alcohol service licence  ",
				"  Oslo Municipality  ",
				today.minusDays(365),
				today.plusDays(7),
				currentUser(manager)
		);
		documentService.createDocument(
				organization.getId(),
				establishment.getId(),
				DocumentServiceArea.IK_MAT,
				"HACCP binder",
				"Kitchen operations",
				today.minusDays(120),
				today.plusDays(60),
				currentUser(manager)
		);

		Document updatedDocument = documentService.updateDocument(
				organization.getId(),
				establishment.getId(),
				createdDocument.getId(),
				DocumentServiceArea.IK_ALKOHOL,
				"  Alcohol service licence 2026  ",
				"  Oslo Municipality Licensing  ",
				today.minusDays(365),
				today.plusDays(45),
				currentUser(manager)
		);

		Page<Document> alkoholDocuments = documentService.listDocuments(
				organization.getId(),
				establishment.getId(),
				DocumentServiceArea.IK_ALKOHOL,
				currentUser(manager),
				PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "renewalDate"))
		);

		assertThat(updatedDocument.getTitle()).isEqualTo("Alcohol service licence 2026");
		assertThat(updatedDocument.getHolderName()).isEqualTo("Oslo Municipality Licensing");
		assertThat(updatedDocument.getStatus(today)).isEqualTo(DocumentStatus.VALID);
		assertThat(alkoholDocuments.getContent()).hasSize(1);
		assertThat(alkoholDocuments.getContent().getFirst().getId()).isEqualTo(createdDocument.getId());
		assertThat(alkoholDocuments.getContent().getFirst().getCreatedByUser().getId()).isEqualTo(manager.getId());
		assertThat(documentRepository.findAll()).hasSize(2);
	}

	@Test
	void invalidDateRangeIsRejectedForCreateAndUpdate() {
		User manager = createUser("documents-dates@example.com", "Manager", "Dates", true);
		Organization organization = createOrganization("Kontrolla Documents Dates");
		Establishment establishment = createEstablishment(organization, "Date Validation");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);
		LocalDate today = LocalDate.now(clock);

		assertThatThrownBy(() -> documentService.createDocument(
				organization.getId(),
				establishment.getId(),
				DocumentServiceArea.IK_ALKOHOL,
				"Responsible service certificate",
				"Lina Dahl",
				today,
				today.minusDays(1),
				currentUser(manager)
		))
				.isInstanceOf(ApplicationException.class)
				.hasMessage("Renewal date cannot be before issue date");

		Document createdDocument = documentService.createDocument(
				organization.getId(),
				establishment.getId(),
				DocumentServiceArea.IK_ALKOHOL,
				"Responsible service certificate",
				"Lina Dahl",
				today.minusDays(180),
				today.plusDays(5),
				currentUser(manager)
		);

		assertThatThrownBy(() -> documentService.updateDocument(
				organization.getId(),
				establishment.getId(),
				createdDocument.getId(),
				DocumentServiceArea.IK_ALKOHOL,
				"Responsible service certificate",
				"Lina Dahl",
				today,
				today.minusDays(2),
				currentUser(manager)
		))
				.isInstanceOf(ApplicationException.class)
				.hasMessage("Renewal date cannot be before issue date");
	}

	@Test
	void employeeCannotCreateOrUpdateDocuments() {
		User manager = createUser("documents-manager-access@example.com", "Manager", "Access", true);
		User employee = createUser("documents-employee@example.com", "Employee", "Access", true);
		Organization organization = createOrganization("Kontrolla Documents Access");
		Establishment establishment = createEstablishment(organization, "Access Check");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE, true);
		LocalDate today = LocalDate.now(clock);

		Document createdDocument = documentService.createDocument(
				organization.getId(),
				establishment.getId(),
				DocumentServiceArea.IK_ALKOHOL,
				"Age control routine acknowledgement",
				"Bar team",
				today.minusDays(90),
				today.plusDays(30),
				currentUser(manager)
		);

		assertThatThrownBy(() -> documentService.createDocument(
				organization.getId(),
				establishment.getId(),
				DocumentServiceArea.IK_ALKOHOL,
				"Staff permit register",
				"People operations",
				today.minusDays(180),
				today.plusDays(10),
				currentUser(employee)
		)).isInstanceOf(ForbiddenException.class);

		assertThatThrownBy(() -> documentService.updateDocument(
				organization.getId(),
				establishment.getId(),
				createdDocument.getId(),
				DocumentServiceArea.IK_ALKOHOL,
				"Updated title",
				"Updated holder",
				today.minusDays(90),
				today.plusDays(40),
				currentUser(employee)
		)).isInstanceOf(ForbiddenException.class);
	}

	private User createUser(String email, String firstName, String lastName, boolean active) {
		User user = new User(email, firstName, lastName, "hashed-password", active, Set.of());
		return userRepository.saveAndFlush(user);
	}

	private Organization createOrganization(String name) {
		Organization organization = new Organization(name, OrganizationStatus.ACTIVE);
		return organizationRepository.saveAndFlush(organization);
	}

	private Establishment createEstablishment(Organization organization, String name) {
		Establishment establishment = new Establishment(
				organization,
				name,
				EstablishmentType.BAR,
				EstablishmentStatus.ACTIVE
		);
		return establishmentRepository.saveAndFlush(establishment);
	}

	private void createMembership(Organization organization, User user, OrganizationRole role, boolean active) {
		OrganizationMembership membership = new OrganizationMembership(organization, user, role, active);
		organizationMembershipRepository.saveAndFlush(membership);
	}

	private CurrentUser currentUser(User user) {
		return new CurrentUser(user.getId(), user.getEmail(), user.getGlobalRoles());
	}
}
