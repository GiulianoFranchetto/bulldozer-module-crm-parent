package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

/** A CRM record, identified by its type and the CRM's own id. */
@Builder
public record CrmObjectRef(@NonNull CrmObjectType type,
                           @NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String id) {
}
