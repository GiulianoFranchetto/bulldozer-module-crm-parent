package com.bulldozer_collective.modules.crm.api;

import java.util.List;
import org.jspecify.annotations.NonNull;

public record ListCrmStageMappingsResponse(@NonNull List<CrmStageMapping> mappings) {
}
