package com.bulldozer_collective.modules.crm.gateway;

import com.bulldozer_collective.modules.crm.api.CrmObjectRef;
import com.bulldozer_collective.modules.crm.api.CrmObjectType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListCrmObjectsBuilderTest {

    @Test
    void associationNeedsBothParameters() {
        assertThat(ListCrmObjectsBuilder.association(null, null)).isNull();
        assertThat(ListCrmObjectsBuilder.association(CrmObjectType.CRM_OBJECT_TYPE_DEAL, "12"))
                .isEqualTo(new CrmObjectRef(CrmObjectType.CRM_OBJECT_TYPE_DEAL, "12"));
        assertThatThrownBy(() -> ListCrmObjectsBuilder.association(CrmObjectType.CRM_OBJECT_TYPE_DEAL, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ListCrmObjectsBuilder.association(null, "12"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void searchOnlyWhenFiltered() {
        assertThat(ListCrmObjectsBuilder.builder().limit(20).build().filtered()).isFalse();
        assertThat(ListCrmObjectsBuilder.builder().q(" ").limit(20).build().filtered()).isFalse();
        assertThat(ListCrmObjectsBuilder.builder().q("acme").limit(20).build().filtered()).isTrue();
    }
}
