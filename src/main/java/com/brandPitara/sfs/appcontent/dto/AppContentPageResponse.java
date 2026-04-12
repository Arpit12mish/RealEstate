package com.brandPitara.sfs.appcontent.dto;

import com.brandPitara.sfs.appcontent.enums.AppContentType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppContentPageResponse {
    private String slug;
    private String title;
    private String content;
    private AppContentType contentType;
}