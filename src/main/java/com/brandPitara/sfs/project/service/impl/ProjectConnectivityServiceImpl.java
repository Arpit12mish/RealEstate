package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.connectivity.provider.NearbyPlaceProvider;
import com.brandPitara.sfs.project.connectivity.provider.dto.NearbyPlaceProviderResult;
import com.brandPitara.sfs.project.dto.*;
import com.brandPitara.sfs.project.entity.*;
import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import com.brandPitara.sfs.project.mapper.ProjectConnectivityCategoryMapper;
import com.brandPitara.sfs.project.mapper.ProjectConnectivityMapper;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.*;
import com.brandPitara.sfs.project.service.ProjectConnectivityService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectConnectivityServiceImpl implements ProjectConnectivityService {

  private final ProjectRepository projectRepository;
  private final ProjectConnectivityRepository connectivityRepository;
  private final ProjectConnectivityPlaceRepository placeRepository;
  private final ContentVersionService contentVersionService;
  private final ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy;
  private final NearbyPlaceProvider nearbyPlaceProvider;

  private static final String KEY_PROJECTS = "PROJECTS";
  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional(readOnly = true)
  public ProjectConnectivityResponse adminGet(Long projectId) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

    ProjectConnectivityEntity connectivity = connectivityRepository.findByProjectIdAndDeletedFalse(projectId).orElse(null);
    List<ProjectConnectivityPlaceEntity> places = placeRepository.findByProjectIdAndDeletedFalseOrderBySortOrderAscIdAsc(projectId);

    return ProjectConnectivityMapper.toResponse(connectivity, project, places);
  }

  @Override
  @Transactional(readOnly = true)
  public ProjectConnectivityResponse publicGet(Long projectId) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

    projectPublicVisibilityPolicy.assertPubliclyVisible(project, projectId);

    ProjectConnectivityEntity connectivity = connectivityRepository.findByProjectIdAndActiveTrueAndDeletedFalse(projectId).orElse(null);
    List<ProjectConnectivityPlaceEntity> places = placeRepository.findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(projectId);

    return ProjectConnectivityMapper.toResponse(connectivity, project, places);
  }

  @Override
  @Transactional
  public ProjectConnectivityResponse upsertOverview(Long projectId, ProjectConnectivityUpsertRequest request) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

    ProjectConnectivityEntity entity = connectivityRepository.findByProjectIdAndDeletedFalse(projectId)
        .orElseGet(() -> ProjectConnectivityEntity.builder()
            .project(project)
            .build());

    entity.setTitle(clean(request.getTitle()));
    entity.setSubtitle(clean(request.getSubtitle()));
    entity.setMapImageUrl(clean(request.getMapImageUrl()));
    entity.setSummary(clean(request.getSummary()));
    entity.setDefaultRadiusMeters(request.getDefaultRadiusMeters());
    entity.setSearchEnabled(request.getSearchEnabled() != null ? request.getSearchEnabled() : true);
    entity.setActive(request.getActive() != null ? request.getActive() : true);
    if (entity.getDeleted() == null) entity.setDeleted(false);

    connectivityRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    if (Boolean.TRUE.equals(project.getPublished()) && Boolean.TRUE.equals(project.getActive())) {
      contentVersionService.bump(KEY_HOME);
    }

    List<ProjectConnectivityPlaceEntity> places = placeRepository.findByProjectIdAndDeletedFalseOrderBySortOrderAscIdAsc(projectId);
    return ProjectConnectivityMapper.toResponse(entity, project, places);
  }

  @Override
  @Transactional
  public ProjectConnectivityPlaceResponse addPlace(Long projectId, ProjectConnectivityPlaceUpsertRequest request) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

    ProjectConnectivityEntity connectivity = connectivityRepository.findByProjectIdAndDeletedFalse(projectId).orElse(null);

    ProjectConnectivityCategory category = request.getCategory() != null
        ? request.getCategory()
        : ProjectConnectivityCategoryMapper.fromType(request.getPlaceType());

    ProjectConnectivityPlaceEntity entity = ProjectConnectivityPlaceEntity.builder()
        .project(project)
        .connectivity(connectivity)
        .placeName(cleanRequired(request.getPlaceName()))
        .placeType(request.getPlaceType())
        .distanceLabel(clean(request.getDistanceLabel()))
        .imageUrl(clean(request.getImageUrl()))
        .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .latitude(request.getLatitude())
        .longitude(request.getLongitude())
        .distanceMeters(request.getDistanceMeters())
        .durationSeconds(request.getDurationSeconds())
        .durationLabel(clean(request.getDurationLabel()))
        .externalPlaceId(clean(request.getExternalPlaceId()))
        .provider(clean(request.getProvider()))
        .rating(request.getRating())
        .userRatingCount(request.getUserRatingCount())
        .address(clean(request.getAddress()))
        .category(category)
        .verified(request.getVerified() != null ? request.getVerified() : false)
        .featured(request.getFeatured() != null ? request.getFeatured() : false)
        .build();

    ProjectConnectivityPlaceEntity saved = placeRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    if (Boolean.TRUE.equals(project.getPublished()) && Boolean.TRUE.equals(project.getActive())) {
      contentVersionService.bump(KEY_HOME);
    }

    return ProjectConnectivityMapper.toPlaceResponse(saved);
  }

  @Override
  @Transactional
  public ProjectConnectivityPlaceResponse updatePlace(Long projectId, Long placeId, ProjectConnectivityPlaceUpsertRequest request) {
    ProjectConnectivityPlaceEntity entity = placeRepository.findByIdAndProjectIdAndDeletedFalse(placeId, projectId)
        .orElseThrow(() -> new EntityNotFoundException("Connectivity place not found: " + placeId));

    if (StringUtils.hasText(request.getPlaceName())) entity.setPlaceName(cleanRequired(request.getPlaceName()));
    if (request.getPlaceType() != null) {
      entity.setPlaceType(request.getPlaceType());
      if (request.getCategory() == null) {
        entity.setCategory(ProjectConnectivityCategoryMapper.fromType(request.getPlaceType()));
      }
    }
    if (request.getDistanceLabel() != null) entity.setDistanceLabel(clean(request.getDistanceLabel()));
    if (request.getImageUrl() != null) entity.setImageUrl(clean(request.getImageUrl()));
    if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
    if (request.getActive() != null) entity.setActive(request.getActive());

    if (request.getLatitude() != null) entity.setLatitude(request.getLatitude());
    if (request.getLongitude() != null) entity.setLongitude(request.getLongitude());
    if (request.getDistanceMeters() != null) entity.setDistanceMeters(request.getDistanceMeters());
    if (request.getDurationSeconds() != null) entity.setDurationSeconds(request.getDurationSeconds());
    if (request.getDurationLabel() != null) entity.setDurationLabel(clean(request.getDurationLabel()));
    if (request.getExternalPlaceId() != null) entity.setExternalPlaceId(clean(request.getExternalPlaceId()));
    if (request.getProvider() != null) entity.setProvider(clean(request.getProvider()));
    if (request.getRating() != null) entity.setRating(request.getRating());
    if (request.getUserRatingCount() != null) entity.setUserRatingCount(request.getUserRatingCount());
    if (request.getAddress() != null) entity.setAddress(clean(request.getAddress()));
    if (request.getCategory() != null) entity.setCategory(request.getCategory());
    if (request.getVerified() != null) entity.setVerified(request.getVerified());
    if (request.getFeatured() != null) entity.setFeatured(request.getFeatured());

    ProjectConnectivityPlaceEntity saved = placeRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    if (Boolean.TRUE.equals(entity.getProject().getPublished()) && Boolean.TRUE.equals(entity.getProject().getActive())) {
      contentVersionService.bump(KEY_HOME);
    }

    return ProjectConnectivityMapper.toPlaceResponse(saved);
  }

  @Override
  @Transactional
  public ProjectConnectivityPlaceResponse setPlaceActive(Long projectId, Long placeId, boolean active) {
    ProjectConnectivityPlaceEntity entity = placeRepository.findByIdAndProjectIdAndDeletedFalse(placeId, projectId)
        .orElseThrow(() -> new EntityNotFoundException("Connectivity place not found: " + placeId));

    entity.setActive(active);
    ProjectConnectivityPlaceEntity saved = placeRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    if (Boolean.TRUE.equals(entity.getProject().getPublished()) && Boolean.TRUE.equals(entity.getProject().getActive())) {
      contentVersionService.bump(KEY_HOME);
    }

    return ProjectConnectivityMapper.toPlaceResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProjectConnectivityPlaceResponse> adminListPlaces(Long projectId) {
    return placeRepository.findByProjectIdAndDeletedFalseOrderBySortOrderAscIdAsc(projectId)
        .stream()
        .map(ProjectConnectivityMapper::toPlaceResponse)
        .toList();
  }

  @Override
  @Transactional
  public void softDeletePlace(Long projectId, Long placeId) {
    ProjectConnectivityPlaceEntity entity = placeRepository.findByIdAndProjectIdAndDeletedFalse(placeId, projectId)
        .orElseThrow(() -> new EntityNotFoundException("Connectivity place not found: " + placeId));

    entity.setDeleted(true);
    placeRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    contentVersionService.bump(KEY_HOME);
  }

  @Override
  @Transactional(readOnly = true)
  public ProjectConnectivitySearchResponse searchNearby(Long projectId, String query) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

    projectPublicVisibilityPolicy.assertPubliclyVisible(project, projectId);

    String cleanedQuery = clean(query);
    if (!StringUtils.hasText(cleanedQuery)) {
      throw new IllegalArgumentException("Search query is required");
    }

    List<ProjectConnectivityPlaceEntity> results = placeRepository.searchSavedPlaces(
        projectId,
        cleanedQuery,
        PageRequest.of(0, 20)
    );

    return ProjectConnectivitySearchResponse.builder()
        .projectId(project.getId())
        .query(cleanedQuery)
        .projectLatitude(project.getLatitude())
        .projectLongitude(project.getLongitude())
        .results(results.stream().map(ProjectConnectivityMapper::toPlaceResponse).toList())
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public ConnectivityProviderSearchResponse providerSearch(Long projectId, ConnectivityProviderSearchRequest request) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

    if (project.getLatitude() == null || project.getLongitude() == null) {
      throw new IllegalArgumentException(
          "Project latitude/longitude is missing. Please add project location before searching nearby places.");
    }

    int maxRadius = nearbyPlaceProvider.getMaxRadiusMeters();
    int userRadius = request.getRadiusMeters() != null ? request.getRadiusMeters() : 3000;
    if (userRadius > maxRadius) {
      throw new IllegalArgumentException(
          String.format("Radius cannot exceed %d meters.", maxRadius));
    }
    int safeRadius = Math.max(userRadius, 500);

    String query = firstText(request.getKeyword(), request.getQuery(), defaultQueryFor(request.getCategory(), request.getPlaceType()));
    int limit = request.getLimit() != null ? Math.min(Math.max(request.getLimit(), 1), 50) : 10;

    List<NearbyPlaceProviderResult> providerResults = nearbyPlaceProvider.searchNearby(
        project.getLatitude(),
        project.getLongitude(),
        query,
        request.getCategory(),
        safeRadius
    );

    int totalFetched = providerResults.size();

    // Strict backend filter: only keep results whose distance is known AND within the requested radius.
    // locationRestriction on the Google call reduces far-away results at source; this is the safety net.
    List<NearbyPlaceProviderResult> withinRadius = providerResults.stream()
        .filter(r -> r.getDistanceMeters() != null && r.getDistanceMeters() <= safeRadius)
        .toList();

    int totalWithinRadius = withinRadius.size();
    int totalFilteredOut = totalFetched - totalWithinRadius;

    List<NearbyPlaceProviderResult> results = withinRadius.stream()
        .limit(limit)
        .toList();

    Set<String> savedProviderKeys = placeRepository.findAllProviderExternalIdKeysByProjectId(projectId);
    Set<String> savedNameTypeKeys = placeRepository.findAllNameTypeKeysByProjectId(projectId);
    Set<String> savedNameCategoryKeys = placeRepository.findAllNameCategoryKeysByProjectId(projectId);
    results.forEach(r -> r.setAlreadySaved(isAlreadySaved(r, savedProviderKeys, savedNameTypeKeys, savedNameCategoryKeys)));

    return ConnectivityProviderSearchResponse.builder()
        .projectId(project.getId())
        .category(request.getCategory())
        .categoryLabel(request.getCategory() != null ? request.getCategory().toLabel() : null)
        .query(query)
        .radiusMeters(safeRadius)
        .projectLatitude(project.getLatitude())
        .projectLongitude(project.getLongitude())
        .provider("GOOGLE_PLACES")
        .totalFetched(totalFetched)
        .totalWithinRadius(totalWithinRadius)
        .totalFilteredOut(totalFilteredOut)
        .results(results)
        .build();
  }

  @Override
  public List<ConnectivityProviderCategoryMetaResponse> providerCategories() {
    return List.of(
        meta(ProjectConnectivityCategory.TRANSIT, "metro station bus stop railway station", 5000,
            preset("METRO", "Metro", "metro station"),
            preset("BUS_STOP", "Bus Stop", "bus stop"),
            preset("RAILWAY_STATION", "Railway Station", "railway station"),
            preset("AIRPORT", "Airport", "airport")),
        meta(ProjectConnectivityCategory.SCHOOLS, "school", 5000,
            preset("SCHOOL", "School", "school")),
        meta(ProjectConnectivityCategory.COLLEGES, "college university", 7000,
            preset("COLLEGE", "College", "college"),
            preset("UNIVERSITY", "University", "university")),
        meta(ProjectConnectivityCategory.HOSPITALS, "hospital clinic", 5000,
            preset("HOSPITAL", "Hospital", "hospital"),
            preset("CLINIC", "Clinic", "clinic"),
            preset("PHARMACY", "Pharmacy", "pharmacy")),
        meta(ProjectConnectivityCategory.PARKS, "park", 5000,
            preset("PARK", "Park", "park")),
        meta(ProjectConnectivityCategory.RETAIL, "retail shop market", 4000,
            preset("RETAIL_SHOP", "Retail Shop", "retail shop"),
            preset("MARKET", "Market", "market")),
        meta(ProjectConnectivityCategory.MALLS, "shopping mall", 7000,
            preset("MALL", "Mall", "shopping mall")),
        meta(ProjectConnectivityCategory.GYMS, "gym fitness center", 4000,
            preset("GYM", "Gym", "gym"),
            preset("FITNESS_CENTER", "Fitness Center", "fitness center")),
        meta(ProjectConnectivityCategory.OFFICES, "IT park business park office hub", 8000,
            preset("TECH_PARK", "Tech Park", "IT park"),
            preset("BUSINESS_HUB", "Business Hub", "business park"),
            preset("OFFICE_HUB", "Office Hub", "office hub")),
        meta(ProjectConnectivityCategory.RESTAURANTS, "restaurant cafe", 3000,
            preset("RESTAURANT", "Restaurant", "restaurant"),
            preset("CAFE", "Cafe", "cafe")),
        meta(ProjectConnectivityCategory.BANKS, "bank ATM", 3000,
            preset("BANK", "Bank", "bank"),
            preset("ATM", "ATM", "ATM")),
        meta(ProjectConnectivityCategory.DAILY_NEEDS, "supermarket grocery store pharmacy", 3000,
            preset("SUPERMARKET", "Supermarket", "supermarket"),
            preset("GROCERY_STORE", "Grocery Store", "grocery store"),
            preset("CONVENIENCE_STORE", "Convenience Store", "convenience store")),
        meta(ProjectConnectivityCategory.SAFETY, "police station fire station", 7000,
            preset("POLICE_STATION", "Police Station", "police station"),
            preset("FIRE_STATION", "Fire Station", "fire station")),
        meta(ProjectConnectivityCategory.LIFESTYLE, "temple landmark", 5000,
            preset("LANDMARK", "Landmark", "landmark"),
            preset("TEMPLE", "Temple", "temple"))
    );
  }

  @Override
  @Transactional
  public ProjectConnectivityPlaceBulkSaveResponse bulkSavePlaces(Long projectId, ProjectConnectivityPlaceBulkUpsertRequest request) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

    ProjectConnectivityEntity connectivity = connectivityRepository.findByProjectIdAndDeletedFalse(projectId).orElse(null);

    int sortBase = placeRepository.findMaxSortOrderByProjectId(projectId).map(m -> m + 1).orElse(0);
    int sortCounter = sortBase;

    Set<String> savedProviderKeys = placeRepository.findAllProviderExternalIdKeysByProjectId(projectId);
    Set<String> savedNameTypeKeys = placeRepository.findAllNameTypeKeysByProjectId(projectId);
    Set<String> savedNameCategoryKeys = placeRepository.findAllNameCategoryKeysByProjectId(projectId);

    List<ProjectConnectivityPlaceResponse> savedPlaces = new ArrayList<>();
    List<ProjectConnectivityPlaceResponse> skippedDuplicates = new ArrayList<>();

    for (ProjectConnectivityPlaceUpsertRequest req : request.getPlaces()) {
      if (!StringUtils.hasText(req.getPlaceName())) {
        throw new IllegalArgumentException("placeName is required for every place in a bulk save request");
      }
      ProjectConnectivityCategory category = req.getCategory() != null
          ? req.getCategory()
          : ProjectConnectivityCategoryMapper.fromType(req.getPlaceType());

      if (isBulkDuplicate(req, category, savedProviderKeys, savedNameTypeKeys, savedNameCategoryKeys)) {
        skippedDuplicates.add(buildSkippedResponse(req, projectId));
        continue;
      }

      String distanceLabel = req.getDistanceLabel();
      if (distanceLabel == null && req.getDistanceMeters() != null) {
        distanceLabel = formatDistance(req.getDistanceMeters());
      }

      int sortOrder = req.getSortOrder() != null ? req.getSortOrder() : sortCounter++;

      ProjectConnectivityPlaceEntity entity = ProjectConnectivityPlaceEntity.builder()
          .project(project)
          .connectivity(connectivity)
          .placeName(cleanRequired(req.getPlaceName()))
          .placeType(req.getPlaceType())
          .distanceLabel(distanceLabel)
          .imageUrl(clean(req.getImageUrl()))
          .sortOrder(sortOrder)
          .active(req.getActive() != null ? req.getActive() : true)
          .deleted(false)
          .latitude(req.getLatitude())
          .longitude(req.getLongitude())
          .distanceMeters(req.getDistanceMeters())
          .durationSeconds(req.getDurationSeconds())
          .durationLabel(clean(req.getDurationLabel()))
          .externalPlaceId(clean(req.getExternalPlaceId()))
          .provider(clean(req.getProvider()))
          .rating(req.getRating())
          .userRatingCount(req.getUserRatingCount())
          .address(clean(req.getAddress()))
          .category(category)
          .verified(req.getVerified() != null ? req.getVerified() : false)
          .featured(req.getFeatured() != null ? req.getFeatured() : false)
          .build();

      ProjectConnectivityPlaceEntity saved = placeRepository.save(entity);
      savedPlaces.add(ProjectConnectivityMapper.toPlaceResponse(saved));
      if (StringUtils.hasText(saved.getProvider()) && StringUtils.hasText(saved.getExternalPlaceId())) {
        savedProviderKeys.add(saved.getProvider() + "::" + saved.getExternalPlaceId());
      }
      if (saved.getPlaceType() != null && StringUtils.hasText(saved.getPlaceName())) {
        savedNameTypeKeys.add(saved.getPlaceName().toLowerCase(Locale.ROOT) + "::" + saved.getPlaceType().name());
      }
      if (saved.getCategory() != null && StringUtils.hasText(saved.getPlaceName())) {
        savedNameCategoryKeys.add(saved.getPlaceName().toLowerCase(Locale.ROOT) + "::" + saved.getCategory().name());
      }
    }

    if (!savedPlaces.isEmpty()) {
      contentVersionService.bump(KEY_PROJECTS);
      if (Boolean.TRUE.equals(project.getPublished()) && Boolean.TRUE.equals(project.getActive())) {
        contentVersionService.bump(KEY_HOME);
      }
    }

    return ProjectConnectivityPlaceBulkSaveResponse.builder()
        .projectId(projectId)
        .requestedCount(request.getPlaces().size())
        .savedCount(savedPlaces.size())
        .skippedDuplicateCount(skippedDuplicates.size())
        .savedPlaces(savedPlaces)
        .skippedDuplicates(skippedDuplicates)
        .build();
  }

  // --- Private helpers ---

  private boolean isBulkDuplicate(ProjectConnectivityPlaceUpsertRequest req,
                                   ProjectConnectivityCategory category,
                                   Set<String> savedProviderKeys,
                                   Set<String> savedNameTypeKeys,
                                   Set<String> savedNameCategoryKeys) {
    if (StringUtils.hasText(req.getProvider()) && StringUtils.hasText(req.getExternalPlaceId())) {
      return savedProviderKeys.contains(req.getProvider() + "::" + req.getExternalPlaceId());
    }
    if (req.getPlaceType() != null && StringUtils.hasText(req.getPlaceName())) {
      return savedNameTypeKeys.contains(req.getPlaceName().toLowerCase(Locale.ROOT) + "::" + req.getPlaceType().name());
    }
    if (category != null && StringUtils.hasText(req.getPlaceName())) {
      return savedNameCategoryKeys.contains(req.getPlaceName().toLowerCase(Locale.ROOT) + "::" + category.name());
    }
    return false;
  }

  private boolean isAlreadySaved(NearbyPlaceProviderResult result,
                                  Set<String> savedProviderKeys,
                                  Set<String> savedNameTypeKeys,
                                  Set<String> savedNameCategoryKeys) {
    if (StringUtils.hasText(result.getProvider()) && StringUtils.hasText(result.getExternalPlaceId())) {
      return savedProviderKeys.contains(result.getProvider() + "::" + result.getExternalPlaceId());
    }
    if (result.getPlaceType() != null && StringUtils.hasText(result.getPlaceName())) {
      return savedNameTypeKeys.contains(result.getPlaceName().toLowerCase(Locale.ROOT) + "::" + result.getPlaceType().name());
    }
    if (result.getCategory() != null && StringUtils.hasText(result.getPlaceName())) {
      return savedNameCategoryKeys.contains(result.getPlaceName().toLowerCase(Locale.ROOT) + "::" + result.getCategory().name());
    }
    return false;
  }

  private ProjectConnectivityPlaceResponse buildSkippedResponse(ProjectConnectivityPlaceUpsertRequest req, Long projectId) {
    ProjectConnectivityCategory category = req.getCategory() != null
        ? req.getCategory()
        : ProjectConnectivityCategoryMapper.fromType(req.getPlaceType());

    return ProjectConnectivityPlaceResponse.builder()
        .projectId(projectId)
        .placeName(req.getPlaceName())
        .placeType(req.getPlaceType())
        .placeTypeLabel(req.getPlaceType() != null ? req.getPlaceType().toLabel() : null)
        .category(category)
        .categoryLabel(category != null ? category.toLabel() : null)
        .distanceLabel(req.getDistanceLabel())
        .distanceMeters(req.getDistanceMeters())
        .externalPlaceId(req.getExternalPlaceId())
        .provider(req.getProvider())
        .build();
  }

  private ConnectivityProviderCategoryMetaResponse meta(
      ProjectConnectivityCategory category,
      String defaultQuery,
      int defaultRadiusMeters,
      ConnectivityProviderCategoryMetaResponse.QueryPreset... queries
  ) {
    return ConnectivityProviderCategoryMetaResponse.builder()
        .category(category)
        .categoryLabel(category.toLabel())
        .iconKey(category.iconKey())
        .defaultQuery(defaultQuery)
        .defaultRadiusMeters(defaultRadiusMeters)
        .queries(List.of(queries))
        .build();
  }

  private ConnectivityProviderCategoryMetaResponse.QueryPreset preset(String placeType, String label, String keyword) {
    return ConnectivityProviderCategoryMetaResponse.QueryPreset.builder()
        .placeType(placeType)
        .label(label)
        .keyword(keyword)
        .build();
  }

  private String defaultQueryFor(ProjectConnectivityCategory category, com.brandPitara.sfs.project.enums.ProjectConnectivityType placeType) {
    if (placeType != null) {
      return placeType.toLabel();
    }
    if (category == null) {
      return "nearby places";
    }
    return switch (category) {
      case TRANSIT -> "metro station bus stop railway station";
      case SCHOOLS -> "school";
      case COLLEGES -> "college university";
      case HOSPITALS -> "hospital clinic";
      case PARKS -> "park";
      case RETAIL -> "retail shop market";
      case MALLS -> "shopping mall";
      case GYMS -> "gym fitness center";
      case OFFICES -> "IT park business park office hub";
      case RESTAURANTS -> "restaurant cafe";
      case BANKS -> "bank ATM";
      case DAILY_NEEDS -> "supermarket grocery store pharmacy";
      case SAFETY -> "police station fire station";
      case LIFESTYLE -> "temple landmark";
      case SEARCH -> "nearby places";
    };
  }

  private String firstText(String... values) {
    if (values == null) return null;
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private String formatDistance(Integer meters) {
    if (meters == null) return null;
    if (meters < 1000) return meters + " m";
    return String.format(Locale.US, "%.1f km", meters / 1000.0);
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }

  private String cleanRequired(String s) {
    return s.trim();
  }
}
