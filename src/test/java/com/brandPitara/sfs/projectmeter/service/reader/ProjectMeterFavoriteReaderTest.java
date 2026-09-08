package com.brandPitara.sfs.projectmeter.service.reader;

import com.brandPitara.sfs.enums.FavoriteTargetType;
import com.brandPitara.sfs.enums.Role;
import com.brandPitara.sfs.repository.UserFavoriteRepository;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.security.identity.MobileAuthenticationUserSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMeterFavoriteReaderTest {

    @Mock private UserFavoriteRepository userFavoriteRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private ProjectMeterFavoriteReader reader;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousRequestLoadsCountWithoutUserOrFavoriteStateLookup() {
        when(userFavoriteRepository.countByTargetTypeAndTargetId(FavoriteTargetType.PROJECT, 42L))
            .thenReturn(5L);

        var state = reader.read(42L);

        assertThat(state.count()).isEqualTo(5L);
        assertThat(state.favorite()).isFalse();
        verify(userFavoriteRepository, never())
            .existsByUser_IdAndTargetTypeAndTargetId(7L, FavoriteTargetType.PROJECT, 42L);
        verifyNoInteractions(userRepository);
    }

    @Test
    void snapshotPrincipalUsesUserIdWithoutReloadingUser() {
        var principal = new MobileAuthenticationUserSnapshot(7L, "+919999999999", Role.CUSTOMER, true);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        when(userFavoriteRepository.countByTargetTypeAndTargetId(FavoriteTargetType.PROJECT, 42L))
            .thenReturn(5L);
        when(userFavoriteRepository.existsByUser_IdAndTargetTypeAndTargetId(
            7L,
            FavoriteTargetType.PROJECT,
            42L
        )).thenReturn(true);

        var state = reader.read(42L);

        assertThat(state.count()).isEqualTo(5L);
        assertThat(state.favorite()).isTrue();
        verifyNoInteractions(userRepository);
    }
}
