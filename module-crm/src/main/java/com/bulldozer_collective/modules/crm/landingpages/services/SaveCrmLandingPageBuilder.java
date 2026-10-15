package com.bulldozer_collective.modules.crm.landingpages.services;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.stream.Stream;

/**
 * The fields of a landing page to write to the CRM. A {@code null} field is not written.
 *
 * @param templatePath creation only: the CRM template the page is built on.
 * @param content      the page's content in the CRM's own format, passed through.
 */
@Builder
public record SaveCrmLandingPageBuilder(@Nullable String name,
                                        @Nullable String templatePath,
                                        @Nullable String slug,
                                        @Nullable String domain,
                                        @Nullable String htmlTitle,
                                        @Nullable String metaDescription,
                                        @Nullable String language,
                                        @Nullable Map<String, Object> content) {

    public boolean isEmpty() {
        return Stream.of(this.name, this.templatePath, this.slug, this.domain, this.htmlTitle, this.metaDescription,
                        this.language, this.content)
                .allMatch(value -> value == null);
    }
}
