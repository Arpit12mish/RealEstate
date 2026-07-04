package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.home.dto.GenericCardDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.entity.HomeSectionItemEntity;
import com.brandPitara.sfs.home.enums.HomeSectionItemType;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.repository.HomeSectionItemRepository;
import com.brandPitara.sfs.home.service.section.SectionContext;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenericCardSectionLoaderTest {

  @Test
  void loadEnrichesGenericProjectCardsWithFavoriteFields() {
    HomeSectionItemRepository itemRepository = mock(HomeSectionItemRepository.class);
    BrandRepository brandRepository = mock(BrandRepository.class);
    BuilderRepository builderRepository = mock(BuilderRepository.class);
    CompanyRepository companyRepository = mock(CompanyRepository.class);
    ProjectRepository projectRepository = mock(ProjectRepository.class);
    ProjectFavoriteService projectFavoriteService = mock(ProjectFavoriteService.class);

    GenericCardSectionLoader loader = new GenericCardSectionLoader(
        itemRepository,
        brandRepository,
        builderRepository,
        companyRepository,
        projectRepository,
        projectFavoriteService
    );

    HomeSectionConfigEntity config = HomeSectionConfigEntity.builder()
        .id(9L)
        .sectionType(HomeSectionType.GENERIC_CARDS)
        .title("Recommended")
        .param1("recommended")
        .maxItems(5)
        .build();

    HomeSectionItemEntity row = HomeSectionItemEntity.builder()
        .id(1001L)
        .config(config)
        .itemType(HomeSectionItemType.PROJECT)
        .refId(101L)
        .imageUrl("https://cdn.example/project.jpg")
        .build();

    BuilderEntity builder = BuilderEntity.builder()
        .id(55L)
        .name("Builder")
        .logoUrl("https://cdn.example/logo.jpg")
        .build();

    ProjectEntity project = ProjectEntity.builder()
        .id(101L)
        .name("Project 101")
        .builder(builder)
        .build();

    when(itemRepository.findByConfig_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(9L))
        .thenReturn(List.of(row));
    when(projectRepository.findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalse(List.of(101L)))
        .thenReturn(List.of(project));

    var section = loader.load(config, SectionContext.builder().categoryId(0L).build());

    assertThat(section.getItems()).hasSize(1);
    GenericCardDto card = (GenericCardDto) section.getItems().get(0);
    assertThat(card.getItemType()).isEqualTo(HomeSectionItemType.PROJECT);
    assertThat(card.getRefId()).isEqualTo(101L);
    verify(projectFavoriteService).enrichGenericProjectCards(List.of(card));
  }
}
