package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.UserFavoriteEntity;
import com.brandPitara.sfs.enums.FavoriteTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserFavoriteRepository extends JpaRepository<UserFavoriteEntity, Long> {

    boolean existsByUser_IdAndTargetTypeAndTargetId(
            Long userId,
            FavoriteTargetType targetType,
            Long targetId
    );

    Optional<UserFavoriteEntity> findByUser_IdAndTargetTypeAndTargetId(
            Long userId,
            FavoriteTargetType targetType,
            Long targetId
    );

    void deleteByUser_IdAndTargetTypeAndTargetId(
            Long userId,
            FavoriteTargetType targetType,
            Long targetId
    );

    @Modifying
    @Query("DELETE FROM UserFavoriteEntity uf WHERE uf.user.id = :userId")
    void deleteAllByUserId(Long userId);

    long countByTargetTypeAndTargetId(FavoriteTargetType targetType, Long targetId);

    @Query("""
        select uf.targetId, count(uf.id)
        from UserFavoriteEntity uf
        where uf.targetType = :targetType
          and uf.targetId in :targetIds
        group by uf.targetId
    """)
    List<Object[]> countByTargetTypeAndTargetIds(
            FavoriteTargetType targetType,
            Collection<Long> targetIds
    );

    @Query("""
        select uf.targetId
        from UserFavoriteEntity uf
        where uf.user.id = :userId
          and uf.targetType = :targetType
          and uf.targetId in :targetIds
    """)
    List<Long> findFavoritedTargetIds(
            Long userId,
            FavoriteTargetType targetType,
            Collection<Long> targetIds
    );

    List<UserFavoriteEntity> findByUser_IdAndTargetTypeOrderByCreatedAtDesc(
            Long userId,
            FavoriteTargetType targetType
    );
}