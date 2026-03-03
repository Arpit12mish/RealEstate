package com.brandPitara.sfs.home.controller.publicapi;

import com.brandPitara.sfs.home.dto.HomeCategoryDto;
import com.brandPitara.sfs.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/home")
@RequiredArgsConstructor
public class HomeCategoriesPublicController {

  private final CategoryRepository categoryRepository;

  @GetMapping("/categories")
  public List<HomeCategoryDto> listCategories() {
    return categoryRepository.findByActiveTrueOrderByPriorityAscIdAsc()
        .stream()
        .map(c -> new HomeCategoryDto(
            c.getId(),
            c.getName(),
            c.getSlug(),
            c.getPriority()
        ))
        .toList();
  }
}
