ALTER TABLE organization_membership DROP CONSTRAINT ck_membership_role;
ALTER TABLE organization_membership ADD CONSTRAINT ck_membership_role CHECK (role IN ('ORG_ADMIN','PROGRAM_MANAGER','GRANT_MANAGER','DONATION_MANAGER','VOLUNTEER_COORDINATOR','EVENT_COORDINATOR','CASE_MANAGER','CASE_WORKER','EQUIPMENT_MANAGER','FACILITY_MANAGER','VOLUNTEER','VIEWER'));

CREATE TABLE facility (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), name VARCHAR(200) NOT NULL,
 description TEXT, facility_type VARCHAR(30) NOT NULL, address_line1 VARCHAR(250), address_line2 VARCHAR(250),
 city VARCHAR(120), state VARCHAR(120), postal_code VARCHAR(30), country VARCHAR(2), active BOOLEAN NOT NULL DEFAULT TRUE,
 timezone VARCHAR(80) NOT NULL, notes TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_facility_id_org UNIQUE(id,organization_id),
 CONSTRAINT ck_facility_type CHECK(facility_type IN ('COMMUNITY_CENTER','OFFICE','CHURCH','SCHOOL','WAREHOUSE','OUTDOOR_SITE','OTHER'))
);
CREATE INDEX idx_facility_org_active ON facility(organization_id,active);

CREATE TABLE facility_space (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), facility_id UUID NOT NULL,
 name VARCHAR(200) NOT NULL, normalized_name VARCHAR(200) NOT NULL, description TEXT, capacity INTEGER,
 active BOOLEAN NOT NULL DEFAULT TRUE, reservable BOOLEAN NOT NULL DEFAULT TRUE, location_details VARCHAR(500),
 accessibility_notes TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_facility_space_id_org UNIQUE(id,organization_id),
 CONSTRAINT uk_facility_space_name UNIQUE(facility_id,normalized_name),
 CONSTRAINT fk_facility_space_facility_org FOREIGN KEY(facility_id,organization_id) REFERENCES facility(id,organization_id),
 CONSTRAINT ck_facility_space_capacity CHECK(capacity IS NULL OR capacity > 0)
);
CREATE INDEX idx_facility_space_org_facility ON facility_space(organization_id,facility_id);

CREATE TABLE facility_operating_hours (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), facility_id UUID NOT NULL,
 day_of_week VARCHAR(10) NOT NULL, open_time TIME, close_time TIME, closed BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 version BIGINT NOT NULL DEFAULT 0, CONSTRAINT uk_facility_hours_day UNIQUE(facility_id,day_of_week),
 CONSTRAINT fk_facility_hours_facility_org FOREIGN KEY(facility_id,organization_id) REFERENCES facility(id,organization_id),
 CONSTRAINT ck_facility_hours_day CHECK(day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
 CONSTRAINT ck_facility_hours_time CHECK((closed AND open_time IS NULL AND close_time IS NULL) OR (NOT closed AND open_time IS NOT NULL AND close_time IS NOT NULL AND close_time > open_time))
);

CREATE TABLE facility_blackout (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), facility_id UUID NOT NULL,
 facility_space_id UUID, start_date_time TIMESTAMPTZ NOT NULL, end_date_time TIMESTAMPTZ NOT NULL,
 reason VARCHAR(1000) NOT NULL, created_by_user_id UUID NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 cancelled_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_facility_blackout_id_org UNIQUE(id,organization_id),
 CONSTRAINT fk_facility_blackout_facility_org FOREIGN KEY(facility_id,organization_id) REFERENCES facility(id,organization_id),
 CONSTRAINT fk_facility_blackout_space_org FOREIGN KEY(facility_space_id,organization_id) REFERENCES facility_space(id,organization_id),
 CONSTRAINT fk_facility_blackout_creator_membership FOREIGN KEY(organization_id,created_by_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT ck_facility_blackout_time CHECK(end_date_time > start_date_time),
 CONSTRAINT ck_facility_blackout_cancel CHECK((active AND cancelled_at IS NULL) OR (NOT active AND cancelled_at IS NOT NULL))
);
CREATE INDEX idx_facility_blackout_window ON facility_blackout(organization_id,facility_id,start_date_time,end_date_time) WHERE active;

CREATE TABLE facility_reservation (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), facility_space_id UUID NOT NULL,
 requested_by_user_id UUID, requester_name VARCHAR(200), requester_email VARCHAR(320), event_id UUID,
 title VARCHAR(200) NOT NULL, purpose TEXT, start_date_time TIMESTAMPTZ NOT NULL, end_date_time TIMESTAMPTZ NOT NULL,
 expected_attendance INTEGER, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', requested_at TIMESTAMPTZ NOT NULL,
 approved_by_user_id UUID, approved_at TIMESTAMPTZ, rejected_by_user_id UUID, rejected_at TIMESTAMPTZ,
 rejection_reason VARCHAR(2000), cancelled_at TIMESTAMPTZ, cancellation_reason VARCHAR(2000), notes TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 version BIGINT NOT NULL DEFAULT 0, CONSTRAINT uk_facility_reservation_id_org UNIQUE(id,organization_id),
 CONSTRAINT fk_facility_reservation_space_org FOREIGN KEY(facility_space_id,organization_id) REFERENCES facility_space(id,organization_id),
 CONSTRAINT fk_facility_reservation_requester_membership FOREIGN KEY(organization_id,requested_by_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT fk_facility_reservation_approver_membership FOREIGN KEY(organization_id,approved_by_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT fk_facility_reservation_rejector_membership FOREIGN KEY(organization_id,rejected_by_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT fk_facility_reservation_event_org FOREIGN KEY(event_id,organization_id) REFERENCES event_record(id,organization_id),
 CONSTRAINT ck_facility_reservation_requester CHECK(requested_by_user_id IS NOT NULL OR requester_name IS NOT NULL),
 CONSTRAINT ck_facility_reservation_time CHECK(end_date_time > start_date_time),
 CONSTRAINT ck_facility_reservation_attendance CHECK(expected_attendance IS NULL OR expected_attendance > 0),
 CONSTRAINT ck_facility_reservation_status CHECK(status IN ('PENDING','APPROVED','REJECTED','CANCELLED','COMPLETED')),
 CONSTRAINT ck_facility_reservation_approval CHECK((status IN ('APPROVED','COMPLETED') AND approved_at IS NOT NULL AND approved_by_user_id IS NOT NULL) OR status NOT IN ('APPROVED','COMPLETED')),
 CONSTRAINT ck_facility_reservation_rejection CHECK((status='REJECTED' AND rejected_at IS NOT NULL AND rejected_by_user_id IS NOT NULL AND rejection_reason IS NOT NULL) OR status<>'REJECTED'),
 CONSTRAINT ck_facility_reservation_cancellation CHECK((status='CANCELLED' AND cancelled_at IS NOT NULL) OR status<>'CANCELLED')
);
CREATE INDEX idx_facility_reservation_window ON facility_reservation(organization_id,facility_space_id,start_date_time,end_date_time);
CREATE INDEX idx_facility_reservation_status ON facility_reservation(organization_id,status);
