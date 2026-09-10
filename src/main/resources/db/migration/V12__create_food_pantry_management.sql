ALTER TABLE organization_membership DROP CONSTRAINT ck_membership_role;
ALTER TABLE organization_membership ADD CONSTRAINT ck_membership_role CHECK (role IN ('ORG_ADMIN','PROGRAM_MANAGER','GRANT_MANAGER','DONATION_MANAGER','VOLUNTEER_COORDINATOR','EVENT_COORDINATOR','CASE_MANAGER','CASE_WORKER','EQUIPMENT_MANAGER','FACILITY_MANAGER','SCHOLARSHIP_MANAGER','SCHOLARSHIP_REVIEWER','FOOD_PANTRY_MANAGER','VOLUNTEER','VIEWER'));

CREATE TABLE food_pantry_location (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), name VARCHAR(200) NOT NULL,
 description TEXT, address_line1 VARCHAR(250), address_line2 VARCHAR(250), city VARCHAR(120), state VARCHAR(120), postal_code VARCHAR(30), country VARCHAR(2),
 active BOOLEAN NOT NULL DEFAULT TRUE, timezone VARCHAR(100) NOT NULL, notes TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_food_pantry_location_id_org UNIQUE(id,organization_id)
);
CREATE INDEX idx_food_pantry_location_org ON food_pantry_location(organization_id,active);

CREATE TABLE pantry_item (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), sku VARCHAR(100), normalized_sku VARCHAR(100), name VARCHAR(200) NOT NULL,
 description TEXT, category VARCHAR(30) NOT NULL, unit_type VARCHAR(20) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 track_expiration BOOLEAN NOT NULL DEFAULT FALSE, reorder_threshold NUMERIC(19,3), notes TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_pantry_item_id_org UNIQUE(id,organization_id), CONSTRAINT uk_pantry_item_sku UNIQUE(organization_id,normalized_sku),
 CONSTRAINT ck_pantry_item_category CHECK(category IN ('CANNED_GOODS','GRAINS','PRODUCE','DAIRY','MEAT','FROZEN','BAKERY','BEVERAGES','BABY_FOOD','PERSONAL_CARE','HOUSEHOLD','OTHER')),
 CONSTRAINT ck_pantry_item_unit CHECK(unit_type IN ('EACH','CAN','BOX','BAG','BOTTLE','POUND','OUNCE','KILOGRAM','LITER','PACKAGE','OTHER')),
 CONSTRAINT ck_pantry_item_threshold CHECK(reorder_threshold IS NULL OR reorder_threshold>=0)
);
CREATE INDEX idx_pantry_item_org_category ON pantry_item(organization_id,category);

CREATE TABLE pantry_inventory_lot (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), pantry_location_id UUID NOT NULL, pantry_item_id UUID NOT NULL,
 lot_number VARCHAR(100), received_date DATE NOT NULL, expiration_date DATE, quantity_received NUMERIC(19,3) NOT NULL, quantity_remaining NUMERIC(19,3) NOT NULL,
 source_type VARCHAR(30), source_reference VARCHAR(200), notes TEXT, created_by_user_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_pantry_inventory_lot_id_org UNIQUE(id,organization_id),
 CONSTRAINT uk_pantry_inventory_lot_identity UNIQUE(id,organization_id,pantry_location_id,pantry_item_id),
 CONSTRAINT fk_pantry_lot_location_org FOREIGN KEY(pantry_location_id,organization_id) REFERENCES food_pantry_location(id,organization_id),
 CONSTRAINT fk_pantry_lot_item_org FOREIGN KEY(pantry_item_id,organization_id) REFERENCES pantry_item(id,organization_id),
 CONSTRAINT fk_pantry_lot_creator_membership FOREIGN KEY(organization_id,created_by_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT ck_pantry_lot_quantities CHECK(quantity_received>0 AND quantity_remaining>=0 AND quantity_remaining<=quantity_received),
 CONSTRAINT ck_pantry_lot_expiration CHECK(expiration_date IS NULL OR expiration_date>=received_date),
 CONSTRAINT ck_pantry_lot_source CHECK(source_type IS NULL OR source_type IN ('DONATION','PURCHASE','FOOD_BANK','GOVERNMENT_PROGRAM','TRANSFER','OTHER'))
);
CREATE INDEX idx_pantry_lot_location_item ON pantry_inventory_lot(pantry_location_id,pantry_item_id);
CREATE INDEX idx_pantry_lot_expiration ON pantry_inventory_lot(expiration_date);
CREATE INDEX idx_pantry_lot_remaining ON pantry_inventory_lot(quantity_remaining);

CREATE TABLE pantry_inventory_transaction (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), pantry_location_id UUID NOT NULL, pantry_item_id UUID NOT NULL,
 inventory_lot_id UUID NOT NULL, transaction_type VARCHAR(30) NOT NULL, quantity NUMERIC(19,3) NOT NULL, occurred_at TIMESTAMPTZ NOT NULL,
 reference_type VARCHAR(50), reference_id UUID, notes TEXT, created_by_user_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_pantry_tx_lot_identity FOREIGN KEY(inventory_lot_id,organization_id,pantry_location_id,pantry_item_id) REFERENCES pantry_inventory_lot(id,organization_id,pantry_location_id,pantry_item_id),
 CONSTRAINT fk_pantry_tx_creator_membership FOREIGN KEY(organization_id,created_by_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT ck_pantry_tx_type CHECK(transaction_type IN ('RECEIPT','DISTRIBUTION','ADJUSTMENT_INCREASE','ADJUSTMENT_DECREASE','EXPIRATION','SPOILAGE','TRANSFER_OUT','TRANSFER_IN')),
 CONSTRAINT ck_pantry_tx_quantity CHECK(quantity>0)
);
CREATE INDEX idx_pantry_tx_lot_occurred ON pantry_inventory_transaction(inventory_lot_id,occurred_at);
CREATE INDEX idx_pantry_tx_org_occurred ON pantry_inventory_transaction(organization_id,occurred_at);

CREATE TABLE pantry_household (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), external_reference_number VARCHAR(100), household_name VARCHAR(200),
 primary_contact_first_name VARCHAR(100), primary_contact_last_name VARCHAR(100), email VARCHAR(320), normalized_email VARCHAR(320), phone VARCHAR(50), address TEXT,
 household_size INTEGER NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE, notes TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_pantry_household_id_org UNIQUE(id,organization_id), CONSTRAINT uk_pantry_household_reference UNIQUE(organization_id,external_reference_number),
 CONSTRAINT ck_pantry_household_size CHECK(household_size>0)
);
CREATE INDEX idx_pantry_household_org_active ON pantry_household(organization_id,active);
CREATE INDEX idx_pantry_household_email ON pantry_household(organization_id,normalized_email);

CREATE TABLE pantry_distribution_visit (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), pantry_location_id UUID NOT NULL, household_id UUID,
 recipient_name VARCHAR(200), visit_date_time TIMESTAMPTZ NOT NULL, household_size_at_visit INTEGER NOT NULL, notes TEXT,
 served_by_user_id UUID NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'OPEN', completed_at TIMESTAMPTZ, cancelled_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_pantry_visit_id_org UNIQUE(id,organization_id),
 CONSTRAINT fk_pantry_visit_location_org FOREIGN KEY(pantry_location_id,organization_id) REFERENCES food_pantry_location(id,organization_id),
 CONSTRAINT fk_pantry_visit_household_org FOREIGN KEY(household_id,organization_id) REFERENCES pantry_household(id,organization_id),
 CONSTRAINT fk_pantry_visit_server_membership FOREIGN KEY(organization_id,served_by_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT ck_pantry_visit_recipient CHECK(household_id IS NOT NULL OR recipient_name IS NOT NULL),
 CONSTRAINT ck_pantry_visit_size CHECK(household_size_at_visit>0),
 CONSTRAINT ck_pantry_visit_status CHECK(status IN ('OPEN','COMPLETED','CANCELLED')),
 CONSTRAINT ck_pantry_visit_completion CHECK((status='OPEN' AND completed_at IS NULL AND cancelled_at IS NULL) OR (status='COMPLETED' AND completed_at IS NOT NULL AND cancelled_at IS NULL) OR (status='CANCELLED' AND cancelled_at IS NOT NULL AND completed_at IS NULL))
);
CREATE INDEX idx_pantry_visit_org_datetime ON pantry_distribution_visit(organization_id,visit_date_time);
CREATE INDEX idx_pantry_visit_household ON pantry_distribution_visit(household_id);
CREATE INDEX idx_pantry_visit_location_status ON pantry_distribution_visit(pantry_location_id,status);

CREATE TABLE pantry_distribution_item (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), distribution_visit_id UUID NOT NULL, pantry_item_id UUID NOT NULL,
 quantity NUMERIC(19,3) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_pantry_distribution_visit_item UNIQUE(distribution_visit_id,pantry_item_id),
 CONSTRAINT fk_pantry_distribution_visit_org FOREIGN KEY(distribution_visit_id,organization_id) REFERENCES pantry_distribution_visit(id,organization_id),
 CONSTRAINT fk_pantry_distribution_item_org FOREIGN KEY(pantry_item_id,organization_id) REFERENCES pantry_item(id,organization_id),
 CONSTRAINT ck_pantry_distribution_quantity CHECK(quantity>0)
);
CREATE INDEX idx_pantry_distribution_item_visit ON pantry_distribution_item(distribution_visit_id);
