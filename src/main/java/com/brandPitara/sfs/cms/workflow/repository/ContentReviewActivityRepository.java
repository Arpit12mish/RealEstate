package com.brandPitara.sfs.cms.workflow.repository;

import com.brandPitara.sfs.cms.workflow.dto.ContentWorkflowActivityResponse;
import com.brandPitara.sfs.cms.workflow.entity.ContentReviewActivityEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentReviewActivityRepository extends JpaRepository<ContentReviewActivityEntity, Long> {

    @Query("""
            select new com.brandPitara.sfs.cms.workflow.dto.ContentWorkflowActivityResponse(
                a.id, revision.id, revision.revisionNumber, a.action, a.comment,
                actor.id, actor.name, a.createdAt
            )
            from ContentReviewActivityEntity a
            join a.actor actor
            left join a.revision revision
            where a.contentPost.id = :postId
            """)
    Page<ContentWorkflowActivityResponse> findHistoryByPostId(
            @Param("postId") Long postId,
            Pageable pageable
    );
}
