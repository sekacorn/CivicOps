package org.civicops.core;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.civicops.core.membership.dto.CreateMembershipRequest;
import org.civicops.core.organization.dto.CreateOrganizationRequest;
import org.civicops.core.user.dto.CreateUserRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CoreValidationTest {
    private static Validator validator;

    @BeforeAll
    static void configureValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsInvalidOrganizationRequest() {
        var request = new CreateOrganizationRequest(" ", null, null, null, "not-an-email", null,
                null, null, null, null, null, null, "USA");
        assertThat(validator.validate(request)).extracting(violation -> violation.getPropertyPath().toString())
                .contains("name", "organizationType", "email", "country");
    }

    @Test
    void rejectsInvalidUserRequest() {
        var request = new CreateUserRequest("", "", "bad-email", "short");
        assertThat(validator.validate(request)).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void rejectsMembershipWithoutUserOrRole() {
        assertThat(validator.validate(new CreateMembershipRequest(null, null))).hasSize(2);
    }
}
