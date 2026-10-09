package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateCrmCompanyRequest(@Nullable @Size(max = CrmConstraints.SHORT_TEXT) String name,
                                      @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String domain,
                                      @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String industry,
                                      @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String phone,
                                      @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String city,
                                      @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String country,
                                      @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String ownerId) {
}
