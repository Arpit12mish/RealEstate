package com.brandPitara.sfs.dbsearch.mapper;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.dbsearch.dto.SearchEntityType;
import com.brandPitara.sfs.dbsearch.dto.SearchItemDto;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.List;

@Component
public class SearchMapper {

    public SearchItemDto toProjectItem(
            ProjectEntity p,
            String imageUrl,
            List<String> tags
    ) {
        return SearchItemDto.builder()
                .id(p.getId())
                .entityType(SearchEntityType.PROJECT)
                .title(p.getName())
                .subtitle(buildProjectSubtitle(p))
                .imageUrl(imageUrl)
                .slug(p.getSlug())
                .cityId(p.getCity() != null ? p.getCity().getId() : null)
                .cityName(p.getCity() != null ? p.getCity().getName() : null)
                .builderId(p.getBuilder() != null ? p.getBuilder().getId() : null)
                .builderName(p.getBuilder() != null ? p.getBuilder().getName() : null)
                .priorityRank(p.getPriority())
                .location(buildProjectLocation(p))
                .priceMin(p.getPriceMin())
                .priceMax(p.getPriceMax())
                .priceLabel(buildPriceLabel(p.getPriceMin(), p.getPriceMax()))
                .tags(tags)
                .build();
    }

    public SearchItemDto toBuilderItem(BuilderEntity b) {
        return SearchItemDto.builder()
                .id(b.getId())
                .entityType(SearchEntityType.BUILDER)
                .title(b.getName())
                .subtitle(b.getCity() != null ? b.getCity().getName() : null)
                .imageUrl(b.getLogoUrl())
                .cityId(b.getCity() != null ? b.getCity().getId() : null)
                .cityName(b.getCity() != null ? b.getCity().getName() : null)
                .priorityRank(b.getPriority())
                .location(b.getAddressLine())
                .build();
    }

    public SearchItemDto toCompanyItem(CompanyEntity c) {
        return SearchItemDto.builder()
                .id(c.getId())
                .entityType(resolveCompanyEntityType(c.getCompanyType()))
                .title(c.getName())
                .subtitle(c.getCompanyType())
                .imageUrl(c.getLogoUrl())
                .slug(c.getSlug())
                .companyType(c.getCompanyType())
                .priorityRank(c.getPriority())
                .build();
    }

    private String buildProjectSubtitle(ProjectEntity p) {
        String builderName = p.getBuilder() != null ? p.getBuilder().getName() : null;
        String cityName = p.getCity() != null ? p.getCity().getName() : null;

        if (builderName != null && cityName != null) {
            return builderName + " • " + cityName;
        }
        if (builderName != null) return builderName;
        return cityName;
    }

    private String buildProjectLocation(ProjectEntity p) {
        String address = p.getAddressLine() != null ? p.getAddressLine().trim() : null;
        String city = p.getCity() != null ? p.getCity().getName() : null;

        boolean hasAddress = address != null && !address.isBlank();
        boolean hasCity = city != null && !city.isBlank();

        if (hasAddress && hasCity) {
            return address + ", " + city;
        }
        if (hasAddress) return address;
        return city;
    }

    private String buildPriceLabel(Long min, Long max) {
        if (min == null && max == null) return null;
        if (min != null && max != null) {
            if (min.equals(max)) {
                return formatIndianPrice(min);
            }
            return formatIndianPrice(min) + " - " + formatIndianPrice(max);
        }
        if (min != null) {
            return "From " + formatIndianPrice(min);
        }
        return "Up to " + formatIndianPrice(max);
    }

    private String formatIndianPrice(Long amount) {
        if (amount == null) return null;

        double value = amount.doubleValue();

        if (value >= 10000000) {
            return "₹" + trimZero(value / 10000000d) + "Cr";
        }
        if (value >= 100000) {
            return "₹" + trimZero(value / 100000d) + "L";
        }
        if (value >= 1000) {
            return "₹" + trimZero(value / 1000d) + "K";
        }
        return "₹" + amount;
    }

    private String trimZero(double value) {
        DecimalFormat df = new DecimalFormat("0.#");
        return df.format(value);
    }

    private SearchEntityType resolveCompanyEntityType(String companyType) {
        if (companyType == null) return SearchEntityType.COMPANY;

        String normalized = companyType.trim().toUpperCase();
        return switch (normalized) {
            case "ARCHITECT" -> SearchEntityType.ARCHITECT;
            case "DESIGNER" -> SearchEntityType.DESIGNER;
            default -> SearchEntityType.COMPANY;
        };
    }
}