package com.bulldozer_collective.modules.crm.contacts.data;

import com.bulldozer_collective.shared.IntegrationPlatform;
import com.bulldozer_collective.shared.jpa.ProjectAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "crm_contacts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "project_id", "platform", "source_id"})
)
public class CrmContactEntity extends ProjectAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationPlatform platform;

    @Column(nullable = false)
    private String sourceId;

    /**
     * Marks the sync pass that last saw this record in the CRM. Set to the pass's start instant on every
     * upsert, then used to soft-delete whatever the pass did not see — which replaces passing the whole set of
     * seen source ids as an {@code IN} list (PostgreSQL caps a statement at 32767 bind parameters, and a real
     * portal holds more objects than that).
     */
    @Nullable
    @Column
    private Instant lastSeenAt;

    /**
     * Id of the canonical Bulldozer {@code Contact} this mirror row resolves to, in the contacts module.
     * Null until the linking step runs, or when the CRM record carries too little identity to create a
     * contact (the contacts module requires a first and last name).
     */
    @Nullable
    @Column
    private UUID contactId;

    @Nullable
    @Column
    private String firstName;

    @Nullable
    @Column
    private String lastName;

    @Nullable
    @Column
    private String email;

    @Nullable
    @Column
    private String phone;

    @Nullable
    @Column
    private String jobTitle;

    @Nullable
    @Column
    private String linkedInUrl;
}
