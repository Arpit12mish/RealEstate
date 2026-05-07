package com.brandPitara.sfs.location.service.impl;

import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.location.dto.LocationResolveRequest;
import com.brandPitara.sfs.location.dto.LocationResolveResponse;
import com.brandPitara.sfs.location.service.LocationService;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {

    private static final double MAX_SERVICEABLE_CITY_DISTANCE_KM = 60.0;

    private final CityRepository cityRepository;
    private final ProjectRepository projectRepository;

    @Override
    public LocationResolveResponse resolve(LocationResolveRequest request) {
        double userLat = request.getLatitude();
        double userLon = request.getLongitude();

        String deviceCity = clean(request.getDeviceCity());

        if (deviceCity != null) {
            Optional<CityEntity> cityFromDevice =
                    cityRepository.findFirstByNameIgnoreCase(deviceCity);

            if (cityFromDevice.isPresent()) {
                return buildResponseForCity(
                        cityFromDevice.get(),
                        request,
                        0.0,
                        true
                );
            }

            return LocationResolveResponse.builder()
                    .serviceable(false)
                    .cityId(null)
                    .cityName(deviceCity)
                    .latitude(userLat)
                    .longitude(userLon)
                    .accuracyMeters(request.getAccuracyMeters())
                    .nearestCityDistanceKm(null)
                    .message("We are not serving projects in " + deviceCity + " yet.")
                    .build();
        }

        CityDistance nearestCity = cityRepository.findAllWithCoordinates()
                .stream()
                .map(city -> new CityDistance(
                        city,
                        calculateDistanceKm(
                                userLat,
                                userLon,
                                city.getLatitude(),
                                city.getLongitude()
                        )
                ))
                .min(Comparator.comparingDouble(CityDistance::distanceKm))
                .orElse(null);

        if (nearestCity == null) {
            return LocationResolveResponse.builder()
                    .serviceable(false)
                    .latitude(userLat)
                    .longitude(userLon)
                    .accuracyMeters(request.getAccuracyMeters())
                    .message("We are not serving projects in your location yet.")
                    .build();
        }

        boolean cityWithinServiceRange =
                nearestCity.distanceKm() <= MAX_SERVICEABLE_CITY_DISTANCE_KM;

        return buildResponseForCity(
                nearestCity.city(),
                request,
                nearestCity.distanceKm(),
                cityWithinServiceRange
        );
    }

    private LocationResolveResponse buildResponseForCity(
            CityEntity city,
            LocationResolveRequest request,
            double distanceKm,
            boolean cityWithinServiceRange
    ) {
        long projectCount = projectRepository
                .countByCityIdAndPublishedTrueAndActiveTrueAndDeletedFalse(city.getId());

        boolean serviceable = cityWithinServiceRange && projectCount > 0;

        return LocationResolveResponse.builder()
                .serviceable(serviceable)
                .cityId(city.getId())
                .cityName(city.getName())
                .state(city.getState())
                .countryCode(city.getCountryCode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .accuracyMeters(request.getAccuracyMeters())
                .nearestCityDistanceKm(round(distanceKm))
                .message(
                        serviceable
                                ? "Projects available near your location"
                                : "We are not serving projects in " + city.getName() + " yet."
                )
                .build();
    }

    private String clean(String value) {
        if (value == null) return null;

        String cleaned = value.trim();

        return cleaned.isBlank() ? null : cleaned;
    }

    private double calculateDistanceKm(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {
        final int earthRadiusKm = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2)
                        * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }

    private Double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record CityDistance(CityEntity city, double distanceKm) {
    }
}