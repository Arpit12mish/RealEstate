package com.brandPitara.sfs.dashboard.validator;

import com.brandPitara.sfs.project.dto.ProjectUpsertRequest;
import org.springframework.stereotype.Service;

@Service
public class DashboardProjectValidator {

    public void validate(ProjectUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Project request body is required");
        }

        validatePriceRange(request);
        validateCoordinatesPaired(request);
    }

    private void validatePriceRange(ProjectUpsertRequest request) {
        Long priceMin = request.getPriceMin();
        Long priceMax = request.getPriceMax();

        if (priceMin != null && priceMax != null && priceMin > priceMax) {
            throw new IllegalArgumentException(
                "priceMin (" + priceMin + ") must not exceed priceMax (" + priceMax + ")"
            );
        }
    }

    private void validateCoordinatesPaired(ProjectUpsertRequest request) {
        boolean hasLat = request.getLatitude() != null;
        boolean hasLon = request.getLongitude() != null;

        if (hasLat != hasLon) {
            throw new IllegalArgumentException(
                "latitude and longitude must both be provided together or both omitted"
            );
        }
    }
}
