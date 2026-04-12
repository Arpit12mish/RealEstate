package com.brandPitara.sfs.appcontent.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppShareLinksResponse {
    private String androidUrl;
    private String iosUrl;
}