ALTER TABLE organization_membership DROP CONSTRAINT ck_membership_role;
ALTER TABLE organization_membership ADD CONSTRAINT ck_membership_role CHECK (role IN ('ORG_ADMIN','PROGRAM_MANAGER','GRANT_MANAGER','DONATION_MANAGER','VOLUNTEER_COORDINATOR','EVENT_COORDINATOR','CASE_MANAGER','CASE_WORKER','EQUIPMENT_MANAGER','VOLUNTEER','VIEWER'));

CREATE TABLE equipment_category (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization(id),
  name VARCHAR(120) NOT NULL,
  normalized_name VARCHAR(120) NOT NULL,
  description TEXT,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_equipment_category_id_org UNIQUE (id, organization_id),
  CONSTRAINT uk_equipment_category_org_name UNIQUE (organization_id, normalized_name)
);

CREATE TABLE equipment_asset (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization(id),
  asset_tag VARCHAR(100) NOT NULL,
  name VARCHAR(200) NOT NULL,
  description TEXT,
  category_id UUID,
  manufacturer VARCHAR(150),
  model VARCHAR(150),
  serial_number VARCHAR(150),
  purchase_date DATE,
  purchase_value NUMERIC(19,2),
  condition VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
  location VARCHAR(250),
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_equipment_asset_id_org UNIQUE (id, organization_id),
  CONSTRAINT uk_equipment_asset_org_tag UNIQUE (organization_id, asset_tag),
  CONSTRAINT fk_equipment_asset_category_org FOREIGN KEY (category_id, organization_id) REFERENCES equipment_category(id, organization_id),
  CONSTRAINT ck_equipment_asset_condition CHECK (condition IN ('EXCELLENT','GOOD','FAIR','DAMAGED','UNUSABLE')),
  CONSTRAINT ck_equipment_asset_status CHECK (status IN ('AVAILABLE','CHECKED_OUT','MAINTENANCE','LOST','RETIRED')),
  CONSTRAINT ck_equipment_asset_purchase_value CHECK (purchase_value IS NULL OR purchase_value >= 0)
);
CREATE INDEX idx_equipment_asset_org_status ON equipment_asset(organization_id, status);
CREATE INDEX idx_equipment_asset_org_category ON equipment_asset(organization_id, category_id);

CREATE TABLE equipment_checkout (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization(id),
  equipment_asset_id UUID NOT NULL,
  borrower_user_id UUID,
  borrower_name VARCHAR(200),
  borrower_email VARCHAR(320),
  checked_out_at TIMESTAMPTZ NOT NULL,
  due_at TIMESTAMPTZ NOT NULL,
  checked_in_at TIMESTAMPTZ,
  checkout_condition VARCHAR(20) NOT NULL,
  return_condition VARCHAR(20),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  notes TEXT,
  issued_by_user_id UUID NOT NULL,
  received_by_user_id UUID,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_equipment_checkout_id_org UNIQUE (id, organization_id),
  CONSTRAINT fk_equipment_checkout_asset_org FOREIGN KEY (equipment_asset_id, organization_id) REFERENCES equipment_asset(id, organization_id),
  CONSTRAINT fk_equipment_checkout_borrower_membership FOREIGN KEY (organization_id, borrower_user_id) REFERENCES organization_membership(organization_id, user_id),
  CONSTRAINT fk_equipment_checkout_issuer_membership FOREIGN KEY (organization_id, issued_by_user_id) REFERENCES organization_membership(organization_id, user_id),
  CONSTRAINT fk_equipment_checkout_receiver_membership FOREIGN KEY (organization_id, received_by_user_id) REFERENCES organization_membership(organization_id, user_id),
  CONSTRAINT ck_equipment_checkout_borrower CHECK (borrower_user_id IS NOT NULL OR borrower_name IS NOT NULL),
  CONSTRAINT ck_equipment_checkout_status CHECK (status IN ('ACTIVE','RETURNED','LOST','CANCELLED')),
  CONSTRAINT ck_equipment_checkout_condition CHECK (checkout_condition IN ('EXCELLENT','GOOD','FAIR','DAMAGED','UNUSABLE')),
  CONSTRAINT ck_equipment_return_condition CHECK (return_condition IS NULL OR return_condition IN ('EXCELLENT','GOOD','FAIR','DAMAGED','UNUSABLE')),
  CONSTRAINT ck_equipment_checkout_due CHECK (due_at > checked_out_at),
  CONSTRAINT ck_equipment_checkout_return_time CHECK (checked_in_at IS NULL OR checked_in_at >= checked_out_at),
  CONSTRAINT ck_equipment_checkout_return_state CHECK ((status = 'RETURNED' AND checked_in_at IS NOT NULL AND return_condition IS NOT NULL) OR (status <> 'RETURNED'))
);
CREATE UNIQUE INDEX uk_equipment_checkout_one_active ON equipment_checkout(equipment_asset_id) WHERE status = 'ACTIVE' AND checked_in_at IS NULL;
CREATE INDEX idx_equipment_checkout_org_due ON equipment_checkout(organization_id, due_at);
CREATE INDEX idx_equipment_checkout_asset_history ON equipment_checkout(equipment_asset_id, checked_out_at DESC);

CREATE TABLE equipment_maintenance (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization(id),
  equipment_asset_id UUID NOT NULL,
  maintenance_type VARCHAR(30) NOT NULL,
  description TEXT NOT NULL,
  started_at TIMESTAMPTZ NOT NULL,
  completed_at TIMESTAMPTZ,
  cost NUMERIC(19,2),
  vendor VARCHAR(200),
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_by_user_id UUID NOT NULL,
  completed_by_user_id UUID,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_equipment_maintenance_id_org UNIQUE (id, organization_id),
  CONSTRAINT fk_equipment_maintenance_asset_org FOREIGN KEY (equipment_asset_id, organization_id) REFERENCES equipment_asset(id, organization_id),
  CONSTRAINT fk_equipment_maintenance_creator_membership FOREIGN KEY (organization_id, created_by_user_id) REFERENCES organization_membership(organization_id, user_id),
  CONSTRAINT fk_equipment_maintenance_completer_membership FOREIGN KEY (organization_id, completed_by_user_id) REFERENCES organization_membership(organization_id, user_id),
  CONSTRAINT ck_equipment_maintenance_type CHECK (maintenance_type IN ('INSPECTION','REPAIR','CLEANING','CALIBRATION','UPGRADE','OTHER')),
  CONSTRAINT ck_equipment_maintenance_status CHECK (status IN ('OPEN','IN_PROGRESS','COMPLETED','CANCELLED')),
  CONSTRAINT ck_equipment_maintenance_cost CHECK (cost IS NULL OR cost >= 0),
  CONSTRAINT ck_equipment_maintenance_time CHECK (completed_at IS NULL OR completed_at >= started_at),
  CONSTRAINT ck_equipment_maintenance_complete_state CHECK ((status = 'COMPLETED' AND completed_at IS NOT NULL AND completed_by_user_id IS NOT NULL) OR status <> 'COMPLETED')
);
CREATE INDEX idx_equipment_maintenance_org_status ON equipment_maintenance(organization_id, status);
CREATE INDEX idx_equipment_maintenance_asset_history ON equipment_maintenance(equipment_asset_id, started_at DESC);
