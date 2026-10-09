package com.brandPitara.sfs.marketplace.service;

import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.marketplace.dto.PublicContactResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerRateResponse;
import com.brandPitara.sfs.provider.entity.ProviderRateEntity;
import com.brandPitara.sfs.provider.enums.ProviderRateType;
import com.brandPitara.sfs.util.PhoneNumberNormalizer;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Stateless helpers shared by the dealer and worker public projections. */
public final class MarketplaceMappers {

    private MarketplaceMappers() {
    }

    /**
     * Builds validated call/WhatsApp handles. Mobile numbers are normalised to E.164 by the same
     * rules as login; an Indian STD landline (0 + 10 digits) is accepted for calls only, since it
     * cannot receive WhatsApp. Anything else is dropped rather than producing a broken link.
     */
    public static PublicContactResponse contact(String phone, String whatsapp) {
        String callNumber = normalizeForCall(phone);
        String waNumber = normalizeMobile(StringUtils.hasText(whatsapp) ? whatsapp : null);
        return new PublicContactResponse(
                callNumber,
                callNumber == null ? null : "tel:" + callNumber,
                waNumber,
                waNumber == null ? null : "https://wa.me/" + waNumber.substring(1)
        );
    }

    static String normalizeMobile(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            return PhoneNumberNormalizer.normalize(raw);
        } catch (ResponseStatusException ex) {
            return null;
        }
    }

    static String normalizeForCall(String raw) {
        String mobile = normalizeMobile(raw);
        if (mobile != null) return mobile;
        if (!StringUtils.hasText(raw)) return null;
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 11 && digits.startsWith("0") && digits.charAt(1) != '0') {
            return "+91" + digits.substring(1);
        }
        return null;
    }

    /** "Manish Singh" -> "MS", "Lata" -> "L". */
    public static String initials(String name) {
        if (!StringUtils.hasText(name)) return null;
        List<String> parts = Arrays.stream(name.trim().split("\\s+"))
                .filter(p -> !p.isEmpty() && Character.isLetterOrDigit(p.codePointAt(0)))
                .toList();
        if (parts.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        sb.appendCodePoint(parts.get(0).codePointAt(0));
        if (parts.size() > 1) sb.appendCodePoint(parts.get(parts.size() - 1).codePointAt(0));
        return sb.toString().toUpperCase(Locale.ROOT);
    }

    public static String locationText(BusinessEntity b) {
        List<String> parts = new ArrayList<>(2);
        String local = firstText(b.getLocality(), b.getLandmark(), b.getAddressLine2());
        if (local != null) parts.add(local);
        if (b.getCity() != null && StringUtils.hasText(b.getCity().getName())) parts.add(b.getCity().getName());
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    public static String rateLabel(ProviderRateType type) {
        return switch (type) {
            case VISITING_CHARGE -> "Visiting Charge";
            case SERVICE_FEE -> "Service Fee";
            case MATERIAL_COST -> "Material Cost";
            case HOURLY_RATE -> "Hourly Rate";
            case DAILY_RATE -> "Daily Rate";
        };
    }

    public static WorkerRateResponse rate(ProviderRateEntity r) {
        return new WorkerRateResponse(
                r.getRateType(),
                rateLabel(r.getRateType()),
                r.getAmount(),
                r.getCurrency(),
                r.getUnit(),
                r.getNote()
        );
    }

    /** Visiting charge first (what the worker detail tile shows), otherwise the first listed rate. */
    public static WorkerRateResponse primaryRate(List<ProviderRateEntity> ordered) {
        if (ordered == null || ordered.isEmpty()) return null;
        return ordered.stream()
                .filter(r -> r.getRateType() == ProviderRateType.VISITING_CHARGE)
                .findFirst()
                .or(() -> ordered.stream().findFirst())
                .map(MarketplaceMappers::rate)
                .orElse(null);
    }

    static String firstText(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) return v.trim();
        }
        return null;
    }
}
