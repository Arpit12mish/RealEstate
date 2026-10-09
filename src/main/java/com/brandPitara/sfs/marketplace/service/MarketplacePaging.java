package com.brandPitara.sfs.marketplace.service;

import com.brandPitara.sfs.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public final class MarketplacePaging {

    public static final int MAX_PAGE_SIZE = 20;

    private MarketplacePaging() {
    }

    /** Bounded page request; ordering comes from the query itself unless a sort is given. */
    public static Pageable page(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    public static Pageable page(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);
    }

    public static <T> PageResponse<T> response(Page<?> source, List<T> content) {
        return PageResponse.<T>builder()
                .content(content)
                .page(source.getNumber())
                .size(source.getSize())
                .totalElements(source.getTotalElements())
                .totalPages(source.getTotalPages())
                .last(source.isLast())
                .build();
    }
}
