package com.brandPitara.sfs.request.repository;

import com.brandPitara.sfs.request.entity.ServiceRequestInterestEntity;
import com.brandPitara.sfs.request.enums.ServiceRequestInterestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRequestInterestRepository extends JpaRepository<ServiceRequestInterestEntity, Long> {

    Optional<ServiceRequestInterestEntity> findByRequestIdAndProviderId(Long requestId, Long providerId);

    List<ServiceRequestInterestEntity> findByRequestIdOrderByCreatedAtDesc(Long requestId);

    long countByRequestIdAndStatus(Long requestId, ServiceRequestInterestStatus status);
}
