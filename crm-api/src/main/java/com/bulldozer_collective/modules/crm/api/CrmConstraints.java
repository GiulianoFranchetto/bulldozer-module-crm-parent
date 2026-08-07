package com.bulldozer_collective.modules.crm.api;

/**
 * Shared bounds for the module's request records.
 */
public final class CrmConstraints {

    /** Applied when the caller sends no {@code size}. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Upper bound on {@code size}. A CRM portal holds tens of thousands of objects, so an unbounded page size
     * is a way to make the service load all of them in one request.
     */
    public static final int MAX_PAGE_SIZE = 200;

    private CrmConstraints() {
    }
}
