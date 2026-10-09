package com.bulldozer_collective.modules.crm.api;

import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One page of a CRM listing. Not a {@code ResponsePage}: CRMs paginate by cursor and the bridge stores nothing,
 * so there is no page number and no reliable total. Pass {@code nextCursor} back as {@code cursor} to get the
 * next page; it is {@code null} on the last one. The cursor is opaque and only valid with the same filters.
 */
@Builder
public record CrmPage<T>(@NonNull List<T> items,
                         @Nullable String nextCursor) {
}
