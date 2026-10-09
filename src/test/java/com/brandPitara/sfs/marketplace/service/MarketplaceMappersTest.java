package com.brandPitara.sfs.marketplace.service;

import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.marketplace.dto.PublicContactResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerRateResponse;
import com.brandPitara.sfs.provider.entity.ProviderRateEntity;
import com.brandPitara.sfs.provider.enums.ProviderRateType;
import com.brandPitara.sfs.provider.enums.ProviderRateUnit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarketplaceMappersTest {

    @Test
    void mobileNumbersBecomeE164CallAndWhatsappLinks() {
        PublicContactResponse c = MarketplaceMappers.contact("98765 43210", "+91 98765-43210");

        assertThat(c.phone()).isEqualTo("+919876543210");
        assertThat(c.callUrl()).isEqualTo("tel:+919876543210");
        assertThat(c.whatsappPhone()).isEqualTo("+919876543210");
        assertThat(c.whatsappUrl()).isEqualTo("https://wa.me/919876543210");
    }

    @Test
    void landlineIsCallableButNeverOfferedAsWhatsapp() {
        PublicContactResponse c = MarketplaceMappers.contact("0124-4567890", "0124-4567890");

        assertThat(c.callUrl()).isEqualTo("tel:+911244567890");
        assertThat(c.whatsappUrl()).isNull();
    }

    @Test
    void invalidOrMissingNumbersProduceNoActions() {
        PublicContactResponse c = MarketplaceMappers.contact("12345", null);

        assertThat(c.phone()).isNull();
        assertThat(c.callUrl()).isNull();
        assertThat(c.whatsappUrl()).isNull();
    }

    @Test
    void initialsUseFirstAndLastNameParts() {
        assertThat(MarketplaceMappers.initials("Manish Singh")).isEqualTo("MS");
        assertThat(MarketplaceMappers.initials("  lata   devi patel ")).isEqualTo("LP");
        assertThat(MarketplaceMappers.initials("Rakesh")).isEqualTo("R");
        assertThat(MarketplaceMappers.initials("   ")).isNull();
    }

    @Test
    void primaryRatePrefersVisitingChargeAndKeepsItsOwnLabel() {
        ProviderRateEntity fee = rate(ProviderRateType.SERVICE_FEE, "450.00");
        ProviderRateEntity visit = rate(ProviderRateType.VISITING_CHARGE, "150.00");

        WorkerRateResponse primary = MarketplaceMappers.primaryRate(List.of(fee, visit));

        assertThat(primary.type()).isEqualTo(ProviderRateType.VISITING_CHARGE);
        assertThat(primary.label()).isEqualTo("Visiting Charge");
        assertThat(primary.amount()).isEqualByComparingTo("150.00");

        WorkerRateResponse fallback = MarketplaceMappers.primaryRate(List.of(fee));
        assertThat(fallback.label()).isEqualTo("Service Fee");
        assertThat(MarketplaceMappers.primaryRate(List.of())).isNull();
    }

    @Test
    void locationTextPrefersLocalityThenLandmark() {
        CityEntity city = new CityEntity();
        city.setName("Gurgaon");
        BusinessEntity b = BusinessEntity.builder().city(city).locality("Sector 26").landmark("Near Metro").build();

        assertThat(MarketplaceMappers.locationText(b)).isEqualTo("Sector 26, Gurgaon");
        b.setLocality(null);
        assertThat(MarketplaceMappers.locationText(b)).isEqualTo("Near Metro, Gurgaon");
    }

    private static ProviderRateEntity rate(ProviderRateType type, String amount) {
        return ProviderRateEntity.builder()
                .rateType(type)
                .amount(new BigDecimal(amount))
                .currency("INR")
                .unit(ProviderRateUnit.PER_VISIT)
                .build();
    }
}
