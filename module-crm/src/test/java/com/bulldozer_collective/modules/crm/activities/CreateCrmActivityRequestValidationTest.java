package com.bulldozer_collective.modules.crm.activities;

import com.bulldozer_collective.modules.crm.api.CreateCrmActivityRequest;
import com.bulldozer_collective.modules.crm.api.CrmActivityType;
import com.bulldozer_collective.modules.crm.api.CrmObjectRef;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The activity is created in the CRM before its associations are made: a bad association found only then leaves
 * an orphan activity behind. It must be refused by validation, before anything reaches the CRM.
 */
class CreateCrmActivityRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("an association without a type is refused")
    void refusesAssociationWithoutType() {
        var request = request(List.of(new CrmObjectRef(null, "42")));

        assertThat(this.validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("associations[0].type");
    }

    @Test
    @DisplayName("a null association is refused")
    void refusesNullAssociation() {
        var request = request(Arrays.asList((CrmObjectRef) null));

        assertThat(this.validator.validate(request)).hasSize(1);
    }

    private static CreateCrmActivityRequest request(List<CrmObjectRef> associations) {
        return new CreateCrmActivityRequest(CrmActivityType.CRM_ACTIVITY_TYPE_NOTE, Instant.now(), null, "recap",
                null, associations);
    }
}
