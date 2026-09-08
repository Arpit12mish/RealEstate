package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.service.model.ProjectPublicCoreData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectPublicV2MapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void mapsPublicCoreWithoutViewerMembershipAndKeepsConservativeV1FieldNames() throws Exception {
        ProjectPublicCoreData core = ProjectPublicCoreData.builder()
                .id(27L)
                .name("Project 27")
                .startDate(LocalDate.of(2025, 1, 2))
                .favoriteCount(125L)
                .propertyTypes(Set.of())
                .floorPlanGroups(List.of())
                .glimpses(List.of())
                .failedSections(Set.of())
                .build();

        JsonNode json = objectMapper.valueToTree(ProjectPublicV2Mapper.toResponse(core));
        JsonNode legacyPublicFields = objectMapper.valueToTree(
                ProjectPublicCoreMapper.toV1Response(core, true));
        ((com.fasterxml.jackson.databind.node.ObjectNode) legacyPublicFields).remove("isFavorite");

        assertThat(json.get("id").asLong()).isEqualTo(27L);
        assertThat(json.get("favoriteCount").asLong()).isEqualTo(125L);
        assertThat(json.get("startDate").asText()).isEqualTo("2025-01-02");
        assertThat(json.get("projectStartDate").asText()).isEqualTo("2025-01-02");
        assertThat(json.has("isFavorite")).isFalse();
        assertThat(json).isEqualTo(legacyPublicFields);
    }
}
