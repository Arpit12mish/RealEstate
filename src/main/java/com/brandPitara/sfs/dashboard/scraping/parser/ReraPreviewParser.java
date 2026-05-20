package com.brandPitara.sfs.dashboard.scraping.parser;

import com.brandPitara.sfs.dashboard.scraping.dto.ScrapeExtractedPreviewDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic table-based HTML parser.
 *
 * Phase S1: Parses any HTML table structure without assumptions about
 * a specific RERA portal layout. Source-specific parsers will be added
 * in Phase S3 for individual portals (e.g. MahaRERA, RERA Gujarat).
 */
@Slf4j
@Component
public class ReraPreviewParser {

    private static final int MAX_RECORDS = 10;
    private static final int FALLBACK_TEXT_PREVIEW_LENGTH = 1000;

    public List<ScrapeExtractedPreviewDto> parse(String html, String baseUrl) {
        Document doc = Jsoup.parse(html, baseUrl);
        Elements tables = doc.select("table");

        if (tables.isEmpty()) {
            log.debug("No tables found, returning fallback text preview for url={}", baseUrl);
            return List.of(buildFallbackRecord(doc, baseUrl));
        }

        List<ScrapeExtractedPreviewDto> results = new ArrayList<>();
        for (Element table : tables) {
            if (results.size() >= MAX_RECORDS) break;
            int remaining = MAX_RECORDS - results.size();
            results.addAll(parseTable(table, baseUrl, remaining));
        }

        if (results.isEmpty()) {
            return List.of(buildFallbackRecord(doc, baseUrl));
        }

        return results;
    }

    private List<ScrapeExtractedPreviewDto> parseTable(Element table, String baseUrl, int limit) {
        Elements rows = table.select("tr");
        if (rows.isEmpty()) return List.of();

        Element firstRow = rows.first();
        Elements thElements = firstRow.select("th");

        List<String> headers;
        if (!thElements.isEmpty()) {
            headers = thElements.stream()
                    .map(th -> normalizeKey(th.text()))
                    .toList();
        } else {
            headers = firstRow.select("td").stream()
                    .map(td -> normalizeKey(td.text()))
                    .toList();
        }

        if (headers.isEmpty()) return List.of();

        List<ScrapeExtractedPreviewDto> records = new ArrayList<>();

        for (int i = 1; i < rows.size() && records.size() < limit; i++) {
            Element row = rows.get(i);
            Elements cells = row.select("td");
            if (cells.isEmpty()) continue;

            Map<String, Object> raw = new LinkedHashMap<>();
            String detailUrl = null;

            for (int j = 0; j < Math.min(headers.size(), cells.size()); j++) {
                String key = headers.get(j);
                if (key.isBlank()) continue;

                Element cell = cells.get(j);
                raw.put(key, cell.text().strip());

                if (detailUrl == null) {
                    Element anchor = cell.selectFirst("a[href]");
                    if (anchor != null) {
                        String href = anchor.absUrl("href");
                        if (!href.isBlank()) detailUrl = href;
                    }
                }
            }

            if (detailUrl != null) raw.put("detailUrl", detailUrl);

            boolean hasContent = raw.values().stream()
                    .anyMatch(v -> v != null && !v.toString().isBlank());
            if (!hasContent) continue;

            records.add(ScrapeExtractedPreviewDto.builder()
                    .projectName(inferField(raw, "project", "projectname", "name"))
                    .builderName(inferField(raw, "builder", "promoter", "developer", "company"))
                    .reraNumber(inferField(raw, "rera", "registration", "regno", "registrationno"))
                    .cityName(inferField(raw, "city", "district", "location"))
                    .statusText(inferField(raw, "status", "projectstatus"))
                    .sourceUrl(detailUrl != null ? detailUrl : baseUrl)
                    .raw(raw)
                    .build());
        }

        return records;
    }

    private String normalizeKey(String text) {
        return text.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private String inferField(Map<String, Object> raw, String... hints) {
        for (String hint : hints) {
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                if (entry.getKey().contains(hint) && entry.getValue() != null) {
                    String val = entry.getValue().toString().strip();
                    if (!val.isBlank()) return val;
                }
            }
        }
        return null;
    }

    private ScrapeExtractedPreviewDto buildFallbackRecord(Document doc, String baseUrl) {
        String bodyText = doc.body() != null ? doc.body().text() : "";
        String preview = bodyText.length() > FALLBACK_TEXT_PREVIEW_LENGTH
                ? bodyText.substring(0, FALLBACK_TEXT_PREVIEW_LENGTH)
                : bodyText;

        return ScrapeExtractedPreviewDto.builder()
                .sourceUrl(baseUrl)
                .raw(Map.of("textPreview", preview))
                .build();
    }
}
