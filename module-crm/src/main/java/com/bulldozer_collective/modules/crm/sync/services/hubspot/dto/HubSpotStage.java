package com.bulldozer_collective.modules.crm.sync.services.hubspot.dto;

public record HubSpotStage(
        String id,
        String label,
        int displayOrder
) {}
