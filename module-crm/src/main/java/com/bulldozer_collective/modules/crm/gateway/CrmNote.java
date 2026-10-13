package com.bulldozer_collective.modules.crm.gateway;

import com.bulldozer_collective.modules.crm.api.CrmObjectRef;
import org.jspecify.annotations.NonNull;

import java.time.Instant;

/**
 * A note to log on one CRM record.
 *
 * @param body  HTML, as the CRM renders a note's body.
 * @param about the contact or company the note is attached to.
 */
public record CrmNote(@NonNull String body, @NonNull Instant timestamp, @NonNull CrmObjectRef about) {
}
