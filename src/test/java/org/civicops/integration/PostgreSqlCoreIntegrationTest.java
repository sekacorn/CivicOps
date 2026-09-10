package org.civicops.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import org.civicops.donations.campaign.*;
import org.civicops.donations.campaign.dto.CreateCampaignRequest;
import org.civicops.donations.donation.*;
import org.civicops.donations.donation.dto.CreateDonationRequest;
import org.civicops.donations.donor.*;
import org.civicops.donations.donor.dto.CreateDonorRequest;
import org.civicops.donations.reporting.DonationReportingService;
import org.civicops.events.event.EventService;
import org.civicops.events.event.EventStatus;
import org.civicops.events.registration.EventRegistrationService;
import org.civicops.events.registration.RegistrationStatus;
import org.civicops.events.registration.dto.CreateRegistrationRequest;
import org.civicops.events.reporting.EventReportingService;
import org.civicops.grants.expense.*;
import org.civicops.grants.expense.dto.CreateGrantExpenseRequest;
import org.civicops.grants.grant.*;
import org.civicops.grants.grant.dto.CreateGrantRequest;
import org.civicops.grants.reporting.GrantReportingService;
import org.civicops.scholarships.applicant.dto.CreateScholarshipApplicantRequest;
import org.civicops.scholarships.application.dto.CreateScholarshipApplicationRequest;
import org.civicops.scholarships.award.ScholarshipAwardStatus;
import org.civicops.scholarships.award.dto.CreateScholarshipAwardRequest;
import org.civicops.scholarships.program.ScholarshipProgramStatus;
import org.civicops.scholarships.program.dto.CreateScholarshipProgramRequest;
import org.civicops.scholarships.review.ReviewRecommendation;
import org.civicops.scholarships.review.dto.SubmitReviewRequest;
import org.civicops.volunteers.assignment.AssignmentService;
import org.civicops.volunteers.hours.HourEntryService;
import org.civicops.volunteers.hours.HourEntryStatus;
import org.civicops.volunteers.opportunity.OpportunityService;
import org.civicops.volunteers.opportunity.OpportunityStatus;
import org.civicops.volunteers.reporting.VolunteerReportingService;
import org.civicops.volunteers.volunteer.VolunteerService;
import org.civicops.volunteers.volunteer.VolunteerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "civicops.security.jwt.secret=0123456789abcdef0123456789abcdef")
class PostgreSqlCoreIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("civicops_test")
          .withUsername("civicops")
          .withPassword("civicops");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired AssignmentService assignmentService;
  @Autowired VolunteerService volunteerService;
  @Autowired OpportunityService opportunityService;
  @Autowired HourEntryService hourEntryService;
  @Autowired VolunteerReportingService reportingService;
  @Autowired GrantService grantService;
  @Autowired GrantExpenseService grantExpenseService;
  @Autowired GrantReportingService grantReportingService;
  @Autowired DonorService donorService;
  @Autowired DonationCampaignService donationCampaignService;
  @Autowired DonationService donationService;
  @Autowired DonationReportingService donationReportingService;
  @Autowired EventRegistrationService eventRegistrationService;
  @Autowired EventService eventService;
  @Autowired EventReportingService eventReportingService;
  @Autowired org.civicops.cases.client.ClientService clientService;
  @Autowired org.civicops.cases.casefile.CaseRecordService caseRecordService;
  @Autowired org.civicops.cases.note.CaseNoteService caseNoteService;
  @Autowired org.civicops.cases.task.CaseTaskService caseTaskService;
  @Autowired org.civicops.cases.service.CaseServiceRecordService caseServiceRecordService;
  @Autowired org.civicops.cases.reporting.CaseReportingService caseReportingService;
  @Autowired org.civicops.equipment.category.EquipmentCategoryService equipmentCategoryService;
  @Autowired org.civicops.equipment.asset.EquipmentAssetService equipmentAssetService;
  @Autowired org.civicops.equipment.checkout.EquipmentCheckoutService equipmentCheckoutService;

  @Autowired
  org.civicops.equipment.maintenance.EquipmentMaintenanceService equipmentMaintenanceService;

  @Autowired org.civicops.equipment.reporting.EquipmentReportingService equipmentReportingService;
  @Autowired org.civicops.facilities.facility.FacilityService facilityService;
  @Autowired org.civicops.facilities.space.FacilitySpaceService facilitySpaceService;
  @Autowired org.civicops.facilities.availability.OperatingHoursService operatingHoursService;
  @Autowired org.civicops.facilities.availability.AvailabilityService facilityAvailabilityService;
  @Autowired org.civicops.facilities.availability.FacilityBlackoutService facilityBlackoutService;

  @Autowired
  org.civicops.facilities.reservation.FacilityReservationService facilityReservationService;

  @Autowired org.civicops.facilities.reporting.FacilityReportingService facilityReportingService;
  @Autowired org.civicops.scholarships.program.ScholarshipProgramService scholarshipProgramService;

  @Autowired
  org.civicops.scholarships.applicant.ScholarshipApplicantService scholarshipApplicantService;

  @Autowired
  org.civicops.scholarships.application.ScholarshipApplicationService scholarshipApplicationService;

  @Autowired org.civicops.scholarships.review.ScholarshipReviewService scholarshipReviewService;
  @Autowired org.civicops.scholarships.award.ScholarshipAwardService scholarshipAwardService;

  @Autowired
  org.civicops.scholarships.reporting.ScholarshipReportingService scholarshipReportingService;

  @Autowired org.civicops.foodpantry.pantry.FoodPantryLocationService foodPantryLocationService;
  @Autowired org.civicops.foodpantry.item.PantryItemService pantryItemService;
  @Autowired org.civicops.foodpantry.inventory.PantryInventoryService pantryInventoryService;
  @Autowired org.civicops.foodpantry.household.PantryHouseholdService pantryHouseholdService;

  @Autowired
  org.civicops.foodpantry.distribution.PantryDistributionService pantryDistributionService;

  @Autowired
  org.civicops.foodpantry.reporting.FoodPantryReportingService foodPantryReportingService;

  @Autowired org.civicops.board.member.BoardMemberService boardMemberService;
  @Autowired org.civicops.board.committee.BoardCommitteeService boardCommitteeService;
  @Autowired org.civicops.board.meeting.BoardMeetingService boardMeetingService;
  @Autowired org.civicops.board.agenda.BoardAgendaService boardAgendaService;
  @Autowired org.civicops.board.motion.BoardMotionService boardMotionService;
  @Autowired org.civicops.board.minutes.BoardMinutesService boardMinutesService;
  @Autowired org.civicops.board.resolution.BoardResolutionService boardResolutionService;
  @Autowired org.civicops.board.reporting.BoardReportingService boardReportingService;

  @Autowired
  org.civicops.grantreporting.template.GrantReportTemplateService grantReportTemplateService;

  @Autowired org.civicops.grantreporting.report.GrantReportService grantReportService;
  @Autowired org.civicops.grantreporting.export.GrantReportExportService grantReportExportService;

  @Test
  void allFlywayMigrationsInitializeCleanPostgreSql() {
    Integer migrations =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class);
    assertThat(migrations).isEqualTo(14);
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.refresh_token')", String.class))
        .isEqualTo("refresh_token");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.volunteer')", String.class))
        .isEqualTo("volunteer");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.grant_record')", String.class))
        .isEqualTo("grant_record");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.grant_expense')", String.class))
        .isEqualTo("grant_expense");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.donor')", String.class))
        .isEqualTo("donor");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.donation')", String.class))
        .isEqualTo("donation");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.donation_campaign')", String.class))
        .isEqualTo("donation_campaign");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.event_record')", String.class))
        .isEqualTo("event_record");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.event_registration')", String.class))
        .isEqualTo("event_registration");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.client')", String.class))
        .isEqualTo("client");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.case_record')", String.class))
        .isEqualTo("case_record");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.case_note')", String.class))
        .isEqualTo("case_note");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.case_task')", String.class))
        .isEqualTo("case_task");
    assertThat(
            jdbc.queryForObject("SELECT to_regclass('public.case_service_record')", String.class))
        .isEqualTo("case_service_record");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.equipment_category')", String.class))
        .isEqualTo("equipment_category");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.equipment_asset')", String.class))
        .isEqualTo("equipment_asset");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.equipment_checkout')", String.class))
        .isEqualTo("equipment_checkout");
    assertThat(
            jdbc.queryForObject("SELECT to_regclass('public.equipment_maintenance')", String.class))
        .isEqualTo("equipment_maintenance");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.facility')", String.class))
        .isEqualTo("facility");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.facility_space')", String.class))
        .isEqualTo("facility_space");
    assertThat(
            jdbc.queryForObject(
                "SELECT to_regclass('public.facility_operating_hours')", String.class))
        .isEqualTo("facility_operating_hours");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.facility_blackout')", String.class))
        .isEqualTo("facility_blackout");
    assertThat(
            jdbc.queryForObject("SELECT to_regclass('public.facility_reservation')", String.class))
        .isEqualTo("facility_reservation");
    assertThat(
            jdbc.queryForObject("SELECT to_regclass('public.scholarship_program')", String.class))
        .isEqualTo("scholarship_program");
    assertThat(
            jdbc.queryForObject("SELECT to_regclass('public.scholarship_applicant')", String.class))
        .isEqualTo("scholarship_applicant");
    assertThat(
            jdbc.queryForObject(
                "SELECT to_regclass('public.scholarship_application')", String.class))
        .isEqualTo("scholarship_application");
    assertThat(
            jdbc.queryForObject("SELECT to_regclass('public.scholarship_document')", String.class))
        .isEqualTo("scholarship_document");
    assertThat(
            jdbc.queryForObject(
                "SELECT to_regclass('public.scholarship_review_assignment')", String.class))
        .isEqualTo("scholarship_review_assignment");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.scholarship_review')", String.class))
        .isEqualTo("scholarship_review");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.scholarship_award')", String.class))
        .isEqualTo("scholarship_award");
    assertThat(
            jdbc.queryForObject("SELECT to_regclass('public.food_pantry_location')", String.class))
        .isEqualTo("food_pantry_location");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.pantry_item')", String.class))
        .isEqualTo("pantry_item");
    assertThat(
            jdbc.queryForObject("SELECT to_regclass('public.pantry_inventory_lot')", String.class))
        .isEqualTo("pantry_inventory_lot");
    assertThat(
            jdbc.queryForObject(
                "SELECT to_regclass('public.pantry_inventory_transaction')", String.class))
        .isEqualTo("pantry_inventory_transaction");
    assertThat(jdbc.queryForObject("SELECT to_regclass('public.pantry_household')", String.class))
        .isEqualTo("pantry_household");
    assertThat(
            jdbc.queryForObject(
                "SELECT to_regclass('public.pantry_distribution_visit')", String.class))
        .isEqualTo("pantry_distribution_visit");
    assertThat(
            jdbc.queryForObject(
                "SELECT to_regclass('public.pantry_distribution_item')", String.class))
        .isEqualTo("pantry_distribution_item");
    for (String table :
        List.of(
            "board_member",
            "board_term",
            "board_officer_assignment",
            "board_committee",
            "board_committee_membership",
            "board_meeting",
            "board_meeting_attendance",
            "board_agenda_item",
            "board_motion",
            "board_vote",
            "board_meeting_minutes",
            "board_resolution"))
      assertThat(jdbc.queryForObject("SELECT to_regclass('public." + table + "')", String.class))
          .isEqualTo(table);
    for (String table :
        List.of(
            "grant_report_template",
            "grant_report_template_section",
            "grant_report",
            "grant_report_section",
            "grant_report_evidence_snapshot"))
      assertThat(jdbc.queryForObject("SELECT to_regclass('public." + table + "')", String.class))
          .isEqualTo(table);
  }

  @Test
  void caseOperationalWorkflowPreservesHistoryAndReportsPostgreSqlState() {
    UUID orgId = UUID.randomUUID(), manager = UUID.randomUUID(), worker = UUID.randomUUID();
    insertOrganization(orgId, "NONPROFIT");
    insertUser(manager, "case-manager-" + manager + "@example.org");
    insertUser(worker, "case-worker-" + worker + "@example.org");
    insertMembership(UUID.randomUUID(), orgId, manager, "CASE_MANAGER");
    insertMembership(UUID.randomUUID(), orgId, worker, "CASE_WORKER");
    var client =
        clientService.create(
            orgId,
            new org.civicops.cases.client.dto.CreateClientRequest(
                "CLIENT-1",
                "Ada",
                "Lovelace",
                null,
                LocalDate.of(1990, 1, 1),
                " ADA@EXAMPLE.ORG ",
                null,
                null,
                null,
                null,
                null,
                null,
                "US",
                null));
    var record =
        caseRecordService.create(
            orgId,
            manager,
            new org.civicops.cases.casefile.dto.CreateCaseRequest(
                client.id(),
                "CASE-1",
                "Housing support",
                null,
                org.civicops.cases.casefile.CaseType.HOUSING,
                org.civicops.cases.casefile.CasePriority.HIGH,
                LocalDate.now().minusDays(2),
                "Housing",
                "Referral"));
    caseRecordService.assign(orgId, record.id(), worker);
    caseRecordService.transition(
        orgId, record.id(), org.civicops.cases.casefile.CaseStatus.IN_PROGRESS);
    caseNoteService.create(
        orgId,
        record.id(),
        worker,
        new org.civicops.cases.note.dto.CreateCaseNoteRequest(
            org.civicops.cases.note.CaseNoteType.PROGRESS, "Initial assessment", true));
    var task =
        caseTaskService.create(
            orgId,
            record.id(),
            worker,
            new org.civicops.cases.task.dto.CreateCaseTaskRequest(
                "Submit application",
                null,
                worker,
                LocalDate.now().minusDays(1),
                org.civicops.cases.casefile.CasePriority.URGENT));
    caseServiceRecordService.create(
        orgId,
        record.id(),
        new org.civicops.cases.service.dto.CreateCaseServiceRequest(
            org.civicops.cases.service.CaseServiceType.HOUSING_ASSISTANCE,
            "Rental referral",
            LocalDate.now(),
            BigDecimal.ONE,
            "referral",
            new BigDecimal("125.50"),
            worker,
            "Delivered"));
    assertThat(caseReportingService.summary(orgId, null, null).overdueTasks()).isEqualTo(1);
    assertThat(caseReportingService.workload(orgId))
        .singleElement()
        .satisfies(x -> assertThat(x.inProgressCases()).isEqualTo(1));
    assertThat(caseReportingService.services(orgId, null, null))
        .singleElement()
        .satisfies(
            x -> {
              assertThat(x.serviceCount()).isEqualTo(1);
              assertThat(x.totalValueAmount()).isEqualByComparingTo("125.50");
            });
    caseTaskService.complete(orgId, record.id(), task.id());
    caseRecordService.transition(
        orgId, record.id(), org.civicops.cases.casefile.CaseStatus.ON_HOLD);
    caseRecordService.transition(
        orgId, record.id(), org.civicops.cases.casefile.CaseStatus.IN_PROGRESS);
    caseRecordService.transition(orgId, record.id(), org.civicops.cases.casefile.CaseStatus.CLOSED);
    assertThatThrownBy(
            () ->
                caseNoteService.create(
                    orgId,
                    record.id(),
                    worker,
                    new org.civicops.cases.note.dto.CreateCaseNoteRequest(
                        org.civicops.cases.note.CaseNoteType.GENERAL, "Late note", false)))
        .isInstanceOf(org.civicops.shared.exception.BusinessRuleException.class);
    LocalDate applicationToday = LocalDate.now(ZoneOffset.UTC);
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM case_record WHERE id=?", String.class, record.id()))
        .isEqualTo("CLOSED");
    assertThat(
            jdbc.queryForObject(
                "SELECT closed_date IS NOT NULL FROM case_record WHERE id=?",
                Boolean.class,
                record.id()))
        .isTrue();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM case_note WHERE case_id=?", Integer.class, record.id()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM case_task WHERE case_id=? AND status='COMPLETED'",
                Integer.class,
                record.id()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM case_service_record WHERE case_id=?",
                Integer.class,
                record.id()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject("SELECT active FROM client WHERE id=?", Boolean.class, client.id()))
        .isTrue();
    assertThat(
            caseReportingService
                .summary(orgId, applicationToday.minusDays(1), applicationToday)
                .casesClosedInPeriod())
        .isEqualTo(1);
  }

  @Test
  void caseNumberUniquenessIsOrganizationScoped() {
    UUID orgA = UUID.randomUUID(), orgB = UUID.randomUUID(), user = UUID.randomUUID();
    insertOrganization(orgA, "NONPROFIT");
    insertOrganization(orgB, "NONPROFIT");
    insertUser(user, "case-number-" + user + "@example.org");
    var a =
        clientService.create(
            orgA,
            new org.civicops.cases.client.dto.CreateClientRequest(
                null, "A", "Client", null, null, null, null, null, null, null, null, null, null,
                null));
    var b =
        clientService.create(
            orgB,
            new org.civicops.cases.client.dto.CreateClientRequest(
                null, "B", "Client", null, null, null, null, null, null, null, null, null, null,
                null));
    var requestA =
        new org.civicops.cases.casefile.dto.CreateCaseRequest(
            a.id(),
            "SHARED-1",
            "Case",
            null,
            org.civicops.cases.casefile.CaseType.OTHER,
            org.civicops.cases.casefile.CasePriority.NORMAL,
            null,
            null,
            null);
    caseRecordService.create(orgA, user, requestA);
    assertThatThrownBy(() -> caseRecordService.create(orgA, user, requestA))
        .isInstanceOf(org.civicops.shared.exception.ConflictException.class);
    assertThat(
            caseRecordService
                .create(
                    orgB,
                    user,
                    new org.civicops.cases.casefile.dto.CreateCaseRequest(
                        b.id(),
                        "SHARED-1",
                        "Case",
                        null,
                        org.civicops.cases.casefile.CaseType.OTHER,
                        org.civicops.cases.casefile.CasePriority.NORMAL,
                        null,
                        null,
                        null))
                .caseNumber())
        .isEqualTo("SHARED-1");
  }

  @Test
  void caseDatabaseConstraintsRejectCrossOrganizationRelationships() {
    UUID orgA = UUID.randomUUID(),
        orgB = UUID.randomUUID(),
        user = UUID.randomUUID(),
        clientA = UUID.randomUUID(),
        caseA = UUID.randomUUID();
    insertOrganization(orgA, "NONPROFIT");
    insertOrganization(orgB, "NONPROFIT");
    insertUser(user, "case-fk-" + user + "@example.org");
    jdbc.update(
        "INSERT INTO client(id,organization_id,first_name,last_name) VALUES (?,?,'A','Client')",
        clientA,
        orgA);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO case_record(id,organization_id,client_id,case_number,title,case_type,priority,status,opened_date,created_by_user_id) VALUES (?,?,?,'BAD','Bad','OTHER','NORMAL','OPEN',CURRENT_DATE,?)",
                    UUID.randomUUID(),
                    orgB,
                    clientA,
                    user))
        .isInstanceOf(DataIntegrityViolationException.class);
    jdbc.update(
        "INSERT INTO case_record(id,organization_id,client_id,case_number,title,case_type,priority,status,opened_date,created_by_user_id) VALUES (?,?,?,'GOOD','Good','OTHER','NORMAL','OPEN',CURRENT_DATE,?)",
        caseA,
        orgA,
        clientA,
        user);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO case_note(id,organization_id,case_id,author_user_id,note_type,content) VALUES (?,?,?,?,'GENERAL','bad')",
                    UUID.randomUUID(),
                    orgB,
                    caseA,
                    user))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO case_task(id,organization_id,case_id,title,priority,created_by_user_id) VALUES (?,?,?,'bad','NORMAL',?)",
                    UUID.randomUUID(),
                    orgB,
                    caseA,
                    user))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO case_service_record(id,organization_id,case_id,service_type,service_date) VALUES (?,?,?,'OTHER',CURRENT_DATE)",
                    UUID.randomUUID(),
                    orgB,
                    caseA))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void equipmentOperationalLifecycleOverdueAndReportingUsePostgreSql() {
    UUID orgId = UUID.randomUUID(), manager = UUID.randomUUID();
    insertOrganization(orgId, "NONPROFIT");
    insertUser(manager, "equipment-manager-" + manager + "@example.org");
    insertMembership(UUID.randomUUID(), orgId, manager, "EQUIPMENT_MANAGER");
    var category =
        equipmentCategoryService.create(
            orgId,
            new org.civicops.equipment.category.dto.CreateEquipmentCategoryRequest(
                " Laptops ", "Portable computers"));
    assertThatThrownBy(
            () ->
                equipmentCategoryService.create(
                    orgId,
                    new org.civicops.equipment.category.dto.CreateEquipmentCategoryRequest(
                        "laptops", null)))
        .isInstanceOf(org.civicops.shared.exception.ConflictException.class);
    var asset =
        equipmentAssetService.create(
            orgId,
            new org.civicops.equipment.asset.dto.CreateEquipmentAssetRequest(
                "LAP-001",
                "Laptop",
                null,
                category.id(),
                "Framework",
                "13",
                null,
                LocalDate.now(),
                new BigDecimal("1200.00"),
                org.civicops.equipment.asset.EquipmentCondition.GOOD,
                "Office",
                null));
    var first =
        equipmentCheckoutService.checkout(
            orgId,
            asset.id(),
            manager,
            new org.civicops.equipment.checkout.dto.CreateEquipmentCheckoutRequest(
                null,
                "External Borrower",
                " EXTERNAL@EXAMPLE.ORG ",
                Instant.now().plus(Duration.ofDays(3)),
                org.civicops.equipment.asset.EquipmentCondition.GOOD,
                "Issued"));
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM equipment_asset WHERE id=?", String.class, asset.id()))
        .isEqualTo("CHECKED_OUT");
    equipmentCheckoutService.checkIn(
        orgId,
        first.id(),
        manager,
        new org.civicops.equipment.checkout.dto.CheckInEquipmentRequest(
            org.civicops.equipment.asset.EquipmentCondition.GOOD, "Returned"));
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM equipment_asset WHERE id=?", String.class, asset.id()))
        .isEqualTo("AVAILABLE");
    var second =
        equipmentCheckoutService.checkout(
            orgId,
            asset.id(),
            manager,
            new org.civicops.equipment.checkout.dto.CreateEquipmentCheckoutRequest(
                manager,
                null,
                null,
                Instant.now().plus(Duration.ofDays(3)),
                org.civicops.equipment.asset.EquipmentCondition.GOOD,
                null));
    equipmentCheckoutService.checkIn(
        orgId,
        second.id(),
        manager,
        new org.civicops.equipment.checkout.dto.CheckInEquipmentRequest(
            org.civicops.equipment.asset.EquipmentCondition.DAMAGED, "Screen damaged"));
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM equipment_asset WHERE id=?", String.class, asset.id()))
        .isEqualTo("MAINTENANCE");
    var maintenance =
        equipmentMaintenanceService.create(
            orgId,
            asset.id(),
            manager,
            new org.civicops.equipment.maintenance.dto.CreateMaintenanceRequest(
                org.civicops.equipment.maintenance.MaintenanceType.REPAIR,
                "Replace screen",
                null,
                new BigDecimal("75.25"),
                "Repair Shop",
                null));
    equipmentMaintenanceService.start(orgId, maintenance.id());
    equipmentMaintenanceService.complete(
        orgId,
        maintenance.id(),
        manager,
        new org.civicops.equipment.maintenance.dto.CompleteMaintenanceRequest(
            org.civicops.equipment.asset.EquipmentCondition.GOOD));
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM equipment_asset WHERE id=?", String.class, asset.id()))
        .isEqualTo("AVAILABLE");
    var overdue =
        equipmentCheckoutService.checkout(
            orgId,
            asset.id(),
            manager,
            new org.civicops.equipment.checkout.dto.CreateEquipmentCheckoutRequest(
                null,
                "Late Borrower",
                null,
                Instant.now().plus(Duration.ofDays(1)),
                org.civicops.equipment.asset.EquipmentCondition.GOOD,
                null));
    jdbc.update(
        "UPDATE equipment_checkout SET checked_out_at=CURRENT_TIMESTAMP-INTERVAL '2 days',due_at=CURRENT_TIMESTAMP-INTERVAL '1 day' WHERE id=?",
        overdue.id());
    var summary = equipmentReportingService.summary(orgId);
    assertThat(summary.totalAssets()).isEqualTo(1);
    assertThat(summary.checkedOutAssets()).isEqualTo(1);
    assertThat(summary.overdueCheckouts()).isEqualTo(1);
    var utilization =
        equipmentReportingService.utilization(
            orgId, LocalDate.now().minusDays(10), LocalDate.now().plusDays(1));
    assertThat(utilization.checkoutCount()).isEqualTo(3);
    assertThat(utilization.mostFrequentlyCheckedOut())
        .singleElement()
        .satisfies(x -> assertThat(x.checkoutCount()).isEqualTo(3));
    var report =
        equipmentReportingService.maintenance(
            orgId, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
    assertThat(report.totalCost()).isEqualByComparingTo("75.25");
    assertThat(report.recordsByType())
        .containsEntry(org.civicops.equipment.maintenance.MaintenanceType.REPAIR, 1L);
  }

  @Test
  void concurrentEquipmentCheckoutCreatesExactlyOneActiveCheckout() throws Exception {
    UUID orgId = UUID.randomUUID(), manager = UUID.randomUUID();
    insertOrganization(orgId, "NONPROFIT");
    insertUser(manager, "equipment-race-" + manager + "@example.org");
    insertMembership(UUID.randomUUID(), orgId, manager, "EQUIPMENT_MANAGER");
    var asset =
        equipmentAssetService.create(
            orgId,
            new org.civicops.equipment.asset.dto.CreateEquipmentAssetRequest(
                "RACE-1",
                "Race Laptop",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                org.civicops.equipment.asset.EquipmentCondition.GOOD,
                null,
                null));
    var a =
        new org.civicops.equipment.checkout.dto.CreateEquipmentCheckoutRequest(
            null,
            "Borrower A",
            null,
            Instant.now().plus(Duration.ofDays(1)),
            org.civicops.equipment.asset.EquipmentCondition.GOOD,
            null);
    var b =
        new org.civicops.equipment.checkout.dto.CreateEquipmentCheckoutRequest(
            null,
            "Borrower B",
            null,
            Instant.now().plus(Duration.ofDays(1)),
            org.civicops.equipment.asset.EquipmentCondition.GOOD,
            null);
    List<Boolean> outcomes =
        race(
            () -> equipmentCheckoutService.checkout(orgId, asset.id(), manager, a),
            () -> equipmentCheckoutService.checkout(orgId, asset.id(), manager, b));
    assertThat(outcomes).containsExactlyInAnyOrder(true, false);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM equipment_checkout WHERE equipment_asset_id=? AND status='ACTIVE'",
                Integer.class,
                asset.id()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM equipment_asset WHERE id=?", String.class, asset.id()))
        .isEqualTo("CHECKED_OUT");
  }

  @Test
  void equipmentUniquenessAndOrganizationConstraintsAreEnforcedByPostgreSql() {
    UUID orgA = UUID.randomUUID(),
        orgB = UUID.randomUUID(),
        userA = UUID.randomUUID(),
        userB = UUID.randomUUID();
    insertOrganization(orgA, "NONPROFIT");
    insertOrganization(orgB, "NONPROFIT");
    insertUser(userA, "equipment-db-a-" + userA + "@example.org");
    insertUser(userB, "equipment-db-b-" + userB + "@example.org");
    insertMembership(UUID.randomUUID(), orgA, userA, "EQUIPMENT_MANAGER");
    insertMembership(UUID.randomUUID(), orgB, userB, "EQUIPMENT_MANAGER");
    var a =
        equipmentAssetService.create(
            orgA,
            new org.civicops.equipment.asset.dto.CreateEquipmentAssetRequest(
                "SHARED-TAG",
                "A",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                org.civicops.equipment.asset.EquipmentCondition.GOOD,
                null,
                null));
    assertThatThrownBy(
            () ->
                equipmentAssetService.create(
                    orgA,
                    new org.civicops.equipment.asset.dto.CreateEquipmentAssetRequest(
                        "SHARED-TAG",
                        "Duplicate",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        org.civicops.equipment.asset.EquipmentCondition.GOOD,
                        null,
                        null)))
        .isInstanceOf(org.civicops.shared.exception.ConflictException.class);
    assertThat(
            equipmentAssetService
                .create(
                    orgB,
                    new org.civicops.equipment.asset.dto.CreateEquipmentAssetRequest(
                        "SHARED-TAG",
                        "B",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        org.civicops.equipment.asset.EquipmentCondition.GOOD,
                        null,
                        null))
                .assetTag())
        .isEqualTo("SHARED-TAG");
    UUID categoryA = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO equipment_category(id,organization_id,name,normalized_name)VALUES (?,?,'Tools','tools')",
        categoryA,
        orgA);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO equipment_asset(id,organization_id,asset_tag,name,category_id,condition,status)VALUES (?,?, 'BAD-CAT','Bad',?,'GOOD','AVAILABLE')",
                    UUID.randomUUID(),
                    orgB,
                    categoryA))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO equipment_checkout(id,organization_id,equipment_asset_id,borrower_name,checked_out_at,due_at,checkout_condition,status,issued_by_user_id)VALUES (?,?,?,'Bad',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP+INTERVAL '1 day','GOOD','ACTIVE',?)",
                    UUID.randomUUID(),
                    orgB,
                    a.id(),
                    userB))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO equipment_maintenance(id,organization_id,equipment_asset_id,maintenance_type,description,started_at,status,created_by_user_id)VALUES (?,?,?,'OTHER','Bad',CURRENT_TIMESTAMP,'OPEN',?)",
                    UUID.randomUUID(),
                    orgB,
                    a.id(),
                    userB))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO equipment_checkout(id,organization_id,equipment_asset_id,borrower_name,checked_out_at,due_at,checkout_condition,status,issued_by_user_id)VALUES (?,?,?,'Bad dates',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'GOOD','ACTIVE',?)",
                    UUID.randomUUID(),
                    orgA,
                    a.id(),
                    userA))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void facilityWorkflowTimezoneBlackoutEventIsolationAndReportingUsePostgreSql() {
    UUID orgId = UUID.randomUUID(), manager = UUID.randomUUID();
    insertOrganization(orgId, "NONPROFIT");
    insertUser(manager, "facility-manager-" + manager + "@example.org");
    insertMembership(UUID.randomUUID(), orgId, manager, "FACILITY_MANAGER");
    assertThatThrownBy(
            () ->
                facilityService.create(
                    orgId,
                    new org.civicops.facilities.facility.dto.CreateFacilityRequest(
                        "Bad",
                        null,
                        org.civicops.facilities.facility.FacilityType.OTHER,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Mars/Olympus",
                        null)))
        .isInstanceOf(org.civicops.shared.exception.BusinessRuleException.class);
    var facility =
        facilityService.create(
            orgId,
            new org.civicops.facilities.facility.dto.CreateFacilityRequest(
                "Hope Community Center",
                null,
                org.civicops.facilities.facility.FacilityType.COMMUNITY_CENTER,
                null,
                null,
                "Hope",
                "NY",
                null,
                "US",
                "America/New_York",
                null));
    var space =
        facilitySpaceService.create(
            orgId,
            facility.id(),
            new org.civicops.facilities.space.dto.CreateFacilitySpaceRequest(
                " Training Room ", null, 30, true, null, null));
    assertThatThrownBy(
            () ->
                facilitySpaceService.create(
                    orgId,
                    facility.id(),
                    new org.civicops.facilities.space.dto.CreateFacilitySpaceRequest(
                        "training   room", null, 10, true, null, null)))
        .isInstanceOf(org.civicops.shared.exception.ConflictException.class);
    ZoneId zone = ZoneId.of("America/New_York");
    LocalDate day = LocalDate.now(zone).plusDays(10);
    operatingHoursService.replace(
        orgId,
        facility.id(),
        List.of(
            new org.civicops.facilities.availability.dto.OperatingHoursRequest(
                day.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(21, 0), false)));
    Instant ten = ZonedDateTime.of(day, LocalTime.of(10, 0), zone).toInstant(),
        eleven = ZonedDateTime.of(day, LocalTime.of(11, 0), zone).toInstant(),
        twelve = ZonedDateTime.of(day, LocalTime.of(12, 0), zone).toInstant(),
        fourteen = ZonedDateTime.of(day, LocalTime.of(14, 0), zone).toInstant();
    var first =
        facilityReservationService.create(
            orgId,
            manager,
            new org.civicops.facilities.reservation.dto.CreateReservationRequest(
                space.id(),
                manager,
                null,
                null,
                null,
                "Morning meeting",
                null,
                ten,
                twelve,
                20,
                null));
    var overlap =
        facilityReservationService.create(
            orgId,
            manager,
            new org.civicops.facilities.reservation.dto.CreateReservationRequest(
                space.id(),
                manager,
                null,
                null,
                null,
                "Overlap",
                null,
                eleven,
                fourteen,
                20,
                null));
    facilityReservationService.approve(orgId, first.id(), manager);
    assertThatThrownBy(() -> facilityReservationService.approve(orgId, overlap.id(), manager))
        .isInstanceOf(org.civicops.shared.exception.BusinessRuleException.class);
    assertThat(facilityAvailabilityService.check(orgId, space.id(), twelve, fourteen).available())
        .isTrue();
    assertThat(facilityAvailabilityService.check(orgId, space.id(), eleven, fourteen).reason())
        .isEqualTo("RESERVATION_CONFLICT");
    Instant fifteen = ZonedDateTime.of(day, LocalTime.of(15, 0), zone).toInstant(),
        seventeen = ZonedDateTime.of(day, LocalTime.of(17, 0), zone).toInstant();
    facilityBlackoutService.create(
        orgId,
        manager,
        new org.civicops.facilities.availability.dto.CreateBlackoutRequest(
            facility.id(), space.id(), fifteen, seventeen, "Cleaning"));
    assertThat(
            facilityAvailabilityService
                .check(orgId, space.id(), fifteen.plusSeconds(3600), seventeen.plusSeconds(3600))
                .reason())
        .isEqualTo("BLACKOUT_CONFLICT");
    assertThat(
            facilityAvailabilityService
                .check(
                    orgId,
                    space.id(),
                    ZonedDateTime.of(day, LocalTime.of(8, 0), zone).toInstant(),
                    ten)
                .reason())
        .isEqualTo("OUTSIDE_OPERATING_HOURS");
    UUID event =
        insertEvent(
            orgId,
            manager,
            "DRAFT",
            null,
            false,
            null,
            null,
            ZonedDateTime.of(day, LocalTime.of(13, 0), zone).toInstant());
    var linked =
        facilityReservationService.create(
            orgId,
            manager,
            new org.civicops.facilities.reservation.dto.CreateReservationRequest(
                space.id(),
                manager,
                null,
                null,
                event,
                "Workshop",
                null,
                twelve,
                fifteen,
                20,
                null));
    assertThat(linked.eventId()).isEqualTo(event);
    facilityReservationService.cancel(orgId, linked.id(), "Changed venue");
    assertThat(
            jdbc.queryForObject("SELECT status FROM event_record WHERE id=?", String.class, event))
        .isEqualTo("DRAFT");
    eventService.transition(orgId, event, EventStatus.CANCELLED);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM facility_reservation WHERE id=?", Integer.class, linked.id()))
        .isEqualTo(1);
    UUID otherOrg = UUID.randomUUID();
    insertOrganization(otherOrg, "NONPROFIT");
    UUID otherEvent =
        insertEvent(
            otherOrg,
            manager,
            "DRAFT",
            null,
            false,
            null,
            null,
            ZonedDateTime.of(day, LocalTime.of(13, 0), zone).toInstant());
    assertThatThrownBy(
            () ->
                facilityReservationService.create(
                    orgId,
                    manager,
                    new org.civicops.facilities.reservation.dto.CreateReservationRequest(
                        space.id(),
                        manager,
                        null,
                        null,
                        otherEvent,
                        "Cross org",
                        null,
                        twelve,
                        fifteen,
                        20,
                        null)))
        .isInstanceOf(org.civicops.shared.exception.BusinessRuleException.class);
    var summary = facilityReportingService.summary(orgId, day.minusDays(1), day.plusDays(1));
    assertThat(summary.activeFacilities()).isEqualTo(1);
    assertThat(summary.reservableSpaces()).isEqualTo(1);
    assertThat(summary.approvedReservations()).isEqualTo(1);
    assertThat(summary.cancelledReservations()).isEqualTo(1);
    var utilization =
        facilityReportingService.utilization(orgId, day.minusDays(1), day.plusDays(1));
    assertThat(utilization.reservedHours()).isEqualByComparingTo("2.00");
    assertThat(utilization.mostUsedSpaces())
        .singleElement()
        .satisfies(x -> assertThat(x.reservations()).isEqualTo(1));
  }

  @Test
  void concurrentFacilityApprovalAllowsExactlyOneOverlappingReservation() throws Exception {
    UUID orgId = UUID.randomUUID(), manager = UUID.randomUUID();
    insertOrganization(orgId, "NONPROFIT");
    insertUser(manager, "facility-race-" + manager + "@example.org");
    insertMembership(UUID.randomUUID(), orgId, manager, "FACILITY_MANAGER");
    var facility =
        facilityService.create(
            orgId,
            new org.civicops.facilities.facility.dto.CreateFacilityRequest(
                "Race Center",
                null,
                org.civicops.facilities.facility.FacilityType.OTHER,
                null,
                null,
                null,
                null,
                null,
                null,
                "UTC",
                null));
    var space =
        facilitySpaceService.create(
            orgId,
            facility.id(),
            new org.civicops.facilities.space.dto.CreateFacilitySpaceRequest(
                "Room", null, 20, true, null, null));
    LocalDate day = LocalDate.now(ZoneOffset.UTC).plusDays(20);
    operatingHoursService.replace(
        orgId,
        facility.id(),
        List.of(
            new org.civicops.facilities.availability.dto.OperatingHoursRequest(
                day.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(20, 0), false)));
    Instant a = day.atTime(10, 0).toInstant(ZoneOffset.UTC),
        b = day.atTime(11, 0).toInstant(ZoneOffset.UTC),
        c = day.atTime(10, 30).toInstant(ZoneOffset.UTC),
        d = day.atTime(11, 30).toInstant(ZoneOffset.UTC);
    var one =
        facilityReservationService.create(
            orgId,
            manager,
            new org.civicops.facilities.reservation.dto.CreateReservationRequest(
                space.id(), manager, null, null, null, "A", null, a, b, 10, null));
    var two =
        facilityReservationService.create(
            orgId,
            manager,
            new org.civicops.facilities.reservation.dto.CreateReservationRequest(
                space.id(), manager, null, null, null, "B", null, c, d, 10, null));
    List<Boolean> outcomes =
        race(
            () -> facilityReservationService.approve(orgId, one.id(), manager),
            () -> facilityReservationService.approve(orgId, two.id(), manager));
    assertThat(outcomes).containsExactlyInAnyOrder(true, false);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM facility_reservation WHERE facility_space_id=? AND status='APPROVED'",
                Integer.class,
                space.id()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM facility_reservation WHERE facility_space_id=? AND status='PENDING'",
                Integer.class,
                space.id()))
        .isEqualTo(1);
  }

  @Test
  void facilityDatabaseConstraintsEnforceOrganizationAndTimeIntegrity() {
    UUID orgA = UUID.randomUUID(),
        orgB = UUID.randomUUID(),
        user = UUID.randomUUID(),
        facility = UUID.randomUUID(),
        space = UUID.randomUUID();
    insertOrganization(orgA, "NONPROFIT");
    insertOrganization(orgB, "NONPROFIT");
    insertUser(user, "facility-db-" + user + "@example.org");
    insertMembership(UUID.randomUUID(), orgA, user, "FACILITY_MANAGER");
    jdbc.update(
        "INSERT INTO facility(id,organization_id,name,facility_type,timezone)VALUES (?,?,'Center','OTHER','UTC')",
        facility,
        orgA);
    jdbc.update(
        "INSERT INTO facility_space(id,organization_id,facility_id,name,normalized_name,capacity)VALUES (?,?,?,'Room','room',10)",
        space,
        orgA,
        facility);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO facility_space(id,organization_id,facility_id,name,normalized_name)VALUES (?,?,?,'Bad','bad')",
                    UUID.randomUUID(),
                    orgB,
                    facility))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO facility_space(id,organization_id,facility_id,name,normalized_name)VALUES (?,?,?,'Duplicate','room')",
                    UUID.randomUUID(),
                    orgA,
                    facility))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO facility_operating_hours(id,organization_id,facility_id,day_of_week,open_time,close_time,closed)VALUES (?,?,?,'MONDAY','12:00','10:00',false)",
                    UUID.randomUUID(),
                    orgA,
                    facility))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO facility_blackout(id,organization_id,facility_id,start_date_time,end_date_time,reason,created_by_user_id)VALUES (?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'Bad',?)",
                    UUID.randomUUID(),
                    orgA,
                    facility,
                    user))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO facility_reservation(id,organization_id,facility_space_id,requester_name,title,start_date_time,end_date_time,requested_at)VALUES (?,?,?,'Requester','Bad',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                    UUID.randomUUID(),
                    orgA,
                    space))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void scholarshipWorkflowReviewerAwardAndReportingUsePostgreSql() {
    UUID org = UUID.randomUUID(), manager = UUID.randomUUID(), reviewer = UUID.randomUUID();
    insertOrganization(org, "NONPROFIT");
    insertUser(manager, "scholarship-manager-" + manager + "@example.org");
    insertUser(reviewer, "scholarship-reviewer-" + reviewer + "@example.org");
    insertMembership(UUID.randomUUID(), org, manager, "SCHOLARSHIP_MANAGER");
    insertMembership(UUID.randomUUID(), org, reviewer, "SCHOLARSHIP_REVIEWER");
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    var program =
        scholarshipProgramService.create(
            org,
            manager,
            new CreateScholarshipProgramRequest(
                "Community Scholars",
                null,
                "2026-27",
                today.minusDays(1),
                today,
                new BigDecimal("2500.00"),
                2,
                "Community service"));
    scholarshipProgramService.transition(org, program.id(), ScholarshipProgramStatus.OPEN);
    var applicant =
        scholarshipApplicantService.create(
            org,
            new CreateScholarshipApplicantRequest(
                null,
                "Ada",
                "Lovelace",
                null,
                " ADA.SCHOLAR@Example.org ",
                null,
                LocalDate.of(2006, 1, 1),
                null,
                "Central High",
                2026,
                null,
                "private notes"));
    assertThat(applicant.email()).isEqualTo("ADA.SCHOLAR@Example.org");
    assertThatThrownBy(
            () ->
                scholarshipApplicantService.create(
                    org,
                    new CreateScholarshipApplicantRequest(
                        null,
                        "Duplicate",
                        "Applicant",
                        null,
                        "ada.scholar@example.org",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)))
        .isInstanceOf(org.civicops.shared.exception.ConflictException.class);
    var application =
        scholarshipApplicationService.create(
            org,
            program.id(),
            new CreateScholarshipApplicationRequest(
                applicant.id(),
                true,
                "Eligible",
                "Service statement",
                "Need statement",
                new BigDecimal("3.80"),
                new BigDecimal("35000"),
                new BigDecimal("2500")));
    scholarshipApplicationService.submit(org, application.id());
    scholarshipProgramService.transition(org, program.id(), ScholarshipProgramStatus.CLOSED);
    scholarshipProgramService.transition(org, program.id(), ScholarshipProgramStatus.REVIEWING);
    var assignment = scholarshipReviewService.assign(org, application.id(), reviewer, manager);
    assertThatThrownBy(
            () -> scholarshipReviewService.assign(org, application.id(), reviewer, manager))
        .isInstanceOf(org.civicops.shared.exception.ConflictException.class);
    scholarshipReviewService.start(org, assignment.id());
    var review =
        scholarshipReviewService.submit(
            org,
            assignment.id(),
            new SubmitReviewRequest(
                new BigDecimal("92.50"), ReviewRecommendation.STRONGLY_RECOMMEND, "Excellent"));
    assertThat(review.score()).isEqualByComparingTo("92.50");
    assertThatThrownBy(
            () ->
                scholarshipReviewService.submit(
                    org,
                    assignment.id(),
                    new SubmitReviewRequest(
                        new BigDecimal("90"), ReviewRecommendation.RECOMMEND, null)))
        .isInstanceOf(org.civicops.shared.exception.ConflictException.class);
    scholarshipApplicationService.finalist(org, application.id());
    scholarshipApplicationService.select(org, application.id());
    var award =
        scholarshipAwardService.create(
            org,
            application.id(),
            manager,
            new CreateScholarshipAwardRequest(new BigDecimal("2500.00"), today, "Offer"));
    assertThatThrownBy(
            () ->
                scholarshipAwardService.create(
                    org,
                    application.id(),
                    manager,
                    new CreateScholarshipAwardRequest(new BigDecimal("2500.00"), today, null)))
        .isInstanceOf(org.civicops.shared.exception.ConflictException.class);
    scholarshipAwardService.transition(org, award.id(), ScholarshipAwardStatus.ACCEPTED);
    scholarshipAwardService.transition(org, award.id(), ScholarshipAwardStatus.DISBURSED);
    assertThat(scholarshipReportingService.summary(org).totalAwarded())
        .isEqualByComparingTo("2500.00");
    assertThat(scholarshipReportingService.program(org, program.id()).selected()).isEqualTo(1);
    assertThat(scholarshipReportingService.reviews(org).averageScore())
        .isEqualByComparingTo("92.50");
  }

  @Test
  void concurrentScholarshipSelectionHonorsConfiguredAwardLimit() throws Exception {
    UUID org = UUID.randomUUID(), manager = UUID.randomUUID(), reviewer = UUID.randomUUID();
    insertOrganization(org, "NONPROFIT");
    insertUser(manager, "scholarship-race-manager-" + manager + "@example.org");
    insertUser(reviewer, "scholarship-race-reviewer-" + reviewer + "@example.org");
    insertMembership(UUID.randomUUID(), org, manager, "SCHOLARSHIP_MANAGER");
    insertMembership(UUID.randomUUID(), org, reviewer, "SCHOLARSHIP_REVIEWER");
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    var program =
        scholarshipProgramService.create(
            org,
            manager,
            new CreateScholarshipProgramRequest(
                "One Award",
                null,
                null,
                today.minusDays(1),
                today.plusDays(10),
                new BigDecimal("1000"),
                1,
                null));
    scholarshipProgramService.transition(org, program.id(), ScholarshipProgramStatus.OPEN);
    var a =
        scholarshipApplicantService.create(
            org,
            new CreateScholarshipApplicantRequest(
                null,
                "First",
                "Finalist",
                null,
                "first-" + org + "@example.org",
                null,
                null,
                null,
                null,
                null,
                null,
                null));
    var b =
        scholarshipApplicantService.create(
            org,
            new CreateScholarshipApplicantRequest(
                null,
                "Second",
                "Finalist",
                null,
                "second-" + org + "@example.org",
                null,
                null,
                null,
                null,
                null,
                null,
                null));
    var one =
        scholarshipApplicationService.create(
            org,
            program.id(),
            new CreateScholarshipApplicationRequest(
                a.id(), true, null, "Statement A", null, null, null, null));
    var two =
        scholarshipApplicationService.create(
            org,
            program.id(),
            new CreateScholarshipApplicationRequest(
                b.id(), true, null, "Statement B", null, null, null, null));
    scholarshipApplicationService.submit(org, one.id());
    scholarshipApplicationService.submit(org, two.id());
    scholarshipProgramService.transition(org, program.id(), ScholarshipProgramStatus.CLOSED);
    scholarshipProgramService.transition(org, program.id(), ScholarshipProgramStatus.REVIEWING);
    scholarshipReviewService.assign(org, one.id(), reviewer, manager);
    scholarshipReviewService.assign(org, two.id(), reviewer, manager);
    scholarshipApplicationService.finalist(org, one.id());
    scholarshipApplicationService.finalist(org, two.id());
    List<Boolean> outcomes =
        race(
            () -> scholarshipApplicationService.select(org, one.id()),
            () -> scholarshipApplicationService.select(org, two.id()));
    assertThat(outcomes).containsExactlyInAnyOrder(true, false);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM scholarship_application WHERE scholarship_program_id=? AND status='SELECTED'",
                Integer.class,
                program.id()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM scholarship_application WHERE scholarship_program_id=? AND status='FINALIST'",
                Integer.class,
                program.id()))
        .isEqualTo(1);
  }

  @Test
  void scholarshipDatabaseConstraintsProtectOrganizationsScoresAndUniqueness() {
    UUID orgA = UUID.randomUUID(),
        orgB = UUID.randomUUID(),
        manager = UUID.randomUUID(),
        reviewer = UUID.randomUUID(),
        program = UUID.randomUUID(),
        applicant = UUID.randomUUID();
    insertOrganization(orgA, "NONPROFIT");
    insertOrganization(orgB, "NONPROFIT");
    insertUser(manager, "scholarship-db-" + manager + "@example.org");
    insertUser(reviewer, "scholarship-db-reviewer-" + reviewer + "@example.org");
    insertMembership(UUID.randomUUID(), orgA, manager, "SCHOLARSHIP_MANAGER");
    insertMembership(UUID.randomUUID(), orgA, reviewer, "SCHOLARSHIP_REVIEWER");
    jdbc.update(
        "INSERT INTO scholarship_program(id,organization_id,name,application_open_date,application_deadline,status,created_by_user_id)VALUES (?,?,?,CURRENT_DATE,CURRENT_DATE+1,'DRAFT',?)",
        program,
        orgA,
        "DB Program",
        manager);
    jdbc.update(
        "INSERT INTO scholarship_applicant(id,organization_id,first_name,last_name,email,normalized_email)VALUES (?,?, 'A','Applicant','a@example.org','a@example.org')",
        applicant,
        orgA);
    UUID application = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO scholarship_application(id,organization_id,scholarship_program_id,applicant_id,status)VALUES (?,?,?,?,'DRAFT')",
        application,
        orgA,
        program,
        applicant);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO scholarship_application(id,organization_id,scholarship_program_id,applicant_id,status)VALUES (?,?,?,?,'DRAFT')",
                    UUID.randomUUID(),
                    orgA,
                    program,
                    applicant))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO scholarship_application(id,organization_id,scholarship_program_id,applicant_id,status)VALUES (?,?,?,?,'DRAFT')",
                    UUID.randomUUID(),
                    orgB,
                    program,
                    applicant))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO scholarship_program(id,organization_id,name,application_open_date,application_deadline,status,created_by_user_id)VALUES (?,?,?,CURRENT_DATE,CURRENT_DATE,'DRAFT',?)",
                    UUID.randomUUID(),
                    orgA,
                    "Bad dates",
                    manager))
        .isInstanceOf(DataIntegrityViolationException.class);
    UUID assignment = UUID.randomUUID();
    jdbc.update(
        "UPDATE scholarship_application SET status='UNDER_REVIEW',submitted_at=CURRENT_TIMESTAMP WHERE id=?",
        application);
    jdbc.update(
        "INSERT INTO scholarship_review_assignment(id,organization_id,application_id,reviewer_user_id,status,assigned_at,assigned_by_user_id)VALUES (?,?,?,?, 'ASSIGNED',CURRENT_TIMESTAMP,?)",
        assignment,
        orgA,
        application,
        reviewer,
        manager);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO scholarship_review_assignment(id,organization_id,application_id,reviewer_user_id,status,assigned_at,assigned_by_user_id)VALUES (?,?,?,?, 'ASSIGNED',CURRENT_TIMESTAMP,?)",
                    UUID.randomUUID(),
                    orgA,
                    application,
                    reviewer,
                    manager))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO scholarship_review(id,organization_id,assignment_id,score,recommendation,submitted_at)VALUES (?,?,?,?, 'NEUTRAL',CURRENT_TIMESTAMP)",
                    UUID.randomUUID(),
                    orgA,
                    assignment,
                    new BigDecimal("101")))
        .isInstanceOf(DataIntegrityViolationException.class);
    UUID otherApplicant = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO scholarship_applicant(id,organization_id,first_name,last_name,email,normalized_email)VALUES (?,?, 'Other','Applicant','other@example.org','other@example.org')",
        otherApplicant,
        orgA);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO scholarship_award(id,organization_id,scholarship_program_id,application_id,applicant_id,amount,award_date,status,created_by_user_id)VALUES (?,?,?,?,?,100,CURRENT_DATE,'OFFERED',?)",
                    UUID.randomUUID(),
                    orgA,
                    program,
                    application,
                    otherApplicant,
                    manager))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void foodPantryWorkflowAllocatesMultipleLotsByFefoAndReportsHistory() {
    UUID orgId = UUID.randomUUID(), manager = UUID.randomUUID();
    insertOrganization(orgId, "NONPROFIT");
    insertUser(manager, "pantry-fefo-" + manager + "@example.org");
    insertMembership(UUID.randomUUID(), orgId, manager, "FOOD_PANTRY_MANAGER");
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    var pantry =
        foodPantryLocationService.create(
            orgId,
            new org.civicops.foodpantry.pantry.dto.CreatePantryLocationRequest(
                "Hope Community Pantry", null, null, null, null, null, null, "US", "UTC", null));
    var item =
        pantryItemService.create(
            orgId,
            new org.civicops.foodpantry.item.dto.CreatePantryItemRequest(
                " beans-1 ",
                "Canned Beans",
                null,
                org.civicops.foodpantry.item.FoodCategory.CANNED_GOODS,
                org.civicops.foodpantry.item.UnitType.CAN,
                true,
                new BigDecimal("10"),
                null));
    var lotA =
        pantryInventoryService.receive(
            orgId,
            pantry.id(),
            manager,
            new org.civicops.foodpantry.inventory.dto.CreateInventoryReceiptRequest(
                item.id(),
                new BigDecimal("3"),
                today.minusDays(1),
                "A",
                today.plusDays(5),
                org.civicops.foodpantry.inventory.InventorySourceType.FOOD_BANK,
                null,
                null));
    var lotB =
        pantryInventoryService.receive(
            orgId,
            pantry.id(),
            manager,
            new org.civicops.foodpantry.inventory.dto.CreateInventoryReceiptRequest(
                item.id(),
                new BigDecimal("10"),
                today.minusDays(1),
                "B",
                today.plusDays(10),
                org.civicops.foodpantry.inventory.InventorySourceType.DONATION,
                "IN-KIND-1",
                null));
    var household =
        pantryHouseholdService.create(
            orgId,
            new org.civicops.foodpantry.household.dto.CreatePantryHouseholdRequest(
                "HH-1",
                "Citizen Household",
                null,
                null,
                " HOUSEHOLD@EXAMPLE.ORG ",
                null,
                null,
                4,
                "private"));
    var visit =
        pantryDistributionService.create(
            orgId,
            pantry.id(),
            manager,
            new org.civicops.foodpantry.distribution.dto.CreateDistributionVisitRequest(
                household.id(), null, Instant.now(), null, null));
    pantryDistributionService.addItem(
        orgId,
        visit.id(),
        new org.civicops.foodpantry.distribution.dto.AddDistributionItemRequest(
            item.id(), new BigDecimal("8")));
    pantryDistributionService.complete(orgId, visit.id(), manager);
    assertThat(
            jdbc.queryForObject(
                "SELECT quantity_remaining FROM pantry_inventory_lot WHERE id=?",
                BigDecimal.class,
                lotA.id()))
        .isEqualByComparingTo("0");
    assertThat(
            jdbc.queryForObject(
                "SELECT quantity_remaining FROM pantry_inventory_lot WHERE id=?",
                BigDecimal.class,
                lotB.id()))
        .isEqualByComparingTo("5");
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM pantry_inventory_transaction WHERE reference_id=? AND transaction_type='DISTRIBUTION'",
                Integer.class,
                visit.id()))
        .isEqualTo(2);
    var distributionReport =
        foodPantryReportingService.distributions(orgId, pantry.id(), today, today);
    assertThat(distributionReport.visitsCompleted()).isEqualTo(1);
    assertThat(distributionReport.totalQuantityDistributed()).isEqualByComparingTo("8.000");
    assertThat(distributionReport.uniqueHouseholdsServed()).isEqualTo(1);
    assertThat(distributionReport.quantityByCategory())
        .singleElement()
        .satisfies(x -> assertThat(x.quantity()).isEqualByComparingTo("8.000"));
  }

  @Test
  void concurrentFoodPantryDepletionNeverCreatesNegativeStock() throws Exception {
    UUID orgId = UUID.randomUUID(), manager = UUID.randomUUID();
    insertOrganization(orgId, "NONPROFIT");
    insertUser(manager, "pantry-race-" + manager + "@example.org");
    insertMembership(UUID.randomUUID(), orgId, manager, "FOOD_PANTRY_MANAGER");
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    var pantry =
        foodPantryLocationService.create(
            orgId,
            new org.civicops.foodpantry.pantry.dto.CreatePantryLocationRequest(
                "Race Pantry", null, null, null, null, null, null, null, "UTC", null));
    var item =
        pantryItemService.create(
            orgId,
            new org.civicops.foodpantry.item.dto.CreatePantryItemRequest(
                null,
                "Rice",
                null,
                org.civicops.foodpantry.item.FoodCategory.GRAINS,
                org.civicops.foodpantry.item.UnitType.POUND,
                false,
                null,
                null));
    var lot =
        pantryInventoryService.receive(
            orgId,
            pantry.id(),
            manager,
            new org.civicops.foodpantry.inventory.dto.CreateInventoryReceiptRequest(
                item.id(), new BigDecimal("10"), today, "RACE", null, null, null, null));
    var one =
        pantryDistributionService.create(
            orgId,
            pantry.id(),
            manager,
            new org.civicops.foodpantry.distribution.dto.CreateDistributionVisitRequest(
                null, "Recipient One", Instant.now(), 1, null));
    var two =
        pantryDistributionService.create(
            orgId,
            pantry.id(),
            manager,
            new org.civicops.foodpantry.distribution.dto.CreateDistributionVisitRequest(
                null, "Recipient Two", Instant.now(), 1, null));
    pantryDistributionService.addItem(
        orgId,
        one.id(),
        new org.civicops.foodpantry.distribution.dto.AddDistributionItemRequest(
            item.id(), new BigDecimal("7")));
    pantryDistributionService.addItem(
        orgId,
        two.id(),
        new org.civicops.foodpantry.distribution.dto.AddDistributionItemRequest(
            item.id(), new BigDecimal("7")));
    List<Boolean> outcomes =
        race(
            () -> pantryDistributionService.complete(orgId, one.id(), manager),
            () -> pantryDistributionService.complete(orgId, two.id(), manager));
    assertThat(outcomes).containsExactlyInAnyOrder(true, false);
    assertThat(
            jdbc.queryForObject(
                "SELECT quantity_remaining FROM pantry_inventory_lot WHERE id=?",
                BigDecimal.class,
                lot.id()))
        .isEqualByComparingTo("3");
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM pantry_distribution_visit WHERE id IN (?,?) AND status='COMPLETED'",
                Integer.class,
                one.id(),
                two.id()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT MIN(quantity_remaining) FROM pantry_inventory_lot WHERE id=?",
                BigDecimal.class,
                lot.id()))
        .isGreaterThanOrEqualTo(BigDecimal.ZERO);
  }

  @Test
  void foodPantryExpirationAdjustmentsAndConstraintsUsePostgreSql() {
    UUID orgId = UUID.randomUUID(), other = UUID.randomUUID(), manager = UUID.randomUUID();
    insertOrganization(orgId, "NONPROFIT");
    insertOrganization(other, "NONPROFIT");
    insertUser(manager, "pantry-expiration-" + manager + "@example.org");
    insertMembership(UUID.randomUUID(), orgId, manager, "FOOD_PANTRY_MANAGER");
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    var pantry =
        foodPantryLocationService.create(
            orgId,
            new org.civicops.foodpantry.pantry.dto.CreatePantryLocationRequest(
                "Expiration Pantry", null, null, null, null, null, null, null, "UTC", null));
    var item =
        pantryItemService.create(
            orgId,
            new org.civicops.foodpantry.item.dto.CreatePantryItemRequest(
                "MILK-1",
                "Milk",
                null,
                org.civicops.foodpantry.item.FoodCategory.DAIRY,
                org.civicops.foodpantry.item.UnitType.BOTTLE,
                true,
                new BigDecimal("5"),
                null));
    assertThatThrownBy(
            () ->
                pantryItemService.create(
                    orgId,
                    new org.civicops.foodpantry.item.dto.CreatePantryItemRequest(
                        " milk-1 ",
                        "Duplicate",
                        null,
                        org.civicops.foodpantry.item.FoodCategory.DAIRY,
                        org.civicops.foodpantry.item.UnitType.BOTTLE,
                        true,
                        null,
                        null)))
        .isInstanceOf(org.civicops.shared.exception.ConflictException.class);
    var expired =
        pantryInventoryService.receive(
            orgId,
            pantry.id(),
            manager,
            new org.civicops.foodpantry.inventory.dto.CreateInventoryReceiptRequest(
                item.id(),
                new BigDecimal("4"),
                today.minusDays(5),
                "OLD",
                today.minusDays(1),
                null,
                null,
                null));
    var boundary =
        pantryInventoryService.receive(
            orgId,
            pantry.id(),
            manager,
            new org.civicops.foodpantry.inventory.dto.CreateInventoryReceiptRequest(
                item.id(),
                new BigDecimal("2"),
                today.minusDays(1),
                "TODAY",
                today,
                null,
                null,
                null));
    var future =
        pantryInventoryService.receive(
            orgId,
            pantry.id(),
            manager,
            new org.civicops.foodpantry.inventory.dto.CreateInventoryReceiptRequest(
                item.id(),
                new BigDecimal("5"),
                today,
                "FUTURE",
                today.plusDays(3),
                null,
                null,
                null));
    var availability =
        pantryInventoryService.availability(orgId, pantry.id(), item.id()).getFirst();
    assertThat(availability.availableQuantity()).isEqualByComparingTo("7.000");
    assertThat(availability.expiredQuantity()).isEqualByComparingTo("4.000");
    assertThat(availability.expiringSoonQuantity()).isEqualByComparingTo("7.000");
    pantryInventoryService.adjust(
        orgId,
        future.id(),
        manager,
        new org.civicops.foodpantry.inventory.dto.AdjustInventoryRequest(
            org.civicops.foodpantry.inventory.InventoryAdjustmentType.SPOILAGE,
            new BigDecimal("2"),
            "Damaged seal"));
    assertThat(pantryInventoryService.history(orgId, future.id())).hasSize(2);
    assertThat(foodPantryReportingService.waste(orgId, pantry.id(), today, today).spoiledQuantity())
        .isEqualByComparingTo("2.000");
    assertThatThrownBy(
            () ->
                pantryInventoryService.adjust(
                    orgId,
                    future.id(),
                    manager,
                    new org.civicops.foodpantry.inventory.dto.AdjustInventoryRequest(
                        org.civicops.foodpantry.inventory.InventoryAdjustmentType.COUNT_DECREASE,
                        new BigDecimal("99"),
                        "Invalid")))
        .isInstanceOf(org.civicops.shared.exception.BusinessRuleException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE pantry_inventory_lot SET quantity_remaining=-1 WHERE id=?",
                    boundary.id()))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                pantryInventoryService.receive(
                    other,
                    pantry.id(),
                    manager,
                    new org.civicops.foodpantry.inventory.dto.CreateInventoryReceiptRequest(
                        item.id(),
                        BigDecimal.ONE,
                        today,
                        "BAD",
                        today.plusDays(1),
                        null,
                        null,
                        null)))
        .isInstanceOf(org.civicops.shared.exception.ResourceNotFoundException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT quantity_remaining FROM pantry_inventory_lot WHERE id=?",
                BigDecimal.class,
                expired.id()))
        .isEqualByComparingTo("4");
  }

  @Test
  void foodPantryCancellationExpirationHistoryAndEmptyReportsUsePostgreSql() {
    UUID orgId = UUID.randomUUID(), emptyOrg = UUID.randomUUID(), manager = UUID.randomUUID();
    insertOrganization(orgId, "NONPROFIT");
    insertOrganization(emptyOrg, "NONPROFIT");
    insertUser(manager, "pantry-rules-" + manager + "@example.org");
    insertMembership(UUID.randomUUID(), orgId, manager, "FOOD_PANTRY_MANAGER");
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    var pantry =
        foodPantryLocationService.create(
            orgId,
            new org.civicops.foodpantry.pantry.dto.CreatePantryLocationRequest(
                "Rules Pantry", null, null, null, "Old City", null, null, "US", "UTC", null));
    pantry =
        foodPantryLocationService.update(
            orgId,
            pantry.id(),
            new org.civicops.foodpantry.pantry.dto.UpdatePantryLocationRequest(
                "Rules Pantry Updated",
                null,
                null,
                null,
                "New City",
                null,
                null,
                null,
                null,
                null));
    assertThat(pantry.name()).isEqualTo("Rules Pantry Updated");
    assertThat(pantry.city()).isEqualTo("New City");
    var item =
        pantryItemService.create(
            orgId,
            new org.civicops.foodpantry.item.dto.CreatePantryItemRequest(
                "RULE-1",
                "Rules Item",
                null,
                org.civicops.foodpantry.item.FoodCategory.OTHER,
                org.civicops.foodpantry.item.UnitType.EACH,
                true,
                BigDecimal.ONE,
                null));
    var expired =
        pantryInventoryService.receive(
            orgId,
            pantry.id(),
            manager,
            new org.civicops.foodpantry.inventory.dto.CreateInventoryReceiptRequest(
                item.id(),
                new BigDecimal("10"),
                today.minusDays(2),
                "EXPIRED",
                today.minusDays(1),
                null,
                null,
                null));
    var fresh =
        pantryInventoryService.receive(
            orgId,
            pantry.id(),
            manager,
            new org.civicops.foodpantry.inventory.dto.CreateInventoryReceiptRequest(
                item.id(),
                new BigDecimal("2"),
                today,
                "FRESH",
                today.plusDays(10),
                null,
                null,
                null));
    var insufficient =
        pantryDistributionService.create(
            orgId,
            pantry.id(),
            manager,
            new org.civicops.foodpantry.distribution.dto.CreateDistributionVisitRequest(
                null, "Expired stock recipient", Instant.now(), 1, null));
    pantryDistributionService.addItem(
        orgId,
        insufficient.id(),
        new org.civicops.foodpantry.distribution.dto.AddDistributionItemRequest(
            item.id(), new BigDecimal("3")));
    assertThatThrownBy(() -> pantryDistributionService.complete(orgId, insufficient.id(), manager))
        .isInstanceOf(org.civicops.shared.exception.BusinessRuleException.class)
        .hasMessageContaining("Insufficient non-expired inventory");
    assertThat(
            jdbc.queryForObject(
                "SELECT quantity_remaining FROM pantry_inventory_lot WHERE id=?",
                BigDecimal.class,
                expired.id()))
        .isEqualByComparingTo("10");
    assertThat(
            jdbc.queryForObject(
                "SELECT quantity_remaining FROM pantry_inventory_lot WHERE id=?",
                BigDecimal.class,
                fresh.id()))
        .isEqualByComparingTo("2");
    var cancelled =
        pantryDistributionService.create(
            orgId,
            pantry.id(),
            manager,
            new org.civicops.foodpantry.distribution.dto.CreateDistributionVisitRequest(
                null, "Cancelled recipient", Instant.now(), 1, null));
    pantryDistributionService.addItem(
        orgId,
        cancelled.id(),
        new org.civicops.foodpantry.distribution.dto.AddDistributionItemRequest(
            item.id(), BigDecimal.ONE));
    assertThat(pantryDistributionService.cancel(orgId, cancelled.id()).status())
        .isEqualTo(org.civicops.foodpantry.distribution.DistributionVisitStatus.CANCELLED);
    assertThat(
            jdbc.queryForObject(
                "SELECT quantity_remaining FROM pantry_inventory_lot WHERE id=?",
                BigDecimal.class,
                fresh.id()))
        .isEqualByComparingTo("2");
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM pantry_inventory_transaction WHERE reference_id=?",
                Integer.class,
                cancelled.id()))
        .isZero();
    pantryInventoryService.adjust(
        orgId,
        fresh.id(),
        manager,
        new org.civicops.foodpantry.inventory.dto.AdjustInventoryRequest(
            org.civicops.foodpantry.inventory.InventoryAdjustmentType.COUNT_DECREASE,
            BigDecimal.ONE,
            "Count correction down"));
    pantryInventoryService.adjust(
        orgId,
        fresh.id(),
        manager,
        new org.civicops.foodpantry.inventory.dto.AdjustInventoryRequest(
            org.civicops.foodpantry.inventory.InventoryAdjustmentType.COUNT_INCREASE,
            BigDecimal.ONE,
            "Count correction up"));
    assertThat(pantryInventoryService.history(orgId, fresh.id())).hasSize(3);
    pantryHouseholdService.create(
        orgId,
        new org.civicops.foodpantry.household.dto.CreatePantryHouseholdRequest(
            "RULE-HH", "Rules Household", null, null, null, null, null, 1, null));
    assertThatThrownBy(
            () ->
                pantryHouseholdService.create(
                    orgId,
                    new org.civicops.foodpantry.household.dto.CreatePantryHouseholdRequest(
                        "RULE-HH", "Duplicate", null, null, null, null, null, 1, null)))
        .isInstanceOf(org.civicops.shared.exception.ConflictException.class);
    foodPantryLocationService.active(orgId, pantry.id(), false);
    assertThat(foodPantryLocationService.detail(orgId, pantry.id()).active()).isFalse();
    assertThat(pantryInventoryService.detail(orgId, fresh.id()).quantityRemaining())
        .isEqualByComparingTo("2");
    assertThat(foodPantryReportingService.inventory(emptyOrg, null).totalAvailableQuantity())
        .isEqualByComparingTo("0.000");
    assertThat(
            foodPantryReportingService
                .distributions(emptyOrg, null, today.minusDays(1), today)
                .visitsCompleted())
        .isZero();
    assertThat(
            foodPantryReportingService
                .households(emptyOrg, today.minusDays(1), today)
                .activeHouseholds())
        .isZero();
    assertThat(
            foodPantryReportingService
                .waste(emptyOrg, null, today.minusDays(1), today)
                .spoiledQuantity())
        .isEqualByComparingTo("0.000");
    assertThatThrownBy(
            () -> foodPantryReportingService.distributions(orgId, null, today, today.minusDays(1)))
        .isInstanceOf(org.civicops.shared.exception.BusinessRuleException.class);
  }

  @Test
  void boardGovernanceWorkflowCalculatesQuorumVotingMinutesResolutionAndReports() {
    UUID orgId = UUID.randomUUID(), manager = UUID.randomUUID();
    insertOrganization(orgId, "NONPROFIT");
    insertUser(manager, "board-manager-" + manager + "@example.org");
    insertMembership(UUID.randomUUID(), orgId, manager, "BOARD_MANAGER");
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    List<UUID> users = new java.util.ArrayList<>();
    List<org.civicops.board.member.BoardMemberDtos.Detail> members = new java.util.ArrayList<>();
    for (int i = 0; i < 5; i++) {
      UUID user = UUID.randomUUID();
      users.add(user);
      insertUser(user, "board-member-" + i + "-" + user + "@example.org");
      insertMembership(UUID.randomUUID(), orgId, user, "BOARD_MEMBER");
      var member =
          boardMemberService.create(
              orgId,
              new org.civicops.board.member.BoardMemberDtos.Create(
                  "Member", "Number" + i, null, null, null, today.minusYears(1), null, user));
      members.add(member);
      boardMemberService.addTerm(
          orgId,
          member.id(),
          new org.civicops.board.member.BoardMemberDtos.CreateTerm(
              today.minusYears(1), today.plusYears(1)));
    }
    var officer =
        boardMemberService.addOfficer(
            orgId,
            members.getFirst().id(),
            new org.civicops.board.member.BoardMemberDtos.CreateOfficer(
                org.civicops.board.member.BoardOfficerRole.CHAIR,
                null,
                today.minusMonths(1),
                null));
    assertThat(officer.active()).isTrue();
    var committee =
        boardCommitteeService.create(
            orgId,
            new org.civicops.board.committee.BoardCommitteeDtos.Create("Finance Committee", null));
    var committeeMember =
        boardCommitteeService.addMember(
            orgId,
            committee.id(),
            new org.civicops.board.committee.BoardCommitteeDtos.AddMember(
                members.getFirst().id(), "Chair", today.minusMonths(1)));
    assertThatThrownBy(
            () ->
                boardCommitteeService.addMember(
                    orgId,
                    committee.id(),
                    new org.civicops.board.committee.BoardCommitteeDtos.AddMember(
                        members.getFirst().id(), "Duplicate", today)))
        .isInstanceOf(org.civicops.shared.exception.ConflictException.class);
    assertThat(boardCommitteeService.endMember(orgId, committeeMember.id(), today).active())
        .isFalse();
    Instant start = Instant.now().minusSeconds(3600), end = start.plusSeconds(7200);
    var meeting =
        boardMeetingService.create(
            orgId,
            manager,
            new org.civicops.board.meeting.BoardMeetingDtos.Create(
                "Annual Governance Meeting",
                org.civicops.board.meeting.BoardMeetingType.ANNUAL,
                start,
                end,
                "Board Room",
                null,
                4));
    meeting =
        boardMeetingService.update(
            orgId,
            meeting.id(),
            new org.civicops.board.meeting.BoardMeetingDtos.Update(
                "Annual Governance Meeting Updated", null, null, null, null, null, null));
    UUID meetingId = meeting.id();
    boardMeetingService.transition(
        orgId, meetingId, org.civicops.board.meeting.BoardMeetingStatus.PUBLISHED);
    var statuses =
        List.of(
            org.civicops.board.meeting.AttendanceStatus.PRESENT,
            org.civicops.board.meeting.AttendanceStatus.REMOTE,
            org.civicops.board.meeting.AttendanceStatus.PRESENT,
            org.civicops.board.meeting.AttendanceStatus.PRESENT,
            org.civicops.board.meeting.AttendanceStatus.ABSENT);
    for (int i = 0; i < 5; i++)
      boardMeetingService.recordAttendance(
          orgId,
          meeting.id(),
          new org.civicops.board.meeting.BoardMeetingDtos.AttendanceRequest(
              members.get(i).id(), statuses.get(i), null));
    var quorum = boardMeetingService.quorum(orgId, meeting.id());
    assertThat(quorum.presentOrRemote()).isEqualTo(4);
    assertThat(quorum.quorumMet()).isTrue();
    assertThat(quorum.attendanceRate()).isEqualByComparingTo("80.00");
    var agenda =
        boardAgendaService.create(
            orgId,
            meetingId,
            new org.civicops.board.agenda.BoardAgendaDtos.Create(
                1,
                "Budget",
                null,
                org.civicops.board.agenda.AgendaItemType.MOTION,
                "Treasurer",
                15));
    assertThatThrownBy(
            () ->
                boardAgendaService.create(
                    orgId,
                    meetingId,
                    new org.civicops.board.agenda.BoardAgendaDtos.Create(
                        1,
                        "Duplicate",
                        null,
                        org.civicops.board.agenda.AgendaItemType.OTHER,
                        null,
                        null)))
        .isInstanceOf(org.civicops.shared.exception.ConflictException.class);
    boardMeetingService.transition(
        orgId, meetingId, org.civicops.board.meeting.BoardMeetingStatus.IN_PROGRESS);
    boardAgendaService.transition(
        orgId, agenda.id(), org.civicops.board.agenda.AgendaItemStatus.IN_PROGRESS);
    boardAgendaService.transition(
        orgId, agenda.id(), org.civicops.board.agenda.AgendaItemStatus.COMPLETED);
    var motion =
        boardMotionService.create(
            orgId,
            meetingId,
            new org.civicops.board.motion.BoardMotionDtos.Create(
                agenda.id(), "Approve the annual budget", members.getFirst().id()));
    UUID motionId = motion.id();
    boardMotionService.second(orgId, motionId, members.get(1).id());
    boardMotionService.open(orgId, motionId);
    var choices =
        List.of(
            org.civicops.board.motion.VoteChoice.YES,
            org.civicops.board.motion.VoteChoice.YES,
            org.civicops.board.motion.VoteChoice.YES,
            org.civicops.board.motion.VoteChoice.NO,
            org.civicops.board.motion.VoteChoice.ABSTAIN);
    for (int i = 0; i < 5; i++)
      boardMotionService.voteSelf(orgId, motionId, users.get(i), choices.get(i));
    motion = boardMotionService.close(orgId, motionId);
    assertThat(motion.status()).isEqualTo(org.civicops.board.motion.MotionStatus.PASSED);
    assertThat(motion.yesVotes()).isEqualTo(3);
    assertThat(motion.noVotes()).isEqualTo(1);
    assertThat(motion.abstainVotes()).isEqualTo(1);
    var resolution =
        boardResolutionService.create(
            orgId,
            motionId,
            manager,
            new org.civicops.board.resolution.BoardResolutionDtos.Create(
                "RES-2030-01", "Annual Budget", "The annual budget is adopted.", today));
    assertThatThrownBy(
            () ->
                boardResolutionService.create(
                    orgId,
                    motionId,
                    manager,
                    new org.civicops.board.resolution.BoardResolutionDtos.Create(
                        "res-2030-01", "Duplicate", "Duplicate", today)))
        .isInstanceOf(org.civicops.shared.exception.ConflictException.class);
    var minutes = boardMinutesService.put(orgId, meetingId, manager, "Draft minutes");
    minutes = boardMinutesService.put(orgId, meetingId, manager, "Updated minutes");
    boardMinutesService.submit(orgId, meetingId);
    minutes = boardMinutesService.approve(orgId, meetingId, manager);
    assertThat(minutes.status()).isEqualTo(org.civicops.board.minutes.MinutesStatus.APPROVED);
    assertThatThrownBy(() -> boardMinutesService.put(orgId, meetingId, manager, "Illegal edit"))
        .isInstanceOf(org.civicops.shared.exception.BusinessRuleException.class);
    boardMeetingService.transition(
        orgId, meetingId, org.civicops.board.meeting.BoardMeetingStatus.COMPLETED);
    assertThatThrownBy(
            () ->
                boardMeetingService.update(
                    orgId,
                    meetingId,
                    new org.civicops.board.meeting.BoardMeetingDtos.Update(
                        "Illegal", null, null, null, null, null, null)))
        .isInstanceOf(org.civicops.shared.exception.BusinessRuleException.class);
    var report = boardReportingService.summary(orgId, today, today);
    assertThat(report.activeBoardMembers()).isEqualTo(5);
    assertThat(report.activeCommittees()).isEqualTo(1);
    assertThat(report.meetingsInPeriod()).isEqualTo(1);
    assertThat(report.averageAttendanceRate()).isEqualByComparingTo("80.00");
    assertThat(report.quorumFailures()).isZero();
    assertThat(report.motionsPassed()).isEqualTo(1);
    assertThat(report.resolutionsAdopted()).isEqualTo(1);
    assertThat(boardReportingService.voting(orgId, today, today).yesVotes()).isEqualTo(3);
    assertThat(boardResolutionService.rescind(orgId, resolution.id()).status())
        .isEqualTo(org.civicops.board.resolution.ResolutionStatus.RESCINDED);
  }

  @Test
  void concurrentBoardSelfVoteCreatesExactlyOneVote() throws Exception {
    UUID orgId = UUID.randomUUID(),
        manager = UUID.randomUUID(),
        voter = UUID.randomUUID(),
        seconder = UUID.randomUUID();
    insertOrganization(orgId, "NONPROFIT");
    insertUser(manager, "vote-manager-" + manager + "@example.org");
    insertUser(voter, "vote-voter-" + voter + "@example.org");
    insertUser(seconder, "vote-second-" + seconder + "@example.org");
    insertMembership(UUID.randomUUID(), orgId, manager, "BOARD_MANAGER");
    insertMembership(UUID.randomUUID(), orgId, voter, "BOARD_MEMBER");
    insertMembership(UUID.randomUUID(), orgId, seconder, "BOARD_MEMBER");
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    var a =
        boardMemberService.create(
            orgId,
            new org.civicops.board.member.BoardMemberDtos.Create(
                "Concurrent", "Voter", null, null, null, today, null, voter));
    var b =
        boardMemberService.create(
            orgId,
            new org.civicops.board.member.BoardMemberDtos.Create(
                "Motion", "Seconder", null, null, null, today, null, seconder));
    boardMemberService.addTerm(
        orgId,
        a.id(),
        new org.civicops.board.member.BoardMemberDtos.CreateTerm(
            today.minusDays(1), today.plusDays(1)));
    boardMemberService.addTerm(
        orgId,
        b.id(),
        new org.civicops.board.member.BoardMemberDtos.CreateTerm(
            today.minusDays(1), today.plusDays(1)));
    Instant start = Instant.now().minusSeconds(60);
    var meeting =
        boardMeetingService.create(
            orgId,
            manager,
            new org.civicops.board.meeting.BoardMeetingDtos.Create(
                "Vote Race",
                org.civicops.board.meeting.BoardMeetingType.SPECIAL,
                start,
                start.plusSeconds(3600),
                null,
                null,
                1));
    boardMeetingService.transition(
        orgId, meeting.id(), org.civicops.board.meeting.BoardMeetingStatus.PUBLISHED);
    boardMeetingService.recordAttendance(
        orgId,
        meeting.id(),
        new org.civicops.board.meeting.BoardMeetingDtos.AttendanceRequest(
            a.id(), org.civicops.board.meeting.AttendanceStatus.PRESENT, null));
    boardMeetingService.transition(
        orgId, meeting.id(), org.civicops.board.meeting.BoardMeetingStatus.IN_PROGRESS);
    var motion =
        boardMotionService.create(
            orgId,
            meeting.id(),
            new org.civicops.board.motion.BoardMotionDtos.Create(null, "Concurrent vote", a.id()));
    boardMotionService.second(orgId, motion.id(), b.id());
    boardMotionService.open(orgId, motion.id());
    List<Boolean> outcomes =
        race(
            () ->
                boardMotionService.voteSelf(
                    orgId, motion.id(), voter, org.civicops.board.motion.VoteChoice.YES),
            () ->
                boardMotionService.voteSelf(
                    orgId, motion.id(), voter, org.civicops.board.motion.VoteChoice.NO));
    assertThat(outcomes).containsExactlyInAnyOrder(true, false);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM board_vote WHERE motion_id=? AND board_member_id=?",
                Integer.class,
                motion.id(),
                a.id()))
        .isEqualTo(1);
    assertThat(boardMotionService.close(orgId, motion.id()).status())
        .isIn(
            org.civicops.board.motion.MotionStatus.PASSED,
            org.civicops.board.motion.MotionStatus.FAILED);
  }

  @Test
  void boardDatabaseConstraintsProtectGovernanceOrganizationsAndUniqueness() {
    UUID orgA = UUID.randomUUID(), orgB = UUID.randomUUID(), user = UUID.randomUUID();
    insertOrganization(orgA, "NONPROFIT");
    insertOrganization(orgB, "NONPROFIT");
    insertUser(user, "board-db-" + user + "@example.org");
    insertMembership(UUID.randomUUID(), orgA, user, "BOARD_MANAGER");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO board_member(id,organization_id,user_id,first_name,last_name,joined_date)VALUES (?,?,?,?,?,CURRENT_DATE)",
                    UUID.randomUUID(),
                    orgB,
                    user,
                    "Cross",
                    "Org"))
        .isInstanceOf(DataIntegrityViolationException.class);
    UUID member = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO board_member(id,organization_id,first_name,last_name,joined_date)VALUES (?,?, 'External','Member',CURRENT_DATE)",
        member,
        orgA);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO board_term(id,organization_id,board_member_id,term_start,term_end,status)VALUES (?,?,?,CURRENT_DATE,CURRENT_DATE,'ACTIVE')",
                    UUID.randomUUID(),
                    orgA,
                    member))
        .isInstanceOf(DataIntegrityViolationException.class);
    UUID meeting = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO board_meeting(id,organization_id,title,meeting_type,start_date_time,end_date_time,status,quorum_required,created_by_user_id)VALUES (?,?,?,'REGULAR',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP+INTERVAL '1 hour','PUBLISHED',1,?)",
        meeting,
        orgA,
        "Constraint Meeting",
        user);
    jdbc.update(
        "INSERT INTO board_meeting_attendance(id,organization_id,meeting_id,board_member_id,attendance_status)VALUES (?,?,?,?,'PRESENT')",
        UUID.randomUUID(),
        orgA,
        meeting,
        member);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO board_meeting_attendance(id,organization_id,meeting_id,board_member_id,attendance_status)VALUES (?,?,?,?,'ABSENT')",
                    UUID.randomUUID(),
                    orgA,
                    meeting,
                    member))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO board_agenda_item(id,organization_id,meeting_id,sequence_number,title,item_type,status)VALUES (?,?,?,0,'Bad','OTHER','PENDING')",
                    UUID.randomUUID(),
                    orgA,
                    meeting))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void grantReportEvidenceFreezesRegeneratesFinalizesAndExportsFromPostgreSql() {
    UUID orgId = UUID.randomUUID(), user = UUID.randomUUID();
    insertOrganization(orgId, "NONPROFIT");
    insertUser(user, "grant-report-" + user + "@example.org");
    insertMembership(UUID.randomUUID(), orgId, user, "GRANT_MANAGER");
    UUID grant = insertGrant(orgId, user, new BigDecimal("50000.00"), "ACTIVE");
    insertExpense(orgId, grant, user, new BigDecimal("5000.00"), "SUPPLIES");
    var template =
        grantReportTemplateService.create(
            orgId,
            user,
            new org.civicops.grantreporting.template.GrantReportTemplateDtos.Create(
                "Foundation Report", null, grant));
    grantReportTemplateService.addSection(
        orgId,
        template.id(),
        new org.civicops.grantreporting.template.GrantReportTemplateDtos.AddSection(
            "financial",
            "Financials",
            null,
            1,
            org.civicops.grantreporting.template.ReportSectionType.FINANCIAL,
            true,
            null));
    LocalDate from = LocalDate.now().minusDays(30), to = LocalDate.now();
    var report =
        grantReportService.create(
            orgId,
            grant,
            user,
            new org.civicops.grantreporting.report.GrantReportDtos.Create(
                template.id(),
                from,
                to,
                Set.of(org.civicops.grantreporting.evidence.EvidenceSourceModule.GRANT)));
    report = grantReportService.generate(orgId, report.id(), false);
    var frozen = grantReportService.evidence(orgId, report.id(), true);
    assertThat(frozen)
        .filteredOn(x -> x.metricKey().equals("grant.total_spent"))
        .singleElement()
        .satisfies(x -> assertThat(x.monetaryValue()).isEqualByComparingTo("5000.00"));
    insertExpense(orgId, grant, user, new BigDecimal("2500.00"), "TRANSPORTATION");
    assertThat(grantReportService.evidence(orgId, report.id(), true))
        .filteredOn(x -> x.metricKey().equals("grant.total_spent"))
        .singleElement()
        .satisfies(x -> assertThat(x.monetaryValue()).isEqualByComparingTo("5000.00"));
    grantReportService.generate(orgId, report.id(), true);
    assertThat(grantReportService.evidence(orgId, report.id(), true))
        .filteredOn(x -> x.metricKey().equals("grant.total_spent"))
        .singleElement()
        .satisfies(x -> assertThat(x.monetaryValue()).isEqualByComparingTo("7500.00"));
    var section = grantReportService.sectionList(orgId, report.id(), true).getFirst();
    grantReportService.editSection(orgId, section.id(), "Human-reviewed financial narrative.");
    grantReportService.approveSection(orgId, section.id());
    var finalized = grantReportService.finalizeReport(orgId, report.id(), user);
    assertThat(finalized.status())
        .isEqualTo(org.civicops.grantreporting.report.GrantReportStatus.FINALIZED);
    assertThatThrownBy(() -> grantReportService.generate(orgId, finalized.id(), true))
        .isInstanceOf(org.civicops.shared.exception.BusinessRuleException.class);
    String first = grantReportExportService.markdown(orgId, report.id(), true),
        second = grantReportExportService.markdown(orgId, report.id(), true);
    assertThat(first)
        .isEqualTo(second)
        .contains("Human-reviewed financial narrative")
        .contains("$7500.00");
  }

  @Test
  void grantReportGenerationAndFinalizationSerializeAndMissingIsNotZero() throws Exception {
    UUID orgId = UUID.randomUUID(), user = UUID.randomUUID();
    insertOrganization(orgId, "NONPROFIT");
    insertUser(user, "grant-report-race-" + user + "@example.org");
    insertMembership(UUID.randomUUID(), orgId, user, "GRANT_MANAGER");
    UUID grant = insertGrant(orgId, user, new BigDecimal("1000.00"), "ACTIVE");
    var template =
        grantReportTemplateService.create(
            orgId,
            user,
            new org.civicops.grantreporting.template.GrantReportTemplateDtos.Create(
                "Race Report", null, null));
    grantReportTemplateService.addSection(
        orgId,
        template.id(),
        new org.civicops.grantreporting.template.GrantReportTemplateDtos.AddSection(
            "outcomes",
            "Outcomes",
            null,
            1,
            org.civicops.grantreporting.template.ReportSectionType.OUTCOMES,
            true,
            null));
    var report =
        grantReportService.create(
            orgId,
            grant,
            user,
            new org.civicops.grantreporting.report.GrantReportDtos.Create(
                template.id(),
                LocalDate.now().minusDays(1),
                LocalDate.now(),
                Set.of(
                    org.civicops.grantreporting.evidence.EvidenceSourceModule.GRANT,
                    org.civicops.grantreporting.evidence.EvidenceSourceModule.VOLUNTEERS)));
    assertThat(
            race(
                () -> grantReportService.generate(orgId, report.id(), false),
                () -> grantReportService.generate(orgId, report.id(), false)))
        .containsExactlyInAnyOrder(true, false);
    assertThat(grantReportService.missing(orgId, report.id(), true))
        .extracting(org.civicops.grantreporting.evidence.EvidenceDtos.Missing::metricKey)
        .contains("volunteer.approved_hours");
    var missing =
        grantReportService.evidence(orgId, report.id(), true).stream()
            .filter(x -> x.metricKey().equals("volunteer.approved_hours"))
            .findFirst()
            .orElseThrow();
    assertThat(missing.valueState())
        .isEqualTo(org.civicops.grantreporting.evidence.EvidenceValueState.MISSING);
    assertThat(missing.numericValue()).isNull();
    var section = grantReportService.sectionList(orgId, report.id(), true).getFirst();
    grantReportService.approveSection(orgId, section.id());
    assertThat(
            race(
                () -> grantReportService.finalizeReport(orgId, report.id(), user),
                () -> grantReportService.finalizeReport(orgId, report.id(), user)))
        .containsExactlyInAnyOrder(true, false);
    assertThat(grantReportService.detail(orgId, report.id(), true).status())
        .isEqualTo(org.civicops.grantreporting.report.GrantReportStatus.FINALIZED);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM grant_report_evidence_snapshot WHERE report_id=?",
                Integer.class,
                report.id()))
        .isEqualTo(5);
  }

  @Test
  void donationDatabaseConstraintsProtectFinanceAndOrganizationRelationships() {
    UUID orgA = UUID.randomUUID(), orgB = UUID.randomUUID(), user = UUID.randomUUID();
    insertOrganization(orgA, "NONPROFIT");
    insertOrganization(orgB, "NONPROFIT");
    insertUser(user, "donation-db-" + user + "@example.org");
    UUID donorA = insertDonor(orgA, "donor-a@example.org"),
        campaignA = insertCampaign(orgA, user, "ACTIVE", new BigDecimal("1000"));
    assertThatThrownBy(
            () ->
                insertDonation(orgB, donorA, null, user, new BigDecimal("10"), "CASH", false, null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                insertDonation(
                    orgB, null, campaignA, user, new BigDecimal("10"), "CASH", true, null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> insertDonation(orgA, donorA, null, user, BigDecimal.ZERO, "CASH", false, null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void concurrentDuplicateDonationReferenceCreatesExactlyOneRecord() throws Exception {
    UUID org = UUID.randomUUID(), user = UUID.randomUUID();
    insertOrganization(org, "NONPROFIT");
    insertUser(user, "donation-race-" + user + "@example.org");
    UUID donor = insertDonor(org, "race-donor@example.org");
    CreateDonationRequest request =
        new CreateDonationRequest(
            donor,
            false,
            new BigDecimal("100.00"),
            LocalDate.now(),
            DonationPaymentMethod.ACH,
            null,
            null,
            false,
            null,
            null,
            "PROCESSOR-RACE-1",
            null,
            null,
            null);
    List<Boolean> outcomes =
        race(
            () -> donationService.create(org, user, request),
            () -> donationService.create(org, user, request));
    assertThat(outcomes).containsExactlyInAnyOrder(true, false);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM donation WHERE organization_id=? AND reference_number='PROCESSOR-RACE-1'",
                Integer.class,
                org))
        .isEqualTo(1);
  }

  @Test
  void donationFiltersAndReportingUseOrganizationScopedPostgreSqlData() {
    UUID orgA = UUID.randomUUID(),
        orgB = UUID.randomUUID(),
        emptyOrg = UUID.randomUUID(),
        userA = UUID.randomUUID(),
        userB = UUID.randomUUID();
    insertOrganization(orgA, "NONPROFIT");
    insertOrganization(orgB, "NONPROFIT");
    insertOrganization(emptyOrg, "NONPROFIT");
    insertUser(userA, "donation-report-a-" + userA + "@example.org");
    insertUser(userB, "donation-report-b-" + userB + "@example.org");
    var donorA =
        donorService.create(
            orgA,
            new CreateDonorRequest(
                DonorType.INDIVIDUAL,
                "Grace",
                "Hopper",
                null,
                " GRACE.DONOR@EXAMPLE.ORG ",
                null,
                null,
                null,
                null,
                null,
                null,
                "US",
                false,
                false,
                null));
    UUID donorA2 = insertDonor(orgA, "second@example.org"),
        donorB = insertDonor(orgB, "other@example.org");
    UUID campaignA = insertCampaign(orgA, userA, "ACTIVE", new BigDecimal("25000"));
    UUID campaignB = insertCampaign(orgB, userB, "ACTIVE", new BigDecimal("90000"));
    insertDonation(
        orgA, donorA.id(), campaignA, userA, new BigDecimal("10000"), "ACH", false, null);
    insertDonation(orgA, donorA2, campaignA, userA, new BigDecimal("5000"), "CARD", false, null);
    insertDonation(
        orgA, null, campaignA, userA, new BigDecimal("3750"), "CHECK", true, "Food pantry");
    insertDonation(orgB, donorB, campaignB, userB, new BigDecimal("80000"), "WIRE", false, null);
    assertThat(
            donorService
                .list(
                    orgA,
                    DonorType.INDIVIDUAL,
                    "GRACE.DONOR@EXAMPLE.ORG",
                    false,
                    PageRequest.of(0, 20))
                .getTotalElements())
        .isEqualTo(1);
    assertThat(
            donationCampaignService
                .list(orgA, CampaignStatus.ACTIVE, null, null, PageRequest.of(0, 20))
                .getTotalElements())
        .isEqualTo(1);
    assertThat(
            donationService
                .list(
                    orgA,
                    null,
                    campaignA,
                    null,
                    null,
                    LocalDate.now(),
                    LocalDate.now(),
                    PageRequest.of(0, 20))
                .getTotalElements())
        .isEqualTo(3);
    var financial = donationReportingService.campaign(orgA, campaignA);
    assertThat(financial.amountRaised()).isEqualByComparingTo("18750.00");
    assertThat(financial.remainingToGoal()).isEqualByComparingTo("6250.00");
    assertThat(financial.percentageOfGoal()).isEqualByComparingTo("75.00");
    var summary = donationReportingService.summary(orgA, LocalDate.now(), LocalDate.now());
    assertThat(summary.totalDonations()).isEqualTo(3);
    assertThat(summary.totalAmount()).isEqualByComparingTo("18750.00");
    assertThat(summary.averageDonation()).isEqualByComparingTo("6250.00");
    assertThat(summary.restrictedAmount()).isEqualByComparingTo("3750.00");
    assertThat(summary.unrestrictedAmount()).isEqualByComparingTo("15000.00");
    assertThat(summary.uniqueDonors()).isEqualTo(2);
    assertThat(summary.activeCampaigns()).isEqualTo(1);
    assertThat(donationReportingService.byPayment(orgA, null, null)).hasSize(3);
    assertThat(donationReportingService.summary(emptyOrg, null, null).totalAmount())
        .isEqualByComparingTo("0.00");
    assertThat(donationReportingService.byCampaign(orgA)).hasSize(1);
  }

  @Test
  void campaignLifecyclePersistsAndTerminalCampaignRejectsDonation() {
    UUID org = UUID.randomUUID(), user = UUID.randomUUID();
    insertOrganization(org, "NONPROFIT");
    insertUser(user, "campaign-life-" + user + "@example.org");
    UUID donor = insertDonor(org, "campaign-donor@example.org");
    var campaign =
        donationCampaignService.create(
            org,
            user,
            new CreateCampaignRequest(
                "Lifecycle Drive",
                null,
                new BigDecimal("500"),
                LocalDate.now(),
                LocalDate.now().plusDays(30)));
    donationCampaignService.transition(org, campaign.id(), CampaignStatus.ACTIVE);
    donationCampaignService.transition(org, campaign.id(), CampaignStatus.CLOSED);
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM donation_campaign WHERE id=?", String.class, campaign.id()))
        .isEqualTo("CLOSED");
    CreateDonationRequest request =
        new CreateDonationRequest(
            donor,
            false,
            BigDecimal.TEN,
            LocalDate.now(),
            DonationPaymentMethod.CASH,
            null,
            campaign.id(),
            false,
            null,
            null,
            null,
            null,
            null,
            null);
    assertThatThrownBy(() -> donationService.create(org, user, request))
        .isInstanceOf(org.civicops.shared.exception.BusinessRuleException.class);
  }

  @Test
  void concurrentEventRegistrationsWithoutWaitlistFillOnlyOneSeat() throws Exception {
    UUID org = UUID.randomUUID(), user = UUID.randomUUID();
    insertOrganization(org, "NONPROFIT");
    insertUser(user, "event-no-wait-" + user + "@example.org");
    UUID event = insertOpenEvent(org, user, 1, false);
    List<Boolean> outcomes =
        race(
            () ->
                eventRegistrationService.register(
                    org,
                    event,
                    null,
                    new CreateRegistrationRequest("A", "a-" + event + "@example.org", null)),
            () ->
                eventRegistrationService.register(
                    org,
                    event,
                    null,
                    new CreateRegistrationRequest("B", "b-" + event + "@example.org", null)));
    assertThat(outcomes).containsExactlyInAnyOrder(true, false);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM event_registration WHERE event_id=? AND status='REGISTERED'",
                Integer.class,
                event))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM event_registration WHERE event_id=? AND status IN ('REGISTERED','ATTENDED')",
                Integer.class,
                event))
        .isEqualTo(1);
  }

  @Test
  void concurrentEventRegistrationsWithWaitlistCreateOneSeatAndOneWaitlisted() throws Exception {
    UUID org = UUID.randomUUID(), user = UUID.randomUUID();
    insertOrganization(org, "NONPROFIT");
    insertUser(user, "event-wait-" + user + "@example.org");
    UUID event = insertOpenEvent(org, user, 1, true);
    List<Boolean> outcomes =
        race(
            () ->
                eventRegistrationService.register(
                    org,
                    event,
                    null,
                    new CreateRegistrationRequest("A", "a-" + event + "@example.org", null)),
            () ->
                eventRegistrationService.register(
                    org,
                    event,
                    null,
                    new CreateRegistrationRequest("B", "b-" + event + "@example.org", null)));
    assertThat(outcomes).containsExactly(true, true);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM event_registration WHERE event_id=? AND status='REGISTERED'",
                Integer.class,
                event))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM event_registration WHERE event_id=? AND status='WAITLISTED'",
                Integer.class,
                event))
        .isEqualTo(1);
  }

  @Test
  void cancellationPromotesWaitlistInFifoOrderOnPostgreSql() {
    UUID org = UUID.randomUUID(), user = UUID.randomUUID();
    insertOrganization(org, "NONPROFIT");
    insertUser(user, "event-promote-" + user + "@example.org");
    UUID event = insertOpenEvent(org, user, 1, true);
    var a =
        eventRegistrationService.register(
            org,
            event,
            null,
            new CreateRegistrationRequest("A", "a-" + event + "@example.org", null));
    var b =
        eventRegistrationService.register(
            org,
            event,
            null,
            new CreateRegistrationRequest("B", "b-" + event + "@example.org", null));
    var c =
        eventRegistrationService.register(
            org,
            event,
            null,
            new CreateRegistrationRequest("C", "c-" + event + "@example.org", null));
    assertThat(List.of(a.status(), b.status(), c.status()))
        .containsExactly(
            RegistrationStatus.REGISTERED,
            RegistrationStatus.WAITLISTED,
            RegistrationStatus.WAITLISTED);
    eventRegistrationService.cancel(org, a.id());
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM event_registration WHERE id=?", String.class, a.id()))
        .isEqualTo("CANCELLED");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM event_registration WHERE id=?", String.class, b.id()))
        .isEqualTo("REGISTERED");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM event_registration WHERE id=?", String.class, c.id()))
        .isEqualTo("WAITLISTED");
  }

  @Test
  void eventGrantAndCampaignLinksAreOrganizationSafeAndLifecycleIsolated() {
    UUID orgA = UUID.randomUUID(),
        orgB = UUID.randomUUID(),
        userA = UUID.randomUUID(),
        userB = UUID.randomUUID();
    insertOrganization(orgA, "NONPROFIT");
    insertOrganization(orgB, "NONPROFIT");
    insertUser(userA, "event-link-a-" + userA + "@example.org");
    insertUser(userB, "event-link-b-" + userB + "@example.org");
    UUID grantA = insertGrant(orgA, userA, new BigDecimal("50000.00"), "ACTIVE"),
        grantB = insertGrant(orgB, userB, new BigDecimal("1000.00"), "ACTIVE"),
        campaignA = insertCampaign(orgA, userA, "ACTIVE", new BigDecimal("25000.00")),
        campaignB = insertCampaign(orgB, userB, "ACTIVE", new BigDecimal("1000.00"));
    UUID event =
        insertEvent(
            orgA,
            userA,
            "REGISTRATION_OPEN",
            3,
            true,
            grantA,
            campaignA,
            Instant.now().plus(Duration.ofDays(2)));
    var report = eventReportingService.report(orgA, event);
    assertThat(report.linkedGrantId()).isEqualTo(grantA);
    assertThat(report.linkedGrantName()).isEqualTo("Community Grant");
    assertThat(report.linkedCampaignId()).isEqualTo(campaignA);
    assertThatThrownBy(
            () ->
                insertEvent(
                    orgA,
                    userA,
                    "DRAFT",
                    null,
                    false,
                    grantB,
                    null,
                    Instant.now().plus(Duration.ofDays(3))))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                insertEvent(
                    orgA,
                    userA,
                    "DRAFT",
                    null,
                    false,
                    null,
                    campaignB,
                    Instant.now().plus(Duration.ofDays(3))))
        .isInstanceOf(DataIntegrityViolationException.class);
    eventService.transition(orgA, event, EventStatus.CANCELLED);
    assertThat(
            jdbc.queryForObject("SELECT status FROM grant_record WHERE id=?", String.class, grantA))
        .isEqualTo("ACTIVE");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM donation_campaign WHERE id=?", String.class, campaignA))
        .isEqualTo("ACTIVE");
  }

  @Test
  void eventReportComposesCampaignFinancialsWithoutMutatingDonations() {
    UUID org = UUID.randomUUID(), user = UUID.randomUUID();
    insertOrganization(org, "NONPROFIT");
    insertUser(user, "event-campaign-" + user + "@example.org");
    UUID donor = insertDonor(org, "campaign-event-donor-" + org + "@example.org"),
        campaign = insertCampaign(org, user, "ACTIVE", new BigDecimal("25000.00"));
    insertDonation(org, donor, campaign, user, new BigDecimal("10000.00"), "ACH", false, null);
    insertDonation(org, donor, campaign, user, new BigDecimal("5000.00"), "CARD", false, null);
    insertDonation(org, null, campaign, user, new BigDecimal("3750.00"), "CHECK", true, "Cleanup");
    UUID event =
        insertEvent(
            org,
            user,
            "REGISTRATION_CLOSED",
            null,
            false,
            null,
            campaign,
            Instant.now().plus(Duration.ofDays(2)));
    var report = eventReportingService.report(org, event);
    assertThat(report.campaignGoal()).isEqualByComparingTo("25000.00");
    assertThat(report.campaignRaised()).isEqualByComparingTo("18750.00");
    assertThat(report.campaignDonationCount()).isEqualTo(3);
    eventService.transition(org, event, EventStatus.COMPLETED);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM donation WHERE campaign_id=?", Integer.class, campaign))
        .isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM donation_campaign WHERE id=?", String.class, campaign))
        .isEqualTo("ACTIVE");
  }

  @Test
  void cancelledEventPreservesLinkedVolunteerHistoryAndCrossOrganizationLinkFails() {
    UUID orgA = UUID.randomUUID(), orgB = UUID.randomUUID(), user = UUID.randomUUID();
    insertOrganization(orgA, "NONPROFIT");
    insertOrganization(orgB, "NONPROFIT");
    insertUser(user, "event-volunteer-" + user + "@example.org");
    UUID event =
        insertEvent(
            orgA,
            user,
            "REGISTRATION_OPEN",
            null,
            false,
            null,
            null,
            Instant.now().plus(Duration.ofDays(2)));
    UUID opportunity = insertOpportunity(orgA);
    jdbc.update("UPDATE volunteer_opportunity SET event_id=? WHERE id=?", event, opportunity);
    UUID volunteer = insertVolunteer(orgA, "history-" + event + "@example.org"),
        shift = insertShift(orgA, opportunity);
    jdbc.update(
        "INSERT INTO volunteer_assignment (id,organization_id,volunteer_id,shift_id,status) VALUES (?,?,?,?, 'CONFIRMED')",
        UUID.randomUUID(),
        orgA,
        volunteer,
        shift);
    insertHour(orgA, volunteer, "APPROVED", "20.00");
    UUID foreignOpportunity = insertOpportunity(orgB);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE volunteer_opportunity SET event_id=? WHERE id=?",
                    event,
                    foreignOpportunity))
        .isInstanceOf(DataIntegrityViolationException.class);
    eventService.transition(orgA, event, EventStatus.CANCELLED);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM volunteer_opportunity WHERE id=?",
                Integer.class,
                opportunity))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM volunteer_shift WHERE id=?", Integer.class, shift))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM volunteer_hour_entry WHERE volunteer_id=? AND status='APPROVED'",
                Integer.class,
                volunteer))
        .isEqualTo(1);
    assertThat(reportingService.summary(orgA, LocalDate.now(), LocalDate.now()).approvedHours())
        .isEqualByComparingTo("20.00");
  }

  @Test
  void eventReportsHandleZeroUnlimitedFullAndMixedStatesAndDateIsolation() {
    UUID orgA = UUID.randomUUID(), orgB = UUID.randomUUID(), user = UUID.randomUUID();
    insertOrganization(orgA, "NONPROFIT");
    insertOrganization(orgB, "NONPROFIT");
    insertUser(user, "event-report-" + user + "@example.org");
    UUID zero =
        insertEvent(
            orgA, user, "COMPLETED", 100, false, null, null, Instant.parse("2026-09-10T10:00:00Z"));
    var zeroReport = eventReportingService.report(orgA, zero);
    assertThat(zeroReport.remainingCapacity()).isEqualTo(100);
    assertThat(zeroReport.attendanceRate()).isEqualByComparingTo("0.00");
    UUID unlimited =
        insertEvent(
            orgA,
            user,
            "REGISTRATION_OPEN",
            null,
            true,
            null,
            null,
            Instant.parse("2026-09-11T10:00:00Z"));
    assertThat(eventReportingService.report(orgA, unlimited).remainingCapacity()).isNull();
    UUID mixed =
        insertEvent(
            orgA, user, "COMPLETED", 3, true, null, null, Instant.parse("2026-09-12T10:00:00Z"));
    insertRegistration(orgA, mixed, "REGISTERED");
    insertRegistration(orgA, mixed, "REGISTERED");
    insertRegistration(orgA, mixed, "ATTENDED");
    insertRegistration(orgA, mixed, "WAITLISTED");
    insertRegistration(orgA, mixed, "CANCELLED");
    insertRegistration(orgA, mixed, "NO_SHOW");
    var mixedReport = eventReportingService.report(orgA, mixed);
    assertThat(mixedReport.registered()).isEqualTo(2);
    assertThat(mixedReport.attended()).isEqualTo(1);
    assertThat(mixedReport.waitlisted()).isEqualTo(1);
    assertThat(mixedReport.cancelled()).isEqualTo(1);
    assertThat(mixedReport.noShow()).isEqualTo(1);
    assertThat(mixedReport.remainingCapacity()).isZero();
    assertThat(mixedReport.attendanceRate()).isEqualByComparingTo("33.33");
    insertEvent(
        orgB, user, "COMPLETED", 1, false, null, null, Instant.parse("2026-09-12T10:00:00Z"));
    assertThat(
            eventReportingService
                .summary(orgA, LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 12))
                .upcomingEvents())
        .isEqualTo(2);
  }

  @Test
  void grantDatabaseConstraintsRejectInvalidFinanceAndCrossOrganizationExpense() {
    UUID orgA = UUID.randomUUID(), orgB = UUID.randomUUID(), user = UUID.randomUUID();
    insertOrganization(orgA, "NONPROFIT");
    insertOrganization(orgB, "NONPROFIT");
    insertUser(user, "grant-db-" + user + "@example.org");
    assertThatThrownBy(() -> insertGrant(orgA, user, new BigDecimal("-0.01"), "PROSPECT"))
        .isInstanceOf(DataIntegrityViolationException.class);

    UUID grant = insertGrant(orgA, user, new BigDecimal("1000.00"), "ACTIVE");
    assertThatThrownBy(() -> insertExpense(orgB, grant, user, new BigDecimal("10.00"), "SUPPLIES"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void grantLifecyclePersistsThroughProductionService() {
    UUID org = UUID.randomUUID(), user = UUID.randomUUID();
    insertOrganization(org, "NONPROFIT");
    insertUser(user, "lifecycle-" + user + "@example.org");
    var created =
        grantService.create(
            org,
            user,
            new CreateGrantRequest(
                "Lifecycle Grant",
                "Foundation",
                null,
                null,
                new BigDecimal("25000.00"),
                null,
                null,
                null,
                LocalDate.now(),
                LocalDate.now().plusMonths(6),
                LocalDate.now().plusMonths(7),
                false,
                null,
                null,
                null,
                null));
    UUID id = created.id();
    grantService.transition(org, id, GrantStatus.APPLICATION_IN_PROGRESS);
    grantService.transition(org, id, GrantStatus.SUBMITTED);
    grantService.transition(org, id, GrantStatus.AWARDED);
    grantService.transition(org, id, GrantStatus.ACTIVE);
    grantService.transition(org, id, GrantStatus.CLOSED);
    assertThat(jdbc.queryForObject("SELECT status FROM grant_record WHERE id=?", String.class, id))
        .isEqualTo("CLOSED");
    assertThat(
            jdbc.queryForObject(
                "SELECT submitted_date IS NOT NULL AND award_date IS NOT NULL "
                    + "FROM grant_record WHERE id=?",
                Boolean.class,
                id))
        .isTrue();
  }

  @Test
  void concurrentExpensesCannotOverspendGrantAward() throws Exception {
    UUID org = UUID.randomUUID(), user = UUID.randomUUID();
    insertOrganization(org, "NONPROFIT");
    insertUser(user, "expense-race-" + user + "@example.org");
    UUID grant = insertGrant(org, user, new BigDecimal("1000.00"), "ACTIVE");
    insertExpense(org, grant, user, new BigDecimal("500.00"), "SUPPLIES");
    CreateGrantExpenseRequest requestA = expenseRequest("400.00", ExpenseCategory.EQUIPMENT);
    CreateGrantExpenseRequest requestB = expenseRequest("400.00", ExpenseCategory.TRAVEL);

    List<Boolean> outcomes =
        race(
            () -> grantExpenseService.create(org, grant, user, requestA),
            () -> grantExpenseService.create(org, grant, user, requestB));

    assertThat(outcomes).containsExactlyInAnyOrder(true, false);
    assertThat(grantSpent(grant)).isEqualByComparingTo("900.00");
    assertThat(grantSpent(grant)).isLessThanOrEqualTo(new BigDecimal("1000.00"));
  }

  @Test
  void grantFiltersAndReportingAreOrganizationScoped() {
    UUID orgA = UUID.randomUUID(), orgB = UUID.randomUUID(), emptyOrg = UUID.randomUUID();
    UUID userA = UUID.randomUUID(), userB = UUID.randomUUID();
    insertOrganization(orgA, "NONPROFIT");
    insertOrganization(orgB, "NONPROFIT");
    insertOrganization(emptyOrg, "NONPROFIT");
    insertUser(userA, "report-a-" + userA + "@example.org");
    insertUser(userB, "report-b-" + userB + "@example.org");
    UUID grantA =
        insertGrant(
            orgA,
            userA,
            new BigDecimal("50000.00"),
            "ACTIVE",
            LocalDate.now().plusDays(15),
            true,
            "Youth services only");
    UUID grantB =
        insertGrant(
            orgB,
            userB,
            new BigDecimal("90000.00"),
            "ACTIVE",
            LocalDate.now().plusDays(10),
            false,
            null);
    insertExpense(orgA, grantA, userA, new BigDecimal("4200.00"), "SUPPLIES");
    insertExpense(orgA, grantA, userA, new BigDecimal("1500.00"), "TRANSPORTATION");
    insertExpense(orgB, grantB, userB, new BigDecimal("80000.00"), "SUPPLIES");

    assertThat(
            grantService
                .list(
                    orgA,
                    GrantStatus.ACTIVE,
                    "foundation",
                    LocalDate.now(),
                    LocalDate.now().plusDays(30),
                    null,
                    null,
                    true,
                    PageRequest.of(0, 20))
                .getTotalElements())
        .isEqualTo(1);
    assertThat(
            grantExpenseService
                .list(
                    orgA,
                    grantA,
                    ExpenseCategory.SUPPLIES,
                    LocalDate.now().minusDays(1),
                    LocalDate.now().plusDays(1),
                    PageRequest.of(0, 20))
                .getTotalElements())
        .isEqualTo(1);
    var financial = grantReportingService.financial(orgA, grantA);
    assertThat(financial.totalSpent()).isEqualByComparingTo("5700.00");
    assertThat(financial.remainingBalance()).isEqualByComparingTo("44300.00");
    assertThat(financial.utilizationPercent()).isEqualByComparingTo("11.40");
    assertThat(grantReportingService.byCategory(orgA, grantA)).hasSize(2);
    var summary = grantReportingService.summary(orgA);
    assertThat(summary.totalGrants()).isEqualTo(1);
    assertThat(summary.activeGrants()).isEqualTo(1);
    assertThat(summary.totalAwarded()).isEqualByComparingTo("50000.00");
    assertThat(summary.totalSpent()).isEqualByComparingTo("5700.00");
    assertThat(summary.reportsDueSoon()).isEqualTo(1);
    assertThat(grantReportingService.summary(emptyOrg).totalAwarded()).isEqualByComparingTo("0.00");
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
    assertThatThrownBy(
            () -> insertMembership(UUID.randomUUID(), organizationId, userId, "ORG_ADMIN"))
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
    assertThatThrownBy(
            () -> insertMembership(UUID.randomUUID(), organizationId, userId, "SYSTEM_ADMIN"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @Transactional
  void volunteerAssignmentRejectsCrossOrganizationReferences() {
    UUID organizationA = UUID.randomUUID();
    UUID organizationB = UUID.randomUUID();
    insertOrganization(organizationA, "NONPROFIT");
    insertOrganization(organizationB, "NONPROFIT");
    UUID volunteerId = insertVolunteer(organizationA, "cross@example.org");
    UUID opportunityId = insertOpportunity(organizationB);
    UUID shiftId = insertShift(organizationB, opportunityId);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO volunteer_assignment (id, organization_id, volunteer_id, shift_id,"
                        + " status) VALUES (?, ?, ?, ?, 'REGISTERED')",
                    UUID.randomUUID(),
                    organizationB,
                    volunteerId,
                    shiftId))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @Transactional
  void databaseRejectsDuplicateActiveShiftRegistration() {
    UUID organizationId = UUID.randomUUID();
    insertOrganization(organizationId, "NONPROFIT");
    UUID volunteerId = insertVolunteer(organizationId, "duplicate-assignment@example.org");
    UUID opportunityId = insertOpportunity(organizationId);
    UUID shiftId = insertShift(organizationId, opportunityId);
    jdbc.update(
        "INSERT INTO volunteer_assignment (id, organization_id, volunteer_id, shift_id, status)"
            + " VALUES (?, ?, ?, ?, 'REGISTERED')",
        UUID.randomUUID(),
        organizationId,
        volunteerId,
        shiftId);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO volunteer_assignment (id, organization_id, volunteer_id, shift_id,"
                        + " status) VALUES (?, ?, ?, ?, 'CONFIRMED')",
                    UUID.randomUUID(),
                    organizationId,
                    volunteerId,
                    shiftId))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void concurrentRegistrationsCannotOversubscribeFinalSlot() throws Exception {
    UUID organizationId = UUID.randomUUID();
    insertOrganization(organizationId, "NONPROFIT");
    UUID volunteerA = insertVolunteer(organizationId, "race-a-" + organizationId + "@example.org");
    UUID volunteerB = insertVolunteer(organizationId, "race-b-" + organizationId + "@example.org");
    UUID opportunityId = insertOpportunity(organizationId);
    UUID shiftId = insertShift(organizationId, opportunityId, 1);

    List<Boolean> outcomes =
        race(
            () -> assignmentService.register(organizationId, shiftId, volunteerA),
            () -> assignmentService.register(organizationId, shiftId, volunteerB));

    assertThat(outcomes).containsExactlyInAnyOrder(true, false);
    assertThat(activeAssignments(shiftId)).isEqualTo(1);
  }

  @Test
  void concurrentDuplicateRegistrationCreatesOnlyOneActiveAssignment() throws Exception {
    UUID organizationId = UUID.randomUUID();
    insertOrganization(organizationId, "NONPROFIT");
    UUID volunteerId =
        insertVolunteer(organizationId, "same-race-" + organizationId + "@example.org");
    UUID opportunityId = insertOpportunity(organizationId);
    UUID shiftId = insertShift(organizationId, opportunityId, 2);

    List<Boolean> outcomes =
        race(
            () -> assignmentService.register(organizationId, shiftId, volunteerId),
            () -> assignmentService.register(organizationId, shiftId, volunteerId));

    assertThat(outcomes).containsExactlyInAnyOrder(true, false);
    assertThat(activeAssignments(shiftId)).isEqualTo(1);
  }

  @Test
  void filtersAndReportingUseOrganizationScopedPostgreSqlData() {
    UUID organizationA = UUID.randomUUID(),
        organizationB = UUID.randomUUID(),
        emptyOrganization = UUID.randomUUID();
    insertOrganization(organizationA, "NONPROFIT");
    insertOrganization(organizationB, "NONPROFIT");
    insertOrganization(emptyOrganization, "NONPROFIT");
    UUID volunteerA = insertVolunteer(organizationA, "filter-a@example.org");
    UUID volunteerB = insertVolunteer(organizationB, "filter-b@example.org");
    jdbc.update("INSERT INTO volunteer_skill (volunteer_id, skill) VALUES (?, 'java')", volunteerA);
    UUID opportunityA = insertOpportunity(organizationA);
    insertOpportunity(organizationB);
    UUID shiftA = insertShift(organizationA, opportunityA, 2);
    jdbc.update(
        "INSERT INTO volunteer_assignment (id, organization_id, volunteer_id, shift_id, status)"
            + " VALUES (?, ?, ?, ?, 'CANCELLED')",
        UUID.randomUUID(),
        organizationA,
        volunteerA,
        shiftA);
    insertHour(organizationA, volunteerA, "APPROVED", "2.50");
    insertHour(organizationA, volunteerA, "REJECTED", "9.00");
    insertHour(organizationB, volunteerB, "APPROVED", "20.00");

    assertThat(
            volunteerService
                .list(organizationA, VolunteerStatus.ACTIVE, null, "JAVA", PageRequest.of(0, 20))
                .getTotalElements())
        .isEqualTo(1);
    assertThat(
            opportunityService
                .list(
                    organizationA,
                    OpportunityStatus.OPEN,
                    Instant.now(),
                    Instant.now().plus(Duration.ofDays(3)),
                    PageRequest.of(0, 20))
                .getTotalElements())
        .isEqualTo(1);
    assertThat(
            hourEntryService
                .list(
                    organizationA,
                    HourEntryStatus.APPROVED,
                    volunteerA,
                    LocalDate.now(),
                    LocalDate.now(),
                    PageRequest.of(0, 20))
                .getTotalElements())
        .isEqualTo(1);
    assertThat(
            assignmentService
                .forShift(
                    organizationA,
                    shiftA,
                    volunteerA,
                    org.civicops.volunteers.assignment.AssignmentStatus.CANCELLED,
                    PageRequest.of(0, 20))
                .getTotalElements())
        .isEqualTo(1);

    var report = reportingService.summary(organizationA, LocalDate.now(), LocalDate.now());
    assertThat(report.activeVolunteers()).isEqualTo(1);
    assertThat(report.approvedHours()).isEqualByComparingTo("2.50");
    assertThat(report.upcomingShifts()).isEqualTo(1);
    assertThat(report.openOpportunities()).isEqualTo(1);
    assertThat(report.openShiftCapacity()).isEqualTo(2);
    var empty = reportingService.summary(emptyOrganization, LocalDate.now(), LocalDate.now());
    assertThat(empty.activeVolunteers()).isZero();
    assertThat(empty.approvedHours()).isEqualByComparingTo("0");
  }

  private void insertOrganization(UUID id, String type) {
    jdbc.update(
        "INSERT INTO organization (id, name, organization_type) VALUES (?, ?, ?)",
        id,
        "Test Org",
        type);
  }

  private void insertUser(UUID id, String email) {
    jdbc.update(
        "INSERT INTO app_user (id, first_name, last_name, email, password_hash) VALUES (?, ?, ?, ?,"
            + " ?)",
        id,
        "Test",
        "User",
        email,
        "$2a$10$abcdefghijklmnopqrstuvwxyz123456789012345678901234567");
  }

  private void insertMembership(UUID id, UUID organizationId, UUID userId, String role) {
    jdbc.update(
        "INSERT INTO organization_membership (id, organization_id, user_id, role) VALUES (?, ?, ?,"
            + " ?)",
        id,
        organizationId,
        userId,
        role);
  }

  private UUID insertVolunteer(UUID organizationId, String email) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO volunteer (id, organization_id, first_name, last_name, email, status) VALUES"
            + " (?, ?, 'Test', 'Volunteer', ?, 'ACTIVE')",
        id,
        organizationId,
        email);
    return id;
  }

  private UUID insertOpportunity(UUID organizationId) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO volunteer_opportunity (id, organization_id, title, start_at, end_at, status)"
            + " VALUES (?, ?, 'Test Opportunity', CURRENT_TIMESTAMP + INTERVAL '1 day',"
            + " CURRENT_TIMESTAMP + INTERVAL '2 days', 'OPEN')",
        id,
        organizationId);
    return id;
  }

  private UUID insertShift(UUID organizationId, UUID opportunityId) {
    return insertShift(organizationId, opportunityId, 2);
  }

  private UUID insertShift(UUID organizationId, UUID opportunityId, int capacity) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO volunteer_shift (id, organization_id, opportunity_id, title, start_at, end_at,"
            + " capacity) VALUES (?, ?, ?, 'Test Shift', CURRENT_TIMESTAMP + INTERVAL '1 day',"
            + " CURRENT_TIMESTAMP + INTERVAL '1 day 2 hours', ?)",
        id,
        organizationId,
        opportunityId,
        capacity);
    return id;
  }

  private List<Boolean> race(Callable<?> first, Callable<?> second) throws Exception {
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Boolean> wrapFirst = raceAttempt(first, ready, start);
      Callable<Boolean> wrapSecond = raceAttempt(second, ready, start);
      Future<Boolean> a = executor.submit(wrapFirst);
      Future<Boolean> b = executor.submit(wrapSecond);
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      return List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
    }
  }

  private Callable<Boolean> raceAttempt(
      Callable<?> action, CountDownLatch ready, CountDownLatch start) {
    return () -> {
      ready.countDown();
      start.await();
      try {
        action.call();
        return true;
      } catch (org.civicops.shared.exception.CivicOpsException expected) {
        return false;
      }
    };
  }

  private int activeAssignments(UUID shiftId) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM volunteer_assignment WHERE shift_id=? AND status <> 'CANCELLED'",
        Integer.class,
        shiftId);
  }

  private void insertHour(UUID organizationId, UUID volunteerId, String status, String amount) {
    jdbc.update(
        "INSERT INTO volunteer_hour_entry (id, organization_id, volunteer_id, service_date, hours,"
            + " status) VALUES (?, ?, ?, CURRENT_DATE, ?::numeric, ?)",
        UUID.randomUUID(),
        organizationId,
        volunteerId,
        amount,
        status);
  }

  private UUID insertGrant(UUID organizationId, UUID userId, BigDecimal award, String status) {
    return insertGrant(
        organizationId, userId, award, status, LocalDate.now().plusDays(20), false, null);
  }

  private UUID insertGrant(
      UUID organizationId,
      UUID userId,
      BigDecimal award,
      String status,
      LocalDate reportingDeadline,
      boolean restricted,
      String restrictionDescription) {
    UUID id = UUID.randomUUID();
    LocalDate submitted =
        switch (status) {
          case "SUBMITTED", "AWARDED", "ACTIVE", "CLOSED", "REJECTED" -> LocalDate.now();
          default -> null;
        };
    LocalDate awarded =
        switch (status) {
          case "AWARDED", "ACTIVE", "CLOSED" -> LocalDate.now();
          default -> null;
        };
    jdbc.update(
        "INSERT INTO grant_record (id, organization_id, grant_name, grantor_name, "
            + "award_amount, submitted_date, award_date, start_date, end_date, reporting_deadline, "
            + "status, restricted, restriction_description, created_by_user_id) "
            + "VALUES (?, ?, 'Community Grant', 'Community Foundation', ?, ?, ?, CURRENT_DATE, "
            + "CURRENT_DATE + 5, ?, ?, ?, ?, ?)",
        id,
        organizationId,
        award,
        submitted,
        awarded,
        reportingDeadline,
        status,
        restricted,
        restrictionDescription,
        userId);
    return id;
  }

  private UUID insertExpense(
      UUID organizationId, UUID grantId, UUID userId, BigDecimal amount, String category) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO grant_expense (id, organization_id, grant_id, amount, expense_date, "
            + "category, description, created_by_user_id) VALUES (?, ?, ?, ?, CURRENT_DATE, ?, "
            + "'Integration expense', ?)",
        id,
        organizationId,
        grantId,
        amount,
        category,
        userId);
    return id;
  }

  private CreateGrantExpenseRequest expenseRequest(String amount, ExpenseCategory category) {
    return new CreateGrantExpenseRequest(
        new BigDecimal(amount),
        LocalDate.now(),
        category,
        "Concurrent grant expense",
        null,
        null,
        null);
  }

  private BigDecimal grantSpent(UUID grantId) {
    return jdbc.queryForObject(
        "SELECT COALESCE(SUM(amount), 0) FROM grant_expense WHERE grant_id=?",
        BigDecimal.class,
        grantId);
  }

  private UUID insertDonor(UUID organizationId, String email) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donor (id,organization_id,donor_type,first_name,last_name,email,anonymous,communication_opt_out) VALUES (?,?,'INDIVIDUAL','Test','Donor',?,false,false)",
        id,
        organizationId,
        email);
    return id;
  }

  private UUID insertCampaign(UUID organizationId, UUID userId, String status, BigDecimal goal) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donation_campaign (id,organization_id,name,goal_amount,start_date,end_date,status,created_by_user_id) VALUES (?,?,'Community Food Drive',?,CURRENT_DATE,CURRENT_DATE+30,?,?)",
        id,
        organizationId,
        goal,
        status,
        userId);
    return id;
  }

  private UUID insertOpenEvent(UUID organizationId, UUID userId, int capacity, boolean waitlist) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO event_record (id,organization_id,name,event_type,start_date_time,end_date_time,capacity,registration_required,waitlist_enabled,status,created_by_user_id) VALUES (?,?,'Integration Event','OTHER',CURRENT_TIMESTAMP + INTERVAL '1 day',CURRENT_TIMESTAMP + INTERVAL '2 days',?,true,?,'REGISTRATION_OPEN',?)",
        id,
        organizationId,
        capacity,
        waitlist,
        userId);
    return id;
  }

  private UUID insertEvent(
      UUID org,
      UUID user,
      String status,
      Integer capacity,
      boolean waitlist,
      UUID grant,
      UUID campaign,
      Instant start) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO event_record (id,organization_id,name,event_type,start_date_time,end_date_time,capacity,registration_required,waitlist_enabled,status,linked_grant_id,linked_campaign_id,created_by_user_id) VALUES (?,?,'Integration Event','OTHER',?,?,?,?,?,?,?,?,?)",
        id,
        org,
        java.sql.Timestamp.from(start),
        java.sql.Timestamp.from(start.plus(Duration.ofHours(2))),
        capacity,
        true,
        waitlist,
        status,
        grant,
        campaign,
        user);
    return id;
  }

  private void insertRegistration(UUID org, UUID event, String status) {
    jdbc.update(
        "INSERT INTO event_registration (id,organization_id,event_id,attendee_name,attendee_email,registration_date,status) VALUES (?,?,?,?,?,CURRENT_TIMESTAMP,?)",
        UUID.randomUUID(),
        org,
        event,
        "Test Attendee",
        UUID.randomUUID() + "@example.org",
        status);
  }

  private UUID insertDonation(
      UUID organizationId,
      UUID donorId,
      UUID campaignId,
      UUID userId,
      BigDecimal amount,
      String paymentMethod,
      boolean anonymous,
      String restriction) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donation (id,organization_id,donor_id,campaign_id,anonymous,amount,donation_date,payment_method,restricted,restriction_description,acknowledgement_status,status,created_by_user_id) VALUES (?,?,?,?,?,?,CURRENT_DATE,?,?,?,'PENDING','RECORDED',?)",
        id,
        organizationId,
        donorId,
        campaignId,
        anonymous,
        amount,
        paymentMethod,
        restriction != null,
        restriction,
        userId);
    return id;
  }
}
