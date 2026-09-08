package com.brandPitara.sfs.cms.workflow.repository;

import com.brandPitara.sfs.cms.workflow.dto.ContentRevisionListResponse;
import com.brandPitara.sfs.cms.workflow.entity.ContentPostRevisionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContentPostRevisionRepository extends JpaRepository<ContentPostRevisionEntity, Long> {

    @Query("select coalesce(max(r.revisionNumber), 0) from ContentPostRevisionEntity r where r.contentPost.id = :postId")
    int findMaximumRevisionNumber(@Param("postId") Long postId);

    @Query("""
            select new com.brandPitara.sfs.cms.workflow.dto.ContentRevisionListResponse(
                r.id, r.revisionNumber, r.contentType, r.title, r.slug,
                r.createdFromPostVersion, r.revisionReason,
                creator.id, creator.name, r.createdAt
            )
            from ContentPostRevisionEntity r
            join r.createdBy creator
            where r.contentPost.id = :postId
            """)
    Page<ContentRevisionListResponse> findListByPostId(
            @Param("postId") Long postId,
            Pageable pageable
    );

    @Query("""
            select r from ContentPostRevisionEntity r
            join fetch r.contentPost post
            join fetch r.createdBy creator
            where r.id = :revisionId and post.id = :postId
            """)
    Optional<ContentPostRevisionEntity> findDetailed(
            @Param("postId") Long postId,
            @Param("revisionId") Long revisionId
    );
}
