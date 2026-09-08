package com.brandPitara.sfs.project.service.reader;

import com.brandPitara.sfs.enums.FavoriteTargetType;
import com.brandPitara.sfs.repository.UserFavoriteRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectFavoriteMembershipReaderTest {

    @Test
    void usesOneMembershipQueryForTheResolvedViewer() {
        UserFavoriteRepository repository = mock(UserFavoriteRepository.class);
        ProjectFavoriteMembershipReader reader = new ProjectFavoriteMembershipReader(repository);
        when(repository.findFavoritedTargetIds(77L, FavoriteTargetType.PROJECT, List.of(27L)))
                .thenReturn(List.of(27L));

        assertThat(reader.isFavorite(77L, 27L)).isTrue();
        verify(repository).findFavoritedTargetIds(
                77L, FavoriteTargetType.PROJECT, List.of(27L));
    }
}
