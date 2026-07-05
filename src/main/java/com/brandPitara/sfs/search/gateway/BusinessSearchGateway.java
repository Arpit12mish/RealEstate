package com.brandPitara.sfs.search.gateway;

import com.brandPitara.sfs.search.model.BusinessSearchDocument;

import java.util.List;

/**
 * App-owned port for the business search index. BusinessSearchServiceImpl
 * depends on this, not on ElasticsearchClient directly - all raw query/sort
 * DSL construction and index/delete calls live in
 * {@link com.brandPitara.sfs.search.gateway.impl.ElasticsearchBusinessSearchGateway}.
 * <p>
 * Exceptions are intentionally NOT swallowed here - the caller (which already
 * knows whether a fallback is safe/appropriate) decides that, exactly as it
 * did before this port existed.
 */
public interface BusinessSearchGateway {

    void indexBusiness(BusinessSearchDocument document) throws Exception;

    void deleteBusiness(Long businessId) throws Exception;

    /** Returns matched business IDs in ranked order. */
    List<Long> search(BusinessSearchQuery query) throws Exception;
}
