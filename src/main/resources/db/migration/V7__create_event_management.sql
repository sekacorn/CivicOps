CREATE TABLE event_record (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    event_type VARCHAR(40) NOT NULL,
    location VARCHAR(300),
    start_date_time TIMESTAMPTZ NOT NULL,
    end_date_time TIMESTAMPTZ NOT NULL,
    capacity INTEGER,
    registration_required BOOLEAN NOT NULL DEFAULT TRUE,
    registration_deadline TIMESTAMPTZ,
    waitlist_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    linked_grant_id UUID,
    linked_campaign_id UUID,
    created_by_user_id UUID NOT NULL REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_event_record_id_org UNIQUE (id, organization_id),
    CONSTRAINT fk_event_grant_org FOREIGN KEY (linked_grant_id, organization_id) REFERENCES grant_record(id, organization_id),
    CONSTRAINT fk_event_campaign_org FOREIGN KEY (linked_campaign_id, organization_id) REFERENCES donation_campaign(id, organization_id),
    CONSTRAINT ck_event_dates CHECK (end_date_time > start_date_time),
    CONSTRAINT ck_event_capacity CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT ck_event_deadline CHECK (registration_deadline IS NULL OR registration_deadline <= start_date_time),
    CONSTRAINT ck_event_status CHECK (status IN ('DRAFT','PUBLISHED','REGISTRATION_OPEN','REGISTRATION_CLOSED','COMPLETED','CANCELLED')),
    CONSTRAINT ck_event_type CHECK (event_type IN ('COMMUNITY_OUTREACH','FUNDRAISER','VOLUNTEER_ACTIVITY','TRAINING','WORKSHOP','FOOD_DISTRIBUTION','BOARD_MEETING','PUBLIC_MEETING','SCHOLARSHIP_EVENT','OTHER'))
);
CREATE INDEX idx_event_org_status ON event_record(organization_id, status);
CREATE INDEX idx_event_org_start ON event_record(organization_id, start_date_time);
CREATE INDEX idx_event_org_type ON event_record(organization_id, event_type);
CREATE INDEX idx_event_grant ON event_record(linked_grant_id) WHERE linked_grant_id IS NOT NULL;
CREATE INDEX idx_event_campaign ON event_record(linked_campaign_id) WHERE linked_campaign_id IS NOT NULL;

CREATE TABLE event_registration (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    event_id UUID NOT NULL,
    registered_user_id UUID REFERENCES app_user(id),
    attendee_name VARCHAR(200) NOT NULL,
    attendee_email VARCHAR(320),
    attendee_phone VARCHAR(50),
    registration_date TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL,
    checked_in_at TIMESTAMPTZ,
    checked_out_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_event_registration_event_org FOREIGN KEY (event_id, organization_id) REFERENCES event_record(id, organization_id),
    CONSTRAINT ck_event_registration_status CHECK (status IN ('REGISTERED','WAITLISTED','CANCELLED','ATTENDED','NO_SHOW')),
    CONSTRAINT ck_event_registration_checkout CHECK (checked_out_at IS NULL OR checked_in_at IS NOT NULL),
    CONSTRAINT ck_event_registration_attendee CHECK (registered_user_id IS NOT NULL OR NULLIF(BTRIM(attendee_email),'') IS NOT NULL)
);
CREATE INDEX idx_event_registration_event_status ON event_registration(event_id, status);
CREATE INDEX idx_event_registration_org ON event_registration(organization_id);
CREATE INDEX idx_event_registration_user ON event_registration(registered_user_id) WHERE registered_user_id IS NOT NULL;
CREATE INDEX idx_event_registration_email ON event_registration(event_id, lower(attendee_email)) WHERE attendee_email IS NOT NULL;

ALTER TABLE volunteer_opportunity ADD COLUMN event_id UUID;
ALTER TABLE volunteer_opportunity ADD CONSTRAINT fk_volunteer_opportunity_event_org FOREIGN KEY (event_id, organization_id) REFERENCES event_record(id, organization_id);
CREATE INDEX idx_volunteer_opportunity_event ON volunteer_opportunity(event_id) WHERE event_id IS NOT NULL;
