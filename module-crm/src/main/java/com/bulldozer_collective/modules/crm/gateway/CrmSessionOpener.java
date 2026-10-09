package com.bulldozer_collective.modules.crm.gateway;

import com.bulldozer_collective.modules.crm.connections.services.CrmConnectionFetcher;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Opens a {@link CrmSession} on a connection of the project: the connection must belong to the project (else
 * {@code 404}), and the modulith must hold a token for its platform (else {@link NoTokenException}).
 */
@Service
@RequiredArgsConstructor
public class CrmSessionOpener {

    private final CrmConnectionFetcher connectionFetcher;
    private final PlatformTokenFetcher tokenFetcher;

    public @NonNull CrmSession open(@NonNull UUID customerId,
                                    @NonNull UUID projectId,
                                    @NonNull UUID connectionId) throws NoTokenException {
        var connection = this.connectionFetcher.fetch(customerId, projectId, connectionId);
        var token = this.tokenFetcher.fetchToken(customerId, projectId, connection.getPlatform());
        return CrmSession.builder()
                .customerId(customerId)
                .projectId(projectId)
                .connectionId(connectionId)
                .platform(connection.getPlatform())
                .accessToken(token.token())
                .instanceUrl(connection.getInstanceUrl())
                .build();
    }
}
