package com.brandPitara.sfs.dashboard.validator;

import com.brandPitara.sfs.dashboard.projectmeter.dto.DashboardProjectCostBreakdownRequest;
import com.brandPitara.sfs.dashboard.projectmeter.dto.DashboardProjectConstructionStageRequest;
import com.brandPitara.sfs.dashboard.projectmeter.dto.DashboardProjectLandUtilizationRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DashboardProjectMeterValidator {

    public void validateConstructionStage(DashboardProjectConstructionStageRequest request) {
        validateDateRange(request.getPlannedStartDate(), request.getPlannedEndDate(), "Planned");
        validateDateRange(request.getActualStartDate(), request.getActualEndDate(), "Actual");
    }

    public void validateCostBreakdown(DashboardProjectCostBreakdownRequest request) {
        if (request.getTotalCost() == null) return;

        long partsSum = 0L;
        if (request.getLandCost() != null)           partsSum += request.getLandCost();
        if (request.getConstructionCost() != null)   partsSum += request.getConstructionCost();
        if (request.getInfrastructureCost() != null) partsSum += request.getInfrastructureCost();
        if (request.getOtherCost() != null)          partsSum += request.getOtherCost();

        if (partsSum > request.getTotalCost()) {
            throw new IllegalArgumentException(
                "Sum of cost components (" + partsSum + ") exceeds totalCost (" + request.getTotalCost() + ")"
            );
        }
    }

    public void validateLandUtilization(DashboardProjectLandUtilizationRequest request) {
        Double total = request.getTotalLandAreaSqm();
        if (total == null || total <= 0) return;

        double partsSum = 0.0;
        if (request.getResidentialAreaSqm() != null)  partsSum += request.getResidentialAreaSqm();
        if (request.getCommercialAreaSqm() != null)   partsSum += request.getCommercialAreaSqm();
        if (request.getParksAreaSqm() != null)        partsSum += request.getParksAreaSqm();
        if (request.getOpenAreaSqm() != null)         partsSum += request.getOpenAreaSqm();
        if (request.getParkingAreaSqm() != null)      partsSum += request.getParkingAreaSqm();
        if (request.getUtilityAreaSqm() != null)      partsSum += request.getUtilityAreaSqm();

        if (partsSum > total * 1.001) {
            throw new IllegalArgumentException(
                "Sum of land area components (" + String.format("%.2f", partsSum)
                + " sqm) exceeds totalLandAreaSqm (" + total + " sqm)"
            );
        }
    }

    private void validateDateRange(LocalDate start, LocalDate end, String label) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException(
                label + " start date (" + start + ") must not be after end date (" + end + ")"
            );
        }
    }
}
