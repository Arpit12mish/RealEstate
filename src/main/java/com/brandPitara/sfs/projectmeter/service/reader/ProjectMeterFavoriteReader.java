package com.brandPitara.sfs.projectmeter.service.reader;

import com.brandPitara.sfs.enums.FavoriteTargetType;
import com.brandPitara.sfs.repository.UserFavoriteRepository;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.security.identity.MobileAuthenticationUserSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProjectMeterFavoriteReader {

    private final UserFavoriteRepository userFavoriteRepository;
    private final UserRepository userRepository;

    public FavoriteState read(Long projectId) {
        long count = userFavoriteRepository.countByTargetTypeAndTargetId(
            FavoriteTargetType.PROJECT,
            projectId
        );

        Optional<Long> userId = currentUserId();
        boolean favorite = userId.isPresent()
            && userFavoriteRepository.existsByUser_IdAndTargetTypeAndTargetId(
                userId.get(),
                FavoriteTargetType.PROJECT,
                projectId
            );

        return new FavoriteState(favorite, count);
    }

    private Optional<Long> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        if (authentication.getPrincipal() instanceof MobileAuthenticationUserSnapshot snapshot) {
            return Optional.ofNullable(snapshot.userId());
        }

        String phone = authentication.getName();
        if (phone == null || phone.isBlank() || "anonymousUser".equalsIgnoreCase(phone)) {
            return Optional.empty();
        }

        return userRepository.findByPhoneNumber(phone).map(user -> user.getId());
    }

    public record FavoriteState(boolean favorite, long count) {
    }
}
