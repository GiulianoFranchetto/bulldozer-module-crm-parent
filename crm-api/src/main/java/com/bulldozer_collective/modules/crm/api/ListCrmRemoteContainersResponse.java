package com.bulldozer_collective.modules.crm.api;

import org.jspecify.annotations.NonNull;

import java.util.List;

public record ListCrmRemoteContainersResponse(@NonNull List<CrmRemoteContainer> containers) {
}
