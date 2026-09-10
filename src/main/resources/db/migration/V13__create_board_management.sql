ALTER TABLE organization_membership DROP CONSTRAINT ck_membership_role;
ALTER TABLE organization_membership ADD CONSTRAINT ck_membership_role CHECK (role IN ('ORG_ADMIN','PROGRAM_MANAGER','GRANT_MANAGER','DONATION_MANAGER','VOLUNTEER_COORDINATOR','EVENT_COORDINATOR','CASE_MANAGER','CASE_WORKER','EQUIPMENT_MANAGER','FACILITY_MANAGER','SCHOLARSHIP_MANAGER','SCHOLARSHIP_REVIEWER','FOOD_PANTRY_MANAGER','BOARD_MANAGER','BOARD_MEMBER','VOLUNTEER','VIEWER'));

CREATE TABLE board_member (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), user_id UUID,
 first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100) NOT NULL, email VARCHAR(320), normalized_email VARCHAR(320), phone VARCHAR(50), title VARCHAR(150),
 active BOOLEAN NOT NULL DEFAULT TRUE, joined_date DATE NOT NULL, notes TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_board_member_id_org UNIQUE(id,organization_id), CONSTRAINT uk_board_member_user UNIQUE(organization_id,user_id),
 CONSTRAINT fk_board_member_user_org FOREIGN KEY(organization_id,user_id) REFERENCES organization_membership(organization_id,user_id)
);
CREATE INDEX idx_board_member_org_active ON board_member(organization_id,active);
CREATE INDEX idx_board_member_org_name ON board_member(organization_id,last_name,first_name);

CREATE TABLE board_term (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), board_member_id UUID NOT NULL,
 term_start DATE NOT NULL, term_end DATE NOT NULL, status VARCHAR(20) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_board_term_id_org UNIQUE(id,organization_id), CONSTRAINT fk_board_term_member_org FOREIGN KEY(board_member_id,organization_id) REFERENCES board_member(id,organization_id),
 CONSTRAINT ck_board_term_dates CHECK(term_end>term_start), CONSTRAINT ck_board_term_status CHECK(status IN ('ACTIVE','COMPLETED','RESIGNED','REMOVED'))
);
CREATE INDEX idx_board_term_member_status ON board_term(board_member_id,status);

CREATE TABLE board_officer_assignment (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), board_member_id UUID NOT NULL,
 officer_role VARCHAR(30) NOT NULL, other_title VARCHAR(150), start_date DATE NOT NULL, end_date DATE, active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_board_officer_id_org UNIQUE(id,organization_id), CONSTRAINT fk_board_officer_member_org FOREIGN KEY(board_member_id,organization_id) REFERENCES board_member(id,organization_id),
 CONSTRAINT ck_board_officer_role CHECK(officer_role IN ('CHAIR','VICE_CHAIR','SECRETARY','TREASURER','MEMBER_AT_LARGE','OTHER')),
 CONSTRAINT ck_board_officer_dates CHECK(end_date IS NULL OR end_date>=start_date), CONSTRAINT ck_board_officer_other CHECK(officer_role<>'OTHER' OR other_title IS NOT NULL)
);
CREATE INDEX idx_board_officer_member_active ON board_officer_assignment(board_member_id,active);

CREATE TABLE board_committee (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), name VARCHAR(200) NOT NULL, description TEXT, active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_board_committee_id_org UNIQUE(id,organization_id), CONSTRAINT uk_board_committee_name UNIQUE(organization_id,name)
);
CREATE INDEX idx_board_committee_org_active ON board_committee(organization_id,active);

CREATE TABLE board_committee_membership (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), committee_id UUID NOT NULL, board_member_id UUID NOT NULL,
 committee_role VARCHAR(100), start_date DATE NOT NULL, end_date DATE, active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_board_committee_membership_id_org UNIQUE(id,organization_id),
 CONSTRAINT fk_board_committee_membership_committee_org FOREIGN KEY(committee_id,organization_id) REFERENCES board_committee(id,organization_id),
 CONSTRAINT fk_board_committee_membership_member_org FOREIGN KEY(board_member_id,organization_id) REFERENCES board_member(id,organization_id),
 CONSTRAINT ck_board_committee_membership_dates CHECK(end_date IS NULL OR end_date>=start_date)
);
CREATE UNIQUE INDEX uk_board_committee_active_member ON board_committee_membership(committee_id,board_member_id) WHERE active;
CREATE INDEX idx_board_committee_membership_member ON board_committee_membership(board_member_id,active);

CREATE TABLE board_meeting (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), title VARCHAR(200) NOT NULL, meeting_type VARCHAR(20) NOT NULL,
 start_date_time TIMESTAMPTZ NOT NULL, end_date_time TIMESTAMPTZ NOT NULL, location VARCHAR(300), virtual_meeting_url VARCHAR(500),
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', quorum_required INTEGER NOT NULL, created_by_user_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_board_meeting_id_org UNIQUE(id,organization_id), CONSTRAINT fk_board_meeting_creator_org FOREIGN KEY(organization_id,created_by_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT ck_board_meeting_type CHECK(meeting_type IN ('REGULAR','SPECIAL','ANNUAL','EMERGENCY','COMMITTEE','OTHER')),
 CONSTRAINT ck_board_meeting_status CHECK(status IN ('DRAFT','PUBLISHED','IN_PROGRESS','COMPLETED','CANCELLED')),
 CONSTRAINT ck_board_meeting_dates CHECK(end_date_time>start_date_time), CONSTRAINT ck_board_meeting_quorum CHECK(quorum_required>0)
);
CREATE INDEX idx_board_meeting_org_start ON board_meeting(organization_id,start_date_time);
CREATE INDEX idx_board_meeting_org_status ON board_meeting(organization_id,status);

CREATE TABLE board_meeting_attendance (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), meeting_id UUID NOT NULL, board_member_id UUID NOT NULL,
 attendance_status VARCHAR(20) NOT NULL, checked_in_at TIMESTAMPTZ, notes TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_board_attendance_id_org UNIQUE(id,organization_id), CONSTRAINT uk_board_attendance_meeting_member UNIQUE(meeting_id,board_member_id),
 CONSTRAINT fk_board_attendance_meeting_org FOREIGN KEY(meeting_id,organization_id) REFERENCES board_meeting(id,organization_id),
 CONSTRAINT fk_board_attendance_member_org FOREIGN KEY(board_member_id,organization_id) REFERENCES board_member(id,organization_id),
 CONSTRAINT ck_board_attendance_status CHECK(attendance_status IN ('PRESENT','ABSENT','EXCUSED','REMOTE'))
);
CREATE INDEX idx_board_attendance_meeting_status ON board_meeting_attendance(meeting_id,attendance_status);

CREATE TABLE board_agenda_item (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), meeting_id UUID NOT NULL, sequence_number INTEGER NOT NULL,
 title VARCHAR(200) NOT NULL, description TEXT, item_type VARCHAR(20) NOT NULL, presenter VARCHAR(200), estimated_minutes INTEGER,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_board_agenda_id_org UNIQUE(id,organization_id), CONSTRAINT uk_board_agenda_sequence UNIQUE(meeting_id,sequence_number),
 CONSTRAINT fk_board_agenda_meeting_org FOREIGN KEY(meeting_id,organization_id) REFERENCES board_meeting(id,organization_id),
 CONSTRAINT ck_board_agenda_sequence CHECK(sequence_number>0), CONSTRAINT ck_board_agenda_minutes CHECK(estimated_minutes IS NULL OR estimated_minutes>0),
 CONSTRAINT ck_board_agenda_type CHECK(item_type IN ('REPORT','DISCUSSION','ACTION','MOTION','ANNOUNCEMENT','OTHER')),
 CONSTRAINT ck_board_agenda_status CHECK(status IN ('PENDING','IN_PROGRESS','COMPLETED','SKIPPED'))
);
CREATE INDEX idx_board_agenda_meeting_sequence ON board_agenda_item(meeting_id,sequence_number);

CREATE TABLE board_motion (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), meeting_id UUID NOT NULL, agenda_item_id UUID,
 motion_text TEXT NOT NULL, moved_by_board_member_id UUID NOT NULL, seconded_by_board_member_id UUID,
 status VARCHAR(20) NOT NULL DEFAULT 'PROPOSED', opened_at TIMESTAMPTZ NOT NULL, closed_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_board_motion_id_org UNIQUE(id,organization_id), CONSTRAINT fk_board_motion_meeting_org FOREIGN KEY(meeting_id,organization_id) REFERENCES board_meeting(id,organization_id),
 CONSTRAINT fk_board_motion_agenda_org FOREIGN KEY(agenda_item_id,organization_id) REFERENCES board_agenda_item(id,organization_id),
 CONSTRAINT fk_board_motion_mover_org FOREIGN KEY(moved_by_board_member_id,organization_id) REFERENCES board_member(id,organization_id),
 CONSTRAINT fk_board_motion_seconder_org FOREIGN KEY(seconded_by_board_member_id,organization_id) REFERENCES board_member(id,organization_id),
 CONSTRAINT ck_board_motion_status CHECK(status IN ('PROPOSED','SECONDED','VOTING','PASSED','FAILED','WITHDRAWN','TABLED')),
 CONSTRAINT ck_board_motion_closed CHECK((status IN ('PASSED','FAILED','WITHDRAWN','TABLED') AND closed_at IS NOT NULL) OR (status IN ('PROPOSED','SECONDED','VOTING') AND closed_at IS NULL)),
 CONSTRAINT ck_board_motion_distinct_members CHECK(seconded_by_board_member_id IS NULL OR seconded_by_board_member_id<>moved_by_board_member_id)
);
CREATE INDEX idx_board_motion_meeting_status ON board_motion(meeting_id,status);

CREATE TABLE board_vote (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), motion_id UUID NOT NULL, board_member_id UUID NOT NULL,
 choice VARCHAR(10) NOT NULL, cast_at TIMESTAMPTZ NOT NULL, recorded_by_user_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_board_vote_id_org UNIQUE(id,organization_id), CONSTRAINT uk_board_vote_motion_member UNIQUE(motion_id,board_member_id),
 CONSTRAINT fk_board_vote_motion_org FOREIGN KEY(motion_id,organization_id) REFERENCES board_motion(id,organization_id),
 CONSTRAINT fk_board_vote_member_org FOREIGN KEY(board_member_id,organization_id) REFERENCES board_member(id,organization_id),
 CONSTRAINT fk_board_vote_recorder_org FOREIGN KEY(organization_id,recorded_by_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT ck_board_vote_choice CHECK(choice IN ('YES','NO','ABSTAIN'))
);
CREATE INDEX idx_board_vote_motion_choice ON board_vote(motion_id,choice);

CREATE TABLE board_meeting_minutes (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), meeting_id UUID NOT NULL, draft_content TEXT NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', prepared_by_user_id UUID NOT NULL, approved_by_user_id UUID, submitted_at TIMESTAMPTZ, approved_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_board_minutes_id_org UNIQUE(id,organization_id), CONSTRAINT uk_board_minutes_meeting UNIQUE(meeting_id),
 CONSTRAINT fk_board_minutes_meeting_org FOREIGN KEY(meeting_id,organization_id) REFERENCES board_meeting(id,organization_id),
 CONSTRAINT fk_board_minutes_preparer_org FOREIGN KEY(organization_id,prepared_by_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT fk_board_minutes_approver_org FOREIGN KEY(organization_id,approved_by_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT ck_board_minutes_status CHECK(status IN ('DRAFT','SUBMITTED','APPROVED')),
 CONSTRAINT ck_board_minutes_state CHECK((status='DRAFT' AND submitted_at IS NULL AND approved_at IS NULL AND approved_by_user_id IS NULL) OR (status='SUBMITTED' AND submitted_at IS NOT NULL AND approved_at IS NULL AND approved_by_user_id IS NULL) OR (status='APPROVED' AND submitted_at IS NOT NULL AND approved_at IS NOT NULL AND approved_by_user_id IS NOT NULL))
);

CREATE TABLE board_resolution (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id), meeting_id UUID NOT NULL, motion_id UUID,
 resolution_number VARCHAR(100) NOT NULL, title VARCHAR(200) NOT NULL, resolution_text TEXT NOT NULL, adopted_date DATE NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'ADOPTED', created_by_user_id UUID NOT NULL, rescinded_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_board_resolution_id_org UNIQUE(id,organization_id), CONSTRAINT uk_board_resolution_number UNIQUE(organization_id,resolution_number),
 CONSTRAINT fk_board_resolution_meeting_org FOREIGN KEY(meeting_id,organization_id) REFERENCES board_meeting(id,organization_id),
 CONSTRAINT fk_board_resolution_motion_org FOREIGN KEY(motion_id,organization_id) REFERENCES board_motion(id,organization_id),
 CONSTRAINT fk_board_resolution_creator_org FOREIGN KEY(organization_id,created_by_user_id) REFERENCES organization_membership(organization_id,user_id),
 CONSTRAINT ck_board_resolution_status CHECK(status IN ('ADOPTED','RESCINDED')),
 CONSTRAINT ck_board_resolution_state CHECK((status='ADOPTED' AND rescinded_at IS NULL) OR (status='RESCINDED' AND rescinded_at IS NOT NULL))
);
CREATE INDEX idx_board_resolution_org_adopted ON board_resolution(organization_id,adopted_date);
