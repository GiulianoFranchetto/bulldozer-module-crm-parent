package com.bulldozer_collective.modules.crm.sync.services.hubspot.dto;

import java.util.List;

public record HubSpotAssociationBatchResponse(
        List<HubSpotAssociationResult> results
) {}
