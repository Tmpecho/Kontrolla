package org.kontrolla.audit.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kontrolla.audit.domain.AuditAction;
import org.kontrolla.audit.domain.AuditEvent;
import org.kontrolla.audit.domain.AuditOutcome;
import org.kontrolla.audit.domain.AuditTargetType;
import org.kontrolla.audit.infrastructure.AuditEventRepository;
import org.kontrolla.support.TestDataCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class TransactionalAuditRecorderIntegrationTest {

	@Autowired
	private AuditRecorder auditRecorder;

	@Autowired
	private AuditEventRepository auditEventRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private TestDataCleaner testDataCleaner;

	@BeforeEach
	void setUp() {
		testDataCleaner.clearAll();
	}

	@Test
	void emitsAuditLogOnlyAfterTransactionCommit(CapturedOutput output) {
		AuditRecord auditRecord = auditRecord("commit-log@example.com");

		transactionTemplate.executeWithoutResult(status -> {
			auditRecorder.record(auditRecord);
			assertThat(auditEventRepository.findAll()).hasSize(1);
			assertThat(output.getOut()).doesNotContain("commit-log@example.com");
		});

		AuditEvent auditEvent = singleAuditEvent();
		assertThat(output.getOut()).contains("\"eventId\":\"" + auditEvent.getId() + "\"");
		assertThat(output.getOut()).contains("\"action\":\"USER_CREATE\"");
		assertThat(output.getOut()).contains("\"outcome\":\"SUCCESS\"");
		assertThat(output.getOut()).contains("\"resultCode\":\"admin_user_created\"");
		assertThat(output.getOut()).contains("commit-log@example.com");
	}

	@Test
	void rolledBackAuditWriteDoesNotPersistOrLog(CapturedOutput output) {
		transactionTemplate.executeWithoutResult(status -> {
			auditRecorder.record(auditRecord("rollback-log@example.com"));
			status.setRollbackOnly();
		});

		assertThat(auditEventRepository.findAll()).isEmpty();
		assertThat(output.getOut()).doesNotContain("rollback-log@example.com");
		assertThat(output.getOut()).doesNotContain("\"action\":\"USER_CREATE\"");
	}

	@Test
	void userCreationAuditLogIncludesCoreFieldsAndOmitsPassword(CapturedOutput output) {
		transactionTemplate.executeWithoutResult(status -> auditRecorder.record(auditRecord("structured-log@example.com")));

		AuditEvent auditEvent = singleAuditEvent();
		assertThat(output.getOut()).contains("\"eventId\":\"" + auditEvent.getId() + "\"");
		assertThat(output.getOut()).contains("\"action\":\"USER_CREATE\"");
		assertThat(output.getOut()).contains("\"targetType\":\"USER\"");
		assertThat(output.getOut()).contains("\"resultCode\":\"admin_user_created\"");
		assertThat(output.getOut()).contains("\"createdEmail\":\"structured-log@example.com\"");
		assertThat(output.getOut()).doesNotContain("password123");
	}

	private AuditRecord auditRecord(String createdEmail) {
		return AuditRecord.builder(AuditAction.USER_CREATE, AuditOutcome.SUCCESS, "admin_user_created")
				.target(AuditTargetType.USER, UUID.randomUUID())
				.metadata("createdEmail", createdEmail)
				.metadata("active", false)
				.metadata("globalRoles", List.of("PLATFORM_ADMIN"))
				.metadata("creationPath", "admin")
				.build();
	}

	private AuditEvent singleAuditEvent() {
		List<AuditEvent> auditEvents = auditEventRepository.findAll();
		assertThat(auditEvents).hasSize(1);
		return auditEvents.getFirst();
	}
}
