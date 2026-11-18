package com.example.securityapi.research;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ResearchEvidenceRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public ResearchEvidenceRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public boolean recordExists(long recordId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM research_records WHERE id=?)", Boolean.class, recordId));
    }

    /** The unique constraint on (record_id, normalized_url) makes this safe across requests. */
    public long upsertSource(long recordId, String url, String normalized, String finalUrl,
                             String title, String contentType, String contentHash,
                             String storageUri, OffsetDateTime retrievedAt) {
        Long id = jdbc.queryForObject("""
                INSERT INTO research_evidence_sources
                    (record_id, source_url, normalized_url, final_url, source_title,
                     content_type, content_hash, content_storage_uri, retrieved_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (record_id, normalized_url) DO UPDATE SET
                    final_url = COALESCE(EXCLUDED.final_url, research_evidence_sources.final_url),
                    source_title = COALESCE(EXCLUDED.source_title, research_evidence_sources.source_title),
                    content_type = COALESCE(EXCLUDED.content_type, research_evidence_sources.content_type),
                    content_hash = COALESCE(EXCLUDED.content_hash, research_evidence_sources.content_hash),
                    content_storage_uri = COALESCE(EXCLUDED.content_storage_uri, research_evidence_sources.content_storage_uri),
                    retrieved_at = COALESCE(EXCLUDED.retrieved_at, research_evidence_sources.retrieved_at),
                    updated_at = CURRENT_TIMESTAMP
                RETURNING id
                """, Long.class, recordId, url, normalized, finalUrl, title, contentType,
                contentHash, storageUri, retrievedAt);
        if (id == null) throw new IllegalStateException("Source ID not returned");
        return id;
    }

    /** Lock the parent source row so two concurrent writers cannot allocate the same version. */
    public void lockSource(long sourceId) {
        jdbc.queryForObject("SELECT id FROM research_evidence_sources WHERE id=? FOR UPDATE",
                Long.class, sourceId);
    }

    public int nextVersion(long sourceId) {
        Integer version = jdbc.queryForObject("""
                SELECT COALESCE(MAX(version), 0) + 1
                FROM research_evidence_summaries WHERE source_id=?
                """, Integer.class, sourceId);
        return version == null ? 1 : version;
    }

    public long insertSummary(long sourceId, int version, String summary, String technologies,
                              String keyFindings, String limitations, String relatedPages,
                              String relatedVideos, String model, String promptVersion) {
        Long id = jdbc.queryForObject("""
                INSERT INTO research_evidence_summaries
                  (source_id, version, summary, technologies, key_findings, limitations,
                   related_pages, related_videos, ai_model, prompt_version, approval_status)
                VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?, 'draft')
                RETURNING id
                """, Long.class, sourceId, version, summary, technologies, keyFindings,
                limitations, relatedPages, relatedVideos, model, promptVersion);
        if (id == null) throw new IllegalStateException("Summary ID not returned");
        return id;
    }

    private Map<String, Object> row(ResultSet rs, int index) throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();
        var metadata = rs.getMetaData();
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            String column = metadata.getColumnLabel(i);
            Object value = rs.getObject(i);
            if (value != null && (column.equals("technologies") || column.equals("key_findings") ||
                    column.equals("limitations") || column.equals("related_pages") ||
                    column.equals("related_videos"))) {
                try {
                    JsonNode parsed = mapper.readTree(value.toString());
                    value = parsed;
                } catch (Exception ex) {
                    throw new SQLException("Invalid JSONB in " + column, ex);
                }
            }
            result.put(column, value);
        }
        return result;
    }

    private static final String SELECT_SUMMARY = """
            SELECT e.id, e.source_id, e.version, e.summary, e.technologies,
                   e.key_findings, e.limitations, e.related_pages, e.related_videos,
                   e.ai_model, e.prompt_version, e.approval_status, e.approved_at, e.created_at,
                   s.record_id, s.source_url, s.normalized_url, s.final_url, s.source_title,
                   s.content_type, s.content_hash, s.content_storage_uri, s.retrieved_at
            FROM research_evidence_summaries e
            JOIN research_evidence_sources s ON s.id = e.source_id
            """;

    public List<Map<String, Object>> list(long recordId) {
        return jdbc.query(SELECT_SUMMARY + " WHERE s.record_id=? ORDER BY s.id, e.version DESC",
                this::row, recordId);
    }

    public Map<String, Object> find(long summaryId) {
        List<Map<String, Object>> rows = jdbc.query(SELECT_SUMMARY + " WHERE e.id=?",
                this::row, summaryId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public boolean setApproval(long summaryId, String status) {
        return jdbc.update("""
                UPDATE research_evidence_summaries
                SET approval_status=?,
                    approved_at = CASE WHEN ? = 'approved' THEN CURRENT_TIMESTAMP ELSE NULL END
                WHERE id=?
                """, status, status, summaryId) == 1;
    }
}
