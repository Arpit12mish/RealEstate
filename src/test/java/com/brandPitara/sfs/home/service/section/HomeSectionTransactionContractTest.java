package com.brandPitara.sfs.home.service.section;

import com.brandPitara.sfs.home.dto.HomeFeedRequest;
import com.brandPitara.sfs.home.service.impl.HomeFeedServiceImpl;
import com.brandPitara.sfs.home.service.section.impl.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HomeSectionTransactionContractTest {

    @Test
    void homeCompositionOwnsNoBroadTransaction() throws Exception {
        assertThat(AnnotatedElementUtils.findMergedAnnotation(
            HomeFeedServiceImpl.class.getMethod("getHome", HomeFeedRequest.class), Transactional.class))
            .isNull();
        assertThat(AnnotatedElementUtils.findMergedAnnotation(
            HomeFeedServiceImpl.class.getMethod(
                "getHome", Long.class, Long.class, Long.class, Long.class), Transactional.class))
            .isNull();
    }

    @Test
    void databaseBackedLoadersOwnRequiredReadOnlyTransactions() {
        List<Class<?>> databaseBackedLoaders = List.of(
            ArchitectsAndDesignersSectionLoader.class,
            ArchitectsSectionLoader.class,
            BuilderCredibilityCardsSectionLoader.class,
            CompaniesSectionLoader.class,
            ConnectedBrandsSectionLoader.class,
            DesignersSectionLoader.class,
            FeaturedCarouselSectionLoader.class,
            GenericCardSectionLoader.class,
            InstagramReelsSectionLoader.class,
            NearbyListingsSectionLoader.class,
            ProjectAnalyticsSectionLoader.class,
            ProjectPlanSectionLoader.class,
            PromoBannersSectionLoader.class,
            TopBuildersSectionLoader.class,
            TopCategoriesSectionLoader.class,
            TopDistributorsSectionLoader.class,
            TopProjectsSectionLoader.class,
            TrendingCitiesSectionLoader.class
        );

        databaseBackedLoaders.forEach(loader -> {
            Transactional transaction = AnnotatedElementUtils.findMergedAnnotation(loader, Transactional.class);
            assertThat(transaction).as(loader.getSimpleName()).isNotNull();
            assertThat(transaction.readOnly()).as(loader.getSimpleName()).isTrue();
            assertThat(transaction.propagation()).as(loader.getSimpleName()).isEqualTo(Propagation.REQUIRED);
        });
    }

    @Test
    void pureLoadersOwnNoTransaction() {
        assertThat(AnnotatedElementUtils.findMergedAnnotation(
            ComparePropertiesSectionLoader.class, Transactional.class)).isNull();
        assertThat(AnnotatedElementUtils.findMergedAnnotation(
            SmartCalculatorsSectionLoader.class, Transactional.class)).isNull();
    }
}
