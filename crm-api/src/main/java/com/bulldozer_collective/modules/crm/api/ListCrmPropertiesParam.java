package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A type's properties run to hundreds, some with a hundred options: both filters keep the answer small enough for an
 * agent to read.
 *
 * @param name       only the property of that name — e.g. {@code hs_legal_basis} for a contact's legal basis.
 * @param selectable only the properties a value can be picked from and written: enumerated ({@code options} not
 *                   empty) and not read-only — the candidates for a stage source.
 */
public record ListCrmPropertiesParam(@NonNull @NotNull CrmObjectType type,
                                     @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String name,
                                     @Nullable Boolean selectable) {
}
