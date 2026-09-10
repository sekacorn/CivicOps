CREATE TABLE volunteer (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    user_id UUID REFERENCES app_user(id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(320) NOT NULL,
    phone VARCHAR(50), address_line1 VARCHAR(250), address_line2 VARCHAR(250),
    city VARCHAR(120), state VARCHAR(120), postal_code VARCHAR(30), country VARCHAR(2),
    emergency_contact_name VARCHAR(200), emergency_contact_phone VARCHAR(50), notes TEXT,
    status VARCHAR(30) NOT NULL, start_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_volunteer_org_email UNIQUE (organization_id, email),
    CONSTRAINT uk_volunteer_org_user UNIQUE (organization_id, user_id),
    CONSTRAINT uk_volunteer_id_org UNIQUE (id, organization_id),
    CONSTRAINT ck_volunteer_status CHECK (status IN ('APPLICANT','ACTIVE','INACTIVE','SUSPENDED'))
);
CREATE INDEX idx_volunteer_org_status ON volunteer(organization_id, status);
CREATE UNIQUE INDEX uk_volunteer_org_email_normalized ON volunteer(organization_id, LOWER(email));

CREATE TABLE volunteer_skill (
    volunteer_id UUID NOT NULL REFERENCES volunteer(id) ON DELETE CASCADE,
    skill VARCHAR(100) NOT NULL,
    PRIMARY KEY (volunteer_id, skill)
);

CREATE TABLE volunteer_opportunity (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    title VARCHAR(200) NOT NULL, description TEXT,
    location VARCHAR(300), start_at TIMESTAMPTZ NOT NULL, end_at TIMESTAMPTZ NOT NULL,
    minimum_volunteers INTEGER, maximum_volunteers INTEGER, status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_opportunity_id_org UNIQUE (id, organization_id),
    CONSTRAINT ck_opportunity_dates CHECK (end_at > start_at),
    CONSTRAINT ck_opportunity_capacity CHECK (
        (minimum_volunteers IS NULL OR minimum_volunteers >= 0) AND
        (maximum_volunteers IS NULL OR maximum_volunteers > 0) AND
        (minimum_volunteers IS NULL OR maximum_volunteers IS NULL OR minimum_volunteers <= maximum_volunteers)
    ),
    CONSTRAINT ck_opportunity_status CHECK (status IN ('DRAFT','OPEN','FULL','CLOSED','CANCELLED','COMPLETED'))
);
CREATE INDEX idx_opportunity_org_status_start ON volunteer_opportunity(organization_id, status, start_at);

CREATE TABLE volunteer_shift (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    opportunity_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL, start_at TIMESTAMPTZ NOT NULL, end_at TIMESTAMPTZ NOT NULL,
    capacity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_shift_opportunity_org FOREIGN KEY (opportunity_id, organization_id)
        REFERENCES volunteer_opportunity(id, organization_id),
    CONSTRAINT uk_shift_id_org UNIQUE (id, organization_id),
    CONSTRAINT ck_shift_dates CHECK (end_at > start_at),
    CONSTRAINT ck_shift_capacity CHECK (capacity > 0)
);
CREATE INDEX idx_shift_org_start ON volunteer_shift(organization_id, start_at);

CREATE TABLE volunteer_assignment (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    volunteer_id UUID NOT NULL, shift_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL, checked_in_at TIMESTAMPTZ, checked_out_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_assignment_volunteer_org FOREIGN KEY (volunteer_id, organization_id) REFERENCES volunteer(id, organization_id),
    CONSTRAINT fk_assignment_shift_org FOREIGN KEY (shift_id, organization_id) REFERENCES volunteer_shift(id, organization_id),
    CONSTRAINT uk_assignment_id_org UNIQUE (id, organization_id),
    CONSTRAINT uk_assignment_id_volunteer_org UNIQUE (id, volunteer_id, organization_id),
    CONSTRAINT ck_assignment_status CHECK (status IN ('REGISTERED','CONFIRMED','CANCELLED','ATTENDED','NO_SHOW')),
    CONSTRAINT ck_assignment_checkout CHECK (checked_out_at IS NULL OR (checked_in_at IS NOT NULL AND checked_out_at >= checked_in_at))
);
CREATE UNIQUE INDEX uk_active_volunteer_shift ON volunteer_assignment(volunteer_id, shift_id) WHERE status <> 'CANCELLED';
CREATE INDEX idx_assignment_shift_status ON volunteer_assignment(shift_id, status);
CREATE INDEX idx_assignment_volunteer_status ON volunteer_assignment(volunteer_id, status);

CREATE TABLE volunteer_hour_entry (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    volunteer_id UUID NOT NULL, assignment_id UUID,
    service_date DATE NOT NULL, hours NUMERIC(5,2) NOT NULL, description VARCHAR(500),
    status VARCHAR(30) NOT NULL, reviewed_by_user_id UUID REFERENCES app_user(id), reviewed_at TIMESTAMPTZ,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_hours_volunteer_org FOREIGN KEY (volunteer_id, organization_id) REFERENCES volunteer(id, organization_id),
    CONSTRAINT fk_hours_assignment_identity FOREIGN KEY (assignment_id, volunteer_id, organization_id)
        REFERENCES volunteer_assignment(id, volunteer_id, organization_id),
    CONSTRAINT ck_hours_amount CHECK (hours > 0 AND hours <= 24),
    CONSTRAINT ck_hours_status CHECK (status IN ('SUBMITTED','APPROVED','REJECTED'))
);
CREATE INDEX idx_hours_org_status_date ON volunteer_hour_entry(organization_id, status, service_date);
CREATE INDEX idx_hours_volunteer_date ON volunteer_hour_entry(volunteer_id, service_date);
