package com.brandPitara.sfs.builderhighlight.enums;

public enum BuilderHighlightType {
    BUILDER_UPDATE("Builder Updates", "Official words from builder"),
    SOCIAL_IMPACT("Social Impact", "Corporate responsibility"),
    NEWS_ARTICLE("News & Articles", "Press & editorial"),
    SFS_ANALYSIS("SFS Builder Analysis", "Financial and trust highlights");

    private final String sectionTitle;
    private final String sectionSubtitle;

    BuilderHighlightType(String sectionTitle, String sectionSubtitle) {
        this.sectionTitle = sectionTitle;
        this.sectionSubtitle = sectionSubtitle;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public String getSectionSubtitle() {
        return sectionSubtitle;
    }
}
