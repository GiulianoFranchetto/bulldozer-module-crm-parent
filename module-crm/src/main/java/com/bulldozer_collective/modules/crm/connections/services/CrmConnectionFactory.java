package com.bulldozer_collective.modules.crm.connections.services;

import com.bulldozer_collective.modules.crm.connections.data.CrmConnectionEntity;
import com.bulldozer_collective.modules.crm.connections.data.CrmConnectionRepository;
import com.bulldozer_collective.modules.crm.gateway.NoTokenException;
import com.bulldozer_collective.modules.crm.gateway.PlatformTokenFetcher;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.exceptions.ConflictException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

@Service
@RequiredArgsConstructor
public class CrmConnectionFactory {

    private final CrmConnectionRepository repository;
    private final PlatformTokenFetcher tokenFetcher;
    private final BulldozerLogger logger;

    /**
     * Registers a CRM on the project. The modulith must already hold a token for it — the OAuth flow comes first —
     * so a connection that could never be used is refused here rather than on its first call.
     *
     * @throws IllegalArgumentException      when the platform is not a CRM the bridge supports.
     * @throws UnsupportedOperationException for Salesforce, not implemented yet (→ 501).
     * @throws ConflictException             when the project already has a connection to that platform.
     * @throws NoTokenException              when the modulith holds no token for that platform.
     */
    public @NonNull CrmConnectionEntity create(@NonNull UUID customerId,
                                               @NonNull UUID projectId,
                                               @NonNull IntegrationPlatform platform) throws NoTokenException {
        switch (platform) {
            case INTEGRATION_PLATFORM_HUBSPOT -> {
                // Supported.
            }
            case INTEGRATION_PLATFORM_SALESFORCE ->
                    throw new UnsupportedOperationException("Salesforce is not supported yet");
            default -> throw new IllegalArgumentException(
                    "platform: '%s' is not a supported CRM; accepted values: INTEGRATION_PLATFORM_HUBSPOT"
                            .formatted(platform));
        }

        if (this.repository.existsByCustomerIdAndProjectIdAndPlatform(customerId, projectId, platform)) {
            throw new ConflictException("This project already has a %s connection".formatted(platform.getNiceName()));
        }

        this.tokenFetcher.fetchToken(customerId, projectId, platform);

        var entity = new CrmConnectionEntity();
        entity.setCustomerId(customerId);
        entity.setProjectId(projectId);
        entity.setPlatform(platform);
        try {
            var saved = this.repository.save(entity);
            this.logger.info(op("CRM-001", customerId, projectId) + "Registered %s connection %s",
                    platform, saved.getId());
            return saved;
        } catch (DataIntegrityViolationException e) {
            // Two concurrent registrations: the unique constraint let one through.
            throw new ConflictException("This project already has a %s connection".formatted(platform.getNiceName()));
        }
    }
}
