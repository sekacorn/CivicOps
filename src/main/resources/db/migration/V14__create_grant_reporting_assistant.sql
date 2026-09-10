CREATE TABLE grant_report_template (
  id UUID PRIMARY KEY, organization_id UUID NOT NULL, grant_id UUID, name VARCHAR(200) NOT NULL,
  description TEXT, active BOOLEAN NOT NULL DEFAULT TRUE, created_by_user_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_grant_report_template_org_id UNIQUE (organization_id,id),
  CONSTRAINT fk_grt_org FOREIGN KEY (organization_id) REFERENCES organization(id),
  CONSTRAINT fk_grt_grant FOREIGN KEY (organization_id,grant_id) REFERENCES grant_record(organization_id,id),
  CONSTRAINT fk_grt_creator FOREIGN KEY (created_by_user_id) REFERENCES app_user(id)
);
CREATE TABLE grant_report_template_section (
  id UUID PRIMARY KEY, organization_id UUID NOT NULL, template_id UUID NOT NULL, section_key VARCHAR(100) NOT NULL,
  title VARCHAR(200) NOT NULL, instructions TEXT, sequence_number INTEGER NOT NULL, section_type VARCHAR(30) NOT NULL,
  required BOOLEAN NOT NULL DEFAULT TRUE, max_length INTEGER,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_grts_org_id UNIQUE (organization_id,id), CONSTRAINT uk_grts_key UNIQUE(template_id,section_key),
  CONSTRAINT uk_grts_sequence UNIQUE(template_id,sequence_number),
  CONSTRAINT fk_grts_template FOREIGN KEY (organization_id,template_id) REFERENCES grant_report_template(organization_id,id),
  CONSTRAINT ck_grts_sequence CHECK(sequence_number > 0), CONSTRAINT ck_grts_max CHECK(max_length IS NULL OR max_length > 0),
  CONSTRAINT ck_grts_type CHECK(section_type IN ('NARRATIVE','METRICS','FINANCIAL','OUTCOMES','CHALLENGES','DEMOGRAPHICS','CUSTOM'))
);
CREATE TABLE grant_report (
  id UUID PRIMARY KEY, organization_id UUID NOT NULL, grant_id UUID NOT NULL, template_id UUID NOT NULL,
  reporting_period_start DATE NOT NULL, reporting_period_end DATE NOT NULL, status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  selected_sources VARCHAR(500) NOT NULL, generated_at TIMESTAMPTZ, finalized_at TIMESTAMPTZ,
  created_by_user_id UUID NOT NULL, finalized_by_user_id UUID,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_grant_report_org_id UNIQUE(organization_id,id),
  CONSTRAINT fk_gr_org FOREIGN KEY(organization_id) REFERENCES organization(id),
  CONSTRAINT fk_gr_grant FOREIGN KEY(organization_id,grant_id) REFERENCES grant_record(organization_id,id),
  CONSTRAINT fk_gr_template FOREIGN KEY(organization_id,template_id) REFERENCES grant_report_template(organization_id,id),
  CONSTRAINT fk_gr_creator FOREIGN KEY(created_by_user_id) REFERENCES app_user(id), CONSTRAINT fk_gr_finalizer FOREIGN KEY(finalized_by_user_id) REFERENCES app_user(id),
  CONSTRAINT ck_gr_period CHECK(reporting_period_end >= reporting_period_start),
  CONSTRAINT ck_gr_status CHECK(status IN ('DRAFT','GENERATED','UNDER_REVIEW','FINALIZED','CANCELLED')),
  CONSTRAINT ck_gr_generated CHECK(status='DRAFT' OR status='CANCELLED' OR generated_at IS NOT NULL),
  CONSTRAINT ck_gr_finalized CHECK((status='FINALIZED' AND finalized_at IS NOT NULL AND finalized_by_user_id IS NOT NULL) OR (status<>'FINALIZED' AND finalized_at IS NULL AND finalized_by_user_id IS NULL))
);
CREATE TABLE grant_report_section (
  id UUID PRIMARY KEY, organization_id UUID NOT NULL, report_id UUID NOT NULL, template_section_id UUID NOT NULL,
  sequence_number INTEGER NOT NULL, title VARCHAR(200) NOT NULL, section_type VARCHAR(30) NOT NULL,
  required BOOLEAN NOT NULL, max_length INTEGER, generated_content TEXT, edited_content TEXT, final_content TEXT,
  evidence_summary TEXT, status VARCHAR(30) NOT NULL DEFAULT 'PENDING', generated_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_grs_org_id UNIQUE(organization_id,id), CONSTRAINT uk_grs_template UNIQUE(report_id,template_section_id),
  CONSTRAINT uk_grs_sequence UNIQUE(report_id,sequence_number),
  CONSTRAINT fk_grs_report FOREIGN KEY(organization_id,report_id) REFERENCES grant_report(organization_id,id),
  CONSTRAINT fk_grs_template FOREIGN KEY(organization_id,template_section_id) REFERENCES grant_report_template_section(organization_id,id),
  CONSTRAINT ck_grs_type CHECK(section_type IN ('NARRATIVE','METRICS','FINANCIAL','OUTCOMES','CHALLENGES','DEMOGRAPHICS','CUSTOM')),
  CONSTRAINT ck_grs_status CHECK(status IN ('PENDING','GENERATED','EDITED','APPROVED'))
);
CREATE TABLE grant_report_evidence_snapshot (
  id UUID PRIMARY KEY, organization_id UUID NOT NULL, report_id UUID NOT NULL, source_module VARCHAR(30) NOT NULL,
  source_type VARCHAR(100) NOT NULL, source_id UUID, metric_key VARCHAR(100) NOT NULL, metric_label VARCHAR(200) NOT NULL,
  value_state VARCHAR(30) NOT NULL, numeric_value NUMERIC(19,3), monetary_value NUMERIC(19,2), text_value TEXT, unit VARCHAR(50),
  period_start DATE NOT NULL, period_end DATE NOT NULL, captured_at TIMESTAMPTZ NOT NULL, source_reference VARCHAR(500) NOT NULL,
  metadata_json TEXT, entered_by_user_id UUID,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_gres_org_id UNIQUE(organization_id,id),
  CONSTRAINT fk_gres_report FOREIGN KEY(organization_id,report_id) REFERENCES grant_report(organization_id,id),
  CONSTRAINT fk_gres_entered_by FOREIGN KEY(entered_by_user_id) REFERENCES app_user(id),
  CONSTRAINT ck_gres_module CHECK(source_module IN ('GRANT','VOLUNTEERS','DONATIONS','EVENTS','CASES','SCHOLARSHIPS','FOOD_PANTRY','MANUAL')),
  CONSTRAINT ck_gres_state CHECK(value_state IN ('VERIFIED','MISSING','NOT_APPLICABLE')),
  CONSTRAINT ck_gres_period CHECK(period_end >= period_start),
  CONSTRAINT ck_gres_value CHECK((value_state='VERIFIED' AND num_nonnulls(numeric_value,monetary_value,text_value)=1) OR (value_state<>'VERIFIED' AND num_nonnulls(numeric_value,monetary_value,text_value)=0)),
  CONSTRAINT ck_gres_manual CHECK((source_module='MANUAL' AND entered_by_user_id IS NOT NULL) OR (source_module<>'MANUAL' AND entered_by_user_id IS NULL))
);
CREATE INDEX idx_grt_org_active ON grant_report_template(organization_id,active);
CREATE INDEX idx_gr_org_status ON grant_report(organization_id,status);
CREATE INDEX idx_gr_grant ON grant_report(grant_id);
CREATE INDEX idx_gr_period ON grant_report(reporting_period_start,reporting_period_end);
CREATE INDEX idx_grs_report_sequence ON grant_report_section(report_id,sequence_number);
CREATE INDEX idx_gres_report ON grant_report_evidence_snapshot(report_id);
CREATE INDEX idx_gres_module ON grant_report_evidence_snapshot(source_module);
CREATE INDEX idx_gres_metric ON grant_report_evidence_snapshot(metric_key);
