package com.brandPitara.sfs.service.model;

import com.brandPitara.sfs.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserLoginResult {
    private User user;
    private boolean newUser;
}
