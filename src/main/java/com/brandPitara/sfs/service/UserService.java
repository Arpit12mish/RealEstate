package com.brandPitara.sfs.service;

// import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.service.model.UserLoginResult;

public interface UserService {
    UserLoginResult findOrCreateVerifiedUserByPhone(String phoneNumber);
}
