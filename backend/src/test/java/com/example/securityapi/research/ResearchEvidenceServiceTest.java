package com.example.securityapi.research;

import com.example.securityapi.research.ResearchEvidenceDtos.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResearchEvidenceServiceTest {
    private final ResearchEvidenceRepository repository = mock(ResearchEvidenceRepository.class);
    private final ResearchEvidenceService service = new ResearchEvidenceService(repository);

    @Test
    void normalizesCaseAndFragmentButRetainsQuery() {
        assertEquals("https://example.com/file%20name.pdf?a=1",
                ResearchEvidenceService.normalizeUrl("https://EXAMPLE.com/file%20name.pdf?a=1#section"));
    }

    @Test
    void rejectsNonHttps() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ResearchEvidenceService.normalizeUrl("http://127.0.0.1/private"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void savesSourceAndVersionedSummary() throws Exception {
        when(repository.recordExists(10L)).thenReturn(true);
        when(repository.upsertSource(anyLong(), anyString(), anyString(), nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), nullable(String.class), nullable(java.time.OffsetDateTime.class))).thenReturn(100L);
        when(repository.nextVersion(100L)).thenReturn(2);
        when(repository.insertSummary(eq(100L), eq(2), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), nullable(String.class), nullable(String.class))).thenReturn(501L);
        var json = new ObjectMapper();
        SaveSummaryRequest request = new SaveSummaryRequest(
                "https://example.com/article", null, "Article", "text/html", null, null, null,
                "Summary text", json.readTree("[\"FHIR\"]"), null, null, null, null,
                "elearning-gpt6-dev", "v1");

        Map<String, Object> result = service.save(10L, request);
        assertEquals(501L, result.get("summary_id"));
        assertEquals(2, result.get("version"));
        assertEquals(true, result.get("saved"));
        verify(repository).lockSource(100L);
    }

    @Test
    void rejectsNonexistentRecord() {
        when(repository.recordExists(999L)).thenReturn(false);
        SaveSummaryRequest request = new SaveSummaryRequest("https://example.com", null,
                null, null, null, null, null, "summary", null, null, null,
                null, null, null, null);
        assertThrows(ResponseStatusException.class, () -> service.save(999L, request));
        verify(repository, never()).upsertSource(anyLong(), anyString(), anyString(),
                any(), any(), any(), any(), any(), any());
    }
}
