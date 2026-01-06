package com.brandPitara.sfs.provider.service.impl;

import com.brandPitara.sfs.exception.ForbiddenException;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.media.service.S3ObjectService;
import com.brandPitara.sfs.provider.dto.ProviderMediaListResponse;
import com.brandPitara.sfs.provider.dto.ProviderMediaResponse;
import com.brandPitara.sfs.provider.entity.ProviderMediaEntity;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.provider.enums.ProviderMediaType;
import com.brandPitara.sfs.provider.repository.ProviderMediaRepository;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import com.brandPitara.sfs.provider.service.ProviderMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderMediaServiceImpl implements ProviderMediaService {

    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderMediaRepository providerMediaRepository;
    private final S3ObjectService s3ObjectService;


    private ProviderProfileEntity requireMyProvider(Long currentUserId) {
        return providerProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new NotFoundException("Provider profile not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderMediaListResponse getMyMedia(Long currentUserId) {
        ProviderProfileEntity provider = requireMyProvider(currentUserId);

        List<ProviderMediaEntity> all = providerMediaRepository.findByProviderIdOrderBySortOrderAsc(provider.getId());

        ProviderMediaResponse profile = all.stream()
                .filter(m -> m.getMediaType() == ProviderMediaType.PROFILE_PHOTO)
                .findFirst()
                .map(this::toResponse)
                .orElse(null);

        List<ProviderMediaResponse> gallery = all.stream()
                .filter(m -> m.getMediaType() == ProviderMediaType.GALLERY)
                .map(this::toResponse)
                .toList();

        return new ProviderMediaListResponse(profile, gallery);
    }


    @Override
    @Transactional
    public ProviderMediaResponse addMyGalleryImage(Long currentUserId, String url, String thumbnailUrl, int sortOrder) {
        ProviderProfileEntity provider = requireMyProvider(currentUserId);

        long count = providerMediaRepository.countByProviderIdAndMediaType(provider.getId(), ProviderMediaType.GALLERY);
        if (count >= 3) {
            throw new ForbiddenException("Gallery limit reached (max 3 images)");
        }

        ProviderMediaEntity saved = providerMediaRepository.save(ProviderMediaEntity.builder()
                .provider(provider)
                .mediaType(ProviderMediaType.GALLERY)
                .url(url)
                .thumbnailUrl(thumbnailUrl)
                .sortOrder(Math.max(sortOrder, 0))
                .build());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteMyMedia(Long currentUserId, Long mediaId) {
        ProviderProfileEntity provider = requireMyProvider(currentUserId);

        ProviderMediaEntity media = providerMediaRepository.findById(mediaId)
                .orElseThrow(() -> new NotFoundException("Media not found"));

        if (!media.getProvider().getId().equals(provider.getId())) {
            throw new ForbiddenException("Not allowed");
        }

        // Optional: prevent deleting profile photo if you want
        providerMediaRepository.delete(media);
    }

    private ProviderMediaResponse toResponse(ProviderMediaEntity m) {
        return new ProviderMediaResponse(
                m.getId(),
                m.getMediaType().name(),
                m.getUrl(),
                m.getThumbnailUrl(),
                m.getSortOrder()
        );
    }

    @Override
    @Transactional
    public ProviderMediaResponse upsertMyProfilePhoto(Long currentUserId, String url, String thumbnailUrl, String storageKey) {
        ProviderProfileEntity provider = requireMyProvider(currentUserId);

        // Replace existing profile photo (Step 2.7 will enhance this delete)
        providerMediaRepository.deleteByProviderIdAndMediaType(provider.getId(), ProviderMediaType.PROFILE_PHOTO);

        ProviderMediaEntity saved = providerMediaRepository.save(ProviderMediaEntity.builder()
                .provider(provider)
                .mediaType(ProviderMediaType.PROFILE_PHOTO)
                .storageKey(storageKey)     // ✅ store
                .url(url)
                .thumbnailUrl(thumbnailUrl)
                .sortOrder(0)
                .build());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ProviderMediaResponse addMyGalleryImage(Long currentUserId, String url, String thumbnailUrl, String storageKey, int sortOrder) {
        ProviderProfileEntity provider = requireMyProvider(currentUserId);

        long count = providerMediaRepository.countByProviderIdAndMediaType(provider.getId(), ProviderMediaType.GALLERY);
        if (count >= 3) throw new ForbiddenException("Gallery limit reached (max 3 images)");

        ProviderMediaEntity saved = providerMediaRepository.save(ProviderMediaEntity.builder()
                .provider(provider)
                .mediaType(ProviderMediaType.GALLERY)
                .storageKey(storageKey)     // ✅ store
                .url(url)
                .thumbnailUrl(thumbnailUrl)
                .sortOrder(Math.max(sortOrder, 0))
                .build());

        return toResponse(saved);
    }

}
