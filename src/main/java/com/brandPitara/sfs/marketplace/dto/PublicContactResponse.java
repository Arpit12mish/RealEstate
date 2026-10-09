package com.brandPitara.sfs.marketplace.dto;

/**
 * Validated public contact handles. A field is null when the stored value is missing or not a
 * dialable number, so clients can hide/disable the action instead of opening a broken link.
 */
public record PublicContactResponse(
        String phone,
        String callUrl,
        String whatsappPhone,
        String whatsappUrl
) {
    public static final PublicContactResponse NONE = new PublicContactResponse(null, null, null, null);
}
