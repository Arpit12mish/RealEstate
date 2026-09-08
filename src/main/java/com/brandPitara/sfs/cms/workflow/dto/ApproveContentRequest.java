package com.brandPitara.sfs.cms.workflow.dto;

import jakarta.validation.constraints.NotNull;

public record ApproveContentRequest(@NotNull Long version) {
}
