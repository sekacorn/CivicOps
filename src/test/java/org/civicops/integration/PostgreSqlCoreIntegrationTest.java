package org.civicops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "civicops.security.jwt.secret=0123456789abcdef0123456789abcdef")
class PostgreSqlCoreIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("civicops_test").withUsername("civicops").withPassword("civicops");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;

    @Test
    void allFlywayMigrationsInitializeCleanPostgreSql() {
        Integer migrations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        assertThat(migrations).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refresh_token", Integer.class)).isZero();
    }

    @Test
    @Transactional
    void emailUniquenessIsCaseInsensitive() {
        insertUser(UUID.randomUUID(), "Person@Example.org");
        assertThatThrownBy(() -> insertUser(UUID.randomUUID(), "person@example.org"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void duplicateMembershipIsRejected() {
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        insertOrganization(organizationId, "NONPROFIT");
        insertUser(userId, "member@example.org");
        insertMembership(UUID.randomUUID(), organizationId, userId, "VIEWER");
        assertThatThrownBy(() -> insertMembership(UUID.randomUUID(), organizationId, userId, "ORG_ADMIN"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void invalidOrganizationTypeIsRejected() {
        assertThatThrownBy(() -> insertOrganization(UUID.randomUUID(), "INVALID_TYPE"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void invalidMembershipRoleIsRejected() {
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        insertOrganization(organizationId, "NONPROFIT");
        insertUser(userId, "role@example.org");
        assertThatThrownBy(() -> insertMembership(UUID.randomUUID(), organizationId, userId, "SYSTEM_ADMIN"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertOrganization(UUID id, String type) {
        jdbc.update("INSERT INTO organization (id, name, organization_type) VALUES (?, ?, ?)", id, "Test Org", type);
    }

    private void insertUser(UUID id, String email) {
        jdbc.update("INSERT INTO app_user (id, first_name, last_name, email, password_hash) VALUES (?, ?, ?, ?, ?)",
                id, "Test", "User", email, "$2a$10$abcdefghijklmnopqrstuvwxyz123456789012345678901234567");
    }

    private void insertMembership(UUID id, UUID organizationId, UUID userId, String role) {
        jdbc.update("INSERT INTO organization_membership (id, organization_id, user_id, role) VALUES (?, ?, ?, ?)",
                id, organizationId, userId, role);
    }
}
