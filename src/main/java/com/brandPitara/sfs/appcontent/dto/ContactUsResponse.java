package com.brandPitara.sfs.appcontent.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContactUsResponse {
    private String email;
    private String phone;
    private String whatsapp;
}