package com.brandPitara.sfs.project.service.reader;

import com.brandPitara.sfs.enums.FavoriteTargetType;
import com.brandPitara.sfs.repository.UserFavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Opens a database transaction only after the caller has resolved an authenticated viewer. */
@Service
@RequiredArgsConstructor
public class ProjectFavoriteMembershipReader {

    private final UserFavoriteRepository userFavoriteRepository;

    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long projectId) {
        return userFavoriteRepository.findFavoritedTargetIds(
                userId,
                FavoriteTargetType.PROJECT,
                List.of(projectId)
        ).contains(projectId);
    }
}
