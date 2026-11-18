package com.example.securityapi.research;

import com.example.securityapi.research.ResearchEvidenceDetailsDtos.SaveDetailRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResearchEvidenceDetailsServiceTest {
    private final ResearchEvidenceDetailsRepository repository = mock(ResearchEvidenceDetailsRepository.class);
    private final ResearchEvidenceDetailsService service = new ResearchEvidenceDetailsService(repository);
    private final ObjectMapper mapper = new ObjectMapper();

    private SaveDetailRequest request(Long sourceId, Long summaryId) throws Exception {
        return new SaveDetailRequest(sourceId, summaryId, "FHIR Integration Research",
                mapper.readTree("{\"sections\":[{\"heading\":\"Architecture\"}]}"),
                "# FHIR Integration Research\nDetailed source-grounded analysis.",
                null, "elearning-gpt6-dev", "details-v1");
    }

    @Test
    void savesIndependentVersionWithApprovedRelatedSummary() throws Exception {
        when(repository.recordExists(10)).thenReturn(true);
        when(repository.sourceBelongsToRecord(2, 10)).thenReturn(true);
        when(repository.approvedSummaryBelongsToSource(7, 2)).thenReturn(true);
        when(repository.nextVersion(2)).thenReturn(3);
        when(repository.insert(eq(2L), eq(7L), eq(3), anyString(), anyString(),
                anyString(), nullable(String.class), nullable(String.class), nullable(String.class)))
                .thenReturn(55L);
        var response = service.save(10, request(2L, 7L));
        assertEquals(55L, response.get("detail_id"));
        assertEquals(3, response.get("version"));
        verify(repository).lockSource(2L);
    }

    @Test
    void rejectsSourceFromAnotherRecord() throws Exception {
        when(repository.recordExists(10)).thenReturn(true);
        var ex = assertThrows(ResponseStatusException.class, () -> service.save(10, request(99L, null)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(repository, never()).insert(anyLong(), any(), anyInt(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsUnapprovedOrMismatchedSummary() throws Exception {
        when(repository.recordExists(10)).thenReturn(true);
        when(repository.sourceBelongsToRecord(2, 10)).thenReturn(true);
        var ex = assertThrows(ResponseStatusException.class, () -> service.save(10, request(2L, 7L)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void rejectsNonObjectJson() throws Exception {
        when(repository.recordExists(10)).thenReturn(true);
        when(repository.sourceBelongsToRecord(2, 10)).thenReturn(true);
        var bad = new SaveDetailRequest(2L, null, "Title", mapper.readTree("[]"), "# Markdown", null, null, null);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.save(10, bad)).getStatusCode());
    }
}
