package com.brandPitara.sfs.dashboard.auth.security;

import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.security.identity.DashboardAuthenticationUserSnapshot;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class DashboardUserDetails implements UserDetails {

    private final DashboardAuthenticationUserSnapshot snapshot;

    public DashboardUserDetails(DashboardUserEntity user) {
        this(new DashboardAuthenticationUserSnapshot(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                Boolean.TRUE.equals(user.getActive())
        ));
    }

    public DashboardUserDetails(DashboardAuthenticationUserSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    /**
     * Compatibility view for existing audit/write services. This is a new detached
     * value on every call; neither the cache nor the SecurityContext retains a JPA entity.
     */
    public DashboardUserEntity getUser() {
        return DashboardUserEntity.builder()
                .id(snapshot.userId())
                .email(snapshot.email())
                .name(snapshot.name())
                .role(snapshot.role())
                .active(snapshot.active())
                .build();
    }

    public Long getId() {
        return snapshot.userId();
    }

    public String getEmail() {
        return snapshot.email();
    }

    public String getFullName() {
        return snapshot.name();
    }

    public String getRoleName() {
        return snapshot.role().name();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + snapshot.role().name())
        );
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return snapshot.email();
    }

    @Override
    public boolean isEnabled() {
        return snapshot.active();
    }

    @Override
    public boolean isAccountNonExpired() {
        return isEnabled();
    }

    @Override
    public boolean isAccountNonLocked() {
        return isEnabled();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return isEnabled();
    }
}
