package com.brandPitara.sfs.provider.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Full replacement of a worker's services, in display order. */
public record WorkerServicesReplaceRequest(
        @NotNull @Size(max = 30) List<@NotBlank @Size(max = 120) String> services
) {
}
