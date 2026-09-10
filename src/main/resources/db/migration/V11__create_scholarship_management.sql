ALTER TABLE organization_membership DROP CONSTRAINT ck_membership_role;
ALTER TABLE organization_membership ADD CONSTRAINT ck_membership_role CHECK (role IN ('ORG_ADMIN','PROGRAM_MANAGER','GRANT_MANAGER','DONATION_MANAGER','VOLUNTEER_COORDINATOR','EVENT_COORDINATOR','CASE_MANAGER','CASE_WORKER','EQUIPMENT_MANAGER','FACILITY_MANAGER','SCHOLARSHIP_MANAGER','SCHOLARSHIP_REVIEWER','VOLUNTEER','VIEWER'));

CREATE TABLE scholarship_program (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), name VARCHAR(200) NOT NULL,
 description TEXT, academic_year VARCHAR(30), application_open_date DATE NOT NULL, application_deadline DATE NOT NULL,
 award_amount NUMERIC(19,2), number_of_awards INTEGER, eligibility_description TEXT, status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 created_by_user_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_scholarship_program_id_org UNIQUE(id,organization_id),
 CONSTRAINT fk_scholarship_program_creator_membership FOREIGN KEY(organization_id,created_by_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT ck_scholarship_program_dates CHECK(application_deadline>application_open_date),
 CONSTRAINT ck_scholarship_program_amount CHECK(award_amount IS NULL OR award_amount>=0),
 CONSTRAINT ck_scholarship_program_count CHECK(number_of_awards IS NULL OR number_of_awards>0),
 CONSTRAINT ck_scholarship_program_status CHECK(status IN ('DRAFT','OPEN','CLOSED','REVIEWING','AWARDED','CANCELLED'))
);
CREATE INDEX idx_scholarship_program_org_status ON scholarship_program(organization_id,status);
CREATE INDEX idx_scholarship_program_deadline ON scholarship_program(organization_id,application_deadline);

CREATE TABLE scholarship_applicant (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), user_id UUID,
 first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100) NOT NULL, preferred_name VARCHAR(100),
 email VARCHAR(320) NOT NULL, normalized_email VARCHAR(320) NOT NULL, phone VARCHAR(50), date_of_birth DATE,
 address TEXT, school_name VARCHAR(200), graduation_year INTEGER, student_id VARCHAR(100), notes TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 version BIGINT NOT NULL DEFAULT 0, CONSTRAINT uk_scholarship_applicant_id_org UNIQUE(id,organization_id),
 CONSTRAINT uk_scholarship_applicant_email UNIQUE(organization_id,normalized_email),
 CONSTRAINT fk_scholarship_applicant_user_membership FOREIGN KEY(organization_id,user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT ck_scholarship_applicant_grad_year CHECK(graduation_year IS NULL OR graduation_year BETWEEN 1900 AND 2200)
);
CREATE INDEX idx_scholarship_applicant_school ON scholarship_applicant(organization_id,school_name);

CREATE TABLE scholarship_application (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), scholarship_program_id UUID NOT NULL,
 applicant_id UUID NOT NULL, submitted_at TIMESTAMPTZ, status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 eligibility_confirmed BOOLEAN NOT NULL DEFAULT FALSE, eligibility_notes TEXT, personal_statement TEXT,
 financial_need_statement TEXT, gpa NUMERIC(4,2), household_income NUMERIC(19,2), requested_amount NUMERIC(19,2),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 version BIGINT NOT NULL DEFAULT 0, CONSTRAINT uk_scholarship_application_id_org UNIQUE(id,organization_id),
 CONSTRAINT uk_scholarship_application_award_identity UNIQUE(id,organization_id,scholarship_program_id,applicant_id),
 CONSTRAINT uk_scholarship_application_program_applicant UNIQUE(scholarship_program_id,applicant_id),
 CONSTRAINT fk_scholarship_application_program_org FOREIGN KEY(scholarship_program_id,organization_id) REFERENCES scholarship_program(id,organization_id),
 CONSTRAINT fk_scholarship_application_applicant_org FOREIGN KEY(applicant_id,organization_id) REFERENCES scholarship_applicant(id,organization_id),
 CONSTRAINT ck_scholarship_application_status CHECK(status IN ('DRAFT','SUBMITTED','UNDER_REVIEW','FINALIST','SELECTED','NOT_SELECTED','WITHDRAWN')),
 CONSTRAINT ck_scholarship_application_submission CHECK((status='DRAFT' AND submitted_at IS NULL) OR status='WITHDRAWN' OR (status NOT IN ('DRAFT','WITHDRAWN') AND submitted_at IS NOT NULL)),
 CONSTRAINT ck_scholarship_application_gpa CHECK(gpa IS NULL OR (gpa>=0 AND gpa<=4.00)),
 CONSTRAINT ck_scholarship_application_income CHECK(household_income IS NULL OR household_income>=0),
 CONSTRAINT ck_scholarship_application_requested CHECK(requested_amount IS NULL OR requested_amount>0)
);
CREATE INDEX idx_scholarship_application_org_status ON scholarship_application(organization_id,status);
CREATE INDEX idx_scholarship_application_program ON scholarship_application(scholarship_program_id);
CREATE INDEX idx_scholarship_application_applicant ON scholarship_application(applicant_id);

CREATE TABLE scholarship_document (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), application_id UUID NOT NULL,
 document_type VARCHAR(30) NOT NULL, file_name VARCHAR(255) NOT NULL, external_storage_reference VARCHAR(500) NOT NULL,
 uploaded_at TIMESTAMPTZ NOT NULL, verified BOOLEAN NOT NULL DEFAULT FALSE, verified_by_user_id UUID,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_scholarship_document_application_org FOREIGN KEY(application_id,organization_id) REFERENCES scholarship_application(id,organization_id),
 CONSTRAINT fk_scholarship_document_verifier_membership FOREIGN KEY(organization_id,verified_by_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT ck_scholarship_document_type CHECK(document_type IN ('TRANSCRIPT','RECOMMENDATION','ESSAY','PROOF_OF_ENROLLMENT','FINANCIAL_DOCUMENT','OTHER'))
);
CREATE INDEX idx_scholarship_document_application ON scholarship_document(organization_id,application_id);

CREATE TABLE scholarship_review_assignment (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), application_id UUID NOT NULL,
 reviewer_user_id UUID NOT NULL, assigned_by_user_id UUID NOT NULL, assigned_at TIMESTAMPTZ NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED', completed_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 version BIGINT NOT NULL DEFAULT 0, CONSTRAINT uk_scholarship_assignment_id_org UNIQUE(id,organization_id),
 CONSTRAINT uk_scholarship_assignment_reviewer UNIQUE(application_id,reviewer_user_id),
 CONSTRAINT fk_scholarship_assignment_application_org FOREIGN KEY(application_id,organization_id) REFERENCES scholarship_application(id,organization_id),
 CONSTRAINT fk_scholarship_assignment_reviewer_membership FOREIGN KEY(organization_id,reviewer_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT fk_scholarship_assignment_assigner_membership FOREIGN KEY(organization_id,assigned_by_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT ck_scholarship_assignment_status CHECK(status IN ('ASSIGNED','IN_PROGRESS','COMPLETED','CANCELLED')),
 CONSTRAINT ck_scholarship_assignment_complete CHECK((status='COMPLETED' AND completed_at IS NOT NULL) OR status<>'COMPLETED')
);
CREATE INDEX idx_scholarship_assignment_reviewer_status ON scholarship_review_assignment(organization_id,reviewer_user_id,status);

CREATE TABLE scholarship_review (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), assignment_id UUID NOT NULL UNIQUE,
 score NUMERIC(5,2) NOT NULL, recommendation VARCHAR(30) NOT NULL, comments TEXT, submitted_at TIMESTAMPTZ NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_scholarship_review_assignment_org FOREIGN KEY(assignment_id,organization_id) REFERENCES scholarship_review_assignment(id,organization_id),
 CONSTRAINT ck_scholarship_review_score CHECK(score>=0 AND score<=100),
 CONSTRAINT ck_scholarship_review_recommendation CHECK(recommendation IN ('STRONGLY_RECOMMEND','RECOMMEND','NEUTRAL','DO_NOT_RECOMMEND'))
);

CREATE TABLE scholarship_award (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), scholarship_program_id UUID NOT NULL,
 application_id UUID NOT NULL UNIQUE, applicant_id UUID NOT NULL, amount NUMERIC(19,2) NOT NULL, award_date DATE NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'OFFERED', notes TEXT, created_by_user_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 version BIGINT NOT NULL DEFAULT 0, CONSTRAINT uk_scholarship_award_id_org UNIQUE(id,organization_id),
 CONSTRAINT fk_scholarship_award_program_org FOREIGN KEY(scholarship_program_id,organization_id) REFERENCES scholarship_program(id,organization_id),
 CONSTRAINT fk_scholarship_award_application_org FOREIGN KEY(application_id,organization_id) REFERENCES scholarship_application(id,organization_id),
 CONSTRAINT fk_scholarship_award_application_identity FOREIGN KEY(application_id,organization_id,scholarship_program_id,applicant_id) REFERENCES scholarship_application(id,organization_id,scholarship_program_id,applicant_id),
 CONSTRAINT fk_scholarship_award_applicant_org FOREIGN KEY(applicant_id,organization_id) REFERENCES scholarship_applicant(id,organization_id),
 CONSTRAINT fk_scholarship_award_creator_membership FOREIGN KEY(organization_id,created_by_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT ck_scholarship_award_amount CHECK(amount>0),
 CONSTRAINT ck_scholarship_award_status CHECK(status IN ('OFFERED','ACCEPTED','DECLINED','DISBURSED','CANCELLED'))
);
CREATE INDEX idx_scholarship_award_org_status ON scholarship_award(organization_id,status);
CREATE INDEX idx_scholarship_award_program ON scholarship_award(scholarship_program_id);
