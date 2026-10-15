package com.bulldozer_collective.modules.crm.forms.services;

import com.bulldozer_collective.modules.crm.api.CrmFormFieldOption;
import com.bulldozer_collective.modules.crm.api.CrmFormFieldType;
import com.bulldozer_collective.modules.crm.api.CrmFormLegalConsent;
import com.bulldozer_collective.modules.crm.api.CrmObjectType;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

/**
 * The parts of a form to write to the CRM. A {@code null} part is not written; {@code fields}, when given, replaces
 * every field of the form.
 */
@Builder(toBuilder = true)
public record SaveCrmFormBuilder(@Nullable String name,
                                 @Nullable List<Field> fields,
                                 @Nullable String submitButtonText,
                                 @Nullable String thankYouMessage,
                                 @Nullable String redirectUrl,
                                 @Nullable String language,
                                 @Nullable CrmFormLegalConsent legalConsent) {

    public boolean isEmpty() {
        return Stream.of(this.name, this.fields, this.submitButtonText, this.thankYouMessage, this.redirectUrl,
                        this.language, this.legalConsent)
                .allMatch(value -> value == null);
    }

    /** @param options the values of a {@code SELECT} or {@code RADIO} field, in display order; empty otherwise. */
    public record Field(@NonNull CrmObjectType objectType,
                        @NonNull String name,
                        @NonNull String label,
                        @NonNull CrmFormFieldType fieldType,
                        boolean required,
                        @NonNull List<CrmFormFieldOption> options) {
    }
}
