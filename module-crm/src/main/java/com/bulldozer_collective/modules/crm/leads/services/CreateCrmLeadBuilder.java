package com.bulldozer_collective.modules.crm.leads.services;

import com.bulldozer_collective.modules.crm.api.CrmLeadStage;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A lead to create in the CRM, attached to the existing CRM contact {@code contactId}. {@code stage} is not
 * written as such: the services translate it into the value its container's mapping designates.
 */
@Builder
public record CreateCrmLeadBuilder(@NonNull String name,
                                   @NonNull String contactId,
                                   @NonNull CrmLeadStage stage,
                                   @Nullable String type,
                                   @Nullable String pipelineStage) {
}
