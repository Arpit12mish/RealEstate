package com.brandPitara.sfs.dashboard.auth.security;

import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.security.identity.DashboardAuthenticationUserSnapshot;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
public class DashboardUserDetails implements UserDetails {

    private final DashboardAuthenticationUserSnapshot snapshot;

    public DashboardUserDetails(DashboardUserEntity user) {
        this(new DashboardAuthenticationUserSnapshot(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                Boolean.TRUE.equals(user.getActive()),
                user.getPermissions()
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
                .permissions(new java.util.LinkedHashSet<>(snapshot.permissions()))
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
        return snapshot.getAuthorities();
    }

    /**
     * Always empty - intentionally, not an oversight. Dashboard authentication is
     * entirely manual: DashboardAuthServiceImpl#login compares the submitted
     * password against DashboardUserEntity#getPasswordHash via PasswordEncoder
     * directly, and issues a JWT itself. This class is never passed through
     * Spring Security's AuthenticationManager/DaoAuthenticationProvider (no such
     * bean exists in this app), so getPassword() is never read for credential
     * verification - it exists purely because UserDetails requires the method.
     * DashboardJwtAuthenticationFilter/DashboardUserDetailsService only ever use
     * this class post-authentication, to read role/active status for
     * authorization. Do not wire this UserDetails into a real AuthenticationManager
     * without first replacing this with the real password hash.
     */
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
