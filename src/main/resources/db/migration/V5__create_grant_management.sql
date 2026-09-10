-- PostgreSQL GRANT is a SQL command, so the aggregate table uses the unambiguous grant_record name.
CREATE TABLE grant_record (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    grant_name VARCHAR(200) NOT NULL,
    grantor_name VARCHAR(200) NOT NULL,
    grant_number VARCHAR(100),
    description TEXT,
    award_amount NUMERIC(19,2) NOT NULL,
    application_deadline DATE,
    submitted_date DATE,
    award_date DATE,
    start_date DATE,
    end_date DATE,
    reporting_deadline DATE,
    status VARCHAR(40) NOT NULL,
    restricted BOOLEAN NOT NULL DEFAULT FALSE,
    restriction_description TEXT,
    primary_contact_name VARCHAR(200),
    primary_contact_email VARCHAR(320),
    notes TEXT,
    created_by_user_id UUID NOT NULL REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_grant_record_id_org UNIQUE (id, organization_id),
    CONSTRAINT ck_grant_award_nonnegative CHECK (award_amount >= 0),
    CONSTRAINT ck_grant_dates CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_grant_reporting_date CHECK (reporting_deadline IS NULL OR end_date IS NULL OR reporting_deadline >= end_date),
    CONSTRAINT ck_grant_restriction CHECK (NOT restricted OR NULLIF(BTRIM(restriction_description), '') IS NOT NULL),
    CONSTRAINT ck_grant_status CHECK (status IN ('PROSPECT','APPLICATION_IN_PROGRESS','SUBMITTED','AWARDED','ACTIVE','CLOSED','REJECTED','WITHDRAWN')),
    CONSTRAINT ck_grant_submitted_lifecycle CHECK (
        status NOT IN ('SUBMITTED','AWARDED','ACTIVE','CLOSED','REJECTED') OR submitted_date IS NOT NULL
    ),
    CONSTRAINT ck_grant_awarded_lifecycle CHECK (
        status NOT IN ('AWARDED','ACTIVE','CLOSED') OR award_date IS NOT NULL
    )
);

CREATE INDEX idx_grant_record_org_status ON grant_record(organization_id, status);
CREATE INDEX idx_grant_record_org_reporting_deadline ON grant_record(organization_id, reporting_deadline);
CREATE INDEX idx_grant_record_org_start_date ON grant_record(organization_id, start_date);

CREATE TABLE grant_expense (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    grant_id UUID NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    expense_date DATE NOT NULL,
    category VARCHAR(40) NOT NULL,
    description VARCHAR(500) NOT NULL,
    vendor VARCHAR(200),
    reference_number VARCHAR(100),
    notes TEXT,
    created_by_user_id UUID NOT NULL REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_grant_expense_grant_org FOREIGN KEY (grant_id, organization_id)
        REFERENCES grant_record(id, organization_id),
    CONSTRAINT ck_grant_expense_amount CHECK (amount > 0),
    CONSTRAINT ck_grant_expense_category CHECK (category IN (
        'PERSONNEL','SUPPLIES','EQUIPMENT','TRAVEL','TRANSPORTATION','PROGRAM_SERVICES',
        'FACILITIES','CONTRACTORS','ADMINISTRATIVE','OTHER'
    ))
);

CREATE INDEX idx_grant_expense_grant_date ON grant_expense(grant_id, expense_date);
CREATE INDEX idx_grant_expense_org_date ON grant_expense(organization_id, expense_date);
CREATE INDEX idx_grant_expense_grant_category ON grant_expense(grant_id, category);
