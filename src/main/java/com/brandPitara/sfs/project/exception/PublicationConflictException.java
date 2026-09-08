package com.brandPitara.sfs.project.exception;

import lombok.Getter;

/**
 * Domain conflict raised when a publication-state transition would violate
 * the project/builder public-visibility invariant.
 */
@Getter
public class PublicationConflictException extends IllegalStateException {

    private final String code;

    public PublicationConflictException(String code, String message) {
        super(message);
        this.code = code;
    }
}
