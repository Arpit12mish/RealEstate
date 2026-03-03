package com.brandPitara.sfs.mapper;


import com.brandPitara.sfs.dto.CategoryCardDto;
import com.brandPitara.sfs.entity.CategoryEntity;

public class CategoryCardMapper {
  public static CategoryCardDto toCard(CategoryEntity c) {
    return CategoryCardDto.builder()
        .id(c.getId())
        .name(c.getName())
        .slug(c.getSlug())
        .build();
  }
}

