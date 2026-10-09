package com.bulldozer_collective.modules.crm.api;

/**
 * Shared bounds for the module's request records.
 */
public final class CrmConstraints {

    /** Applied when the caller sends no {@code limit}. */
    public static final int DEFAULT_LIMIT = 20;

    /** Upper bound on {@code limit}: HubSpot serves at most 100 records per listing page. */
    public static final int MAX_LIMIT = 100;

    /** Default length of a {@code VARCHAR} column, and the bound on short CRM fields. */
    public static final int SHORT_TEXT = 255;

    /** Bound on a free-text field written to the CRM (HubSpot caps a note body at 65,536 characters). */
    public static final int LONG_TEXT = 65_536;

    private CrmConstraints() {
    }
}
