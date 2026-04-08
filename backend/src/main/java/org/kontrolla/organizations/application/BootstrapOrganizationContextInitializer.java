package org.kontrolla.organizations.application;

import org.kontrolla.checklists.application.ChecklistSchedulerService;
import org.kontrolla.checklists.domain.ChecklistDefinition;
import org.kontrolla.checklists.domain.ChecklistDefinitionStatus;
import org.kontrolla.checklists.domain.ChecklistSchedule;
import org.kontrolla.checklists.domain.ChecklistScheduleType;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.checklists.domain.ChecklistTaskDefinition;
import org.kontrolla.checklists.domain.ChecklistTaskKind;
import org.kontrolla.checklists.infrastructure.ChecklistDefinitionRepository;
import org.kontrolla.deviations.domain.Deviation;
import org.kontrolla.deviations.domain.DeviationCategory;
import org.kontrolla.deviations.domain.DeviationEvent;
import org.kontrolla.deviations.domain.DeviationEventType;
import org.kontrolla.deviations.domain.DeviationSeverity;
import org.kontrolla.deviations.domain.DeviationStatus;
import org.kontrolla.deviations.infrastructure.DeviationRepository;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.AppSecurityProperties;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.domain.OrganizationRole;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.kontrolla.organizations.infrastructure.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@Profile("dev")
@Order(20)
public class BootstrapOrganizationContextInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BootstrapOrganizationContextInitializer.class);
	private static final ZoneId DEFAULT_BOOTSTRAP_ZONE = ZoneId.of("Europe/Oslo");
	private static final String IK_MAT_TITLE_SUFFIX = " Daily IK-Mat";
	private static final String IK_MAT_CLOSING_TITLE_SUFFIX = " Closing IK-Mat";
	private static final String IK_ALKOHOL_TITLE_SUFFIX = " Daily IK-Alkohol";
	private static final String IK_ALKOHOL_CLOSING_TITLE_SUFFIX = " Closing IK-Alkohol";

	private final UserRepository userRepository;
	private final OrganizationRepository organizationRepository;
	private final OrganizationMembershipRepository organizationMembershipRepository;
	private final EstablishmentRepository establishmentRepository;
	private final ChecklistDefinitionRepository checklistDefinitionRepository;
	private final ChecklistSchedulerService checklistSchedulerService;
	private final DeviationRepository deviationRepository;
	private final AppSecurityProperties properties;

	public BootstrapOrganizationContextInitializer(
			UserRepository userRepository,
			OrganizationRepository organizationRepository,
			OrganizationMembershipRepository organizationMembershipRepository,
			EstablishmentRepository establishmentRepository,
			ChecklistDefinitionRepository checklistDefinitionRepository,
			ChecklistSchedulerService checklistSchedulerService,
			DeviationRepository deviationRepository,
			AppSecurityProperties properties
	) {
		this.userRepository = userRepository;
		this.organizationRepository = organizationRepository;
		this.organizationMembershipRepository = organizationMembershipRepository;
		this.establishmentRepository = establishmentRepository;
		this.checklistDefinitionRepository = checklistDefinitionRepository;
		this.checklistSchedulerService = checklistSchedulerService;
		this.deviationRepository = deviationRepository;
		this.properties = properties;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		String organizationName = Optional.ofNullable(properties.getBootstrapOrganization().getName()).orElse("").trim();

		if (organizationName.isBlank()) {
			return;
		}

		Organization organization = organizationRepository.findByNameIgnoreCase(organizationName)
				.map(existing -> {
					existing.setStatus(properties.getBootstrapOrganization().getStatus());
					return existing;
				})
				.orElseGet(() -> {
					Organization created = new Organization(
							organizationName,
							properties.getBootstrapOrganization().getStatus()
					);
					organizationRepository.save(created);
					log.info("Created bootstrap organization {}", organizationName);
					return created;
				});

		List<Establishment> bootstrapEstablishments = resolveBootstrapEstablishments().stream()
				.map(establishment -> upsertEstablishment(organization, organizationName, establishment))
				.filter(Objects::nonNull)
				.toList();

		upsertMembership(
				properties.getBootstrapAdmin().getEmail(),
				organization,
				OrganizationRole.ORG_ADMIN,
				organizationName,
				true,
				List.of()
		);
		upsertMembership(
				properties.getBootstrapUser().getEmail(),
				organization,
				OrganizationRole.ORG_MANAGER,
				organizationName,
				true,
				List.of()
		);

		List<AppSecurityProperties.BootstrapUser> bootstrapEmployees = properties.getBootstrapEmployees();
		for (int index = 0; index < bootstrapEmployees.size(); index++) {
			AppSecurityProperties.BootstrapUser employee = bootstrapEmployees.get(index);
			boolean accessAllEstablishments = bootstrapEstablishments.isEmpty();
			List<Establishment> accessibleEstablishments = accessAllEstablishments
					? List.of()
					: List.of(bootstrapEstablishments.get(index % bootstrapEstablishments.size()));
			upsertMembership(
					employee.getEmail(),
					organization,
					OrganizationRole.ORG_EMPLOYEE,
					organizationName,
					accessAllEstablishments,
					accessibleEstablishments
			);
		}

		resolveBootstrapChecklistActor()
				.ifPresent(actor -> {
					bootstrapChecklistRuns(organization, bootstrapEstablishments, actor);
					bootstrapDeviations(organization, bootstrapEstablishments, actor);
				});
	}

	private List<AppSecurityProperties.BootstrapEstablishment> resolveBootstrapEstablishments() {
		List<AppSecurityProperties.BootstrapEstablishment> configuredEstablishments = properties.getBootstrapEstablishments().stream()
				.filter(establishment -> !Optional.ofNullable(establishment.getName()).orElse("").trim().isBlank())
				.toList();

		if (!configuredEstablishments.isEmpty()) {
			return configuredEstablishments;
		}

		String fallbackEstablishmentName = Optional.ofNullable(properties.getBootstrapEstablishment().getName()).orElse("").trim();
		if (fallbackEstablishmentName.isBlank()) {
			return List.of();
		}

		return List.of(properties.getBootstrapEstablishment());
	}

	private Establishment upsertEstablishment(
			Organization organization,
			String organizationName,
			AppSecurityProperties.BootstrapEstablishment bootstrapEstablishment
	) {
		String establishmentName = Optional.ofNullable(bootstrapEstablishment.getName()).orElse("").trim();
		if (establishmentName.isBlank()) {
			return null;
		}

		return establishmentRepository.findFirstByOrganizationIdAndNameIgnoreCase(organization.getId(), establishmentName)
				.map(existing -> {
					existing.setType(bootstrapEstablishment.getType());
					existing.setStatus(bootstrapEstablishment.getStatus());
					return existing;
				})
				.orElseGet(() -> {
					Establishment created = new Establishment(
							organization,
							establishmentName,
							bootstrapEstablishment.getType(),
							bootstrapEstablishment.getStatus()
					);
					establishmentRepository.save(created);
					log.info("Created bootstrap establishment {} in {}", establishmentName, organizationName);
					return created;
				});
	}

	private void upsertMembership(
			String email,
			Organization organization,
			OrganizationRole role,
			String organizationName,
			boolean accessAllEstablishments,
			List<Establishment> accessibleEstablishments
	) {
		String bootstrapUserEmail = Optional.ofNullable(email).orElse("").trim();
		if (bootstrapUserEmail.isBlank()) {
			return;
		}

		User user = userRepository.findByEmailIgnoreCase(bootstrapUserEmail).orElse(null);
		if (user == null) {
			log.warn("Skipped bootstrap organization context because bootstrap user {} does not exist", bootstrapUserEmail);
			return;
		}

		organizationMembershipRepository.findByOrganizationIdAndUserId(organization.getId(), user.getId()).ifPresentOrElse(existing -> {
			existing.setRole(role);
			existing.setActive(true);
			existing.setAccessAllEstablishments(accessAllEstablishments);
			existing.replaceAccessibleEstablishments(accessibleEstablishments);
		}, () -> {
			OrganizationMembership membership = new OrganizationMembership(
					organization,
					user,
					role,
					true,
					accessAllEstablishments
			);
			membership.replaceAccessibleEstablishments(accessibleEstablishments);
			organizationMembershipRepository.save(membership);
			log.info("Created bootstrap membership for {} in {}", bootstrapUserEmail, organizationName);
		});
	}

	private Optional<User> resolveBootstrapChecklistActor() {
		String managerEmail = Optional.ofNullable(properties.getBootstrapUser().getEmail()).orElse("").trim();
		if (!managerEmail.isBlank()) {
			Optional<User> manager = userRepository.findByEmailIgnoreCase(managerEmail);
			if (manager.isPresent()) {
				return manager;
			}
		}

		String adminEmail = Optional.ofNullable(properties.getBootstrapAdmin().getEmail()).orElse("").trim();
		if (adminEmail.isBlank()) {
			return Optional.empty();
		}
		return userRepository.findByEmailIgnoreCase(adminEmail);
	}

	private void bootstrapChecklistRuns(Organization organization, List<Establishment> establishments, User actor) {
		CurrentUser currentUser = new CurrentUser(actor.getId(), actor.getEmail(), actor.getGlobalRoles());
		LocalDate today = LocalDate.now(DEFAULT_BOOTSTRAP_ZONE);

		for (Establishment establishment : establishments) {
			if (establishment.getStatus() != EstablishmentStatus.ACTIVE) {
				continue;
			}

			upsertChecklistDefinition(
					establishment,
					actor,
					ChecklistServiceArea.IK_MAT,
					establishment.getName() + IK_MAT_TITLE_SUFFIX,
					"Daily food safety routine for " + establishment.getName(),
					buildIkMatTasks(),
					List.of(buildDailySchedule(today, LocalTime.of(9, 0), actor))
			);
			upsertChecklistDefinition(
					establishment,
					actor,
					ChecklistServiceArea.IK_MAT,
					establishment.getName() + IK_MAT_CLOSING_TITLE_SUFFIX,
					"Closing food safety routine for " + establishment.getName(),
					buildIkMatClosingTasks(),
					List.of(buildDailySchedule(today, LocalTime.of(21, 0), actor))
			);

			if (establishment.getType() == EstablishmentType.BAR) {
				upsertChecklistDefinition(
						establishment,
						actor,
						ChecklistServiceArea.IK_ALKOHOL,
						establishment.getName() + IK_ALKOHOL_TITLE_SUFFIX,
						"Daily alcohol compliance routine for " + establishment.getName(),
						buildIkAlkoholTasks(),
						List.of(buildDailySchedule(today, LocalTime.of(17, 0), actor))
				);
				upsertChecklistDefinition(
						establishment,
						actor,
						ChecklistServiceArea.IK_ALKOHOL,
						establishment.getName() + IK_ALKOHOL_CLOSING_TITLE_SUFFIX,
						"Closing alcohol compliance routine for " + establishment.getName(),
						buildIkAlkoholClosingTasks(),
						List.of(buildDailySchedule(today, LocalTime.of(23, 30), actor))
				);
			}

			Instant windowStart = today.minusDays(6).atStartOfDay(DEFAULT_BOOTSTRAP_ZONE).toInstant();
			Instant windowEnd = today.plusDays(4).atTime(LocalTime.MAX).atZone(DEFAULT_BOOTSTRAP_ZONE).toInstant();
			int createdRuns = checklistSchedulerService.generateRunsForWindow(
					organization.getId(),
					establishment.getId(),
					windowStart,
					windowEnd,
					currentUser
			);
			if (createdRuns > 0) {
				log.info("Created {} bootstrap checklist runs for {}", createdRuns, establishment.getName());
			}
		}
	}

	private void bootstrapDeviations(Organization organization, List<Establishment> establishments, User actor) {
		for (Establishment establishment : establishments) {
			if (establishment.getStatus() != EstablishmentStatus.ACTIVE) {
				continue;
			}

			createBootstrapDeviation(
					organization,
					establishment,
					actor,
					"Cold storage temperature above safe range",
					"Opening check found the cold line above the internal temperature threshold.",
					DeviationCategory.TEMPERATURE,
					DeviationSeverity.HIGH,
					DeviationStatus.OPEN,
					"Temperature logged and kitchen lead notified."
			);
			createBootstrapDeviation(
					organization,
					establishment,
					actor,
					"Cleaning verification missed on prep sink",
					"Closing cleaning sign-off was not completed for the prep sink area.",
					DeviationCategory.HYGIENE,
					DeviationSeverity.MEDIUM,
					DeviationStatus.IN_PROGRESS,
					"Assigned for follow-up before the next opening shift."
			);
			createBootstrapDeviation(
					organization,
					establishment,
					actor,
					"Container label missing production date",
					"One prepared ingredient container did not have an updated production date label.",
					DeviationCategory.STORAGE,
					DeviationSeverity.LOW,
					DeviationStatus.RESOLVED,
					"Container relabeled and stock routine reviewed."
			);

			if (establishment.getType() == EstablishmentType.BAR) {
				createBootstrapDeviation(
						organization,
						establishment,
						actor,
						"ID verification missed during bar rush",
						"A guest was served before ID verification was completed.",
						DeviationCategory.AGE_CONTROL,
						DeviationSeverity.CRITICAL,
						DeviationStatus.OPEN,
						"Manager asked for shift review and staff retraining."
				);
				createBootstrapDeviation(
						organization,
						establishment,
						actor,
						"Closing incident log not completed",
						"A refusal-of-service note was not entered before shift handover.",
						DeviationCategory.DOCUMENTATION_AND_TRAINING,
						DeviationSeverity.MEDIUM,
						DeviationStatus.IN_PROGRESS,
						"Shift lead reminded to complete the incident log."
				);
			}
		}
	}

	private void upsertChecklistDefinition(
			Establishment establishment,
			User actor,
			ChecklistServiceArea serviceArea,
			String title,
			String description,
			List<ChecklistTaskDefinition> tasks,
			List<ChecklistSchedule> schedules
	) {
		boolean definitionExists = checklistDefinitionRepository.findByEstablishmentIdAndStatus(
				establishment.getId(),
				ChecklistDefinitionStatus.ACTIVE
		).stream().anyMatch(definition ->
				definition.getServiceArea() == serviceArea && definition.getTitle().equalsIgnoreCase(title));
		if (definitionExists) {
			return;
		}

		ChecklistDefinition checklistDefinition = new ChecklistDefinition(
				UUID.randomUUID(),
				establishment,
				serviceArea,
				title,
				description,
				1,
				ChecklistDefinitionStatus.ACTIVE,
				Instant.now(),
				actor,
				actor
		);
		checklistDefinition.replaceTasks(tasks);
		checklistDefinition.replaceSchedules(schedules);
		checklistDefinitionRepository.save(checklistDefinition);
		log.info("Created bootstrap checklist definition {} for {}", title, establishment.getName());
	}

	private ChecklistSchedule buildDailySchedule(LocalDate startDate, LocalTime dueTime, User actor) {
		return new ChecklistSchedule(
				ChecklistScheduleType.DAILY,
				startDate,
				null,
				dueTime,
				null,
				null,
				DEFAULT_BOOTSTRAP_ZONE.getId(),
				true,
				actor,
				actor
		);
	}

	private List<ChecklistTaskDefinition> buildIkMatTasks() {
		return List.of(
				new ChecklistTaskDefinition(
						"Check cold storage temperature",
						"Record the warmest reading from the fridge line.",
						ChecklistTaskKind.MEASUREMENT,
						true,
						0,
						"C",
						new BigDecimal("0"),
						new BigDecimal("4")
				),
				new ChecklistTaskDefinition(
						"Verify handwash station is stocked",
						"Confirm soap and paper are available before service.",
						ChecklistTaskKind.VERIFICATION,
						true,
						1,
						null,
						null,
						null
				)
		);
	}

	private List<ChecklistTaskDefinition> buildIkMatClosingTasks() {
		return List.of(
				new ChecklistTaskDefinition(
						"Confirm hot hold units are powered down and cleaned",
						"Check that all hot hold units have been cleaned after service.",
						ChecklistTaskKind.ACTION,
						true,
						0,
						null,
						null,
						null
				),
				new ChecklistTaskDefinition(
						"Record dishwasher final rinse temperature",
						"Measure the final rinse result from the closing cycle.",
						ChecklistTaskKind.MEASUREMENT,
						true,
						1,
						"C",
						new BigDecimal("80"),
						new BigDecimal("95")
				)
		);
	}

	private List<ChecklistTaskDefinition> buildIkAlkoholTasks() {
		return List.of(
				new ChecklistTaskDefinition(
						"Confirm age verification signage is visible",
						"Check the entrance and bar service point.",
						ChecklistTaskKind.VERIFICATION,
						true,
						0,
						null,
						null,
						null
				),
				new ChecklistTaskDefinition(
						"Document responsible server on shift",
						"Enter the name of the staff member responsible for alcohol service.",
						ChecklistTaskKind.TEXT_ENTRY,
						true,
						1,
						null,
						null,
						null
				)
		);
	}

	private List<ChecklistTaskDefinition> buildIkAlkoholClosingTasks() {
		return List.of(
				new ChecklistTaskDefinition(
						"Confirm guest area is cleared before licensed close",
						"Verify that no guests remain in service areas after closing time.",
						ChecklistTaskKind.VERIFICATION,
						true,
						0,
						null,
						null,
						null
				),
				new ChecklistTaskDefinition(
						"Write handover note for incident follow-up",
						"Document any alcohol-service incidents that need next-shift attention.",
						ChecklistTaskKind.TEXT_ENTRY,
						true,
						1,
						null,
						null,
						null
				)
		);
	}

	private void createBootstrapDeviation(
			Organization organization,
			Establishment establishment,
			User actor,
			String title,
			String description,
			DeviationCategory category,
			DeviationSeverity severity,
			DeviationStatus status,
			String followUpNote
	) {
		if (deviationRepository.existsByEstablishmentIdAndTitleIgnoreCase(establishment.getId(), title)) {
			return;
		}

		Deviation deviation = new Deviation(
				organization,
				establishment,
				actor,
				actor,
				title,
				description,
				severity,
				category
		);
		deviation.setStatus(status);
		deviation.addEvent(new DeviationEvent(
				DeviationEventType.REPORTED,
				actor,
				Instant.now().minusSeconds(7200),
				"Deviation reported."
		));
		deviation.addEvent(new DeviationEvent(
				DeviationEventType.NOTE_ADDED,
				actor,
				Instant.now().minusSeconds(3600),
				followUpNote
		));
		deviationRepository.save(deviation);
		log.info("Created bootstrap deviation {} for {}", title, establishment.getName());
	}
}
