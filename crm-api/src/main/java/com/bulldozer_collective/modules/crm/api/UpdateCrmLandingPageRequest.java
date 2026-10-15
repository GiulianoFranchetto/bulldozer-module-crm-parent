package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/** Only the non-null fields are written; at least one is required. */
public record UpdateCrmLandingPageRequest(@Nullable @Size(max = CrmConstraints.SHORT_TEXT) String name,
                                          @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String slug,
                                          @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String domain,
                                          @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String htmlTitle,
                                          @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String metaDescription,
                                          @Nullable @Size(max = CrmConstraints.LANGUAGE) String language,
                                          @Nullable Map<String, Object> content) {
}
