package com.brandPitara.sfs.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private Long parentId;
    private Integer priority;
    private Boolean active;
}
