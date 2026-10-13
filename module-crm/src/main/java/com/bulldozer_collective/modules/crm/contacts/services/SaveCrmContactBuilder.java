package com.bulldozer_collective.modules.crm.contacts.services;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
 * The fields of a contact to write to the CRM. A {@code null} field is not written.
 *
 * @param legalBasis creation only: the lawful basis for processing the contact's data, a raw value of the CRM's own
 *                   list (HubSpot: an option of {@code hs_legal_basis}, required by a portal with its GDPR features
 *                   on). Not a field of the contact on its own: {@link #isEmpty()} ignores it.
 */
@Builder
public record SaveCrmContactBuilder(@Nullable String firstName,
                                    @Nullable String lastName,
                                    @Nullable String email,
                                    @Nullable String phone,
                                    @Nullable String jobTitle,
                                    @Nullable String legalBasis) {

    public boolean isEmpty() {
        return Stream.of(this.firstName, this.lastName, this.email, this.phone, this.jobTitle)
                .allMatch(value -> value == null);
    }
}
