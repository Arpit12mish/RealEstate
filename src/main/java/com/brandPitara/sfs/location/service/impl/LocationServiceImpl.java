package com.brandPitara.sfs.location.service.impl;

import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.location.dto.LocationResolveRequest;
import com.brandPitara.sfs.location.dto.LocationResolveResponse;
import com.brandPitara.sfs.location.service.LocationService;
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

    private final CityRepository cityRepository;

    @Override
    public LocationResolveResponse resolve(LocationResolveRequest request) {
        return resolveCityContext(
                request.getLatitude(),
                request.getLongitude(),
                request.getDeviceCity(),
                request.getAccuracyMeters()
        );
    }

    @Override
    public LocationResolveResponse resolveCityContext(
            Double latitude,
            Double longitude,
            String deviceCityValue,
            Double accuracyMeters
    ) {
        boolean hasCoordinates = latitude != null && longitude != null;
        LocationResolveRequest request = LocationResolveRequest.builder()
                .latitude(latitude)
                .longitude(longitude)
                .accuracyMeters(accuracyMeters)
                .deviceCity(deviceCityValue)
                .build();

        if (!hasCoordinates && clean(deviceCityValue) == null) {
            return LocationResolveResponse.builder()
                    .serviceable(true)
                    .accuracyMeters(accuracyMeters)
                    .build();
        }

        if (hasCoordinates && !isValidCoordinate(latitude, longitude)) {
            return LocationResolveResponse.builder()
                    .serviceable(true)
                    .accuracyMeters(accuracyMeters)
                    .build();
        }

        if (!hasCoordinates) {
            return resolveDeviceCityOnly(request);
        }

        double userLat = request.getLatitude();
        double userLon = request.getLongitude();

        String deviceCity = clean(request.getDeviceCity());

        if (deviceCity != null) {
            Optional<CityEntity> cityFromDevice =
                    cityRepository.findFirstByNameIgnoreCaseAndActiveTrue(deviceCity);

            if (cityFromDevice.isPresent()) {
                return buildResponseForCity(
                        cityFromDevice.get(),
                        request,
                        0.0
                );
            }

            return LocationResolveResponse.builder()
                    .serviceable(true)
                    .cityId(null)
                    .cityName(deviceCity)
                    .latitude(userLat)
                    .longitude(userLon)
                    .accuracyMeters(request.getAccuracyMeters())
                    .nearestCityDistanceKm(null)
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
                    .serviceable(true)
                    .latitude(userLat)
                    .longitude(userLon)
                    .accuracyMeters(request.getAccuracyMeters())
                    .build();
        }

        return buildResponseForCity(
                nearestCity.city(),
                request,
                nearestCity.distanceKm()
        );
    }

    private LocationResolveResponse resolveDeviceCityOnly(LocationResolveRequest request) {
        String deviceCity = clean(request.getDeviceCity());
        Optional<CityEntity> cityFromDevice =
                cityRepository.findFirstByNameIgnoreCaseAndActiveTrue(deviceCity);

        if (cityFromDevice.isPresent()) {
            return buildResponseForCity(
                    cityFromDevice.get(),
                    request,
                    0.0
            );
        }

        return LocationResolveResponse.builder()
                .serviceable(true)
                .cityName(deviceCity)
                .accuracyMeters(request.getAccuracyMeters())
                .build();
    }

    private LocationResolveResponse buildResponseForCity(
            CityEntity city,
            LocationResolveRequest request,
            double distanceKm
    ) {
        return LocationResolveResponse.builder()
                .serviceable(true)
                .cityId(city.getId())
                .cityName(city.getName())
                .state(city.getState())
                .countryCode(city.getCountryCode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .accuracyMeters(request.getAccuracyMeters())
                .nearestCityDistanceKm(round(distanceKm))
                .build();
    }

    private String clean(String value) {
        if (value == null) return null;

        String cleaned = value.trim();

        return cleaned.isBlank() ? null : cleaned;
    }

    private boolean isValidCoordinate(Double latitude, Double longitude) {
        return latitude != null
                && longitude != null
                && latitude >= -90.0
                && latitude <= 90.0
                && longitude >= -180.0
                && longitude <= 180.0;
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
