package com.brandPitara.sfs.appcontent.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileAppContentResponse {
    private AppShareLinksResponse shareLinks;
    private ContactUsResponse contactUs;
}