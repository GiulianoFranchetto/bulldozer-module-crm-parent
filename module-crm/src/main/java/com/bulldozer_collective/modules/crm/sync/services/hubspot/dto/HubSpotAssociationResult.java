package com.bulldozer_collective.modules.crm.sync.services.hubspot.dto;

import java.util.List;

public record HubSpotAssociationResult(
        HubSpotAssociationFrom from,
        List<HubSpotAssociationTo> to
) {}
