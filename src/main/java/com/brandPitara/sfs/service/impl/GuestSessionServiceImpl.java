package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dto.GuestSessionRequest;
import com.brandPitara.sfs.dto.GuestSessionResponse;
import com.brandPitara.sfs.entity.GuestIdentityLink;
import com.brandPitara.sfs.entity.GuestSession;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.repository.GuestIdentityLinkRepository;
import com.brandPitara.sfs.repository.GuestSessionRepository;
import com.brandPitara.sfs.service.GuestSessionService;
import com.brandPitara.sfs.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GuestSessionServiceImpl implements GuestSessionService {

    private final GuestSessionRepository guestSessionRepository;
    private final GuestIdentityLinkRepository guestIdentityLinkRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public GuestSessionResponse createOrReuseGuestSession(GuestSessionRequest request) {
        guestSessionRepository.lockInstallation(request.getInstallationId());

        GuestSession guestSession = guestSessionRepository
                .findFirstByInstallationIdAndActiveTrueAndLinkedUserIsNullOrderByIdDesc(
                        request.getInstallationId()
                )
                .map(existing -> {
                    existing.setDeviceModel(request.getDeviceModel());
                    existing.setDeviceName(request.getDeviceName());
                    existing.setPlatform(request.getPlatform());
                    existing.setOsVersion(request.getOsVersion());
                    existing.setAppVersion(request.getAppVersion());
                    existing.setLastSeenAt(OffsetDateTime.now());
                    return guestSessionRepository.save(existing);
                })
                .orElseGet(() -> {
                    GuestSession created = GuestSession.builder()
                            .installationId(request.getInstallationId())
                            .deviceModel(request.getDeviceModel())
                            .deviceName(request.getDeviceName())
                            .platform(request.getPlatform())
                            .osVersion(request.getOsVersion())
                            .appVersion(request.getAppVersion())
                            .active(true)
                            .lastSeenAt(OffsetDateTime.now())
                            .build();

                    return guestSessionRepository.save(created);
                });

        String accessToken = jwtTokenUtil.generateGuestToken(
                guestSession.getId(),
                guestSession.getInstallationId()
        );

        return GuestSessionResponse.builder()
                .accessToken(accessToken)
                .guestSessionId(guestSession.getId())
                .role("GUEST")
                .installationId(guestSession.getInstallationId())
                .build();
    }


    @Override
    public void linkGuestSessionToUser(String installationId, User user) {
        if (installationId == null || installationId.isBlank() || user == null) {
            return;
        }
        log.debug("linking guest session installationId={} to userId={}", installationId, user.getId());
        guestSessionRepository.lockInstallation(installationId);

        guestSessionRepository
                .findFirstByInstallationIdAndActiveTrueAndLinkedUserIsNullOrderByIdDesc(installationId)
                .ifPresent(guestSession -> {
                    OffsetDateTime linkedAt = OffsetDateTime.now();

                    guestIdentityLinkRepository.saveAndFlush(GuestIdentityLink.builder()
                            .guestSession(guestSession)
                            .user(user)
                            .installationId(guestSession.getInstallationId())
                            .linkedAt(linkedAt)
                            .linkType(GuestIdentityLink.OTP_CONVERSION)
                            .createdAt(linkedAt)
                            .build());

                    guestSession.setLinkedUser(user);
                    guestSession.setLinkedAt(linkedAt);
                    guestSession.setLastSeenAt(linkedAt);
                    guestSession.setActive(false);
                    guestSessionRepository.save(guestSession);
                    log.debug("guest session {} linked to userId={}", guestSession.getId(), user.getId());
                });
    }
}
