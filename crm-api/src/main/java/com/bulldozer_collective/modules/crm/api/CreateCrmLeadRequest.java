package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The lead is created in the container's pipeline, attached to the existing CRM contact {@code contactId} — the
 * CRM refuses a lead without one. {@code stage} is written as the value mapped to it; {@code type} is the CRM's
 * raw lead type. {@code pipelineStage} is the id of the pipeline's own stage (from {@code GET …/containers}):
 * required when the container reads its stages from a custom property, refused otherwise.
 */
public record CreateCrmLeadRequest(@NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String name,
                                   @NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String contactId,
                                   @NonNull @NotNull CrmLeadStage stage,
                                   @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String type,
                                   @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String pipelineStage) {
}
