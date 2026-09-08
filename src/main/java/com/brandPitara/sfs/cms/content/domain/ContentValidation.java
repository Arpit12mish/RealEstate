package com.brandPitara.sfs.cms.content.domain;

public final class ContentValidation {

    public static final int TITLE_MIN = 3;
    public static final int TITLE_MAX = 220;
    public static final int SLUG_MAX = 180;
    public static final int EXCERPT_MAX = 500;
    public static final int SEO_TITLE_MAX = 200;
    public static final int SEO_DESCRIPTION_MAX = 500;
    public static final int CANONICAL_URL_MAX = 2048;
    public static final int READING_TIME_MIN = 1;
    public static final int READING_TIME_MAX = 180;
    public static final String NO_CONTROL_CHARACTERS = "^[^\\p{Cc}\\p{Cf}]*$";

    private ContentValidation() {
    }
}
