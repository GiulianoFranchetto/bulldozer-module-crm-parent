package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmObjectType;
import com.bulldozer_collective.modules.crm.api.CrmPropertyFieldType;
import com.bulldozer_collective.modules.crm.gateway.CrmGatewayResolver;
import com.bulldozer_collective.modules.crm.gateway.CrmSessionOpener;
import com.bulldozer_collective.logging.BulldozerLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** The options are checked before the CRM is reached: a refused property never opens a session. */
class CrmPropertyFactoryTest {

    private final CrmSessionOpener sessionOpener = mock(CrmSessionOpener.class);
    private final CrmPropertyFactory factory =
            new CrmPropertyFactory(this.sessionOpener, mock(CrmGatewayResolver.class), mock(BulldozerLogger.class));

    @Test
    @DisplayName("a dropdown without options is refused")
    void selectNeedsOptions() throws Exception {
        this.assertRefused(CrmPropertyFieldType.CRM_PROPERTY_FIELD_TYPE_SELECT, List.of(), "needs at least one");
    }

    @Test
    @DisplayName("options on any other field type are refused")
    void onlySelectTakesOptions() throws Exception {
        this.assertRefused(CrmPropertyFieldType.CRM_PROPERTY_FIELD_TYPE_TEXT,
                List.of(new CreateCrmPropertyBuilder.Option("A", "a")), "only a CRM_PROPERTY_FIELD_TYPE_SELECT");
    }

    @Test
    @DisplayName("two options with the same value are refused")
    void optionValuesAreUnique() throws Exception {
        this.assertRefused(CrmPropertyFieldType.CRM_PROPERTY_FIELD_TYPE_SELECT, List.of(
                new CreateCrmPropertyBuilder.Option("A", "a"),
                new CreateCrmPropertyBuilder.Option("Also A", "a")), "given twice");
    }

    private void assertRefused(CrmPropertyFieldType fieldType,
                               List<CreateCrmPropertyBuilder.Option> options,
                               String message) throws Exception {
        var property = CreateCrmPropertyBuilder.builder()
                .objectType(CrmObjectType.CRM_OBJECT_TYPE_CONTACT)
                .name("bdz_lead_source")
                .label("Lead source")
                .groupName("contactinformation")
                .fieldType(fieldType)
                .options(options)
                .build();

        assertThatThrownBy(() -> this.factory.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), property))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(message);
        verify(this.sessionOpener, never()).open(any(), any(), any());
    }
}
