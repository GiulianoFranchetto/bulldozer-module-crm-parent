package com.bulldozer_collective.modules.crm.gateway.hubspot;

import com.bulldozer_collective.modules.crm.api.CrmLandingPageState;
import com.bulldozer_collective.modules.crm.gateway.hubspot.dto.HubSpotPage;
import com.bulldozer_collective.modules.crm.landingpages.services.SaveCrmLandingPageBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HubSpotLandingPageMapperTest {

    @Test
    @DisplayName("a live page is published; a page scheduled ahead is scheduled; anything else is a draft")
    void state() {
        var future = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        var past = Instant.now().minus(1, ChronoUnit.DAYS).toString();

        assertThat(HubSpotLandingPageMapper.state(page("PUBLISHED_OR_SCHEDULED", true, past)))
                .isEqualTo(CrmLandingPageState.CRM_LANDING_PAGE_STATE_PUBLISHED);
        assertThat(HubSpotLandingPageMapper.state(page("PUBLISHED_OR_SCHEDULED", false, future)))
                .isEqualTo(CrmLandingPageState.CRM_LANDING_PAGE_STATE_SCHEDULED);
        assertThat(HubSpotLandingPageMapper.state(page("DRAFT", false, null)))
                .isEqualTo(CrmLandingPageState.CRM_LANDING_PAGE_STATE_DRAFT);
    }

    @Test
    @DisplayName("the content is read on a single page only, and a draft carries no publish date")
    void contentOnlyWhenAsked() {
        var page = page("DRAFT", false, "2026-10-01T00:00:00Z");

        assertThat(HubSpotLandingPageMapper.page(page, true).content()).containsOnlyKeys("layoutSections", "widgets");
        assertThat(HubSpotLandingPageMapper.page(page, false).content()).isNull();
        assertThat(HubSpotLandingPageMapper.page(page, true).publishedAt()).isNull();
    }

    @Test
    @DisplayName("a page is created as a draft, its content's parts at the top of the body")
    void createBody() {
        var body = HubSpotLandingPageMapper.page(SaveCrmLandingPageBuilder.builder()
                .name("Offer").templatePath("theme/templates/lp.html").slug("offer")
                .content(Map.of("widgets", Map.of("hero", Map.of())))
                .build(), true);

        assertThat(body).containsEntry("state", "DRAFT")
                .containsEntry("slug", "offer")
                .containsKey("widgets")
                .doesNotContainKey("domain");
    }

    @Test
    @DisplayName("an update writes only the given fields, never the state")
    void updateBody() {
        var body = HubSpotLandingPageMapper.page(SaveCrmLandingPageBuilder.builder().htmlTitle("New").build(), false);

        assertThat(body).containsOnlyKeys("htmlTitle");
    }

    @Test
    @DisplayName("a content key HubSpot does not take is refused rather than written over another field")
    void unknownContentKey() {
        var page = SaveCrmLandingPageBuilder.builder().content(Map.of("state", "PUBLISHED")).build();

        assertThatThrownBy(() -> HubSpotLandingPageMapper.page(page, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'state'");
    }

    private static HubSpotPage page(String currentState, boolean live, String publishDate) {
        return new HubSpotPage("12", "Offer", "offer", "", "https://example.com/offer", null, null, "fr",
                "theme/templates/lp.html", currentState, live, publishDate, "2026-09-01T00:00:00Z", null,
                Map.of("dnd", Map.of()), Map.of("hero", Map.of()));
    }
}
