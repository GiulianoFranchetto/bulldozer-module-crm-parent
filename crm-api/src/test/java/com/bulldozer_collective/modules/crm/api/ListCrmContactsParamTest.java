package com.bulldozer_collective.modules.crm.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These params used to declare {@code page} and {@code size} as primitives, which Spring bound to {@code 0} for
 * an absent query parameter — and {@code PageRequest.of(0, 0)} throws, so every list endpoint answered 400 to a
 * caller that sent no pagination at all. The defaulting below is what stops that from coming back.
 */
class ListCrmContactsParamTest {

    @Test
    @DisplayName("a caller that sends no pagination gets the first page, not a 400")
    void defaultsPagination() {
        var param = new ListCrmContactsParam(null, null, null, null);

        assertThat(param.page()).isZero();
        assertThat(param.size()).isEqualTo(CrmConstraints.DEFAULT_PAGE_SIZE);
    }

    @Test
    @DisplayName("an explicit pagination is left alone")
    void keepsExplicitPagination() {
        var param = new ListCrmContactsParam(null, null, 3, 50);

        assertThat(param.page()).isEqualTo(3);
        assertThat(param.size()).isEqualTo(50);
    }
}
