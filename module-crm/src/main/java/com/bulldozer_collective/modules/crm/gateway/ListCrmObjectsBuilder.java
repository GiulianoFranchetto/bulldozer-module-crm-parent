package com.bulldozer_collective.modules.crm.gateway;

import com.bulldozer_collective.modules.crm.api.CrmObjectRef;
import com.bulldozer_collective.modules.crm.api.CrmObjectType;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * A listing of one CRM object type.
 *
 * @param q            full-text search on the CRM's searchable properties.
 * @param associatedTo restricts the listing to the records associated with this one.
 * @param cursor       the {@code nextCursor} of the previous page, opaque.
 */
@Builder
public record ListCrmObjectsBuilder(@Nullable String q,
                                    @Nullable CrmObjectRef associatedTo,
                                    @Nullable String cursor,
                                    int limit) {

    /** {@code true} when the CRM's search is needed rather than its plain listing. */
    public boolean filtered() {
        return (this.q != null && !this.q.isBlank()) || this.associatedTo != null;
    }

    /**
     * Pairs the two association query parameters: they make sense together or not at all.
     *
     * @throws IllegalArgumentException when only one of the two is given.
     */
    public static @Nullable CrmObjectRef association(@Nullable CrmObjectType type, @Nullable String id) {
        var hasId = id != null && !id.isBlank();
        if (type == null && !hasId) {
            return null;
        }
        if (type == null || !hasId) {
            throw new IllegalArgumentException("associatedType and associatedId must be given together");
        }
        return new CrmObjectRef(type, id);
    }
}
