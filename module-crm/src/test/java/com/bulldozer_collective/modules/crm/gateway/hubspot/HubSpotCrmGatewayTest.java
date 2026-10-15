package com.bulldozer_collective.modules.crm.gateway.hubspot;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.gateway.CrmSession;
import com.bulldozer_collective.modules.crm.gateway.CrmStageOption;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotModificationMetadata;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotProperty;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotPropertyOption;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotResults;
import com.bulldozer_collective.shared.IntegrationPlatform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A stage source must be a property Bulldozer can write a stage to. */
class HubSpotCrmGatewayTest {

    private static final CrmSession SESSION = CrmSession.builder().customerId(UUID.randomUUID())
            .projectId(UUID.randomUUID()).connectionId(UUID.randomUUID())
            .platform(IntegrationPlatform.INTEGRATION_PLATFORM_HUBSPOT).accessToken("token").build();

    private final HubSpotClient client = mock(HubSpotClient.class);
    private final HubSpotCrmGateway gateway;

    HubSpotCrmGatewayTest() {
        var clientFactory = mock(HubSpotClientFactory.class);
        when(clientFactory.create(SESSION)).thenReturn(this.client);
        this.gateway = new HubSpotCrmGateway(clientFactory);
    }

    @Test
    @DisplayName("a writable enumerated property gives its options as stages")
    void readsAWritableProperty() {
        this.given(property("status", false));

        assertThat(this.gateway.listStageOptions(SESSION, CrmContainerType.CRM_CONTAINER_TYPE_DEAL, "default",
                "status")).extracting(CrmStageOption::value).containsExactly("demo", "signed");
    }

    @Test
    @DisplayName("a read-only property is refused: every stage write to it would fail")
    void refusesAReadOnlyProperty() {
        this.given(property("hs_is_closed", true));

        assertThatThrownBy(() -> this.gateway.listStageOptions(SESSION, CrmContainerType.CRM_CONTAINER_TYPE_DEAL,
                "default", "hs_is_closed"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("read-only");
    }

    private void given(HubSpotProperty property) {
        when(this.client.get(any(), any(), any())).thenReturn(new HubSpotResults<>(List.of(property), null));
    }

    private static HubSpotProperty property(String name, boolean readOnly) {
        return new HubSpotProperty(name, name, "enumeration", "select", "dealinformation", false, false,
                new HubSpotModificationMetadata(readOnly),
                List.of(new HubSpotPropertyOption("Demo", "demo", 0, false),
                        new HubSpotPropertyOption("Signed", "signed", 1, false)));
    }
}
