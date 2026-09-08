package com.brandPitara.sfs.security.identity;

import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.DashboardPermission;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public record DashboardAuthenticationUserSnapshot(
        Long userId,
        String email,
        String name,
        DashboardRole role,
        boolean active,
        Set<DashboardPermission> permissions
) implements UserDetails {

    public DashboardAuthenticationUserSnapshot {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public DashboardAuthenticationUserSnapshot(
            Long userId,
            String email,
            String name,
            DashboardRole role,
            boolean active
    ) {
        this(userId, email, name, role, active, Set.of());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));

        Set<DashboardPermission> effectivePermissions = role == DashboardRole.ADMIN
                ? Set.of(DashboardPermission.values())
                : permissions;
        effectivePermissions.stream()
                .map(Enum::name)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return Set.copyOf(authorities);
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return active;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return active;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
