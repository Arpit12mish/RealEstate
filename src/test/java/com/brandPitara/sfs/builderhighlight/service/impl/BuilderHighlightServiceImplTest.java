package com.brandPitara.sfs.builderhighlight.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilitySummaryResponse;
import com.brandPitara.sfs.buildercredibility.service.BuilderCredibilityService;
import com.brandPitara.sfs.builderhighlight.dto.request.BuilderHighlightItemRequest;
import com.brandPitara.sfs.builderhighlight.dto.request.BuilderHighlightPointRequest;
import com.brandPitara.sfs.builderhighlight.dto.response.BuilderHighlightItemResponse;
import com.brandPitara.sfs.builderhighlight.entity.BuilderHighlightItemEntity;
import com.brandPitara.sfs.builderhighlight.entity.BuilderHighlightPointEntity;
import com.brandPitara.sfs.builderhighlight.enums.*;
import com.brandPitara.sfs.builderhighlight.mapper.BuilderHighlightMapper;
import com.brandPitara.sfs.builderhighlight.repository.BuilderHighlightItemRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.repository.CityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BuilderHighlightServiceImplTest {

    @Test
    void createDefaultsToDraftAndPublicHidden() {
        Fixture fx = fixture(DashboardRole.DATA_ENTRY);
        when(fx.builderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(publicBuilder()));
        when(fx.itemRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0), 100L));

        BuilderHighlightItemResponse response = fx.service.create(7L, basicRequest().build());

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(BuilderHighlightStatus.DRAFT);
        assertThat(response.getPublicVisible()).isFalse();
        assertThat(response.getActive()).isTrue();
        verify(fx.contentVersionService).bump("BUILDER_HIGHLIGHT");
    }

    @Test
    void updateReplacesPoints() {
        Fixture fx = fixture(DashboardRole.ADMIN);
        BuilderHighlightItemEntity existing = item(101L, BuilderHighlightStatus.DRAFT, true, false, null);
        when(fx.builderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(publicBuilder()));
        when(fx.itemRepository.findByIdAndBuilder_IdAndDeletedAtIsNull(101L, 7L)).thenReturn(Optional.of(existing));
        when(fx.itemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BuilderHighlightItemResponse response = fx.service.update(7L, 101L, basicRequest()
            .title("Updated")
            .points(List.of(BuilderHighlightPointRequest.builder()
                .pointType(BuilderHighlightPointType.ADVANTAGE)
                .title("Good")
                .text("Improved handover communication")
                .displayOrder(0)
                .build()))
            .build());

        assertThat(response.getTitle()).isEqualTo("Updated");
        assertThat(response.getPoints()).hasSize(1);
        assertThat(response.getPoints().get(0).getPointType()).isEqualTo(BuilderHighlightPointType.ADVANTAGE);
    }

    @Test
    void softDeleteSetsDeletedAtAndHidesItem() {
        Fixture fx = fixture(DashboardRole.ADMIN);
        BuilderHighlightItemEntity existing = item(101L, BuilderHighlightStatus.PUBLISHED, true, true, null);
        when(fx.builderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(publicBuilder()));
        when(fx.itemRepository.findByIdAndBuilder_IdAndDeletedAtIsNull(101L, 7L)).thenReturn(Optional.of(existing));

        fx.service.softDelete(7L, 101L);

        assertThat(existing.getDeletedAt()).isNotNull();
        assertThat(existing.getActive()).isFalse();
        assertThat(existing.getPublicVisible()).isFalse();
        verify(fx.itemRepository).save(existing);
    }

    @Test
    void publicListReturnsOnlyRepositoryPublicQueryResultsWithSafePageSize() {
        Fixture fx = fixture(DashboardRole.ADMIN);
        when(fx.builderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(publicBuilder()));
        when(fx.itemRepository.findByBuilder_IdAndHighlightTypeAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(
            eq(7L),
            eq(BuilderHighlightType.NEWS_ARTICLE),
            eq(BuilderHighlightStatus.PUBLISHED),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(item(1L, BuilderHighlightStatus.PUBLISHED, true, true, null))));

        var page = fx.service.publicListItems(7L, BuilderHighlightType.NEWS_ARTICLE, PageRequest.of(0, 99));

        assertThat(page.getContent()).hasSize(1);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(fx.itemRepository).findByBuilder_IdAndHighlightTypeAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(
            eq(7L),
            eq(BuilderHighlightType.NEWS_ARTICLE),
            eq(BuilderHighlightStatus.PUBLISHED),
            pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void publicListResponseDoesNotSerializeAuditFields() throws Exception {
        Fixture fx = fixture(DashboardRole.ADMIN);
        when(fx.builderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(publicBuilder()));
        when(fx.itemRepository.findByBuilder_IdAndHighlightTypeAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(
            eq(7L),
            eq(BuilderHighlightType.NEWS_ARTICLE),
            eq(BuilderHighlightStatus.PUBLISHED),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(auditedPublicItem())));

        var page = fx.service.publicListItems(7L, BuilderHighlightType.NEWS_ARTICLE, PageRequest.of(0, 10));

        String json = objectMapper().writeValueAsString(page.getContent().get(0));
        assertThat(json)
            .contains("\"id\":201")
            .contains("\"title\":\"Audited public item\"")
            .doesNotContain("createdBy", "updatedBy", "approvedBy", "approvedAt", "deletedAt", "createdAt", "updatedAt");
    }

    @Test
    void publicDetailResponseDoesNotSerializeAuditFields() throws Exception {
        Fixture fx = fixture(DashboardRole.ADMIN);
        when(fx.builderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(publicBuilder()));
        when(fx.itemRepository.findByIdAndBuilder_IdAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(
            201L,
            7L,
            BuilderHighlightStatus.PUBLISHED
        )).thenReturn(Optional.of(auditedPublicItem()));

        var response = fx.service.publicGetItem(7L, 201L);

        String json = objectMapper().writeValueAsString(response);
        assertThat(json)
            .contains("\"id\":201")
            .contains("\"points\"")
            .doesNotContain("createdBy", "updatedBy", "approvedBy", "approvedAt", "deletedAt", "createdAt", "updatedAt");
    }

    @Test
    void dashboardResponseStillIncludesAuditFields() {
        Fixture fx = fixture(DashboardRole.ADMIN);
        BuilderHighlightItemEntity existing = auditedPublicItem();
        when(fx.builderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(publicBuilder()));
        when(fx.itemRepository.findByIdAndBuilder_IdAndDeletedAtIsNull(201L, 7L)).thenReturn(Optional.of(existing));

        BuilderHighlightItemResponse response = fx.service.dashboardGet(7L, 201L);

        assertThat(response.getCreatedBy()).isEqualTo(91L);
        assertThat(response.getUpdatedBy()).isEqualTo(92L);
        assertThat(response.getApprovedBy()).isEqualTo(93L);
        assertThat(response.getApprovedAt()).isNotNull();
        assertThat(response.getDeletedAt()).isNull();
    }

    @Test
    void publicDetailBlocksHiddenUnpublishedOrDeletedItem() {
        Fixture fx = fixture(DashboardRole.ADMIN);
        when(fx.builderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(publicBuilder()));
        when(fx.itemRepository.findByIdAndBuilder_IdAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(
            88L,
            7L,
            BuilderHighlightStatus.PUBLISHED
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fx.service.publicGetItem(7L, 88L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Builder highlight item not found");
    }

    @Test
    void publicScreenGroupsAllSections() {
        Fixture fx = fixture(DashboardRole.ADMIN);
        when(fx.builderCredibilityService.publicGetCredibilitySummary(7L)).thenReturn(BuilderCredibilitySummaryResponse.builder()
            .builderId(7L)
            .builderName("M3M Group")
            .builderLogoUrl("https://cdn.example.com/logo.webp")
            .credibilityScore(54)
            .credibilityLabel("Needs Caution")
            .build());
        when(fx.builderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(publicBuilder()));
        when(fx.itemRepository.findByBuilder_IdAndHighlightTypeAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(
            eq(7L),
            any(BuilderHighlightType.class),
            eq(BuilderHighlightStatus.PUBLISHED),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        var screen = fx.service.publicGetScreen(7L);

        assertThat(screen.getBuilder().getCredibilityScore()).isEqualTo(54);
        assertThat(screen.getSections())
            .extracting(section -> section.getType())
            .containsExactly(BuilderHighlightType.BUILDER_UPDATE, BuilderHighlightType.SOCIAL_IMPACT,
                BuilderHighlightType.NEWS_ARTICLE, BuilderHighlightType.SFS_ANALYSIS);
    }

    @Test
    void validationRejectsMissingTitleInvalidMediaAndMissingBuilder() {
        Fixture fx = fixture(DashboardRole.DATA_ENTRY);
        when(fx.builderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(publicBuilder()));

        assertThatThrownBy(() -> fx.service.create(7L, basicRequest().title(" ").build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("title is required");

        assertThatThrownBy(() -> fx.service.create(7L, basicRequest().mediaType(BuilderHighlightMediaType.WEBVIEW).build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("externalUrl is required");

        when(fx.builderRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> fx.service.create(99L, basicRequest().build()))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Builder not found");
    }

    private Fixture fixture(DashboardRole role) {
        BuilderRepository builderRepository = mock(BuilderRepository.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        CityRepository cityRepository = mock(CityRepository.class);
        BuilderHighlightItemRepository itemRepository = mock(BuilderHighlightItemRepository.class);
        BuilderCredibilityService builderCredibilityService = mock(BuilderCredibilityService.class);
        DashboardCurrentUserService currentUserService = mock(DashboardCurrentUserService.class);
        ContentVersionService contentVersionService = mock(ContentVersionService.class);
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(DashboardUserEntity.builder()
            .id(11L)
            .name("User")
            .role(role)
            .active(true)
            .build());

        BuilderHighlightServiceImpl service = new BuilderHighlightServiceImpl(
            builderRepository,
            projectRepository,
            cityRepository,
            itemRepository,
            new BuilderHighlightMapper(),
            builderCredibilityService,
            currentUserService,
            contentVersionService
        );
        return new Fixture(builderRepository, itemRepository, builderCredibilityService, contentVersionService, service);
    }

    private BuilderHighlightItemRequest.BuilderHighlightItemRequestBuilder basicRequest() {
        return BuilderHighlightItemRequest.builder()
            .highlightType(BuilderHighlightType.NEWS_ARTICLE)
            .sourceType(BuilderHighlightSourceType.SFS_EDITORIAL)
            .mediaType(BuilderHighlightMediaType.NONE)
            .title("Market update")
            .readTimeMinutes(3)
            .sortOrder(0);
    }

    private BuilderEntity publicBuilder() {
        return BuilderEntity.builder()
            .id(7L)
            .name("M3M Group")
            .logoUrl("https://cdn.example.com/logo.webp")
            .active(true)
            .published(true)
            .deleted(false)
            .build();
    }

    private BuilderHighlightItemEntity item(
        Long id,
        BuilderHighlightStatus status,
        boolean active,
        boolean publicVisible,
        OffsetDateTime deletedAt
    ) {
        return BuilderHighlightItemEntity.builder()
            .id(id)
            .builder(publicBuilder())
            .highlightType(BuilderHighlightType.NEWS_ARTICLE)
            .sourceType(BuilderHighlightSourceType.SFS_EDITORIAL)
            .mediaType(BuilderHighlightMediaType.NONE)
            .title("Market update")
            .status(status)
            .active(active)
            .publicVisible(publicVisible)
            .deletedAt(deletedAt)
            .readTimeMinutes(3)
            .sortOrder(0)
            .build();
    }

    private BuilderHighlightItemEntity auditedPublicItem() {
        BuilderHighlightItemEntity item = item(201L, BuilderHighlightStatus.PUBLISHED, true, true, null);
        item.setTitle("Audited public item");
        item.setCreatedBy(91L);
        item.setUpdatedBy(92L);
        item.setApprovedBy(93L);
        item.setApprovedAt(OffsetDateTime.parse("2026-07-01T10:00:00Z"));
        item.setPublishedAt(OffsetDateTime.parse("2026-07-01T09:00:00Z"));
        item.setYoutubeVideoId("jNQXAC9IVRw");
        item.getPoints().add(BuilderHighlightPointEntity.builder()
            .id(301L)
            .highlightItem(item)
            .pointType(BuilderHighlightPointType.ADVANTAGE)
            .title("Strong brand recall")
            .text("Visible in NCR corridors")
            .displayOrder(1)
            .active(true)
            .build());
        return item;
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private BuilderHighlightItemEntity withId(BuilderHighlightItemEntity entity, Long id) {
        entity.setId(id);
        return entity;
    }

    private record Fixture(
        BuilderRepository builderRepository,
        BuilderHighlightItemRepository itemRepository,
        BuilderCredibilityService builderCredibilityService,
        ContentVersionService contentVersionService,
        BuilderHighlightServiceImpl service
    ) {}
}
