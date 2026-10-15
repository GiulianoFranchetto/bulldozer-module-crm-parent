package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmObjectType;
import com.bulldozer_collective.modules.crm.api.CrmPropertyFieldType;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A property to create in the CRM. {@code options} are the dropdown's values, in display order: empty for any other
 * field type.
 */
@Builder
public record CreateCrmPropertyBuilder(@NonNull CrmObjectType objectType,
                                       @NonNull String name,
                                       @NonNull String label,
                                       @NonNull String groupName,
                                       @Nullable String description,
                                       @NonNull CrmPropertyFieldType fieldType,
                                       @NonNull List<Option> options) {

    public record Option(@NonNull String label, @NonNull String value) {
    }
}
