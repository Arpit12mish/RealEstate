package com.brandPitara.sfs.migration.wordpress.importer;

import com.brandPitara.sfs.cms.metadata.dto.CmsCategoryRequest;
import com.brandPitara.sfs.cms.metadata.dto.CmsTagRequest;
import com.brandPitara.sfs.cms.metadata.service.CmsMetadataService;
import com.brandPitara.sfs.cms.taxonomy.entity.CmsContentCategoryEntity;
import com.brandPitara.sfs.cms.taxonomy.entity.CmsContentTagEntity;
import com.brandPitara.sfs.cms.taxonomy.repository.CmsContentCategoryRepository;
import com.brandPitara.sfs.cms.taxonomy.repository.CmsContentTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Idempotent find-or-create for WordPress categories/tags: looks up by the same
 * case-insensitive normalized name {@link com.brandPitara.sfs.cms.metadata.service.impl.CmsMetadataServiceImpl}
 * writes on create, and only calls {@link CmsMetadataService} to create when genuinely absent -
 * re-running never produces a duplicate category or tag.
 */
@Service
@RequiredArgsConstructor
public class WordPressTaxonomyResolver {

    private final CmsMetadataService metadataService;
    private final CmsContentCategoryRepository categoryRepository;
    private final CmsContentTagRepository tagRepository;

    private static final int CATEGORY_NAME_MAX = 150;
    private static final int TAG_NAME_MAX = 100;

    @Transactional
    public CmsContentCategoryEntity resolveCategory(String name) {
        String safeName = sanitize(name, CATEGORY_NAME_MAX);
        String normalized = normalize(safeName);
        return categoryRepository.findByNormalizedName(normalized).orElseGet(() -> {
            var response = metadataService.createCategory(new CmsCategoryRequest(safeName, null, null, true, null));
            return categoryRepository.findById(response.id()).orElseThrow();
        });
    }

    @Transactional
    public List<CmsContentTagEntity> resolveTags(List<String> names) {
        Set<CmsContentTagEntity> resolved = new LinkedHashSet<>();
        for (String name : names) {
            String safeName = sanitize(name, TAG_NAME_MAX);
            if (safeName.isBlank()) {
                continue;
            }
            String normalized = normalize(safeName);
            CmsContentTagEntity tag = tagRepository.findByNormalizedName(normalized).orElseGet(() -> {
                var response = metadataService.createTag(new CmsTagRequest(safeName, null, true, null));
                return tagRepository.findById(response.id()).orElseThrow();
            });
            resolved.add(tag);
        }
        return List.copyOf(resolved);
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /**
     * Strips ISO control characters (a raw WordPress term name can carry one, which
     * {@code CmsMetadataServiceImpl.text()} rejects outright as "invalid") and caps length at the
     * CMS's own limit - truncating at a word boundary so a long WordPress tag/category name still
     * resolves to something usable instead of failing the whole post import.
     */
    private String sanitize(String value, int maxLength) {
        String source = value == null ? "" : value;
        StringBuilder cleaned = new StringBuilder(source.length());
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            cleaned.append(Character.isISOControl(c) ? ' ' : c);
        }
        String collapsed = cleaned.toString().trim().replaceAll("\\s+", " ");
        if (collapsed.length() <= maxLength) {
            return collapsed;
        }
        String truncated = collapsed.substring(0, maxLength);
        int lastSpace = truncated.lastIndexOf(' ');
        return (lastSpace > maxLength / 2 ? truncated.substring(0, lastSpace) : truncated).trim();
    }
}
