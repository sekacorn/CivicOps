CREATE TABLE donor (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    donor_type VARCHAR(30) NOT NULL,
    first_name VARCHAR(100), last_name VARCHAR(100), organization_name VARCHAR(200),
    email VARCHAR(320), phone VARCHAR(50), address_line1 VARCHAR(250), address_line2 VARCHAR(250),
    city VARCHAR(120), state VARCHAR(120), postal_code VARCHAR(30), country VARCHAR(2),
    anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    communication_opt_out BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_donor_id_org UNIQUE (id, organization_id),
    CONSTRAINT ck_donor_type CHECK (donor_type IN ('INDIVIDUAL','ORGANIZATION','FOUNDATION','GOVERNMENT','OTHER')),
    CONSTRAINT ck_donor_identity CHECK (
        anonymous OR
        (donor_type = 'INDIVIDUAL' AND NULLIF(BTRIM(first_name),'') IS NOT NULL AND NULLIF(BTRIM(last_name),'') IS NOT NULL) OR
        (donor_type <> 'INDIVIDUAL' AND NULLIF(BTRIM(organization_name),'') IS NOT NULL)
    )
);
CREATE INDEX idx_donor_org_type ON donor(organization_id, donor_type);
CREATE INDEX idx_donor_org_email_normalized ON donor(organization_id, LOWER(email)) WHERE email IS NOT NULL;

CREATE TABLE donation_campaign (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    goal_amount NUMERIC(19,2) NOT NULL,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL,
    created_by_user_id UUID NOT NULL REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_donation_campaign_id_org UNIQUE (id, organization_id),
    CONSTRAINT ck_campaign_goal CHECK (goal_amount >= 0),
    CONSTRAINT ck_campaign_dates CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_campaign_status CHECK (status IN ('DRAFT','ACTIVE','CLOSED','CANCELLED'))
);
CREATE INDEX idx_campaign_org_status ON donation_campaign(organization_id, status);
CREATE INDEX idx_campaign_org_start ON donation_campaign(organization_id, start_date);

CREATE TABLE donation (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    donor_id UUID,
    campaign_id UUID,
    anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    amount NUMERIC(19,2) NOT NULL,
    donation_date DATE NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    in_kind_description VARCHAR(500),
    restricted BOOLEAN NOT NULL DEFAULT FALSE,
    restriction_description TEXT,
    designation VARCHAR(200),
    reference_number VARCHAR(100),
    receipt_number VARCHAR(100),
    acknowledgement_status VARCHAR(20) NOT NULL,
    notes TEXT,
    status VARCHAR(20) NOT NULL,
    reversed_at TIMESTAMPTZ,
    reversed_by_user_id UUID REFERENCES app_user(id),
    reversal_reason VARCHAR(500),
    created_by_user_id UUID NOT NULL REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_donation_donor_org FOREIGN KEY (donor_id, organization_id) REFERENCES donor(id, organization_id),
    CONSTRAINT fk_donation_campaign_org FOREIGN KEY (campaign_id, organization_id) REFERENCES donation_campaign(id, organization_id),
    CONSTRAINT ck_donation_amount CHECK (amount > 0),
    CONSTRAINT ck_donation_payment CHECK (payment_method IN ('CASH','CHECK','CARD','ACH','WIRE','STOCK','IN_KIND','OTHER')),
    CONSTRAINT ck_donation_acknowledgement CHECK (acknowledgement_status IN ('NOT_REQUIRED','PENDING','SENT')),
    CONSTRAINT ck_donation_status CHECK (status IN ('RECORDED','REVERSED')),
    CONSTRAINT ck_donation_anonymous CHECK ((anonymous AND donor_id IS NULL) OR (NOT anonymous AND donor_id IS NOT NULL)),
    CONSTRAINT ck_donation_in_kind CHECK (payment_method <> 'IN_KIND' OR NULLIF(BTRIM(in_kind_description),'') IS NOT NULL),
    CONSTRAINT ck_donation_restriction CHECK (NOT restricted OR NULLIF(BTRIM(restriction_description),'') IS NOT NULL OR NULLIF(BTRIM(designation),'') IS NOT NULL),
    CONSTRAINT ck_donation_reversal CHECK (
        (status = 'RECORDED' AND reversed_at IS NULL AND reversed_by_user_id IS NULL AND reversal_reason IS NULL) OR
        (status = 'REVERSED' AND reversed_at IS NOT NULL AND reversed_by_user_id IS NOT NULL AND NULLIF(BTRIM(reversal_reason),'') IS NOT NULL)
    )
);
CREATE UNIQUE INDEX uk_donation_org_reference ON donation(organization_id, reference_number) WHERE reference_number IS NOT NULL;
CREATE UNIQUE INDEX uk_donation_org_receipt ON donation(organization_id, receipt_number) WHERE receipt_number IS NOT NULL;
CREATE INDEX idx_donation_org_date ON donation(organization_id, donation_date);
CREATE INDEX idx_donation_donor_date ON donation(donor_id, donation_date) WHERE donor_id IS NOT NULL;
CREATE INDEX idx_donation_campaign_date ON donation(campaign_id, donation_date) WHERE campaign_id IS NOT NULL;
CREATE INDEX idx_donation_org_payment ON donation(organization_id, payment_method);
CREATE INDEX idx_donation_org_restricted ON donation(organization_id, restricted);
