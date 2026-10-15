package com.bulldozer_collective.modules.crm.api;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

/** @param publishAt when the page goes live: now when absent. */
public record PublishCrmLandingPageRequest(@Nullable Instant publishAt) {
}
