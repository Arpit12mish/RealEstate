package com.brandPitara.sfs.feed.service.section.impl;

import com.brandPitara.sfs.builder.dto.BuilderProjectCardDto;
import com.brandPitara.sfs.feed.entity.FeedSectionConfigEntity;
import com.brandPitara.sfs.feed.service.section.FeedContext;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BuilderProjectsSectionLoaderTest {

  @Test
  void loadEnrichesBuilderProjectCardsWithFavoriteFields() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    ProjectFavoriteService projectFavoriteService = mock(ProjectFavoriteService.class);
    BuilderProjectsSectionLoader loader = new BuilderProjectsSectionLoader(jdbcTemplate, projectFavoriteService);

    List<BuilderProjectCardDto> cards = List.of(BuilderProjectCardDto.builder()
        .id(101L)
        .name("Project 101")
        .build());

    when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(77L), eq(3)))
        .thenReturn(cards);

    var section = loader.load(
        FeedSectionConfigEntity.builder()
            .title("Recent projects")
            .maxItems(3)
            .build(),
        FeedContext.builder().entityId(77L).build()
    );

    assertThat(section).isNotNull();
    assertThat(section.getItems()).hasSize(1);
    assertThat(section.getItems().get(0)).isSameAs(cards.get(0));
    verify(projectFavoriteService).enrichBuilderProjectCards(cards);
  }
}
