package com.brandPitara.sfs.publiccontent.service;

import com.brandPitara.sfs.publiccontent.dto.PublicCategoryListResponse;
import com.brandPitara.sfs.publiccontent.dto.PublicCategoryResponse;
import com.brandPitara.sfs.publiccontent.repository.PublicCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicCategoryServiceImpl implements PublicCategoryService {

    private final PublicCategoryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public PublicCategoryListResponse list() {
        List<PublicCategoryResponse> items = repository.findActiveCategorySummaries().stream()
                .filter(view -> view.getPublishedContentCount() != null && view.getPublishedContentCount() > 0)
                .map(view -> new PublicCategoryResponse(
                        view.getId(), view.getName(), view.getSlug(), view.getDescription(),
                        view.getPublishedContentCount()
                ))
                .toList();
        return new PublicCategoryListResponse(items);
    }
}
