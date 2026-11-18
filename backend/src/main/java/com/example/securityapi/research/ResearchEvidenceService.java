package com.example.securityapi.research;

import com.example.securityapi.research.ResearchEvidenceDtos.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResearchEvidenceService {
    private final ResearchEvidenceRepository repository;

    public ResearchEvidenceService(ResearchEvidenceRepository repository) {
        this.repository = repository;
    }

    private void requireRecord(long recordId) {
        if (recordId <= 0 || !repository.recordExists(recordId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Research record not found");
        }
    }

    /** Canonicalize only stable URL components; retain path and query (important for evidence). */
    static String normalizeUrl(String input) {
        try {
            URI uri = new URI(input.trim()).normalize();
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || !scheme.equalsIgnoreCase("https") || host == null ||
                    host.isBlank() || uri.getRawUserInfo() != null || uri.getPort() > 0 ||
                    input.length() > 2500) {
                throw new IllegalArgumentException("Expected an HTTPS URL with a hostname and no credentials or port");
            }
            return new URI("https", null, host.toLowerCase(Locale.ROOT), -1,
                    uri.getPath() == null || uri.getPath().isEmpty() ? "/" : uri.getPath(),
                    uri.getQuery(), null).toASCIIString();
        } catch (URISyntaxException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid HTTPS evidence URL");
        }
    }

    private String jsonArray(JsonNode value, String field) {
        if (value == null || value.isNull()) return "[]";
        if (!value.isArray()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                field + " must be a JSON array");
        return value.toString();
    }

    private OffsetDateTime parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "retrieved_at must be ISO-8601 with timezone");
        }
    }

    @Transactional
    public Map<String, Object> save(long recordId, SaveSummaryRequest input) {
        requireRecord(recordId);
        String normalized = normalizeUrl(input.sourceUrl());
        if (input.finalUrl() != null && !input.finalUrl().isBlank()) normalizeUrl(input.finalUrl());
        if (input.summary() == null || input.summary().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Summary is required");
        if (input.contentHash() != null && !input.contentHash().matches("(?i)[0-9a-f]{64}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content_hash must be SHA-256 hex");

        long sourceId = repository.upsertSource(recordId, input.sourceUrl(), normalized,
                input.finalUrl(), input.sourceTitle(), input.contentType(),
                input.contentHash(), input.contentStorageUri(), parseDate(input.retrievedAt()));
        repository.lockSource(sourceId);
        int version = repository.nextVersion(sourceId);
        long summaryId = repository.insertSummary(sourceId, version, input.summary(),
                jsonArray(input.technologies(), "technologies"),
                jsonArray(input.keyFindings(), "key_findings"),
                jsonArray(input.limitations(), "limitations"),
                jsonArray(input.relatedPages(), "related_pages"),
                jsonArray(input.relatedVideos(), "related_videos"),
                input.aiModel(), input.promptVersion());
        return Map.of("record_id", recordId, "source_id", sourceId,
                "summary_id", summaryId, "version", version,
                "approval_status", "draft", "saved", true);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(long recordId) {
        requireRecord(recordId);
        return repository.list(recordId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(long summaryId) {
        Map<String, Object> result = repository.find(summaryId);
        if (result == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Summary not found");
        return result;
    }

    @Transactional
    public Map<String, Object> approve(long summaryId, ApprovalRequest input) {
        if (input.status() == null || !List.of("draft", "reviewed", "approved", "rejected").contains(input.status()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid approval status");
        if (!repository.setApproval(summaryId, input.status()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Summary not found");
        return get(summaryId);
    }
}
