package com.brandPitara.sfs.service;

import com.brandPitara.sfs.dto.GuestSessionRequest;
import com.brandPitara.sfs.dto.GuestSessionResponse;
import com.brandPitara.sfs.entity.User;

public interface GuestSessionService {

    GuestSessionResponse createOrReuseGuestSession(GuestSessionRequest request);
    void linkGuestSessionToUser(String installationId, User user);
}