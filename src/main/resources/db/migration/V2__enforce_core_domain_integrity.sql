CREATE UNIQUE INDEX uk_app_user_email_normalized ON app_user (LOWER(email));

ALTER TABLE organization
    ADD CONSTRAINT ck_organization_type
    CHECK (organization_type IN (
        'NONPROFIT', 'CHARITY', 'FOUNDATION', 'CHURCH',
        'COMMUNITY_ORGANIZATION', 'MUNICIPAL_PROGRAM', 'CIVIC_ORGANIZATION', 'OTHER'
    ));

ALTER TABLE organization_membership
    ADD CONSTRAINT ck_membership_role
    CHECK (role IN (
        'ORG_ADMIN', 'PROGRAM_MANAGER', 'GRANT_MANAGER', 'DONATION_MANAGER',
        'VOLUNTEER_COORDINATOR', 'EVENT_COORDINATOR', 'VOLUNTEER', 'VIEWER'
    ));

CREATE INDEX idx_membership_organization ON organization_membership(organization_id);
