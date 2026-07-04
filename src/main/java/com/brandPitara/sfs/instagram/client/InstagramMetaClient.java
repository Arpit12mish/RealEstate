package com.brandPitara.sfs.instagram.client;

import java.util.List;

public interface InstagramMetaClient {
    List<InstagramMetaMedia> fetchMedia();
    InstagramMetaInsights fetchInsights(String mediaId);
}
