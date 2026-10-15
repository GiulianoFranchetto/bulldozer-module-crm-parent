package com.bulldozer_collective.modules.crm.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * A landing page is created as a draft: publish it with {@code POST …/landing-pages/{id}/publish}.
 *
 * @param templatePath the CRM template the page is built on — e.g. a path of the portal's theme.
 * @param domain       the CRM's default domain for landing pages when absent.
 * @param content      the page's content in the CRM's own format (see {@link CrmLandingPage#content()}).
 */
public record CreateCrmLandingPageRequest(@NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String name,
                                          @NonNull @NotBlank @Size(max = CrmConstraints.SHORT_TEXT) String templatePath,
                                          @NonNull @NotNull @Size(max = CrmConstraints.SHORT_TEXT) String slug,
                                          @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String domain,
                                          @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String htmlTitle,
                                          @Nullable @Size(max = CrmConstraints.SHORT_TEXT) String metaDescription,
                                          @Nullable @Size(max = CrmConstraints.LANGUAGE) String language,
                                          @Nullable Map<String, Object> content) {
}
