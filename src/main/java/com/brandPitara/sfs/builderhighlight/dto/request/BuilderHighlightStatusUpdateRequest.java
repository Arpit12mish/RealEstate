package com.brandPitara.sfs.builderhighlight.dto.request;

import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderHighlightStatusUpdateRequest {

    @NotNull
    private BuilderHighlightStatus status;

    private Boolean publicVisible;
}
