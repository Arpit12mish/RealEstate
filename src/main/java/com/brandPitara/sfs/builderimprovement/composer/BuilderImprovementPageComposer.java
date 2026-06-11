package com.brandPitara.sfs.builderimprovement.composer;

import com.brandPitara.sfs.builderimprovement.dto.response.BuilderImprovementResponse;
import com.brandPitara.sfs.builderimprovement.entity.*;

import java.util.List;

public interface BuilderImprovementPageComposer {

    BuilderImprovementResponse compose(
            BuilderImprovementProfileEntity profile,
            List<BuilderImprovementActionEntity> actions,
            List<BuilderImprovementIssueEntity> issues,
            List<BuilderAfterSalesUpgradeEntity> afterSalesUpgrades,
            List<BuilderImprovementTimelineEntity> timeline
    );
}
