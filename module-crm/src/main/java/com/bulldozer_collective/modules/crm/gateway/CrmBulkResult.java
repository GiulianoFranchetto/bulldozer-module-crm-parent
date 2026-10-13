package com.bulldozer_collective.modules.crm.gateway;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * The outcome of a bulk write: the records written, and why each refused input was refused. A refused input never
 * fails the others.
 *
 * @param done     the records written, in no particular order — match them back by their own fields.
 * @param failures the CRM's message for each refused input, keyed by its index in the request.
 */
public record CrmBulkResult<T>(@NonNull List<T> done, @NonNull Map<Integer, String> failures) {
}
