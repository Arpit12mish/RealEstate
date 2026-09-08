package com.brandPitara.sfs.publiccontent.repository;

public interface PublicCategorySummaryView {
    Long getId();
    String getName();
    String getSlug();
    String getDescription();
    Long getPublishedContentCount();
}
