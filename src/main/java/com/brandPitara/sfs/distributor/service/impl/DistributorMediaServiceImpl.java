package com.brandPitara.sfs.distributor.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.distributor.dto.DistributorMediaResponse;
import com.brandPitara.sfs.distributor.dto.DistributorMediaUpsertRequest;
import com.brandPitara.sfs.distributor.entity.DistributorEntity;
import com.brandPitara.sfs.distributor.entity.DistributorMediaEntity;
import com.brandPitara.sfs.distributor.repository.DistributorMediaRepository;
import com.brandPitara.sfs.distributor.repository.DistributorRepository;
import com.brandPitara.sfs.distributor.service.DistributorMediaService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DistributorMediaServiceImpl implements DistributorMediaService {

  private final DistributorRepository distributorRepository;
  private final DistributorMediaRepository mediaRepository;
  private final ContentVersionService contentVersionService;

  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional
  public DistributorMediaResponse addMedia(Long distributorId, DistributorMediaUpsertRequest request) {

    DistributorEntity distributor = distributorRepository.findByIdAndDeletedFalse(distributorId)
        .orElseThrow(() -> new EntityNotFoundException("Distributor not found: " + distributorId));

    DistributorMediaEntity entity = DistributorMediaEntity.builder()
        .distributor(distributor)
        .mediaType(request.getMediaType())
        .url(clean(request.getUrl()))
        .caption(clean(request.getCaption()))
        .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();

    DistributorMediaEntity saved = mediaRepository.save(entity);

    contentVersionService.bump(KEY_HOME);
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DistributorMediaResponse> listMedia(Long distributorId) {
    distributorRepository.findByIdAndDeletedFalse(distributorId)
        .orElseThrow(() -> new EntityNotFoundException("Distributor not found: " + distributorId));

    return mediaRepository.findByDistributorIdAndDeletedFalseOrderBySortOrderAsc(distributorId)
        .stream()
        .filter(m -> Boolean.TRUE.equals(m.getActive()))
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void softDeleteMedia(Long distributorId, Long mediaId) {
    distributorRepository.findByIdAndDeletedFalse(distributorId)
        .orElseThrow(() -> new EntityNotFoundException("Distributor not found: " + distributorId));

    DistributorMediaEntity media = mediaRepository.findByIdAndDeletedFalse(mediaId)
        .orElseThrow(() -> new EntityNotFoundException("Media not found: " + mediaId));

    if (!media.getDistributor().getId().equals(distributorId)) {
      throw new IllegalArgumentException("Media does not belong to distributor");
    }

    media.setDeleted(true);
    media.setActive(false);
    mediaRepository.save(media);

    contentVersionService.bump(KEY_HOME);
  }

  private DistributorMediaResponse toResponse(DistributorMediaEntity m) {
    return DistributorMediaResponse.builder()
        .id(m.getId())
        .mediaType(m.getMediaType())
        .url(m.getUrl())
        .caption(m.getCaption())
        .sortOrder(m.getSortOrder() != null ? m.getSortOrder() : 0)
        .active(Boolean.TRUE.equals(m.getActive()))
        .build();
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
