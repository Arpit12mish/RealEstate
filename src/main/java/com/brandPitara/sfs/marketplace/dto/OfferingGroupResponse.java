package com.brandPitara.sfs.marketplace.dto;

import java.util.List;

public record OfferingGroupResponse(Long id, String title, List<NamedItemResponse> items) {
}
