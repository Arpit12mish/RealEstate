package com.brandPitara.sfs.search.gateway.impl;

import com.brandPitara.sfs.search.gateway.BusinessSearchGateway;
import com.brandPitara.sfs.search.gateway.BusinessSearchQuery;
import com.brandPitara.sfs.search.model.BusinessSearchDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Satisfies the application-owned search port without constructing any
 * Elasticsearch client while search is disabled. BusinessSearchServiceImpl
 * uses its database fallback before invoking this gateway.
 */
@Service
@ConditionalOnProperty(
        prefix = "sfs.search",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class DisabledBusinessSearchGateway implements BusinessSearchGateway {

    @Override
    public void indexBusiness(BusinessSearchDocument document) {
        // Disabled by configuration; intentionally no external side effect.
    }

    @Override
    public void deleteBusiness(Long businessId) {
        // Disabled by configuration; intentionally no external side effect.
    }

    @Override
    public List<Long> search(BusinessSearchQuery query) {
        return List.of();
    }
}
