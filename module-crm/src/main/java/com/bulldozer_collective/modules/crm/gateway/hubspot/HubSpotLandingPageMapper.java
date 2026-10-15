package com.bulldozer_collective.modules.crm.gateway.hubspot;

import com.bulldozer_collective.modules.crm.api.CrmLandingPage;
import com.bulldozer_collective.modules.crm.api.CrmLandingPageState;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotPage;
import com.bulldozer_collective.modules.crm.landingpages.services.SaveCrmLandingPageBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Translates between HubSpot's landing pages ({@code /cms/pages/2026-09/landing-pages}) and the module's model. A page's
 * {@code content} is HubSpot's own {@code layoutSections} and {@code widgets}, passed through untouched.
 */
final class HubSpotLandingPageMapper {

    /** The keys a page's content may carry: the parts of a HubSpot page that hold its layout and module data. */
    static final Set<String> CONTENT_KEYS = Set.of("layoutSections", "widgets");

    private HubSpotLandingPageMapper() {
    }

    // ── read ─────────────────────────────────────────────────────────────────

    static @NonNull CrmLandingPage page(@NonNull HubSpotPage page, boolean withContent) {
        var state = state(page);
        return CrmLandingPage.builder()
                .id(Objects.requireNonNull(page.id()))
                .name(page.name() == null ? page.id() : page.name())
                .slug(page.slug())
                .domain(blankToNull(page.domain()))
                .url(blankToNull(page.url()))
                .htmlTitle(page.htmlTitle())
                .metaDescription(page.metaDescription())
                .language(page.language())
                .templatePath(page.templatePath())
                .state(state)
                .publishedAt(state == CrmLandingPageState.CRM_LANDING_PAGE_STATE_DRAFT
                        ? null
                        : HubSpotMapper.instant(page.publishDate()))
                .content(withContent ? content(page) : null)
                .createdAt(HubSpotMapper.instant(page.created()))
                .updatedAt(HubSpotMapper.instant(page.updated()))
                .build();
    }

    /**
     * {@code currentlyPublished} tells a live page; HubSpot's {@code currentState} spells a waiting one in several ways
     * ({@code SCHEDULED}, {@code SCHEDULED_AB}, {@code PUBLISHED_OR_SCHEDULED}…), so a page not live is scheduled when its
     * state names a schedule and its publish date is still ahead.
     */
    static @NonNull CrmLandingPageState state(@NonNull HubSpotPage page) {
        if (Boolean.TRUE.equals(page.currentlyPublished())) {
            return CrmLandingPageState.CRM_LANDING_PAGE_STATE_PUBLISHED;
        }
        var publishDate = HubSpotMapper.instant(page.publishDate());
        if (page.currentState() != null && page.currentState().contains("SCHEDULED")
                && publishDate != null && publishDate.isAfter(Instant.now())) {
            return CrmLandingPageState.CRM_LANDING_PAGE_STATE_SCHEDULED;
        }
        return CrmLandingPageState.CRM_LANDING_PAGE_STATE_DRAFT;
    }

    private static @NonNull Map<String, Object> content(@NonNull HubSpotPage page) {
        var content = new LinkedHashMap<String, Object>();
        if (page.layoutSections() != null) {
            content.put("layoutSections", page.layoutSections());
        }
        if (page.widgets() != null) {
            content.put("widgets", page.widgets());
        }
        return content;
    }

    // ── write ────────────────────────────────────────────────────────────────

    /**
     * The body of a creation ({@code create}: a draft, so the page is never published by accident) or of a sparse
     * update: only the non-null fields.
     */
    static @NonNull Map<String, Object> page(@NonNull SaveCrmLandingPageBuilder page, boolean create) {
        var body = new LinkedHashMap<String, Object>();
        putIfSet(body, "name", page.name());
        putIfSet(body, "templatePath", page.templatePath());
        putIfSet(body, "slug", page.slug());
        putIfSet(body, "domain", page.domain());
        putIfSet(body, "htmlTitle", page.htmlTitle());
        putIfSet(body, "metaDescription", page.metaDescription());
        putIfSet(body, "language", page.language());
        if (page.content() != null) {
            page.content().keySet().stream()
                    .filter(key -> !CONTENT_KEYS.contains(key))
                    .findFirst()
                    .ifPresent(key -> {
                        throw new IllegalArgumentException(("content: '%s' is not a part of a HubSpot page's content; "
                                + "it takes %s").formatted(key, CONTENT_KEYS));
                    });
            body.putAll(page.content());
        }
        if (create) {
            body.put("state", "DRAFT");
        }
        return body;
    }

    private static void putIfSet(@NonNull Map<String, Object> body, @NonNull String name, @Nullable Object value) {
        if (value != null) {
            body.put(name, value);
        }
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
