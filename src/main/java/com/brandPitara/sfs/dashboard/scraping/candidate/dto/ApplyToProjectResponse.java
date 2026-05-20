package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ApplyToProjectResponse {

    private boolean createdProject;
    private Long projectId;
    private Long candidateId;
    private int fieldsApplied;
    private int fieldsSkipped;
    private int complianceItemsCreated;
    private int complianceItemsSkipped;
    private List<String> meterSectionsCreated;
    private List<String> warnings;
}
