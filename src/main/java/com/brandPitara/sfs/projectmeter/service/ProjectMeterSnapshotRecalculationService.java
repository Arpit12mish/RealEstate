package com.brandPitara.sfs.projectmeter.service;

public interface ProjectMeterSnapshotRecalculationService {

    void recalculateSnapshot(Long projectId);

    void recalculateAllPublishedSnapshots();
}