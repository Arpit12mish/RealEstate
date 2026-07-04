package com.brandPitara.sfs.builderhighlight.dto.request;

import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightPointType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderHighlightPointRequest {

    @NotNull
    private BuilderHighlightPointType pointType;

    @Size(max = 180)
    private String title;

    @Size(max = 2000)
    private String text;

    @Size(max = 80)
    private String iconKey;

    @Min(0)
    private Integer displayOrder;

    private Boolean active;
}
