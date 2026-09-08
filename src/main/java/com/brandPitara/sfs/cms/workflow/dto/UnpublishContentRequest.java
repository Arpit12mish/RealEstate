package com.brandPitara.sfs.cms.workflow.dto;

import jakarta.validation.constraints.NotNull;

public record UnpublishContentRequest(@NotNull Long version) {
}
