package com.bulldozer_collective.modules.crm.sync.services.hubspot.dto;

import java.util.List;

public record HubSpotPipeline(
        String id,
        String label,
        int displayOrder,
        List<HubSpotStage> stages
) {}
