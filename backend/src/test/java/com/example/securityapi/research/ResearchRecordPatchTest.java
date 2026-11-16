package com.example.securityapi.research;

import com.example.securityapi.research.ResearchDtos.RecordDetailsPatch;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResearchRecordPatchTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ResearchService service = spy(new ResearchService(jdbc, mapper));

    @Test
    void patchDeepMergesWithoutRemovingOriginalProjectFields() throws Exception {
        JsonNode original = mapper.readTree("""
            {"technologies":["Kafka"],"deep_research":{"architecture":{"existing":"keep"},"status":"draft"}}
            """);
        var row = Map.<String, Object>of("id", 10L, "record_type", "it_project", "attributes", original);
        when(jdbc.query(eq("SELECT id,record_type,attributes FROM research_records WHERE id=? FOR UPDATE"),
                any(RowMapper.class), eq(10L))).thenReturn(List.of(row));
        doReturn(Map.of("id", 10L)).when(service).record(10L);
        JsonNode patch = mapper.readTree("""
            {"deep_research":{"architecture":{"new":"added"},"status":"completed"}}
            """);

        var result = service.updateRecordDetails(10L, new RecordDetailsPatch(patch, null));

        assertEquals(10L, result.get("id"));
        var updated = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(startsWith("UPDATE research_records SET attributes="),
                updated.capture(), isNull(), eq(10L));
        JsonNode json = mapper.readTree(updated.getValue());
        assertEquals("Kafka", json.at("/technologies/0").asText());
        assertEquals("keep", json.at("/deep_research/architecture/existing").asText());
        assertEquals("added", json.at("/deep_research/architecture/new").asText());
        assertEquals("completed", json.at("/deep_research/status").asText());
    }

    @Test
    void rejectsNonObjectAttributes() throws Exception {
        var error = assertThrows(ResponseStatusException.class,
                () -> service.updateRecordDetails(10L,
                        new RecordDetailsPatch(mapper.readTree("[]"), null)));
        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }

    @Test
    void missingRecordReturnsNotFound() throws Exception {
        when(jdbc.query(eq("SELECT id,record_type,attributes FROM research_records WHERE id=? FOR UPDATE"),
                any(RowMapper.class), eq(99L))).thenReturn(List.of());
        var error = assertThrows(ResponseStatusException.class,
                () -> service.updateRecordDetails(99L,
                        new RecordDetailsPatch(mapper.readTree("{\"deep_research\":{\"status\":\"done\"}}"), null)));
        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    void nonProjectRecordsCannotBeUpdatedThroughProjectPatch() throws Exception {
        var row = Map.<String, Object>of("id", 10L, "record_type", "company",
                "attributes", mapper.createObjectNode());
        when(jdbc.query(eq("SELECT id,record_type,attributes FROM research_records WHERE id=? FOR UPDATE"),
                any(RowMapper.class), eq(10L))).thenReturn(List.of(row));
        var error = assertThrows(ResponseStatusException.class,
                () -> service.updateRecordDetails(10L,
                        new RecordDetailsPatch(mapper.readTree("{\"x\":1}"), null)));
        assertEquals(400, error.getStatusCode().value());
    }
}
