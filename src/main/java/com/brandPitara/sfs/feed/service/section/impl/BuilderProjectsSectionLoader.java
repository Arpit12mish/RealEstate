package com.brandPitara.sfs.feed.service.section.impl;

import com.brandPitara.sfs.feed.entity.FeedSectionConfigEntity;
import com.brandPitara.sfs.feed.service.section.FeedContext;
import com.brandPitara.sfs.feed.service.section.FeedSectionLoader;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.builder.dto.BuilderProjectCardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BuilderProjectsSectionLoader implements FeedSectionLoader {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public String supports() {
    return "BUILDER_PROJECTS";
  }

  @Override
  public HomeSectionDto<?> load(FeedSectionConfigEntity cfg, FeedContext ctx) {
    if (ctx == null || ctx.entityId() == null) return null;

    int limit = cfg.getMaxItems() == null ? 10 : Math.max(1, Math.min(cfg.getMaxItems(), 50));

    // param1 decides which “bucket”:
    // RECENT / LUXURY / ICONIC (you can expand later without changing API)
    String bucket = cfg.getParam1() == null ? "RECENT" : cfg.getParam1().trim().toUpperCase();

    // NOTE: This assumes your table names/columns similar to your existing API response.
    // If column names differ, you only edit SQL here (single place).
    String sql = """
        select
          p.id as id,
          p.name as name,
          p.city_id as city_id,
          c.name as city_name,
          p.address_line as address_line,
          p.price_min as price_min,
          p.price_max as price_max,
          b.id as builder_id,
          b.name as builder_name,
          b.logo_url as builder_logo_url,
          p.cover_media_url as cover_media_url,
          p.cover_media_type as cover_media_type
        from project p
        left join city c on c.id = p.city_id
        left join builder b on b.id = p.builder_id
        where p.deleted = false
          and p.published = true
          and p.active = true
          and p.builder_id = ?
        order by p.priority asc, p.id desc
        limit ?
        """;

    List<BuilderProjectCardDto> items = jdbcTemplate.query(
        sql,
        (rs, rowNum) -> BuilderProjectCardDto.builder()
            .id(rs.getLong("id"))
            .name(rs.getString("name"))
            .cityId((Long) rs.getObject("city_id"))
            .cityName(rs.getString("city_name"))
            .addressLine(rs.getString("address_line"))
            .priceMin((Long) rs.getObject("price_min"))
            .priceMax((Long) rs.getObject("price_max"))
            .builderId((Long) rs.getObject("builder_id"))
            .builderName(rs.getString("builder_name"))
            .builderLogoUrl(rs.getString("builder_logo_url"))
            .coverMediaUrl(rs.getString("cover_media_url"))
            .coverMediaType(rs.getString("cover_media_type"))
            .build(),
        ctx.entityId(),
        limit
    );

    if (items == null || items.isEmpty()) return null;

    String key = switch (bucket) {
      case "LUXURY" -> "PROJECTS_LUXURY";
      case "ICONIC" -> "PROJECTS_ICONIC";
      default -> "PROJECTS_RECENT";
    };

    return HomeSectionDto.<BuilderProjectCardDto>builder()
    .key(key)
    .type(HomeSectionType.TOP_PROJECTS)
    .title(cfg.getTitle())
    .items(items)
    .build();
  }
}