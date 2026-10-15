package com.bulldozer_collective.modules.crm.forms.controllers;

import com.bulldozer_collective.modules.crm.api.SaveCrmFormFieldRequest;
import com.bulldozer_collective.modules.crm.forms.services.SaveCrmFormBuilder;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** Maps the contract's form fields onto the service's, shared by creation and update. */
final class CrmFormFieldMapper {

    private CrmFormFieldMapper() {
    }

    static @NonNull List<SaveCrmFormBuilder.Field> fields(@NonNull List<SaveCrmFormFieldRequest> fields) {
        return fields.stream()
                .map(field -> new SaveCrmFormBuilder.Field(
                        field.objectType(),
                        field.name(),
                        field.label(),
                        field.fieldType(),
                        Boolean.TRUE.equals(field.required()),
                        field.options() == null ? List.of() : field.options()))
                .toList();
    }
}
