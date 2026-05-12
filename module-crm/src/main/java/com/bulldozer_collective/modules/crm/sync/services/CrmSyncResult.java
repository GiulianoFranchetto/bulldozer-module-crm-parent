package com.bulldozer_collective.modules.crm.sync.services;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CrmSyncResult {
    private final int contactsSynced;
    private final int companiesSynced;
    private final int dealsSynced;
    private final int activitiesSynced;
}
