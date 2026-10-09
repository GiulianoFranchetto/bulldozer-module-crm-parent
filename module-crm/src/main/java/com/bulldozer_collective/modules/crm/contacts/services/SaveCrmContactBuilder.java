package com.bulldozer_collective.modules.crm.contacts.services;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/** The fields of a contact to write to the CRM. A {@code null} field is not written. */
@Builder
public record SaveCrmContactBuilder(@Nullable String firstName,
                                    @Nullable String lastName,
                                    @Nullable String email,
                                    @Nullable String phone,
                                    @Nullable String jobTitle,
                                    @Nullable String ownerId) {

    public boolean isEmpty() {
        return Stream.of(this.firstName, this.lastName, this.email, this.phone, this.jobTitle, this.ownerId)
                .allMatch(value -> value == null);
    }
}
