package com.bulldozer_collective.modules.crm.gateway;

import com.bulldozer_collective.shared.IntegrationPlatform;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Picks the {@link CrmGateway} of a connection's platform. */
@Service
public class CrmGatewayResolver {

    private final Map<IntegrationPlatform, CrmGateway> gateways = new EnumMap<>(IntegrationPlatform.class);

    public CrmGatewayResolver(@NonNull List<CrmGateway> gateways) {
        gateways.forEach(gateway -> this.gateways.put(gateway.platform(), gateway));
    }

    /** @throws UnsupportedOperationException when no gateway exists for the platform (→ 501). */
    public @NonNull CrmGateway resolve(@NonNull CrmSession session) {
        var gateway = this.gateways.get(session.platform());
        if (gateway == null) {
            throw new UnsupportedOperationException(
                    "%s is not supported yet".formatted(session.platform().getNiceName()));
        }
        return gateway;
    }
}
