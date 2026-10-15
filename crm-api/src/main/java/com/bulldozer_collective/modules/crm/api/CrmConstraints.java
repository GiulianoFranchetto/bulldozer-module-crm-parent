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

    /** A language code, as the CRM takes it ({@code fr}, {@code en-us}). */
    public static final int LANGUAGE = 16;

    /** Bound on a URL written to the CRM. */
    public static final int URL = 2048;

    /** Upper bound on a form's fields. */
    public static final int MAX_FORM_FIELDS = 50;

    /** An ISO 4217 currency code, as the CRM stores it: three uppercase letters. */
    public static final String CURRENCY = "[A-Z]{3}";

    public static final String CURRENCY_MESSAGE = "must be an ISO 4217 code: 3 uppercase letters";

    private CrmConstraints() {
    }
}
