package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/** A {@code null} field is left unchanged in the CRM. */
public record UpdateCrmContactRequest(@Nullable @Size(max = CrmConstraints.SHORT_TEXT) String firstName,
                                      @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String lastName,
                                      @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String email,
                                      @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String phone,
                                      @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String jobTitle) {
}
