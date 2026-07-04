package com.brandPitara.sfs.instagram.service;

import com.brandPitara.sfs.instagram.dto.InstagramReelSyncResult;

public interface InstagramReelSyncService {
    InstagramReelSyncResult syncLatestReels();
    InstagramReelSyncResult syncOneMedia(String mediaId);
    void recalculateTrendingScores();
}
