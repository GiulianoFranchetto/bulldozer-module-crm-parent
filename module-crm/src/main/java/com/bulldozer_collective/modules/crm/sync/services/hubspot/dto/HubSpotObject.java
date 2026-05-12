package com.bulldozer_collective.modules.crm.sync.services.hubspot.dto;

import java.util.Map;

public record HubSpotObject(
        String id,
        Map<String, String> properties
) {}
