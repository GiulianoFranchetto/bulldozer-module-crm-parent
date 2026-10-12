package com.bulldozer_collective.modules.crm.containers.services;

import com.bulldozer_collective.modules.crm.api.CrmContainerType;
import com.bulldozer_collective.modules.crm.gateway.CrmGateway;
import com.bulldozer_collective.modules.crm.gateway.CrmSession;
import com.bulldozer_collective.modules.crm.gateway.CrmStageOption;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

/** Reads the values of a stage source property from the CRM and checks it can be one. */
@Service
public class CrmStageSources {

    /**
     * @throws IllegalArgumentException when the property does not exist, or is free-form: a container's stages
     *                                  must be a finite list.
     */
    public @NonNull List<CrmStageOption> read(@NonNull CrmSession session,
                                              @NonNull CrmGateway gateway,
                                              @NonNull CrmContainerType type,
                                              @NonNull String containerRemoteId,
                                              @NonNull String propertyName) {
        var options = gateway.listStageOptions(session, type, containerRemoteId, propertyName);
        if (options.isEmpty()) {
            throw new IllegalArgumentException(("stagePropertyName: '%s' has no fixed list of values in this "
                    + "pipeline; choose an enumerated property").formatted(propertyName));
        }
        return options;
    }
}
