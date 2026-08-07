package com.bulldozer_collective.modules.crm.contacts.services;

import com.bulldozer_collective.contacts.api.CreateContactJobRequest;
import com.bulldozer_collective.contacts.api.CreateContactRequest;
import com.bulldozer_collective.contacts.api.ListContactsResponse;
import com.bulldozer_collective.contacts.api.clients.InternalCreateContactClient;
import com.bulldozer_collective.contacts.api.clients.InternalCreateContactJobClient;
import com.bulldozer_collective.contacts.api.clients.InternalListContactsClient;
import com.bulldozer_collective.logging.BulldozerLogger;
import com.bulldozer_collective.modules.crm.contacts.data.CrmContactEntity;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.bulldozer_collective.modules.crm.configurations.CrmLogContext.op;

/**
 * Resolves a mirrored CRM contact to the canonical Bulldozer contact: searches by email, then LinkedIn URL,
 * and only creates one when nothing matches — so a CRM sync never duplicates a contact created by outbound,
 * a lead list or another CRM. Talks to the contacts module over its internal HTTP clients only.
 *
 * <p>Returns {@code null} when the CRM record has no usable name: the contacts module requires a first and a
 * last name. That is a normal outcome — the mirror row stays unlinked and keeps whatever the CRM holds.
 *
 * <p>Symmetric to {@code module-outbound}'s {@code ContactResolver}.
 */
@Service
@RequiredArgsConstructor
public class CrmContactResolver {

    private final BulldozerLogger logger;

    public @Nullable UUID resolve(@NonNull UUID customerId,
                                  @NonNull CrmContactEntity contact,
                                  @Nullable UUID companyId) {
        var existing = this.findExisting(customerId, contact);
        if (existing != null) {
            this.logger.debug(op("CRM-004", customerId, null)
                    + "Reusing existing contact %s for CRM contact %s", existing, contact.getSourceId());
            this.recordJob(customerId, existing, contact, companyId);
            return existing;
        }

        if (isBlank(contact.getFirstName()) && isBlank(contact.getLastName())) {
            this.logger.warn(op("CRM-004", customerId, null)
                    + "CRM contact %s has no name, cannot link it to a Bulldozer contact",
                    contact.getSourceId());
            return null;
        }

        var response = InternalCreateContactClient.builder()
                .customerId(customerId)
                .request(CreateContactRequest.builder()
                        .companyId(companyId)
                        // Both names are mandatory on the contacts side; a CRM record often fills only one.
                        .firstName(isBlank(contact.getFirstName()) ? "" : contact.getFirstName())
                        .lastName(isBlank(contact.getLastName()) ? "" : contact.getLastName())
                        .email(contact.getEmail())
                        .phone(contact.getPhone())
                        .linkedInUrl(contact.getLinkedInUrl())
                        .build())
                .build()
                .execute();

        if (response == null || response.id() == null) {
            this.logger.warn(op("CRM-004", customerId, null)
                    + "Contact creation returned no id for CRM contact %s", contact.getSourceId());
            return null;
        }

        this.logger.debug(op("CRM-004", customerId, null)
                + "Created contact %s for CRM contact %s", response.id(), contact.getSourceId());
        this.recordJob(customerId, response.id(), contact, companyId);
        return response.id();
    }

    /**
     * Records the CRM job title as a contact job. The contacts module deduplicates on
     * (contact, company, title), so calling this again for a known job is a no-op.
     */
    private void recordJob(@NonNull UUID customerId,
                           @NonNull UUID contactId,
                           @NonNull CrmContactEntity contact,
                           @Nullable UUID companyId) {
        if (isBlank(contact.getJobTitle())) {
            return;
        }
        InternalCreateContactJobClient.builder()
                .customerId(customerId)
                .contactId(contactId)
                .request(CreateContactJobRequest.builder()
                        .title(contact.getJobTitle())
                        .companyId(companyId)
                        .build())
                .build()
                .execute();
    }

    private @Nullable UUID findExisting(@NonNull UUID customerId, @NonNull CrmContactEntity contact) {
        if (!isBlank(contact.getEmail())) {
            var found = this.searchByEmail(customerId, contact.getEmail());
            if (found != null) {
                return found;
            }
        }
        if (!isBlank(contact.getLinkedInUrl())) {
            return this.searchByLinkedIn(customerId, contact.getLinkedInUrl());
        }
        return null;
    }

    private @Nullable UUID searchByEmail(@NonNull UUID customerId, @NonNull String email) {
        var response = InternalListContactsClient.builder()
                .customerId(customerId)
                .email(email)
                .build()
                .execute();
        return firstId(response);
    }

    private @Nullable UUID searchByLinkedIn(@NonNull UUID customerId, @NonNull String linkedInUrl) {
        var response = InternalListContactsClient.builder()
                .customerId(customerId)
                .linkedInUrl(linkedInUrl)
                .build()
                .execute();
        return firstId(response);
    }

    private static @Nullable UUID firstId(@Nullable ListContactsResponse response) {
        if (response == null || response.contacts().getContent().isEmpty()) {
            return null;
        }
        return response.contacts().getContent().get(0).id();
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
