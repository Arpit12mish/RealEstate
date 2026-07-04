package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.builder.dto.BuilderProjectCardDto;
import com.brandPitara.sfs.dbsearch.dto.SearchEntityType;
import com.brandPitara.sfs.dbsearch.dto.SearchItemDto;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.enums.FavoriteTargetType;
import com.brandPitara.sfs.home.dto.GenericCardDto;
import com.brandPitara.sfs.home.enums.HomeSectionItemType;
import com.brandPitara.sfs.project.dto.ProjectNearbyListingCardDto;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterCardResponse;
import com.brandPitara.sfs.repository.UserFavoriteRepository;
import com.brandPitara.sfs.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectFavoriteServiceImplTest {

    private final UserFavoriteRepository userFavoriteRepository = mock(UserFavoriteRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectMediaRepository projectMediaRepository = mock(ProjectMediaRepository.class);

    private final ProjectFavoriteServiceImpl service = new ProjectFavoriteServiceImpl(
            userFavoriteRepository,
            userRepository,
            projectRepository,
            projectMediaRepository
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void enrichNearbyListingCardsAddsCountsAndDefaultsFavoriteFalseWithoutAuth() {
        List<ProjectNearbyListingCardDto> cards = List.of(
                ProjectNearbyListingCardDto.builder().projectId(101L).build(),
                ProjectNearbyListingCardDto.builder().projectId(202L).build()
        );

        when(userFavoriteRepository.countByTargetTypeAndTargetIds(
                eq(FavoriteTargetType.PROJECT),
                anyCollection()
        )).thenReturn(List.<Object[]>of(new Object[]{101L, 3L}));

        service.enrichNearbyListingCards(cards);

        assertThat(cards.get(0).getFavoriteCount()).isEqualTo(3L);
        assertThat(cards.get(0).getIsFavorite()).isFalse();
        assertThat(cards.get(1).getFavoriteCount()).isZero();
        assertThat(cards.get(1).getIsFavorite()).isFalse();

        verify(userFavoriteRepository).countByTargetTypeAndTargetIds(eq(FavoriteTargetType.PROJECT), anyCollection());
        verify(userFavoriteRepository, never()).findFavoritedTargetIds(any(), any(), anyCollection());
        verify(userRepository, never()).findByPhoneNumber(anyString());
    }

    @Test
    void enrichProjectMeterCardsAddsCurrentUserFavoriteStatusInBatch() {
        String phone = "+919999999999";
        User user = new User();
        user.setId(7L);
        user.setPhoneNumber(phone);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(phone, null, List.of())
        );

        List<ProjectMeterCardResponse> cards = List.of(
                ProjectMeterCardResponse.builder().projectId(11L).build(),
                ProjectMeterCardResponse.builder().projectId(12L).build()
        );

        when(userRepository.findByPhoneNumber(phone)).thenReturn(Optional.of(user));
        when(userFavoriteRepository.countByTargetTypeAndTargetIds(
                eq(FavoriteTargetType.PROJECT),
                anyCollection()
        )).thenReturn(List.<Object[]>of(
                new Object[]{11L, 1L},
                new Object[]{12L, 4L}
        ));
        when(userFavoriteRepository.findFavoritedTargetIds(
                eq(7L),
                eq(FavoriteTargetType.PROJECT),
                anyCollection()
        )).thenReturn(List.of(12L));

        service.enrichProjectMeterCards(cards);

        assertThat(cards.get(0).getFavoriteCount()).isEqualTo(1L);
        assertThat(cards.get(0).getIsFavorite()).isFalse();
        assertThat(cards.get(1).getFavoriteCount()).isEqualTo(4L);
        assertThat(cards.get(1).getIsFavorite()).isTrue();

        verify(userFavoriteRepository).countByTargetTypeAndTargetIds(eq(FavoriteTargetType.PROJECT), anyCollection());
        verify(userFavoriteRepository).findFavoritedTargetIds(eq(7L), eq(FavoriteTargetType.PROJECT), anyCollection());
    }

    @Test
    void enrichBuilderProjectCardsUsesProjectIdFromCardId() {
        List<BuilderProjectCardDto> cards = List.of(
                BuilderProjectCardDto.builder().id(301L).build(),
                BuilderProjectCardDto.builder().id(302L).build()
        );

        when(userFavoriteRepository.countByTargetTypeAndTargetIds(
                eq(FavoriteTargetType.PROJECT),
                anyCollection()
        )).thenReturn(List.<Object[]>of(new Object[]{301L, 8L}));

        service.enrichBuilderProjectCards(cards);

        assertThat(cards.get(0).getFavoriteCount()).isEqualTo(8L);
        assertThat(cards.get(0).isFavorite()).isFalse();
        assertThat(cards.get(1).getFavoriteCount()).isZero();
        assertThat(cards.get(1).isFavorite()).isFalse();

        verify(userFavoriteRepository).countByTargetTypeAndTargetIds(eq(FavoriteTargetType.PROJECT), anyCollection());
        verify(userFavoriteRepository, never()).findFavoritedTargetIds(any(), any(), anyCollection());
    }

    @Test
    void enrichSearchProjectItemsOnlyTouchesProjectItems() {
        String phone = "+919999999999";
        User user = new User();
        user.setId(7L);
        user.setPhoneNumber(phone);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(phone, null, List.of())
        );

        List<SearchItemDto> items = List.of(
                SearchItemDto.builder().id(401L).entityType(SearchEntityType.PROJECT).build(),
                SearchItemDto.builder().id(501L).entityType(SearchEntityType.BUILDER).build()
        );

        when(userRepository.findByPhoneNumber(phone)).thenReturn(Optional.of(user));
        when(userFavoriteRepository.countByTargetTypeAndTargetIds(
                eq(FavoriteTargetType.PROJECT),
                anyCollection()
        )).thenReturn(List.<Object[]>of(new Object[]{401L, 2L}));
        when(userFavoriteRepository.findFavoritedTargetIds(
                eq(7L),
                eq(FavoriteTargetType.PROJECT),
                anyCollection()
        )).thenReturn(List.of(401L));

        service.enrichSearchProjectItems(items);

        assertThat(items.get(0).getFavoriteCount()).isEqualTo(2L);
        assertThat(items.get(0).isFavorite()).isTrue();
        assertThat(items.get(1).getFavoriteCount()).isZero();
        assertThat(items.get(1).isFavorite()).isFalse();
    }

    @Test
    void enrichGenericProjectCardsUsesRefIdOnlyForProjectCards() {
        List<GenericCardDto> cards = List.of(
                GenericCardDto.builder().refId(601L).itemType(HomeSectionItemType.PROJECT).build(),
                GenericCardDto.builder().refId(701L).itemType(HomeSectionItemType.BUILDER).build()
        );

        when(userFavoriteRepository.countByTargetTypeAndTargetIds(
                eq(FavoriteTargetType.PROJECT),
                anyCollection()
        )).thenReturn(List.<Object[]>of(new Object[]{601L, 5L}));

        service.enrichGenericProjectCards(cards);

        assertThat(cards.get(0).getFavoriteCount()).isEqualTo(5L);
        assertThat(cards.get(0).isFavorite()).isFalse();
        assertThat(cards.get(1).getFavoriteCount()).isZero();
        assertThat(cards.get(1).isFavorite()).isFalse();

        verify(userFavoriteRepository).countByTargetTypeAndTargetIds(eq(FavoriteTargetType.PROJECT), anyCollection());
        verify(userFavoriteRepository, never()).findFavoritedTargetIds(any(), any(), anyCollection());
    }
}
