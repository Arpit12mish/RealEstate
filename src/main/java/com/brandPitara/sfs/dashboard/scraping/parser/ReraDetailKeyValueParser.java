package com.brandPitara.sfs.dashboard.scraping.parser;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic HTML key-value extractor for RERA detail pages.
 *
 * Applies five parsing strategies in order; earlier strategies take precedence
 * for the same normalized key:
 *   A. Two-column tables  (col-0 = key, col-1 = value)
 *   B. Definition lists   (<dt> = key, <dd> = value)
 *   C. Label-colon pattern (element text ending in ":" followed by next sibling)
 *   D. Horizontal header+data tables (thead <th> row zipped with first tbody row)
 *   E. Three-column label/value tables (col-0 or col-1 is label, col-2 is value)
 *
 * All keys are normalized: trimmed → lowercased → non-alphanumeric stripped.
 * Example: "RERA Registration No." → "reraregistrationno"
 *
 * Phase S3 will add source-specific sub-parsers on top of this generic base.
 */
@Slf4j
@Component
public class ReraDetailKeyValueParser {

    public Map<String, String> parse(String html, String baseUrl) {
        Document doc = Jsoup.parse(html, baseUrl);
        Map<String, String> result = new LinkedHashMap<>();

        parseTwoColumnTables(doc, result);
        parseDefinitionLists(doc, result);
        parseLabelColonPattern(doc, result);
        parseHorizontalHeaderTables(doc, result);
        parseThreeColumnLabelValueTables(doc, result);

        log.debug("ReraDetailKeyValueParser extracted {} key-value pairs", result.size());
        return result;
    }

    // -------------------------------------------------------------------------
    // Strategy A — Two-column tables
    // -------------------------------------------------------------------------

    private void parseTwoColumnTables(Document doc, Map<String, String> result) {
        for (Element table : doc.select("table")) {
            for (Element row : table.select("tr")) {
                Elements cells = row.select("td, th");
                if (cells.size() != 2) continue;

                String key   = normalizeKey(cells.get(0).text());
                String value = cells.get(1).text().strip();

                if (!key.isBlank() && !value.isBlank() && !result.containsKey(key)) {
                    result.put(key, value);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Strategy B — Definition lists
    // -------------------------------------------------------------------------

    private void parseDefinitionLists(Document doc, Map<String, String> result) {
        for (Element dl : doc.select("dl")) {
            Elements children = dl.children();
            for (int i = 0; i < children.size(); i++) {
                Element child = children.get(i);
                if (!"dt".equalsIgnoreCase(child.tagName())) continue;

                String key = normalizeKey(child.text());
                if (key.isBlank()) continue;

                // Collect consecutive <dd> siblings
                StringBuilder value = new StringBuilder();
                while (i + 1 < children.size()
                        && "dd".equalsIgnoreCase(children.get(i + 1).tagName())) {
                    i++;
                    String part = children.get(i).text().strip();
                    if (!part.isBlank()) {
                        if (value.length() > 0) value.append(", ");
                        value.append(part);
                    }
                }

                String valueStr = value.toString().strip();
                if (!valueStr.isBlank() && !result.containsKey(key)) {
                    result.put(key, valueStr);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Strategy C — Label-colon pattern
    // -------------------------------------------------------------------------

    private void parseLabelColonPattern(Document doc, Map<String, String> result) {
        for (Element el : doc.getAllElements()) {
            String ownText = el.ownText().strip();
            if (!ownText.endsWith(":") || ownText.length() < 2) continue;

            String key = normalizeKey(ownText.substring(0, ownText.length() - 1));
            if (key.isBlank() || result.containsKey(key)) continue;

            Element sibling = el.nextElementSibling();
            if (sibling == null) continue;

            String value = sibling.text().strip();
            if (!value.isBlank() && !value.endsWith(":")) {
                result.put(key, value);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Strategy D — Horizontal header+data tables
    // -------------------------------------------------------------------------

    private void parseHorizontalHeaderTables(Document doc, Map<String, String> result) {
        for (Element table : doc.select("table")) {
            Element thead = table.selectFirst("thead");
            Element headerRow = thead != null
                    ? thead.selectFirst("tr")
                    : table.selectFirst("tr:has(th):not(:has(td))");
            if (headerRow == null) continue;

            Elements headers = headerRow.select("th");
            if (headers.isEmpty()) continue;

            Element tbody = table.selectFirst("tbody");
            Element dataRow = tbody != null
                    ? tbody.selectFirst("tr")
                    : headerRow.nextElementSibling();
            if (dataRow == null) continue;

            Elements cells = dataRow.select("td");
            if (cells.isEmpty()) continue;

            int count = Math.min(headers.size(), cells.size());
            for (int i = 0; i < count; i++) {
                String key   = normalizeKey(headers.get(i).text());
                String value = cells.get(i).text().strip();
                if (!key.isBlank() && !value.isBlank() && !result.containsKey(key)) {
                    result.put(key, value);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Strategy E — Three-column label/value tables
    // -------------------------------------------------------------------------

    private void parseThreeColumnLabelValueTables(Document doc, Map<String, String> result) {
        for (Element table : doc.select("table")) {
            for (Element row : table.select("tr")) {
                Elements cells = row.select("td");
                if (cells.size() != 3) continue;

                String col0 = cells.get(0).text().strip();
                String col1 = cells.get(1).text().strip();
                String col2 = cells.get(2).text().strip();
                if (col2.isBlank()) continue;

                String key = null;
                if (!col0.isBlank() && col1.isBlank()) {
                    key = normalizeKey(col0);
                } else if (col0.isBlank() && !col1.isBlank()) {
                    key = normalizeKey(col1);
                }

                if (key != null && !key.isBlank() && !result.containsKey(key)) {
                    result.put(key, col2);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Normalization
    // -------------------------------------------------------------------------

    private String normalizeKey(String text) {
        return text.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
