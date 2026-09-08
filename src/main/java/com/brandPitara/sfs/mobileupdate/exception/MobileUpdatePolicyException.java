package com.brandPitara.sfs.mobileupdate.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class MobileUpdatePolicyException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public MobileUpdatePolicyException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
