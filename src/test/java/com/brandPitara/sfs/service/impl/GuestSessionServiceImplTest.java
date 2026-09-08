package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dto.GuestSessionRequest;
import com.brandPitara.sfs.dto.GuestSessionResponse;
import com.brandPitara.sfs.entity.GuestIdentityLink;
import com.brandPitara.sfs.entity.GuestSession;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.repository.GuestIdentityLinkRepository;
import com.brandPitara.sfs.repository.GuestSessionRepository;
import com.brandPitara.sfs.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GuestSessionServiceImplTest {

    private GuestSessionRepository guestSessionRepository;
    private GuestIdentityLinkRepository identityLinkRepository;
    private JwtTokenUtil jwtTokenUtil;
    private GuestSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        guestSessionRepository = mock(GuestSessionRepository.class);
        identityLinkRepository = mock(GuestIdentityLinkRepository.class);
        jwtTokenUtil = mock(JwtTokenUtil.class);
        service = new GuestSessionServiceImpl(
                guestSessionRepository,
                identityLinkRepository,
                jwtTokenUtil
        );
    }

    @Test
    void createsNewGuestEpochWhenNoActiveUnconvertedSessionExists() {
        GuestSessionRequest request = request("installation-1");
        when(guestSessionRepository
                .findFirstByInstallationIdAndActiveTrueAndLinkedUserIsNullOrderByIdDesc("installation-1"))
                .thenReturn(Optional.empty());
        when(guestSessionRepository.save(any(GuestSession.class))).thenAnswer(invocation -> {
            GuestSession saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(jwtTokenUtil.generateGuestToken(10L, "installation-1")).thenReturn("guest-jwt");

        GuestSessionResponse response = service.createOrReuseGuestSession(request);

        assertThat(response.getGuestSessionId()).isEqualTo(10L);
        assertThat(response.getAccessToken()).isEqualTo("guest-jwt");
        verify(guestSessionRepository).lockInstallation("installation-1");
        verify(guestSessionRepository).save(argThat(session ->
                session.isActive()
                        && session.getLinkedUser() == null
                        && session.getInstallationId().equals("installation-1")
        ));
    }

    @Test
    void reusesAnActiveUnconvertedGuestEpochAcrossAppRestarts() {
        GuestSession existing = activeGuest(11L, "installation-1");
        when(guestSessionRepository
                .findFirstByInstallationIdAndActiveTrueAndLinkedUserIsNullOrderByIdDesc("installation-1"))
                .thenReturn(Optional.of(existing));
        when(guestSessionRepository.save(existing)).thenReturn(existing);
        when(jwtTokenUtil.generateGuestToken(11L, "installation-1")).thenReturn("same-guest-jwt");

        GuestSessionResponse response = service.createOrReuseGuestSession(request("installation-1"));

        assertThat(response.getGuestSessionId()).isEqualTo(11L);
        verify(guestSessionRepository).save(existing);
    }

    @Test
    void conversionCreatesAppendOnlyLinkAndClosesGuestEpoch() {
        GuestSession guest = activeGuest(12L, "installation-1");
        User user = user(55L);
        when(guestSessionRepository
                .findFirstByInstallationIdAndActiveTrueAndLinkedUserIsNullOrderByIdDesc("installation-1"))
                .thenReturn(Optional.of(guest));
        when(identityLinkRepository.saveAndFlush(any(GuestIdentityLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.linkGuestSessionToUser("installation-1", user);

        assertThat(guest.isActive()).isFalse();
        assertThat(guest.getLinkedUser()).isSameAs(user);
        assertThat(guest.getLinkedAt()).isNotNull();
        verify(identityLinkRepository).saveAndFlush(argThat(link ->
                link.getGuestSession() == guest
                        && link.getUser() == user
                        && link.getInstallationId().equals("installation-1")
                        && link.getLinkType().equals(GuestIdentityLink.OTP_CONVERSION)
        ));
        verify(guestSessionRepository).save(guest);
    }

    @Test
    void convertedGuestCannotBeConvertedAgainOrRelinked() {
        when(guestSessionRepository
                .findFirstByInstallationIdAndActiveTrueAndLinkedUserIsNullOrderByIdDesc("installation-1"))
                .thenReturn(Optional.empty());

        service.linkGuestSessionToUser("installation-1", user(80L));

        verifyNoInteractions(identityLinkRepository);
        verify(guestSessionRepository, never()).save(any());
    }

    @Test
    void convertedEpochIsNotReusedAndSameInstallationCreatesNewGuestEpoch() {
        when(guestSessionRepository
                .findFirstByInstallationIdAndActiveTrueAndLinkedUserIsNullOrderByIdDesc("installation-1"))
                .thenReturn(Optional.empty());
        when(guestSessionRepository.save(any(GuestSession.class))).thenAnswer(invocation -> {
            GuestSession saved = invocation.getArgument(0);
            saved.setId(13L);
            return saved;
        });
        when(jwtTokenUtil.generateGuestToken(13L, "installation-1")).thenReturn("new-epoch-jwt");

        GuestSessionResponse response = service.createOrReuseGuestSession(request("installation-1"));

        assertThat(response.getGuestSessionId()).isEqualTo(13L);
        verify(guestSessionRepository).save(argThat(session ->
                session.getId().equals(13L) && session.getLinkedUser() == null && session.isActive()
        ));
    }

    @Test
    void conversionFailureLeavesGuestObjectUnchangedForTransactionRollback() {
        GuestSession guest = activeGuest(14L, "installation-1");
        when(guestSessionRepository
                .findFirstByInstallationIdAndActiveTrueAndLinkedUserIsNullOrderByIdDesc("installation-1"))
                .thenReturn(Optional.of(guest));
        when(identityLinkRepository.saveAndFlush(any(GuestIdentityLink.class)))
                .thenThrow(new IllegalStateException("database write failed"));

        assertThatThrownBy(() -> service.linkGuestSessionToUser("installation-1", user(55L)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(guest.isActive()).isTrue();
        assertThat(guest.getLinkedUser()).isNull();
        assertThat(guest.getLinkedAt()).isNull();
        verify(guestSessionRepository, never()).save(any());
    }

    @Test
    void blankInstallationNeverLinksAnyGuestToAUser() {
        service.linkGuestSessionToUser(" ", user(55L));

        verifyNoInteractions(guestSessionRepository, identityLinkRepository);
    }

    private GuestSessionRequest request(String installationId) {
        GuestSessionRequest request = new GuestSessionRequest();
        request.setInstallationId(installationId);
        request.setPlatform("IOS");
        request.setAppVersion("1.0.0");
        return request;
    }

    private GuestSession activeGuest(Long id, String installationId) {
        return GuestSession.builder()
                .id(id)
                .installationId(installationId)
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .lastSeenAt(OffsetDateTime.now())
                .build();
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
