package com.brandPitara.sfs.dashboard.project.dto;

import com.brandPitara.sfs.project.enums.PropertyType;
import lombok.*;

@Getter
@AllArgsConstructor
public class PropertyTypeMetaResponse {
    private PropertyType value;
    private String label;
    private String group;
}
