package com.brandPitara.sfs.provider.service.impl;

import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.provider.dto.ProviderProjectCreateRequest;
import com.brandPitara.sfs.provider.dto.ProviderProjectResponse;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.provider.entity.ProviderProjectEntity;
import com.brandPitara.sfs.provider.entity.ProviderProjectMediaEntity;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import com.brandPitara.sfs.provider.repository.ProviderProjectRepository;
import com.brandPitara.sfs.provider.service.ProviderProjectService;
import com.brandPitara.sfs.repository.CategoryRepository;
import com.brandPitara.sfs.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderProjectServiceImpl implements ProviderProjectService {

    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderProjectRepository providerProjectRepository;

    private final CategoryRepository categoryRepository;
    private final CityRepository cityRepository;

    @Override
    @Transactional
    public ProviderProjectResponse createMyProject(Long currentUserId, ProviderProjectCreateRequest request) {

        ProviderProfileEntity provider = providerProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new NotFoundException("Provider profile not found"));

        CategoryEntity category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Category not found: " + request.categoryId()));

        CityEntity city = null;
        if (request.cityId() != null) {
            city = cityRepository.findById(request.cityId())
                    .orElseThrow(() -> new NotFoundException("City not found: " + request.cityId()));
        }

        ProviderProjectEntity project = ProviderProjectEntity.builder()
                .provider(provider)
                .title(request.title())
                .description(request.description())
                .category(category)
                .city(city)
                .locality(request.locality())
                .budgetMin(request.budgetMin())
                .budgetMax(request.budgetMax())
                .visibility(request.visibility())
                .build();

        // Attach media (cascade = ALL, orphanRemoval = true)
        for (ProviderProjectCreateRequest.ProjectMediaRequest m : request.media()) {
            ProviderProjectMediaEntity media = ProviderProjectMediaEntity.builder()
                    .project(project) // IMPORTANT: owning side
                    .mediaType(m.mediaType())
                    .url(m.url())
                    .thumbnailUrl(m.thumbnailUrl())
                    .sortOrder(m.sortOrder())
                    .build();

            project.getMedia().add(media);
        }

        ProviderProjectEntity saved = providerProjectRepository.save(project);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderProjectResponse> listProviderProjects(Long providerId) {
        return providerProjectRepository.findByProviderIdOrderByIdDesc(providerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteMyProject(Long currentUserId, Long projectId) {

        ProviderProfileEntity provider = providerProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new NotFoundException("Provider profile not found"));

        ProviderProjectEntity project = providerProjectRepository
                .findByIdAndProviderId(projectId, provider.getId())
                .orElseThrow(() -> new NotFoundException("Project not found"));

        // Because entity has orphanRemoval=true on media, deleting project deletes media automatically
        providerProjectRepository.delete(project);
    }

    private ProviderProjectResponse toResponse(ProviderProjectEntity p) {

        Long categoryId = p.getCategory() != null ? p.getCategory().getId() : null;
        String categoryName = p.getCategory() != null ? p.getCategory().getName() : null;

        Long cityId = p.getCity() != null ? p.getCity().getId() : null;
        String cityName = p.getCity() != null ? p.getCity().getName() : null;

        List<ProviderProjectResponse.ProjectMediaResponse> media = (p.getMedia() == null ? List.<ProviderProjectMediaEntity>of() : p.getMedia())
                .stream()
                .sorted(Comparator.comparingInt(ProviderProjectMediaEntity::getSortOrder))
                .map(m -> new ProviderProjectResponse.ProjectMediaResponse(
                        m.getId(),
                        m.getMediaType() != null ? m.getMediaType().name() : null,
                        m.getUrl(),
                        m.getThumbnailUrl(),
                        m.getSortOrder()
                ))
                .toList();

        return new ProviderProjectResponse(
                p.getId(),
                p.getProvider() != null ? p.getProvider().getId() : null,
                p.getTitle(),
                p.getDescription(),
                categoryId,
                categoryName,
                cityId,
                cityName,
                p.getLocality(),
                p.getBudgetMin(),
                p.getBudgetMax(),
                p.getVisibility(),
                media
        );
    }

    @Override
  @Transactional(readOnly = true)
  public List<ProviderProjectResponse> listMyProjects(Long currentUserId) {

    ProviderProfileEntity provider = providerProfileRepository.findByUserId(currentUserId)
        .orElseThrow(() -> new NotFoundException("Provider profile not found"));

    // uses your repo method: findByProviderIdOrderByIdDesc(Long providerId)
    return providerProjectRepository.findByProviderIdOrderByIdDesc(provider.getId())
        .stream()
        .map(this::toResponse)
        .toList();
  }

}
