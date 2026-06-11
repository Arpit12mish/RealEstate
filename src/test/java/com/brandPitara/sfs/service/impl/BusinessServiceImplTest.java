package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.repository.BusinessEventRepository;
import com.brandPitara.sfs.repository.BusinessRepository;
import com.brandPitara.sfs.repository.CategoryRepository;
import com.brandPitara.sfs.repository.CityRepository;
import com.brandPitara.sfs.repository.FavoriteRepository;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.search.BusinessSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessServiceImplTest {

    @Mock private BusinessRepository businessRepository;
    @Mock private CityRepository cityRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private BusinessEventRepository businessEventRepository;
    @Mock private FavoriteRepository favoriteRepository;
    @Mock private UserRepository userRepository;
    @Mock private BusinessSearchService businessSearchService;

    @InjectMocks private BusinessServiceImpl service;

    private static final Pageable PAGE = PageRequest.of(0, 20);
    private static final Page<BusinessEntity> EMPTY_PAGE = new PageImpl<>(List.of());

    // ── single-char query → treated as no filter, LIKE not called ─────────────

    @Test
    void listBusinesses_treatsOneCharQuery_asNoFilter() {
        when(businessRepository.findByCity_IdAndActiveTrue(anyLong(), any()))
                .thenReturn(EMPTY_PAGE);

        service.listBusinesses(1L, null, "a", PAGE);

        verify(businessRepository, never())
                .findByCity_IdAndActiveTrueAndNameContainingIgnoreCase(anyLong(), anyString(), any());
        verify(businessRepository).findByCity_IdAndActiveTrue(1L, PAGE);
    }

    @Test
    void listBusinesses_treatsOneCharQuery_asNoFilter_withCategory() {
        when(businessRepository.findByCity_IdAndCategory_IdAndActiveTrue(anyLong(), anyLong(), any()))
                .thenReturn(EMPTY_PAGE);

        service.listBusinesses(1L, 5L, "x", PAGE);

        verify(businessRepository, never())
                .findByCity_IdAndCategory_IdAndActiveTrueAndNameContainingIgnoreCase(
                        anyLong(), anyLong(), anyString(), any());
        verify(businessRepository).findByCity_IdAndCategory_IdAndActiveTrue(1L, 5L, PAGE);
    }

    // ── blank / whitespace-only query → no LIKE ───────────────────────────────

    @Test
    void listBusinesses_treatsBlankedQuery_asNoFilter() {
        when(businessRepository.findByCity_IdAndActiveTrue(anyLong(), any()))
                .thenReturn(EMPTY_PAGE);

        service.listBusinesses(1L, null, "   ", PAGE);

        verify(businessRepository, never())
                .findByCity_IdAndActiveTrueAndNameContainingIgnoreCase(anyLong(), anyString(), any());
        verify(businessRepository).findByCity_IdAndActiveTrue(1L, PAGE);
    }

    // ── null query → no LIKE ──────────────────────────────────────────────────

    @Test
    void listBusinesses_treatsNullQuery_asNoFilter() {
        when(businessRepository.findByCity_IdAndActiveTrue(anyLong(), any()))
                .thenReturn(EMPTY_PAGE);

        service.listBusinesses(1L, null, null, PAGE);

        verify(businessRepository, never())
                .findByCity_IdAndActiveTrueAndNameContainingIgnoreCase(anyLong(), anyString(), any());
        verify(businessRepository).findByCity_IdAndActiveTrue(1L, PAGE);
    }

    // ── trailing space + single char trims to length 1 → no LIKE ─────────────

    @Test
    void listBusinesses_trimsQueryBeforeLengthCheck() {
        when(businessRepository.findByCity_IdAndActiveTrue(anyLong(), any()))
                .thenReturn(EMPTY_PAGE);

        service.listBusinesses(1L, null, "a ", PAGE);

        verify(businessRepository, never())
                .findByCity_IdAndActiveTrueAndNameContainingIgnoreCase(anyLong(), anyString(), any());
    }

    // ── 2-char query → LIKE is called ────────────────────────────────────────

    @Test
    void listBusinesses_usesLikeQuery_forTwoCharQuery() {
        when(businessRepository.findByCity_IdAndActiveTrueAndNameContainingIgnoreCase(
                anyLong(), anyString(), any()))
                .thenReturn(EMPTY_PAGE);

        service.listBusinesses(1L, null, "dl", PAGE);

        verify(businessRepository)
                .findByCity_IdAndActiveTrueAndNameContainingIgnoreCase(1L, "dl", PAGE);
    }

    @Test
    void listBusinesses_trimsQueryBeforePassingToLike() {
        when(businessRepository.findByCity_IdAndActiveTrueAndNameContainingIgnoreCase(
                anyLong(), anyString(), any()))
                .thenReturn(EMPTY_PAGE);

        service.listBusinesses(1L, null, "  dlf  ", PAGE);

        verify(businessRepository)
                .findByCity_IdAndActiveTrueAndNameContainingIgnoreCase(1L, "dlf", PAGE);
    }
}
