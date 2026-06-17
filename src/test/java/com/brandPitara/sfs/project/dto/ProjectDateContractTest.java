package com.brandPitara.sfs.project.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectDateContractTest {

  private final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @Test
  void upsertRequestAcceptsProjectStartDateAlias() throws Exception {
    ProjectUpsertRequest request = objectMapper.readValue(
        "{\"name\":\"Project 41\",\"projectStartDate\":\"2023-04-01\"}",
        ProjectUpsertRequest.class
    );

    assertThat(request.getStartDate()).isEqualTo(LocalDate.of(2023, 4, 1));
  }

  @Test
  void patchRequestAcceptsProjectStartDateAlias() throws Exception {
    ProjectPatchRequest request = objectMapper.readValue(
        "{\"projectStartDate\":\"2023-04-01\"}",
        ProjectPatchRequest.class
    );

    assertThat(request.getStartDate()).isEqualTo(LocalDate.of(2023, 4, 1));
  }

  @Test
  void publicResponseSerializesBothStartDateAndProjectStartDate() throws Exception {
    ProjectPublicResponse response = ProjectPublicResponse.builder()
        .id(41L)
        .startDate(LocalDate.of(2023, 4, 1))
        .build();

    String json = objectMapper.writeValueAsString(response);

    assertThat(json).contains("\"startDate\":\"2023-04-01\"");
    assertThat(json).contains("\"projectStartDate\":\"2023-04-01\"");
  }
}
