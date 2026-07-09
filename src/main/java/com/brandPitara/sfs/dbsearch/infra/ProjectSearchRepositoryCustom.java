package com.brandPitara.sfs.dbsearch.infra;

import com.brandPitara.sfs.dbsearch.app.SearchCriteria;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectSearchRepositoryCustom {
    Page<ProjectEntity> searchProjects(SearchCriteria criteria, Pageable pageable);
}
