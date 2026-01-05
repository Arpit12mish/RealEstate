package com.brandPitara.sfs.request.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ServiceRequestResponse(
        Long id,
        String status,
        OffsetDateTime createdAt,
        City city,
        String locality,
        String pincode,
        String notes,
        List<Category> categories,
        Customer customer
) {
    public record City(Long id, String name, String state) {}
    public record Category(Long id, String name) {}
    public record Customer(Long id, String name, String maskedPhone) {}
}
