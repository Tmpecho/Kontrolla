package org.kontrolla.organizations.application;

import org.kontrolla.checklists.application.ChecklistSchedulerService;
import org.kontrolla.checklists.domain.ChecklistDefinition;
import org.kontrolla.checklists.domain.ChecklistDefinitionStatus;
import org.kontrolla.checklists.domain.ChecklistRun;
import org.kontrolla.checklists.domain.ChecklistRunAssignment;
import org.kontrolla.checklists.domain.ChecklistRunEvent;
import org.kontrolla.checklists.domain.ChecklistRunEventType;
import org.kontrolla.checklists.domain.ChecklistRunStatus;
import org.kontrolla.checklists.domain.ChecklistSchedule;
import org.kontrolla.checklists.domain.ChecklistScheduleType;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.checklists.domain.ChecklistTaskDefinition;
import org.kontrolla.checklists.domain.ChecklistTaskExecution;
import org.kontrolla.checklists.domain.ChecklistTaskExecutionStatus;
import org.kontrolla.checklists.domain.ChecklistTaskKind;
import org.kontrolla.checklists.domain.ChecklistVerificationResult;
import org.kontrolla.checklists.infrastructure.ChecklistDefinitionRepository;
import org.kontrolla.checklists.infrastructure.ChecklistRunRepository;
import org.kontrolla.deviations.domain.Deviation;
import org.kontrolla.deviations.domain.DeviationCategory;
import org.kontrolla.deviations.domain.DeviationEvent;
import org.kontrolla.deviations.domain.DeviationEventType;
import org.kontrolla.deviations.domain.DeviationSeverity;
import org.kontrolla.deviations.domain.DeviationStatus;
import org.kontrolla.deviations.infrastructure.DeviationRepository;
import org.kontrolla.documents.domain.Document;
import org.kontrolla.documents.domain.DocumentFile;
import org.kontrolla.documents.domain.DocumentServiceArea;
import org.kontrolla.documents.infrastructure.DocumentFileRepository;
import org.kontrolla.documents.infrastructure.DocumentRepository;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentServingHours;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.establishments.infrastructure.EstablishmentServingHoursRepository;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.AppSecurityProperties;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.domain.OrganizationRole;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.kontrolla.organizations.infrastructure.OrganizationRepository;
import org.kontrolla.temperatures.domain.TemperatureUnit;
import org.kontrolla.temperatures.domain.TemperatureUnitType;
import org.kontrolla.temperatures.infrastructure.TemperatureUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@Profile("dev")
@Order(20)
public class BootstrapOrganizationContextInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BootstrapOrganizationContextInitializer.class);
	private static final ZoneId DEFAULT_BOOTSTRAP_ZONE = ZoneId.of("Europe/Oslo");
	private static final String IK_MAT_TITLE_SUFFIX = " Daily IK-Mat";
	private static final String IK_MAT_CLOSING_TITLE_SUFFIX = " Closing IK-Mat";
	private static final String IK_MAT_WEEKLY_TITLE_SUFFIX = " Weekly Food Safety Review";
	private static final String IK_MAT_MONTHLY_TITLE_SUFFIX = " Monthly Compliance Review";
	private static final String IK_ALKOHOL_TITLE_SUFFIX = " Daily IK-Alkohol";
	private static final String IK_ALKOHOL_CLOSING_TITLE_SUFFIX = " Closing IK-Alkohol";
	private static final String IK_ALKOHOL_WEEKLY_TITLE_SUFFIX = " Weekly IK-Alkohol Review";
	private static final String IK_ALKOHOL_MONTHLY_TITLE_SUFFIX = " Monthly Licensing Review";
	private static final int CHECKLIST_LOOKBACK_DAYS = 14;
	private static final int CHECKLIST_LOOKAHEAD_DAYS = 60;
	private static final String DEFAULT_SEED_PASSWORD = "password123";

	private final UserRepository userRepository;
	private final OrganizationRepository organizationRepository;
	private final OrganizationMembershipRepository organizationMembershipRepository;
	private final EstablishmentRepository establishmentRepository;
	private final ChecklistDefinitionRepository checklistDefinitionRepository;
	private final ChecklistRunRepository checklistRunRepository;
	private final ChecklistSchedulerService checklistSchedulerService;
	private final DeviationRepository deviationRepository;
	private final DocumentRepository documentRepository;
	private final DocumentFileRepository documentFileRepository;
	private final TemperatureUnitRepository temperatureUnitRepository;
	private final EstablishmentServingHoursRepository establishmentServingHoursRepository;
	private final AppSecurityProperties properties;
	private final PasswordEncoder passwordEncoder;

	public BootstrapOrganizationContextInitializer(
			UserRepository userRepository,
			OrganizationRepository organizationRepository,
			OrganizationMembershipRepository organizationMembershipRepository,
			EstablishmentRepository establishmentRepository,
			ChecklistDefinitionRepository checklistDefinitionRepository,
			ChecklistRunRepository checklistRunRepository,
			ChecklistSchedulerService checklistSchedulerService,
			DeviationRepository deviationRepository,
			DocumentRepository documentRepository,
			DocumentFileRepository documentFileRepository,
			TemperatureUnitRepository temperatureUnitRepository,
			EstablishmentServingHoursRepository establishmentServingHoursRepository,
			AppSecurityProperties properties,
			PasswordEncoder passwordEncoder
	) {
		this.userRepository = userRepository;
		this.organizationRepository = organizationRepository;
		this.organizationMembershipRepository = organizationMembershipRepository;
		this.establishmentRepository = establishmentRepository;
		this.checklistDefinitionRepository = checklistDefinitionRepository;
		this.checklistRunRepository = checklistRunRepository;
		this.checklistSchedulerService = checklistSchedulerService;
		this.deviationRepository = deviationRepository;
		this.documentRepository = documentRepository;
		this.documentFileRepository = documentFileRepository;
		this.temperatureUnitRepository = temperatureUnitRepository;
		this.establishmentServingHoursRepository = establishmentServingHoursRepository;
		this.properties = properties;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		Optional<BootstrapOrganizationSeed> configuredSeed = resolveConfiguredOrganizationSeed();
		if (configuredSeed.isEmpty()) {
			return;
		}

		List<BootstrapOrganizationSeed> organizationSeeds = new ArrayList<>();
		organizationSeeds.add(configuredSeed.get());
		buildCuratedOrganizationSeeds().stream()
				.filter(seed -> organizationSeeds.stream().noneMatch(existing -> existing.name().equalsIgnoreCase(seed.name())))
				.forEach(organizationSeeds::add);

		Optional<User> checklistActor = resolveBootstrapChecklistActor();
		for (BootstrapOrganizationSeed organizationSeed : organizationSeeds) {
			seedOrganization(organizationSeed, checklistActor);
		}
	}

	private Optional<BootstrapOrganizationSeed> resolveConfiguredOrganizationSeed() {
		String organizationName = Optional.ofNullable(properties.getBootstrapOrganization().getName()).orElse("").trim();
		if (organizationName.isBlank()) {
			return Optional.empty();
		}

		List<BootstrapEstablishmentSeed> establishments = resolveBootstrapEstablishments().stream()
				.map(establishment -> new BootstrapEstablishmentSeed(
						establishment.getName(),
						establishment.getType(),
						establishment.getStatus()
				))
				.toList();

		return Optional.of(new BootstrapOrganizationSeed(
				organizationName,
				properties.getBootstrapOrganization().getStatus(),
				establishments,
				OrganizationRole.ORG_MANAGER,
				1
		));
	}

	private void seedOrganization(BootstrapOrganizationSeed seed, Optional<User> checklistActor) {
		String organizationName = seed.name();
		Organization organization = organizationRepository.findByNameIgnoreCase(organizationName)
				.map(existing -> {
					existing.setStatus(seed.status());
					return existing;
				})
				.orElseGet(() -> {
					Organization created = new Organization(
							organizationName,
							seed.status()
					);
					organizationRepository.save(created);
					log.info("Created bootstrap organization {}", organizationName);
					return created;
				});

		List<Establishment> bootstrapEstablishments = seed.establishments().stream()
				.map(establishment -> upsertEstablishment(organization, organizationName, establishment))
				.filter(Objects::nonNull)
				.toList();
		List<Establishment> activeEstablishments = bootstrapEstablishments.stream()
				.filter(establishment -> establishment.getStatus() == EstablishmentStatus.ACTIVE)
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
				seed.bootstrapUserRole(),
				organizationName,
				true,
				List.of()
		);

		List<AppSecurityProperties.BootstrapUser> bootstrapEmployees = properties.getBootstrapEmployees();
		for (int index = 0; index < bootstrapEmployees.size(); index++) {
			AppSecurityProperties.BootstrapUser employee = bootstrapEmployees.get(index);
			boolean accessAllEstablishments = activeEstablishments.isEmpty();
			List<Establishment> accessibleEstablishments = accessAllEstablishments
					? List.of()
					: selectAccessibleEstablishments(activeEstablishments, index, seed.employeeAccessSpan());
			upsertMembership(
					employee.getEmail(),
					organization,
					OrganizationRole.ORG_EMPLOYEE,
					organizationName,
					accessAllEstablishments,
					accessibleEstablishments
			);
		}
		seedAdditionalMembers(seed, organization, organizationName, activeEstablishments);

		bootstrapTemperatureUnits(organization, bootstrapEstablishments);
		bootstrapDocuments(organization, bootstrapEstablishments);
		bootstrapServingHours(bootstrapEstablishments);

		checklistActor.ifPresent(actor -> {
			bootstrapChecklistRuns(organization, bootstrapEstablishments, actor);
			bootstrapDeviations(organization, bootstrapEstablishments, actor);
		});
	}

	private void seedAdditionalMembers(
			BootstrapOrganizationSeed seed,
			Organization organization,
			String organizationName,
			List<Establishment> activeEstablishments
	) {
		List<BootstrapMembershipSeed> syntheticMembers = buildSyntheticMembers(seed, activeEstablishments);
		for (BootstrapMembershipSeed member : syntheticMembers) {
			User user = upsertSeedUser(member.user());
			List<Establishment> accessibleEstablishments = member.accessAllEstablishments()
					? List.of()
					: activeEstablishments.stream()
							.filter(establishment -> member.establishmentNames().contains(establishment.getName()))
							.toList();
			upsertMembership(
					user.getEmail(),
					organization,
					member.role(),
					organizationName,
					member.accessAllEstablishments(),
					accessibleEstablishments
			);
		}
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
			BootstrapEstablishmentSeed bootstrapEstablishment
	) {
		String establishmentName = Optional.ofNullable(bootstrapEstablishment.name()).orElse("").trim();
		if (establishmentName.isBlank()) {
			return null;
		}

		return establishmentRepository.findFirstByOrganizationIdAndNameIgnoreCase(organization.getId(), establishmentName)
				.map(existing -> {
					existing.setType(bootstrapEstablishment.type());
					existing.setStatus(bootstrapEstablishment.status());
					return existing;
				})
				.orElseGet(() -> {
					Establishment created = new Establishment(
							organization,
							establishmentName,
							bootstrapEstablishment.type(),
							bootstrapEstablishment.status()
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

	private User upsertSeedUser(BootstrapSeedUser userSeed) {
		return userRepository.findByEmailIgnoreCase(userSeed.email()).map(existing -> {
			existing.setFirstName(userSeed.firstName());
			existing.setLastName(userSeed.lastName());
			existing.setActive(true);
			return existing;
		}).orElseGet(() -> {
			User created = new User(
					userSeed.email(),
					userSeed.firstName(),
					userSeed.lastName(),
					passwordEncoder.encode(resolveSeedPassword()),
					true,
					Set.of()
			);
			userRepository.save(created);
			log.info("Created bootstrap seed user {}", userSeed.email());
			return created;
		});
	}

	private String resolveSeedPassword() {
		String configuredPassword = Optional.ofNullable(properties.getBootstrapUser().getPassword()).orElse("").trim();
		return configuredPassword.isBlank() ? DEFAULT_SEED_PASSWORD : configuredPassword;
	}

	private List<BootstrapOrganizationSeed> buildCuratedOrganizationSeeds() {
		return List.of(
				new BootstrapOrganizationSeed(
						"Nordic Table Group",
						org.kontrolla.organizations.domain.OrganizationStatus.ACTIVE,
						List.of(
								new BootstrapEstablishmentSeed("Bjorvika Brasserie", EstablishmentType.RESTAURANT, EstablishmentStatus.ACTIVE),
								new BootstrapEstablishmentSeed("Grunerlokka Kaffehus", EstablishmentType.CAFE, EstablishmentStatus.ACTIVE),
								new BootstrapEstablishmentSeed("Vulkan Tasting Room", EstablishmentType.BAR, EstablishmentStatus.ACTIVE),
								new BootstrapEstablishmentSeed("Testkjokkenet HQ", EstablishmentType.OTHER, EstablishmentStatus.ACTIVE)
						),
						OrganizationRole.ORG_OWNER,
						2
				),
				new BootstrapOrganizationSeed(
						"Fjord Service Collective",
						org.kontrolla.organizations.domain.OrganizationStatus.ACTIVE,
						List.of(
								new BootstrapEstablishmentSeed("Havglimt Restaurant", EstablishmentType.RESTAURANT, EstablishmentStatus.ACTIVE),
								new BootstrapEstablishmentSeed("Stasjonskafeen Majorstuen", EstablishmentType.CAFE, EstablishmentStatus.ACTIVE),
								new BootstrapEstablishmentSeed("Bryggekanten Vinbar", EstablishmentType.BAR, EstablishmentStatus.ACTIVE),
								new BootstrapEstablishmentSeed("Festivalpaviljongen", EstablishmentType.OTHER, EstablishmentStatus.INACTIVE)
						),
						OrganizationRole.ORG_OWNER,
						2
				),
				new BootstrapOrganizationSeed(
						"Campus and Co Workspaces",
						org.kontrolla.organizations.domain.OrganizationStatus.ACTIVE,
						List.of(
								new BootstrapEstablishmentSeed("Studentbaren Terminalen", EstablishmentType.BAR, EstablishmentStatus.ACTIVE),
								new BootstrapEstablishmentSeed("Atriumkantina", EstablishmentType.CAFE, EstablishmentStatus.ACTIVE),
								new BootstrapEstablishmentSeed("Laboratoriet Matstudio", EstablishmentType.RESTAURANT, EstablishmentStatus.ACTIVE)
						),
						OrganizationRole.ORG_OWNER,
						2
				)
		);
	}

	private List<BootstrapMembershipSeed> buildSyntheticMembers(
			BootstrapOrganizationSeed seed,
			List<Establishment> activeEstablishments
	) {
		List<BootstrapMembershipSeed> members = new ArrayList<>();
		String orgSlug = slugify(seed.name());

		members.add(orgWideMember(
				orgSlug,
				"ops",
				"Solveig",
				"Dahl",
				OrganizationRole.ORG_ADMIN
		));
		members.add(orgWideMember(
				orgSlug,
				"compliance",
				"Henrik",
				"Nordby",
				OrganizationRole.ORG_MANAGER
		));

		for (int index = 0; index < activeEstablishments.size(); index++) {
			Establishment establishment = activeEstablishments.get(index);
			members.addAll(buildEstablishmentTeam(orgSlug, establishment, index));
		}

		return members;
	}

	private List<BootstrapMembershipSeed> buildEstablishmentTeam(
			String orgSlug,
			Establishment establishment,
			int establishmentIndex
	) {
		String establishmentSlug = slugify(establishment.getName());
		String establishmentName = establishment.getName();
		List<String> scopedAccess = List.of(establishmentName);

		return List.of(
				new BootstrapMembershipSeed(
						new BootstrapSeedUser(
								"manager." + establishmentSlug + "." + orgSlug + "@example.com",
								selectFirstName(establishmentIndex * 3),
								selectLastName(establishmentIndex * 3)
						),
						OrganizationRole.ORG_MANAGER,
						false,
						scopedAccess
				),
				new BootstrapMembershipSeed(
						new BootstrapSeedUser(
								"supervisor." + establishmentSlug + "." + orgSlug + "@example.com",
								selectFirstName(establishmentIndex * 3 + 1),
								selectLastName(establishmentIndex * 3 + 1)
						),
						OrganizationRole.ORG_EMPLOYEE,
						false,
						scopedAccess
				),
				new BootstrapMembershipSeed(
						new BootstrapSeedUser(
								"staff." + establishmentSlug + "." + orgSlug + "@example.com",
								selectFirstName(establishmentIndex * 3 + 2),
								selectLastName(establishmentIndex * 3 + 2)
						),
						OrganizationRole.ORG_EMPLOYEE,
						false,
						scopedAccess
				)
		);
	}

	private BootstrapMembershipSeed orgWideMember(
			String orgSlug,
			String localPart,
			String firstName,
			String lastName,
			OrganizationRole role
	) {
		return new BootstrapMembershipSeed(
				new BootstrapSeedUser(localPart + "." + orgSlug + "@example.com", firstName, lastName),
				role,
				true,
				List.of()
		);
	}

	private String slugify(String value) {
		String normalized = value.toLowerCase()
				.replace("and", "og")
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("(^-|-$)", "");
		return normalized.isBlank() ? "seed" : normalized;
	}

	private String selectFirstName(int index) {
		String[] firstNames = {
				"Ingrid", "Magnus", "Sanna", "Jonas", "Maja", "Lukas",
				"Eline", "Tobias", "Nora", "Filip", "Alva", "Emil"
		};
		return firstNames[index % firstNames.length];
	}

	private String selectLastName(int index) {
		String[] lastNames = {
				"Hagen", "Nilsen", "Bakke", "Aas", "Lunde", "Moen",
				"Lie", "Rosen", "Sunde", "Eidem", "Hovland", "Tangen"
		};
		return lastNames[index % lastNames.length];
	}

	private List<Establishment> selectAccessibleEstablishments(
			List<Establishment> activeEstablishments,
			int employeeIndex,
			int accessSpan
	) {
		if (activeEstablishments.isEmpty()) {
			return List.of();
		}

		if (activeEstablishments.size() <= accessSpan) {
			return List.copyOf(activeEstablishments);
		}

		List<Establishment> accessible = new ArrayList<>();
		int safeAccessSpan = Math.max(1, accessSpan);
		int startIndex = (employeeIndex * safeAccessSpan) % activeEstablishments.size();
		for (int offset = 0; offset < safeAccessSpan; offset++) {
			accessible.add(activeEstablishments.get((startIndex + offset) % activeEstablishments.size()));
		}
		return accessible;
	}

	private void bootstrapChecklistRuns(Organization organization, List<Establishment> establishments, User actor) {
		CurrentUser currentUser = new CurrentUser(actor.getId(), actor.getEmail(), actor.getGlobalRoles());
		LocalDate today = LocalDate.now(DEFAULT_BOOTSTRAP_ZONE);

		for (Establishment establishment : establishments) {
			if (establishment.getStatus() != EstablishmentStatus.ACTIVE) {
				continue;
			}

			bootstrapIkMatDefinitions(establishment, actor, today);
			if (establishment.getType() == EstablishmentType.BAR) {
				bootstrapIkAlkoholDefinitions(establishment, actor, today);
			}

			Instant windowStart = today.minusDays(CHECKLIST_LOOKBACK_DAYS).atStartOfDay(DEFAULT_BOOTSTRAP_ZONE).toInstant();
			Instant windowEnd = today.plusDays(CHECKLIST_LOOKAHEAD_DAYS).atTime(LocalTime.MAX).atZone(DEFAULT_BOOTSTRAP_ZONE).toInstant();
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

			seedChecklistRunStates(organization, establishment, actor);
			checklistSchedulerService.markOverdueRuns(
					organization.getId(),
					establishment.getId(),
					Instant.now(),
					currentUser
			);
		}
	}

	private void bootstrapIkMatDefinitions(Establishment establishment, User actor, LocalDate today) {
		LocalDate scheduleStartDate = today.minusDays(CHECKLIST_LOOKBACK_DAYS);
		upsertChecklistDefinition(
				establishment,
				actor,
				ChecklistServiceArea.IK_MAT,
				establishment.getName() + IK_MAT_TITLE_SUFFIX,
				"Opening food safety routine for " + establishment.getName(),
				buildIkMatTasks(establishment.getType()),
				List.of(buildDailySchedule(scheduleStartDate, resolveOpeningDueTime(establishment.getType()), actor))
		);
		upsertChecklistDefinition(
				establishment,
				actor,
				ChecklistServiceArea.IK_MAT,
				establishment.getName() + IK_MAT_CLOSING_TITLE_SUFFIX,
				"Closing food safety routine for " + establishment.getName(),
				buildIkMatClosingTasks(establishment.getType()),
				List.of(buildDailySchedule(scheduleStartDate, resolveClosingDueTime(establishment.getType()), actor))
		);
		upsertChecklistDefinition(
				establishment,
				actor,
				ChecklistServiceArea.IK_MAT,
				establishment.getName() + IK_MAT_WEEKLY_TITLE_SUFFIX,
				"Weekly food safety review for " + establishment.getName(),
				buildIkMatWeeklyTasks(establishment.getType()),
				List.of(buildWeeklySchedule(scheduleStartDate, resolveWeeklyFoodSafetyDueTime(establishment.getType()), actor, resolveWeeklyFoodSafetyDays(establishment.getType())))
		);
		upsertChecklistDefinition(
				establishment,
				actor,
				ChecklistServiceArea.IK_MAT,
				establishment.getName() + IK_MAT_MONTHLY_TITLE_SUFFIX,
				"Monthly food safety compliance review for " + establishment.getName(),
				buildIkMatMonthlyTasks(establishment.getType()),
				List.of(buildMonthlySchedule(
						scheduleStartDate,
						resolveMonthlyFoodSafetyDueTime(establishment.getType()),
						actor,
						resolveMonthlyFoodSafetyDayOfMonth(establishment.getType())
				))
		);
	}

	private void bootstrapIkAlkoholDefinitions(Establishment establishment, User actor, LocalDate today) {
		LocalDate scheduleStartDate = today.minusDays(CHECKLIST_LOOKBACK_DAYS);
		upsertChecklistDefinition(
				establishment,
				actor,
				ChecklistServiceArea.IK_ALKOHOL,
				establishment.getName() + IK_ALKOHOL_TITLE_SUFFIX,
				"Opening alcohol compliance routine for " + establishment.getName(),
				buildIkAlkoholTasks(),
				List.of(buildDailySchedule(scheduleStartDate, LocalTime.of(16, 0), actor))
		);
		upsertChecklistDefinition(
				establishment,
				actor,
				ChecklistServiceArea.IK_ALKOHOL,
				establishment.getName() + IK_ALKOHOL_CLOSING_TITLE_SUFFIX,
				"Closing alcohol compliance routine for " + establishment.getName(),
				buildIkAlkoholClosingTasks(),
				List.of(buildDailySchedule(scheduleStartDate, LocalTime.of(23, 45), actor))
		);
		upsertChecklistDefinition(
				establishment,
				actor,
				ChecklistServiceArea.IK_ALKOHOL,
				establishment.getName() + IK_ALKOHOL_WEEKLY_TITLE_SUFFIX,
				"Weekly alcohol compliance review for " + establishment.getName(),
				buildIkAlkoholWeeklyTasks(),
				List.of(buildWeeklySchedule(scheduleStartDate, LocalTime.of(15, 0), actor, DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
		);
		upsertChecklistDefinition(
				establishment,
				actor,
				ChecklistServiceArea.IK_ALKOHOL,
				establishment.getName() + IK_ALKOHOL_MONTHLY_TITLE_SUFFIX,
				"Monthly alcohol compliance review for " + establishment.getName(),
				buildIkAlkoholMonthlyTasks(),
				List.of(buildMonthlySchedule(scheduleStartDate, LocalTime.of(14, 0), actor, 5))
		);
	}

	private void bootstrapDeviations(Organization organization, List<Establishment> establishments, User actor) {
		for (Establishment establishment : establishments) {
			if (establishment.getStatus() != EstablishmentStatus.ACTIVE) {
				continue;
			}

			buildDeviationSeeds(establishment.getType()).forEach(seed -> createBootstrapDeviation(
					organization,
					establishment,
					actor,
					seed
			));
		}
	}

	private void bootstrapTemperatureUnits(Organization organization, List<Establishment> establishments) {
		for (Establishment establishment : establishments) {
			if (establishment.getStatus() != EstablishmentStatus.ACTIVE) {
				continue;
			}

			buildTemperatureUnitSeeds(establishment.getType()).forEach(seed ->
					upsertTemperatureUnit(organization, establishment, seed)
			);
		}
	}

	private void bootstrapDocuments(Organization organization, List<Establishment> establishments) {
		for (Establishment establishment : establishments) {
			if (establishment.getStatus() != EstablishmentStatus.ACTIVE) {
				continue;
			}

			List<OrganizationMembership> accessibleMemberships = organizationMembershipRepository
					.findByOrganizationIdAndActiveTrueAndAccessibleEstablishmentId(
							organization.getId(),
							establishment.getId(),
							PageRequest.of(0, 100)
					)
					.getContent()
					.stream()
					.filter(membership -> membership.getUser().isActive())
					.toList();
			Optional<User> actor = resolveBootstrapDocumentActor(accessibleMemberships);
			if (actor.isEmpty()) {
				log.warn("Skipped bootstrap documents for {} because no accessible active members were found", establishment.getName());
				continue;
			}

			List<User> auditReaders = resolveBootstrapAuditReaders(accessibleMemberships, actor.get());
			for (BootstrapDocumentSeed seed : buildDocumentSeeds(DocumentServiceArea.IK_ALKOHOL)) {
				upsertBootstrapDocument(organization, establishment, actor.get(), auditReaders, seed);
			}
			for (BootstrapDocumentSeed seed : buildDocumentSeeds(DocumentServiceArea.IK_MAT)) {
				upsertBootstrapDocument(organization, establishment, actor.get(), auditReaders, seed);
			}
		}
	}

	private void bootstrapServingHours(List<Establishment> establishments) {
		for (Establishment establishment : establishments) {
			if (establishment.getStatus() != EstablishmentStatus.ACTIVE) {
				continue;
			}

			buildServingHoursSeeds(establishment.getType()).forEach(seed ->
					upsertServingHours(establishment, seed)
			);
		}
	}

	private void upsertServingHours(Establishment establishment, BootstrapServingHoursSeed seed) {
		establishmentServingHoursRepository.findByEstablishmentIdAndDayOfWeek(
				establishment.getId(),
				seed.dayOfWeek()
		).ifPresentOrElse(existing -> existing.update(
				seed.closed(),
				seed.opensAt(),
				seed.closesAt()
		), () -> {
			EstablishmentServingHours created = new EstablishmentServingHours(
					establishment,
					seed.dayOfWeek(),
					seed.closed(),
					seed.opensAt(),
					seed.closesAt()
			);
			establishmentServingHoursRepository.save(created);
			log.info("Created bootstrap serving hours for {} on {}", establishment.getName(), seed.dayOfWeek());
		});
	}

	private void upsertTemperatureUnit(
			Organization organization,
			Establishment establishment,
			BootstrapTemperatureUnitSeed seed
	) {
		temperatureUnitRepository.findByEstablishmentIdAndOrganizationIdOrderByNameAsc(
				establishment.getId(),
				organization.getId()
		).stream()
				.filter(existing -> existing.getName().equalsIgnoreCase(seed.name()))
				.findFirst()
				.ifPresentOrElse(existing -> {
					existing.setLocation(seed.location());
					existing.setType(seed.type());
					existing.setDueByTime(seed.dueByTime());
					existing.setMinimumTemperature(seed.minimumTemperature());
					existing.setMaximumTemperature(seed.maximumTemperature());
				}, () -> {
					TemperatureUnit created = new TemperatureUnit(
							organization,
							establishment,
							seed.name(),
							seed.location(),
							seed.type(),
							seed.dueByTime(),
							seed.minimumTemperature(),
							seed.maximumTemperature()
					);
					temperatureUnitRepository.save(created);
					log.info("Created bootstrap temperature unit {} for {}", seed.name(), establishment.getName());
				});
	}

	private Optional<User> resolveBootstrapDocumentActor(List<OrganizationMembership> accessibleMemberships) {
		return accessibleMemberships.stream()
				.sorted((left, right) -> Integer.compare(documentActorPriority(left.getRole()), documentActorPriority(right.getRole())))
				.map(OrganizationMembership::getUser)
				.findFirst();
	}

	private int documentActorPriority(OrganizationRole role) {
		return switch (role) {
			case ORG_OWNER -> 0;
			case ORG_ADMIN -> 1;
			case ORG_MANAGER -> 2;
			case ORG_EMPLOYEE -> 3;
		};
	}

	private List<User> resolveBootstrapAuditReaders(List<OrganizationMembership> accessibleMemberships, User actor) {
		List<User> prioritizedReaders = accessibleMemberships.stream()
				.sorted((left, right) -> Integer.compare(auditReaderPriority(left.getRole()), auditReaderPriority(right.getRole())))
				.map(OrganizationMembership::getUser)
				.filter(user -> !user.getId().equals(actor.getId()))
				.toList();

		if (!prioritizedReaders.isEmpty()) {
			return prioritizedReaders;
		}

		return List.of(actor);
	}

	private int auditReaderPriority(OrganizationRole role) {
		return switch (role) {
			case ORG_EMPLOYEE -> 0;
			case ORG_MANAGER -> 1;
			case ORG_ADMIN -> 2;
			case ORG_OWNER -> 3;
		};
	}

	private void upsertBootstrapDocument(
			Organization organization,
			Establishment establishment,
			User actor,
			List<User> auditReaders,
			BootstrapDocumentSeed seed
	) {
		LocalDate today = LocalDate.now(DEFAULT_BOOTSTRAP_ZONE);
		LocalDate issueDate = today.plusDays(seed.issueDateOffsetDays());
		LocalDate renewalDate = today.plusDays(seed.renewalDateOffsetDays());
		String fileName = slugify(seed.title()) + ".pdf";
		byte[] fileContent = buildSamplePdf(seed.title(), seed.serviceArea());
		List<User> assignedAuditReaders = auditReaders.stream()
				.limit(seed.auditReaderCount())
				.toList();

		documentRepository.findByEstablishmentIdAndOrganizationIdAndServiceAreaOrderByTitleAsc(
				establishment.getId(),
				organization.getId(),
				seed.serviceArea()
		).stream()
				.filter(existing -> existing.getTitle().equalsIgnoreCase(seed.title()))
				.findFirst()
				.ifPresentOrElse(existing -> {
					existing.setHolderName(seed.holderName());
					existing.setIssueDate(issueDate);
					existing.setRenewalDate(renewalDate);
					existing.setFileName(fileName);
					existing.setContentType("application/pdf");
					existing.setFileSizeBytes(fileContent.length);
					existing.replaceAuditAssignments(assignedAuditReaders);

					Document savedDocument = documentRepository.save(existing);
					upsertBootstrapDocumentFile(savedDocument, fileContent);
				}, () -> {
					Document created = new Document(
							organization,
							establishment,
							actor,
							seed.serviceArea(),
							seed.title(),
							seed.holderName(),
							issueDate,
							renewalDate,
							fileName,
							"application/pdf",
							fileContent.length
					);
					created.replaceAuditAssignments(assignedAuditReaders);
					Document savedDocument = documentRepository.save(created);
					documentFileRepository.save(new DocumentFile(savedDocument.getId(), fileContent));
					log.info("Created bootstrap document {} for {}", seed.title(), establishment.getName());
				});
	}

	private void upsertBootstrapDocumentFile(Document document, byte[] fileContent) {
		DocumentFile file = documentFileRepository.findById(document.getId())
				.map(existing -> {
					existing.replaceContent(fileContent);
					return existing;
				})
				.orElseGet(() -> new DocumentFile(document.getId(), fileContent));
		documentFileRepository.save(file);
	}

	private List<BootstrapDocumentSeed> buildDocumentSeeds(DocumentServiceArea serviceArea) {
		return switch (serviceArea) {
			case IK_ALKOHOL -> List.of(
					new BootstrapDocumentSeed(
							DocumentServiceArea.IK_ALKOHOL,
							"Alcohol service licence",
							"Municipal licensing authority",
							-330,
							14,
							0
					),
					new BootstrapDocumentSeed(
							DocumentServiceArea.IK_ALKOHOL,
							"Manager knowledge test certificate",
							"Duty manager register",
							-180,
							160,
							0
					),
					new BootstrapDocumentSeed(
							DocumentServiceArea.IK_ALKOHOL,
							"Age verification routine",
							"Front-of-house operations",
							-90,
							240,
							3
					),
					new BootstrapDocumentSeed(
							DocumentServiceArea.IK_ALKOHOL,
							"Service refusal routine",
							"Bar operations",
							-60,
							300,
							3
					)
			);
			case IK_MAT -> List.of(
					new BootstrapDocumentSeed(
							DocumentServiceArea.IK_MAT,
							"HACCP / food safety plan",
							"Kitchen operations",
							-210,
							21,
							0
					),
					new BootstrapDocumentSeed(
							DocumentServiceArea.IK_MAT,
							"Cleaning and sanitation routine",
							"Sanitation lead",
							-90,
							180,
							3
					),
					new BootstrapDocumentSeed(
							DocumentServiceArea.IK_MAT,
							"Allergen handling procedure",
							"Kitchen operations",
							-120,
							260,
							3
					),
					new BootstrapDocumentSeed(
							DocumentServiceArea.IK_MAT,
							"Temperature monitoring routine",
							"Cold storage team",
							-60,
							320,
							2
					)
			);
		};
	}

	private List<BootstrapServingHoursSeed> buildServingHoursSeeds(EstablishmentType type) {
		return switch (type) {
			case BAR -> List.of(
					openHours(DayOfWeek.MONDAY, 16, 0, 23, 0),
					openHours(DayOfWeek.TUESDAY, 16, 0, 23, 0),
					openHours(DayOfWeek.WEDNESDAY, 16, 0, 23, 30),
					openHours(DayOfWeek.THURSDAY, 16, 0, 0, 30),
					openHours(DayOfWeek.FRIDAY, 16, 0, 2, 0),
					openHours(DayOfWeek.SATURDAY, 15, 0, 2, 0),
					closedHours(DayOfWeek.SUNDAY)
			);
			case CAFE -> List.of(
					openHours(DayOfWeek.MONDAY, 7, 30, 18, 0),
					openHours(DayOfWeek.TUESDAY, 7, 30, 18, 0),
					openHours(DayOfWeek.WEDNESDAY, 7, 30, 18, 0),
					openHours(DayOfWeek.THURSDAY, 7, 30, 18, 0),
					openHours(DayOfWeek.FRIDAY, 7, 30, 18, 0),
					openHours(DayOfWeek.SATURDAY, 9, 0, 16, 0),
					closedHours(DayOfWeek.SUNDAY)
			);
			case OTHER -> List.of(
					openHours(DayOfWeek.MONDAY, 9, 0, 17, 0),
					openHours(DayOfWeek.TUESDAY, 9, 0, 17, 0),
					openHours(DayOfWeek.WEDNESDAY, 9, 0, 17, 0),
					openHours(DayOfWeek.THURSDAY, 9, 0, 17, 0),
					openHours(DayOfWeek.FRIDAY, 9, 0, 17, 0),
					closedHours(DayOfWeek.SATURDAY),
					closedHours(DayOfWeek.SUNDAY)
			);
			case RESTAURANT -> List.of(
					openHours(DayOfWeek.MONDAY, 11, 0, 22, 0),
					openHours(DayOfWeek.TUESDAY, 11, 0, 22, 0),
					openHours(DayOfWeek.WEDNESDAY, 11, 0, 22, 0),
					openHours(DayOfWeek.THURSDAY, 11, 0, 22, 0),
					openHours(DayOfWeek.FRIDAY, 11, 0, 23, 0),
					openHours(DayOfWeek.SATURDAY, 12, 0, 23, 0),
					openHours(DayOfWeek.SUNDAY, 12, 0, 21, 0)
			);
		};
	}

	private BootstrapServingHoursSeed openHours(DayOfWeek dayOfWeek, int opensHour, int opensMinute, int closesHour, int closesMinute) {
		return new BootstrapServingHoursSeed(
				dayOfWeek,
				false,
				LocalTime.of(opensHour, opensMinute),
				LocalTime.of(closesHour, closesMinute)
		);
	}

	private BootstrapServingHoursSeed closedHours(DayOfWeek dayOfWeek) {
		return new BootstrapServingHoursSeed(dayOfWeek, true, null, null);
	}

	private byte[] buildSamplePdf(String title, DocumentServiceArea serviceArea) {
		String stream = """
				BT
				/F1 20 Tf
				72 720 Td
				(%s) Tj
				0 -28 Td
				/F1 12 Tf
				(%s) Tj
				0 -20 Td
				(%s) Tj
				ET
				""".formatted(
				escapePdfText(title),
				escapePdfText(serviceArea.name().replace('_', ' ')),
				escapePdfText("Empty sample")
		);
		String objectOne = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n";
		String objectTwo = "2 0 obj\n<< /Type /Pages /Count 1 /Kids [3 0 R] >>\nendobj\n";
		String objectThree = "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>\nendobj\n";
		String objectFour = "4 0 obj\n<< /Length %d >>\nstream\n%s\nendstream\nendobj\n"
				.formatted(stream.getBytes(StandardCharsets.US_ASCII).length, stream);
		String objectFive = "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n";

		List<String> objects = List.of(objectOne, objectTwo, objectThree, objectFour, objectFive);
		StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
		List<Integer> offsets = new ArrayList<>();
		for (String object : objects) {
			offsets.add(pdf.toString().getBytes(StandardCharsets.US_ASCII).length);
			pdf.append(object);
		}

		int xrefOffset = pdf.toString().getBytes(StandardCharsets.US_ASCII).length;
		pdf.append("xref\n");
		pdf.append("0 ").append(objects.size() + 1).append("\n");
		pdf.append("0000000000 65535 f \n");
		for (Integer offset : offsets) {
			pdf.append(String.format("%010d 00000 n \n", offset));
		}
		pdf.append("trailer\n");
		pdf.append("<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
		pdf.append("startxref\n");
		pdf.append(xrefOffset).append("\n");
		pdf.append("%%EOF\n");
		return pdf.toString().getBytes(StandardCharsets.US_ASCII);
	}

	private String escapePdfText(String value) {
		return value
				.replace("\\", "\\\\")
				.replace("(", "\\(")
				.replace(")", "\\)");
	}

	private List<BootstrapTemperatureUnitSeed> buildTemperatureUnitSeeds(EstablishmentType type) {
		return switch (type) {
			case CAFE -> List.of(
					new BootstrapTemperatureUnitSeed(
							"Milk fridge",
							"Back counter dairy station",
							TemperatureUnitType.FRIDGE,
							LocalTime.of(7, 30),
							new BigDecimal("0.00"),
							new BigDecimal("4.00")
					),
					new BootstrapTemperatureUnitSeed(
							"Pastry freezer",
							"Back-of-house freezer",
							TemperatureUnitType.FREEZER,
							LocalTime.of(17, 30),
							new BigDecimal("-24.00"),
							new BigDecimal("-18.00")
					)
			);
			case BAR -> List.of(
					new BootstrapTemperatureUnitSeed(
							"Garnish fridge",
							"Front bar",
							TemperatureUnitType.FRIDGE,
							LocalTime.of(10, 15),
							new BigDecimal("2.00"),
							new BigDecimal("5.00")
					),
					new BootstrapTemperatureUnitSeed(
							"Frozen storage",
							"Back bar freezer room",
							TemperatureUnitType.FREEZER,
							LocalTime.of(18, 0),
							new BigDecimal("-24.00"),
							new BigDecimal("-18.00")
					)
			);
			case OTHER -> List.of(
					new BootstrapTemperatureUnitSeed(
							"Main cold storage",
							"Primary prep area",
							TemperatureUnitType.FRIDGE,
							LocalTime.of(8, 30),
							new BigDecimal("0.00"),
							new BigDecimal("4.00")
					),
					new BootstrapTemperatureUnitSeed(
							"Main freezer",
							"Primary storage room",
							TemperatureUnitType.FREEZER,
							LocalTime.of(16, 30),
							new BigDecimal("-24.00"),
							new BigDecimal("-18.00")
					)
			);
			case RESTAURANT -> List.of(
					new BootstrapTemperatureUnitSeed(
							"Prep line fridge",
							"Hot kitchen",
							TemperatureUnitType.FRIDGE,
							LocalTime.of(8, 30),
							new BigDecimal("2.00"),
							new BigDecimal("4.00")
					),
					new BootstrapTemperatureUnitSeed(
							"Dessert freezer",
							"Cold dessert station",
							TemperatureUnitType.FREEZER,
							LocalTime.of(20, 30),
							new BigDecimal("-23.00"),
							new BigDecimal("-18.00")
					)
			);
		};
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

	private ChecklistSchedule buildWeeklySchedule(LocalDate startDate, LocalTime dueTime, User actor, DayOfWeek... days) {
		return new ChecklistSchedule(
				ChecklistScheduleType.WEEKLY,
				startDate,
				null,
				dueTime,
				weekdayMask(days),
				null,
				DEFAULT_BOOTSTRAP_ZONE.getId(),
				true,
				actor,
				actor
		);
	}

	private ChecklistSchedule buildMonthlySchedule(LocalDate startDate, LocalTime dueTime, User actor, int dayOfMonth) {
		return new ChecklistSchedule(
				ChecklistScheduleType.MONTHLY,
				startDate,
				null,
				dueTime,
				null,
				dayOfMonth,
				DEFAULT_BOOTSTRAP_ZONE.getId(),
				true,
				actor,
				actor
		);
	}

	private int weekdayMask(DayOfWeek... days) {
		int mask = 0;
		for (DayOfWeek day : days) {
			mask |= 1 << (day.getValue() - 1);
		}
		return mask;
	}

	private LocalTime resolveOpeningDueTime(EstablishmentType type) {
		return switch (type) {
			case CAFE -> LocalTime.of(7, 0);
			case BAR -> LocalTime.of(15, 30);
			case OTHER -> LocalTime.of(8, 30);
			case RESTAURANT -> LocalTime.of(9, 30);
		};
	}

	private LocalTime resolveClosingDueTime(EstablishmentType type) {
		return switch (type) {
			case CAFE -> LocalTime.of(17, 0);
			case BAR -> LocalTime.of(23, 15);
			case OTHER -> LocalTime.of(18, 0);
			case RESTAURANT -> LocalTime.of(22, 0);
		};
	}

	private LocalTime resolveWeeklyFoodSafetyDueTime(EstablishmentType type) {
		return switch (type) {
			case CAFE -> LocalTime.of(13, 30);
			case BAR -> LocalTime.of(14, 0);
			case OTHER -> LocalTime.of(10, 0);
			case RESTAURANT -> LocalTime.of(14, 30);
		};
	}

	private DayOfWeek[] resolveWeeklyFoodSafetyDays(EstablishmentType type) {
		return switch (type) {
			case CAFE -> new DayOfWeek[]{DayOfWeek.MONDAY};
			case BAR -> new DayOfWeek[]{DayOfWeek.TUESDAY};
			case OTHER -> new DayOfWeek[]{DayOfWeek.WEDNESDAY};
			case RESTAURANT -> new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.THURSDAY};
		};
	}

	private LocalTime resolveMonthlyFoodSafetyDueTime(EstablishmentType type) {
		return switch (type) {
			case CAFE -> LocalTime.of(14, 0);
			case BAR -> LocalTime.of(14, 30);
			case OTHER -> LocalTime.of(10, 30);
			case RESTAURANT -> LocalTime.of(15, 0);
		};
	}

	private int resolveMonthlyFoodSafetyDayOfMonth(EstablishmentType type) {
		return switch (type) {
			case CAFE -> 6;
			case BAR -> 7;
			case OTHER -> 8;
			case RESTAURANT -> 5;
		};
	}

	private List<ChecklistTaskDefinition> buildIkMatTasks(EstablishmentType type) {
		return switch (type) {
			case CAFE -> List.of(
					measurementTask("Record milk fridge temperature", "Log the warmest reading from the dairy fridge.", 0, "C", "0", "4"),
					verificationTask("Verify pastry display tools are clean", "Confirm tongs and trays are in place before service.", 1),
					verificationTask("Confirm allergen cards are visible", "Check the pastry cabinet and counter menu.", 2),
					textTask("Document opening shift lead", "Enter the name of the barista or supervisor opening the site.", 3)
			);
			case BAR -> List.of(
					measurementTask("Record garnish fridge temperature", "Measure the warmest point in the garnish and mixer fridge.", 0, "C", "0", "4"),
					verificationTask("Verify ice scoop is stored hygienically", "Check that the scoop is separated from the ice bin.", 1),
					verificationTask("Confirm handwash station is stocked", "Soap, towels and sanitizer must be ready before guests arrive.", 2),
					textTask("Document opening floor lead", "Enter the staff member leading the opening setup.", 3)
			);
			case OTHER -> List.of(
					measurementTask("Record main cold storage temperature", "Log the warmest reading from the primary refrigerated unit.", 0, "C", "0", "4"),
					verificationTask("Verify cleaning chemicals are stored separately", "Confirm chemicals are segregated from food-contact items.", 1),
					verificationTask("Confirm waste handling area is ready", "Check liners, lids and hand hygiene supplies.", 2),
					textTask("Document opening responsible person", "Enter the person responsible for the day's compliance checks.", 3)
			);
			case RESTAURANT -> List.of(
					measurementTask("Record cold line temperature", "Log the warmest reading from the prep or service line fridge.", 0, "C", "0", "4"),
					verificationTask("Verify raw and ready-to-eat prep zones are separated", "Confirm color-coded boards and containers are staged correctly.", 1),
					verificationTask("Confirm today's allergen matrix matches the menu", "Review any updated specials or substitutions.", 2),
					textTask("Document opening kitchen lead", "Enter the name of the person responsible for opening food safety checks.", 3)
			);
		};
	}

	private List<ChecklistTaskDefinition> buildIkMatClosingTasks(EstablishmentType type) {
		return switch (type) {
			case CAFE -> List.of(
					actionTask("Discard expired milk and cream products", "Remove anything beyond the current hold time before closing.", 0),
					measurementTask("Record dishwasher final rinse temperature", "Measure the final rinse result from the closing cycle.", 1, "C", "80", "95"),
					verificationTask("Confirm pastry display is emptied and sanitized", "Check trays, doors and handles after cleaning.", 2),
					textTask("Write handover note for morning prep", "Capture shortages or items needing follow-up.", 3)
			);
			case BAR -> List.of(
					actionTask("Sanitize garnish station and bar tools", "Clean speed rails, cutting boards, knives and garnish tubs.", 0),
					measurementTask("Record glasswasher final rinse temperature", "Log the closing glasswasher rinse temperature.", 1, "C", "80", "95"),
					verificationTask("Confirm draft lines and taps are wiped down", "Check guest-facing taps, handles and drip trays.", 2),
					textTask("Write handover note for opening bartender", "Capture stock, incidents and cleaning follow-up.", 3)
			);
			case OTHER -> List.of(
					actionTask("Confirm shared prep areas are cleaned and closed down", "All benches, sinks and handles should be cleaned before lockup.", 0),
					measurementTask("Record warewashing rinse temperature", "Measure the final sanitation rinse used during close.", 1, "C", "80", "95"),
					verificationTask("Confirm waste and recycling are sealed", "Check that external bins are closed and internal bins are emptied.", 2),
					textTask("Document any outstanding maintenance issues", "Note anything that needs attention before the next service day.", 3)
			);
			case RESTAURANT -> List.of(
					actionTask("Confirm hot hold units are powered down and cleaned", "Check that all hot hold units have been cleaned after service.", 0),
					measurementTask("Record dishwasher final rinse temperature", "Measure the final rinse result from the closing cycle.", 1, "C", "80", "95"),
					verificationTask("Verify cooling labels are applied to prepared food", "Confirm prepared items have time and date labels before storage.", 2),
					textTask("Write handover note for next shift", "Capture any corrective actions or supplier issues.", 3)
			);
		};
	}

	private List<ChecklistTaskDefinition> buildIkMatWeeklyTasks(EstablishmentType type) {
		return switch (type) {
			case CAFE -> List.of(
					actionTask("Review grinder and espresso cleaning log", "Check that the weekly deep clean has been completed and signed.", 0),
					verificationTask("Audit allergen signage across the counter", "Confirm printed labels still match displayed products.", 1),
					textTask("Summarize the week's recurring food safety issues", "Document any repeated issues requiring follow-up.", 2)
			);
			case BAR -> List.of(
					actionTask("Review weekly line-clean and bar sanitation log", "Confirm the documented clean matches the physical condition of the bar.", 0),
					verificationTask("Audit backup storage labeling", "Check syrups, juices and garnishes for date and batch information.", 1),
					textTask("Capture required retraining or maintenance actions", "Document any repeated non-conformances from the week.", 2)
			);
			case OTHER -> List.of(
					actionTask("Review weekly deep-clean completion", "Confirm the shared areas completed their planned deep-clean tasks.", 0),
					verificationTask("Audit storage zoning and labeling", "Check shelves, freezers and cleaning cupboards for correct separation.", 1),
					textTask("Summarize key compliance follow-up items", "Document issues that should be tracked into the next week.", 2)
			);
			case RESTAURANT -> List.of(
					actionTask("Review weekly deep-clean completion", "Check drains, extraction hoods and prep equipment against the cleaning plan.", 0),
					verificationTask("Audit receiving and cooling records", "Confirm logs are complete and corrective actions are documented.", 1),
					textTask("Summarize the week's food safety follow-up", "Document outstanding supplier, training or maintenance actions.", 2)
			);
		};
	}

	private List<ChecklistTaskDefinition> buildIkMatMonthlyTasks(EstablishmentType type) {
		return switch (type) {
			case CAFE -> List.of(
					actionTask("Review monthly supplier and allergen updates", "Confirm product labels and allergen references are current for the month.", 0),
					verificationTask("Audit calibration of fridge thermometers", "Check the monthly calibration note for all cold storage probes.", 1),
					textTask("Record monthly food safety priorities", "Summarize actions the team should focus on this month.", 2)
			);
			case BAR -> List.of(
					actionTask("Review monthly cold-chain and line-clean records", "Confirm logs are complete and corrective actions were followed up.", 0),
					verificationTask("Audit bar-back storage labeling", "Check batch dates, open dates and label consistency in backup storage.", 1),
					textTask("Record monthly maintenance or retraining actions", "Capture follow-up work from the monthly compliance review.", 2)
			);
			case OTHER -> List.of(
					actionTask("Review monthly sanitation and maintenance plan", "Confirm the site completed planned cleaning and maintenance checks.", 0),
					verificationTask("Audit compliance storage zoning", "Check the physical separation of chemicals, disposables and service stock.", 1),
					textTask("Document monthly compliance actions", "Summarize actions that remain open after the review.", 2)
			);
			case RESTAURANT -> List.of(
					actionTask("Review monthly HACCP verification pack", "Check supplier, cooling, receiving and cleaning records for the month.", 0),
					verificationTask("Audit calibration of kitchen thermometers", "Confirm monthly calibration checks are documented for probes and temp guns.", 1),
					textTask("Record monthly kitchen compliance priorities", "Summarize the top food safety actions for the coming month.", 2)
			);
		};
	}

	private List<ChecklistTaskDefinition> buildIkAlkoholTasks() {
		return List.of(
				verificationTask("Confirm age verification signage is visible", "Check the entrance, queue point and bar service area.", 0),
				verificationTask("Verify refusal-of-service guidance is available", "Check the shift folder or digital handover notes.", 1),
				textTask("Document responsible server on shift", "Enter the name of the staff member responsible for alcohol service.", 2),
				textTask("Record planned door host coverage", "Document who is responsible for arrivals during the busiest period.", 3)
		);
	}

	private List<ChecklistTaskDefinition> buildIkAlkoholClosingTasks() {
		return List.of(
				verificationTask("Confirm guest area is cleared before licensed close", "Verify that no guests remain in service areas after closing time.", 0),
				verificationTask("Verify last-call communication was completed", "Check that last-call timing was communicated according to the venue routine.", 1),
				textTask("Write handover note for incident follow-up", "Document any alcohol-service incidents that need next-shift attention.", 2),
				textTask("Document closing manager sign-off", "Capture the name of the person who signed off the closing compliance check.", 3)
		);
	}

	private List<ChecklistTaskDefinition> buildIkAlkoholWeeklyTasks() {
		return List.of(
				actionTask("Review the weekly refusal and incident log", "Confirm entries are complete and any serious events were escalated.", 0),
				verificationTask("Audit staff training coverage for the rota", "Check that every upcoming shift has trained alcohol-service coverage.", 1),
				textTask("Document action items from the alcohol compliance review", "Summarize follow-up tasks for the coming week.", 2)
		);
	}

	private List<ChecklistTaskDefinition> buildIkAlkoholMonthlyTasks() {
		return List.of(
				actionTask("Review monthly incident and refusal trends", "Check whether repeated incidents require policy or staffing adjustments.", 0),
				verificationTask("Audit upcoming licence condition deadlines", "Confirm any inspections, signage renewals or training renewals are scheduled.", 1),
				textTask("Record monthly alcohol compliance actions", "Summarize actions to follow up before the next review.", 2)
		);
	}

	private ChecklistTaskDefinition actionTask(String title, String details, int sortOrder) {
		return new ChecklistTaskDefinition(title, details, ChecklistTaskKind.ACTION, true, sortOrder, null, null, null);
	}

	private ChecklistTaskDefinition verificationTask(String title, String details, int sortOrder) {
		return new ChecklistTaskDefinition(title, details, ChecklistTaskKind.VERIFICATION, true, sortOrder, null, null, null);
	}

	private ChecklistTaskDefinition textTask(String title, String details, int sortOrder) {
		return new ChecklistTaskDefinition(title, details, ChecklistTaskKind.TEXT_ENTRY, true, sortOrder, null, null, null);
	}

	private ChecklistTaskDefinition measurementTask(
			String title,
			String details,
			int sortOrder,
			String unit,
			String minimum,
			String maximum
	) {
		return new ChecklistTaskDefinition(
				title,
				details,
				ChecklistTaskKind.MEASUREMENT,
				true,
				sortOrder,
				unit,
				new BigDecimal(minimum),
				new BigDecimal(maximum)
		);
	}

	private void seedChecklistRunStates(Organization organization, Establishment establishment, User actor) {
		List<ChecklistRun> runs = checklistRunRepository.findByEstablishmentIdOrderByDueAtAsc(establishment.getId());
		if (runs.isEmpty()) {
			return;
		}

		Instant now = Instant.now();
		List<User> assignees = organizationMembershipRepository.findByOrganizationIdAndActiveTrueAndAccessibleEstablishmentId(
				organization.getId(),
				establishment.getId(),
				org.springframework.data.domain.PageRequest.of(0, 20)
		).getContent().stream()
				.map(OrganizationMembership::getUser)
				.filter(user -> !user.getId().equals(actor.getId()))
				.toList();

		List<ChecklistRun> bootstrapCandidates = runs.stream()
				.filter(run -> run.getEvents().size() == 1)
				.toList();

		seedCompletedRuns(bootstrapCandidates, assignees, actor, now);
		seedInProgressRuns(bootstrapCandidates, assignees, actor, now);
		seedOverdueRuns(bootstrapCandidates, assignees, actor, now);
		seedAssignedPendingRuns(bootstrapCandidates, assignees, actor, now);
	}

	private void seedCompletedRuns(List<ChecklistRun> runs, List<User> assignees, User actor, Instant now) {
		runs.stream()
				.filter(run -> run.getDueAt().isBefore(now.minusSeconds(21600)))
				.filter(run -> run.getStatus() == ChecklistRunStatus.PENDING)
				.limit(2)
				.forEach(run -> {
					assignRunIfNeeded(run, assignees, actor, now.minusSeconds(18000));
					User assignee = resolveRunAssignee(run, assignees).orElse(actor);
					run.setStatus(ChecklistRunStatus.COMPLETED);
					run.setStartedAt(run.getDueAt().minusSeconds(5400));
					run.setCompletedAt(run.getDueAt().minusSeconds(1800));
					run.setCompletedByUser(assignee);
					completeRunTasks(run, assignee, run.getCompletedAt());
					run.addEvent(new ChecklistRunEvent(
							ChecklistRunEventType.STARTED,
							assignee,
							run.getStartedAt(),
							null
					));
					run.addEvent(new ChecklistRunEvent(
							ChecklistRunEventType.COMPLETED,
							assignee,
							run.getCompletedAt(),
							null
					));
				});
	}

	private void seedInProgressRuns(List<ChecklistRun> runs, List<User> assignees, User actor, Instant now) {
		runs.stream()
				.filter(run -> !run.getDueAt().isBefore(now.minusSeconds(28800)))
				.filter(run -> !run.getDueAt().isAfter(now.plusSeconds(28800)))
				.filter(run -> run.getStatus() == ChecklistRunStatus.PENDING || run.getStatus() == ChecklistRunStatus.OVERDUE)
				.limit(2)
				.forEach(run -> {
					assignRunIfNeeded(run, assignees, actor, now.minusSeconds(3600));
					User assignee = resolveRunAssignee(run, assignees).orElse(actor);
					run.setStatus(ChecklistRunStatus.IN_PROGRESS);
					run.setStartedAt(now.minusSeconds(2700));
					seedPartialTaskProgress(run, assignee, now.minusSeconds(1200));
					run.addEvent(new ChecklistRunEvent(
							ChecklistRunEventType.STARTED,
							assignee,
							run.getStartedAt(),
							null
					));
				});
	}

	private void seedOverdueRuns(List<ChecklistRun> runs, List<User> assignees, User actor, Instant now) {
		runs.stream()
				.filter(run -> run.getDueAt().isBefore(now.minusSeconds(7200)))
				.filter(run -> run.getStatus() == ChecklistRunStatus.PENDING)
				.limit(1)
				.forEach(run -> {
					assignRunIfNeeded(run, assignees, actor, now.minusSeconds(5400));
					run.setStatus(ChecklistRunStatus.OVERDUE);
				});
	}

	private void seedAssignedPendingRuns(List<ChecklistRun> runs, List<User> assignees, User actor, Instant now) {
		runs.stream()
				.filter(run -> run.getDueAt().isAfter(now))
				.filter(run -> run.getDueAt().isBefore(now.plusSeconds(172800)))
				.filter(run -> run.getStatus() == ChecklistRunStatus.PENDING)
				.filter(run -> run.getAssignments().isEmpty())
				.limit(2)
				.forEach(run -> assignRunIfNeeded(run, assignees, actor, now.minusSeconds(1800)));
	}

	private void assignRunIfNeeded(ChecklistRun run, List<User> assignees, User actor, Instant assignedAt) {
		if (!run.getAssignments().isEmpty()) {
			return;
		}

		User assignee = resolveRunAssignee(run, assignees).orElse(actor);
		run.addAssignment(new ChecklistRunAssignment(assignee, actor, assignedAt));
		run.addEvent(new ChecklistRunEvent(
				ChecklistRunEventType.ASSIGNED,
				actor,
				assignedAt,
				null
		));
	}

	private Optional<User> resolveRunAssignee(ChecklistRun run, List<User> assignees) {
		if (!run.getAssignments().isEmpty()) {
			return Optional.of(run.getAssignments().iterator().next().getAssignedUser());
		}
		if (assignees.isEmpty()) {
			return Optional.empty();
		}
		int assigneeIndex = Math.abs(run.getTitleSnapshot().hashCode()) % assignees.size();
		return Optional.of(assignees.get(assigneeIndex));
	}

	private void completeRunTasks(ChecklistRun run, User actor, Instant resolvedAt) {
		for (ChecklistTaskExecution task : run.getTaskExecutions()) {
			applyCompletedTaskSnapshot(task, actor, resolvedAt);
		}
	}

	private void seedPartialTaskProgress(ChecklistRun run, User actor, Instant resolvedAt) {
		int completedTasks = 0;
		for (ChecklistTaskExecution task : run.getTaskExecutions()) {
			if (completedTasks >= Math.max(1, run.getTaskExecutions().size() - 1)) {
				break;
			}
			applyCompletedTaskSnapshot(task, actor, resolvedAt.plusSeconds(completedTasks * 120L));
			completedTasks++;
		}
	}

	private void applyCompletedTaskSnapshot(ChecklistTaskExecution task, User actor, Instant resolvedAt) {
		task.setExecutionStatus(ChecklistTaskExecutionStatus.COMPLETED);
		task.setResolvedAt(resolvedAt);
		task.setResolvedByUser(actor);

		switch (task.getTaskKindSnapshot()) {
			case ACTION -> task.setComment("Completed during bootstrap walkthrough.");
			case VERIFICATION -> {
				task.setVerificationResult(ChecklistVerificationResult.VERIFIED);
				task.setComment("Verified during bootstrap walkthrough.");
			}
			case MEASUREMENT -> {
				task.setMeasuredValue(resolveBootstrapMeasuredValue(task));
				task.setComment("Measured during bootstrap walkthrough.");
			}
			case TEXT_ENTRY -> task.setEnteredText("Checked and documented during bootstrap walkthrough.");
		}
	}

	private BigDecimal resolveBootstrapMeasuredValue(ChecklistTaskExecution task) {
		if (task.getMinimumAllowedValueSnapshot() != null && task.getMaximumAllowedValueSnapshot() != null) {
			return task.getMinimumAllowedValueSnapshot()
					.add(task.getMaximumAllowedValueSnapshot())
					.divide(new BigDecimal("2"));
		}
		if (task.getMaximumAllowedValueSnapshot() != null) {
			return task.getMaximumAllowedValueSnapshot().subtract(new BigDecimal("1"));
		}
		if (task.getMinimumAllowedValueSnapshot() != null) {
			return task.getMinimumAllowedValueSnapshot().add(new BigDecimal("1"));
		}
		return BigDecimal.ONE;
	}

	private List<BootstrapDeviationSeed> buildDeviationSeeds(EstablishmentType type) {
		return switch (type) {
			case BAR -> List.of(
					new BootstrapDeviationSeed(
							"ID verification missed during bar rush",
							"A guest order was accepted before ID verification was completed during peak service.",
							DeviationCategory.AGE_CONTROL,
							DeviationSeverity.CRITICAL,
							DeviationStatus.OPEN,
							"Manager asked for shift review and refresher coaching before the next weekend service."
					),
					new BootstrapDeviationSeed(
							"Garnish fridge temperature above safe range",
							"The garnish and mixer fridge was logged above the internal temperature threshold during pre-service checks.",
							DeviationCategory.TEMPERATURE,
							DeviationSeverity.HIGH,
							DeviationStatus.IN_PROGRESS,
							"Alternative cold storage was set up while the unit is being checked."
					),
					new BootstrapDeviationSeed(
							"Closing incident log not completed",
							"A refusal-of-service entry was discussed verbally but never added to the closing log.",
							DeviationCategory.DOCUMENTATION_AND_TRAINING,
							DeviationSeverity.MEDIUM,
							DeviationStatus.IN_PROGRESS,
							"Shift lead is updating the log and reviewing the handover routine with the team."
					),
					new BootstrapDeviationSeed(
							"Last-call signage missing near queue entrance",
							"The venue's serving-hours guidance was not visible at the guest queue entrance.",
							DeviationCategory.SERVING_HOURS,
							DeviationSeverity.MEDIUM,
							DeviationStatus.RESOLVED,
							"Replacement signage was installed and verified during the same shift."
					),
					new BootstrapDeviationSeed(
							"Guest removal follow-up note missing",
							"A disturbance was handled correctly, but the post-incident note was not documented.",
							DeviationCategory.INAPPROPRIATE_BEHAVIOUR,
							DeviationSeverity.MEDIUM,
							DeviationStatus.OPEN,
							"Operations manager requested a written note before the next rota handover."
					)
			);
			case CAFE -> List.of(
					new BootstrapDeviationSeed(
							"Milk fridge temperature drift during morning prep",
							"The dairy fridge warmed above the target range during the opening rush.",
							DeviationCategory.TEMPERATURE,
							DeviationSeverity.HIGH,
							DeviationStatus.OPEN,
							"Milk stock was moved and the opening checklist was repeated."
					),
					new BootstrapDeviationSeed(
							"Allergen card missing on pastry display",
							"One pastry tray was presented without the correct allergen information card.",
							DeviationCategory.ALLERGEN,
							DeviationSeverity.HIGH,
							DeviationStatus.IN_PROGRESS,
							"Front counter staff are rechecking all display labels before the next restock."
					),
					new BootstrapDeviationSeed(
							"Espresso machine backflush log not signed",
							"The end-of-day cleaning activity was completed but the log was not signed off.",
							DeviationCategory.DOCUMENTATION_AND_TRAINING,
							DeviationSeverity.LOW,
							DeviationStatus.RESOLVED,
							"Log updated and the closing team reminded to complete sign-off before clock-out."
					),
					new BootstrapDeviationSeed(
							"Reusable cup drying rack stored while still wet",
							"Clean reusable cups were stacked before the drying rack had fully dried.",
							DeviationCategory.HYGIENE,
							DeviationSeverity.MEDIUM,
							DeviationStatus.IN_PROGRESS,
							"Area was reset and the drying sequence was reviewed with the shift lead."
					)
			);
			case OTHER -> List.of(
					new BootstrapDeviationSeed(
							"Receiving log incomplete for supplier delivery",
							"A temperature-sensitive delivery was accepted without a complete receiving record.",
							DeviationCategory.DOCUMENTATION_AND_TRAINING,
							DeviationSeverity.MEDIUM,
							DeviationStatus.OPEN,
							"Receiving checklist is being retrained with the responsible team."
					),
					new BootstrapDeviationSeed(
							"Cleaning chemicals stored beside service disposables",
							"Chemical containers were found on the same shelf as disposable service items.",
							DeviationCategory.STORAGE,
							DeviationSeverity.HIGH,
							DeviationStatus.IN_PROGRESS,
							"Storage zones were relabeled and the shelf reset immediately."
					),
					new BootstrapDeviationSeed(
							"Waste area handwash supplies missing",
							"The waste handling station did not have soap and paper towels available.",
							DeviationCategory.HYGIENE,
							DeviationSeverity.MEDIUM,
							DeviationStatus.RESOLVED,
							"Supplies were restocked and minimum stock levels were added to the ordering note."
					)
			);
			case RESTAURANT -> List.of(
					new BootstrapDeviationSeed(
							"Cold storage temperature above safe range",
							"Opening check found the prep line above the internal temperature threshold.",
							DeviationCategory.TEMPERATURE,
							DeviationSeverity.HIGH,
							DeviationStatus.OPEN,
							"Temperature logged and kitchen lead notified."
					),
					new BootstrapDeviationSeed(
							"Cleaning verification missed on prep sink",
							"Closing cleaning sign-off was not completed for the prep sink area.",
							DeviationCategory.HYGIENE,
							DeviationSeverity.MEDIUM,
							DeviationStatus.IN_PROGRESS,
							"Assigned for follow-up before the next opening shift."
					),
					new BootstrapDeviationSeed(
							"Allergen label missing on ready-to-serve sauce",
							"A prepared sauce was moved into service without the updated allergen label.",
							DeviationCategory.ALLERGEN,
							DeviationSeverity.HIGH,
							DeviationStatus.IN_PROGRESS,
							"Chef on duty relabeled the batch and reviewed the transfer routine with the section."
					),
					new BootstrapDeviationSeed(
							"Cooling log incomplete for soup batch",
							"A batch cooling record was missing the final verification reading before storage.",
							DeviationCategory.DOCUMENTATION_AND_TRAINING,
							DeviationSeverity.MEDIUM,
							DeviationStatus.RESOLVED,
							"Cooling record was corrected and the shift team repeated the cooling check brief."
					),
					new BootstrapDeviationSeed(
							"Container label missing production date",
							"One prepared ingredient container did not have an updated production date label.",
							DeviationCategory.STORAGE,
							DeviationSeverity.LOW,
							DeviationStatus.RESOLVED,
							"Container relabeled and stock rotation routine reviewed."
					)
			);
		};
	}

	private void createBootstrapDeviation(
			Organization organization,
			Establishment establishment,
			User actor,
			BootstrapDeviationSeed seed
	) {
		if (deviationRepository.existsByEstablishmentIdAndTitleIgnoreCase(establishment.getId(), seed.title())) {
			return;
		}

		Deviation deviation = new Deviation(
				organization,
				establishment,
				actor,
				actor,
				seed.title(),
				seed.description(),
				seed.severity(),
				seed.category()
		);
		deviation.setStatus(seed.status());
		Instant now = Instant.now();
		deviation.addEvent(new DeviationEvent(
				DeviationEventType.REPORTED,
				actor,
				now.minusSeconds(21600),
				"Deviation reported."
		));
		if (seed.status() != DeviationStatus.OPEN) {
			deviation.addEvent(new DeviationEvent(
					DeviationEventType.ASSIGNED,
					actor,
					now.minusSeconds(14400),
					"Assigned to the responsible shift lead."
			));
			deviation.addEvent(new DeviationEvent(
					DeviationEventType.STATUS_CHANGED,
					actor,
					now.minusSeconds(10800),
					"Status changed to " + seed.status().name().toLowerCase().replace('_', ' ') + "."
			));
		}
		deviation.addEvent(new DeviationEvent(
				DeviationEventType.NOTE_ADDED,
				actor,
				now.minusSeconds(3600),
				seed.followUpNote()
		));
		deviationRepository.save(deviation);
		log.info("Created bootstrap deviation {} for {}", seed.title(), establishment.getName());
	}

	private record BootstrapOrganizationSeed(
			String name,
			org.kontrolla.organizations.domain.OrganizationStatus status,
			List<BootstrapEstablishmentSeed> establishments,
			OrganizationRole bootstrapUserRole,
			int employeeAccessSpan
	) {
	}

	private record BootstrapEstablishmentSeed(
			String name,
			EstablishmentType type,
			EstablishmentStatus status
	) {
	}

	private record BootstrapServingHoursSeed(
			DayOfWeek dayOfWeek,
			boolean closed,
			LocalTime opensAt,
			LocalTime closesAt
	) {
	}

	private record BootstrapDeviationSeed(
			String title,
			String description,
			DeviationCategory category,
			DeviationSeverity severity,
			DeviationStatus status,
			String followUpNote
	) {
	}

	private record BootstrapSeedUser(
			String email,
			String firstName,
			String lastName
	) {
	}

	private record BootstrapMembershipSeed(
			BootstrapSeedUser user,
			OrganizationRole role,
			boolean accessAllEstablishments,
			List<String> establishmentNames
	) {
	}

	private record BootstrapTemperatureUnitSeed(
			String name,
			String location,
			TemperatureUnitType type,
			LocalTime dueByTime,
			BigDecimal minimumTemperature,
			BigDecimal maximumTemperature
	) {
	}

	private record BootstrapDocumentSeed(
			DocumentServiceArea serviceArea,
			String title,
			String holderName,
			int issueDateOffsetDays,
			int renewalDateOffsetDays,
			int auditReaderCount
	) {
	}
}
