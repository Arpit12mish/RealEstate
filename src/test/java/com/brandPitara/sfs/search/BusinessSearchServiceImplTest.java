package com.brandPitara.sfs.search;

import com.brandPitara.sfs.dto.BusinessResponse;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.repository.BusinessRepository;
import com.brandPitara.sfs.repository.FavoriteRepository;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.search.gateway.BusinessSearchGateway;
import com.brandPitara.sfs.search.gateway.BusinessSearchQuery;
import com.brandPitara.sfs.search.model.BusinessSearchDocument;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Proves BusinessSearchServiceImpl calls the BusinessSearchGateway port (not
 * ElasticsearchClient directly), preserves result ranking/order when mapping
 * gateway IDs back to entities, and preserves the "never crash, fall back to
 * DB" behavior on gateway failure - exactly as when ElasticsearchClient was
 * injected directly.
 */
class BusinessSearchServiceImplTest {

    private BusinessEntity business(Long id, String name) {
        return BusinessEntity.builder()
                .id(id)
                .name(name)
                .city(CityEntity.builder().id(1L).name("Delhi").state("Delhi").build())
                .category(CategoryEntity.builder().id(2L).name("Plumbing").slug("plumbing").build())
                .build();
    }

    private BusinessSearchServiceImpl service(BusinessSearchGateway gateway, BusinessRepository businessRepository, boolean searchEnabled) {
        BusinessSearchServiceImpl service = new BusinessSearchServiceImpl(
                gateway,
                businessRepository,
                mock(FavoriteRepository.class),
                mock(UserRepository.class)
        );
        ReflectionTestUtils.setField(service, "searchEnabled", searchEnabled);
        return service;
    }

    @Test
    void searchDelegatesToGatewayAndPreservesRankedOrder() throws Exception {
        BusinessSearchGateway gateway = mock(BusinessSearchGateway.class);
        BusinessRepository businessRepository = mock(BusinessRepository.class);
        BusinessSearchServiceImpl service = service(gateway, businessRepository, true);

        // Gateway returns IDs in ranked order 20, 10 - the mapping must preserve this,
        // not the DB's natural findAllById order.
        when(gateway.search(any(BusinessSearchQuery.class))).thenReturn(List.of(20L, 10L));
        when(businessRepository.findAllById(List.of(20L, 10L)))
                .thenReturn(List.of(business(10L, "Ravi Plumbing"), business(20L, "Anita Plumbing")));

        List<BusinessResponse> result = service.search(1L, 2L, "plumb", null, null, 0, 10);

        assertThat(result).extracting(BusinessResponse::getId).containsExactly(20L, 10L);
        verify(gateway).search(any(BusinessSearchQuery.class));
        verifyNoMoreInteractions(gateway);
    }

    @Test
    void searchFallsBackToDbWhenGatewayThrows() throws Exception {
        BusinessSearchGateway gateway = mock(BusinessSearchGateway.class);
        BusinessRepository businessRepository = mock(BusinessRepository.class);
        BusinessSearchServiceImpl service = service(gateway, businessRepository, true);

        when(gateway.search(any(BusinessSearchQuery.class))).thenThrow(new RuntimeException("ES down"));
        when(businessRepository.findByCity_IdAndActiveTrue(eq(1L), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(business(10L, "Ravi Plumbing"))));

        List<BusinessResponse> result = service.search(1L, null, null, null, null, 0, 10);

        assertThat(result).extracting(BusinessResponse::getId).containsExactly(10L);
    }

    @Test
    void searchUsesDbFallbackDirectlyWhenSearchDisabled() {
        BusinessSearchGateway gateway = mock(BusinessSearchGateway.class);
        BusinessRepository businessRepository = mock(BusinessRepository.class);
        BusinessSearchServiceImpl service = service(gateway, businessRepository, false);

        when(businessRepository.findByCity_IdAndActiveTrue(eq(1L), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(business(10L, "Ravi Plumbing"))));

        List<BusinessResponse> result = service.search(1L, null, null, null, null, 0, 10);

        assertThat(result).extracting(BusinessResponse::getId).containsExactly(10L);
        verifyNoInteractions(gateway);
    }

    @Test
    void indexBusinessDelegatesToGatewayAndNeverThrowsWhenGatewayFails() throws Exception {
        BusinessSearchGateway gateway = mock(BusinessSearchGateway.class);
        BusinessSearchServiceImpl service = service(gateway, mock(BusinessRepository.class), true);

        BusinessEntity entity = business(5L, "Test Business");
        doThrow(new RuntimeException("ES connection refused"))
                .when(gateway).indexBusiness(any(BusinessSearchDocument.class));

        // Must not throw - indexing failure should never crash the save flow.
        service.indexBusiness(entity);

        verify(gateway).indexBusiness(argThat(doc -> doc.getId().equals(5L)));
    }

    @Test
    void indexBusinessSkipsGatewayWhenSearchDisabled() {
        BusinessSearchGateway gateway = mock(BusinessSearchGateway.class);
        BusinessSearchServiceImpl service = service(gateway, mock(BusinessRepository.class), false);

        service.indexBusiness(business(5L, "Test Business"));

        verifyNoInteractions(gateway);
    }

    @Test
    void deleteBusinessDelegatesToGatewayAndNeverThrowsWhenGatewayFails() throws Exception {
        BusinessSearchGateway gateway = mock(BusinessSearchGateway.class);
        BusinessSearchServiceImpl service = service(gateway, mock(BusinessRepository.class), true);

        doThrow(new RuntimeException("ES down")).when(gateway).deleteBusiness(5L);

        service.deleteBusiness(5L);

        verify(gateway).deleteBusiness(5L);
    }
}
