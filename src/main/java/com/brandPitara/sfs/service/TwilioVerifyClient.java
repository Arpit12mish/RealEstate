package com.brandPitara.sfs.service;

/** External Twilio Verify boundary. Implementations must not be called from a DB transaction. */
public interface TwilioVerifyClient {

    VerificationResult sendVerification(String phoneNumber);

    VerificationResult checkVerification(String phoneNumber, String code);

    record VerificationResult(String sid, String status) {
    }
}
