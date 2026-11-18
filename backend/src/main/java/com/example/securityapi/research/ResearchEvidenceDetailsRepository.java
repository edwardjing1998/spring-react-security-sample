package com.example.securityapi.research;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** SQL adapter for research_evidence_details (Liquibase changeSet 014). */
@Repository
public class ResearchEvidenceDetailsRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public ResearchEvidenceDetailsRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public boolean recordExists(long recordId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM research_records WHERE id=?)", Boolean.class, recordId));
    }

    public boolean sourceBelongsToRecord(long sourceId, long recordId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM research_evidence_sources
                              WHERE id=? AND record_id=?)
                """, Boolean.class, sourceId, recordId));
    }

    /** The summary must belong to this source and be approved for downstream use. */
    public boolean approvedSummaryBelongsToSource(long summaryId, long sourceId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM research_evidence_summaries
                              WHERE id=? AND source_id=? AND approval_status='approved')
                """, Boolean.class, summaryId, sourceId));
    }

    /** Serialize writers for a source to ensure unique version allocation. */
    public void lockSource(long sourceId) {
        jdbc.queryForObject("SELECT id FROM research_evidence_sources WHERE id=? FOR UPDATE",
                Long.class, sourceId);
    }

    public int nextVersion(long sourceId) {
        Integer version = jdbc.queryForObject("""
                SELECT COALESCE(MAX(version), 0) + 1
                FROM research_evidence_details WHERE source_id=?
                """, Integer.class, sourceId);
        return version == null ? 1 : version;
    }

    public long insert(long sourceId, Long summaryId, int version, String title,
                       String contentJson, String markdown, String hash,
                       String model, String promptVersion) {
        Long id = jdbc.queryForObject("""
                INSERT INTO research_evidence_details
                  (source_id, summary_id, version, title, detailed_content,
                   content_markdown, source_content_hash, ai_model, prompt_version,
                   approval_status)
                VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, 'draft')
                RETURNING id
                """, Long.class, sourceId, summaryId, version, title, contentJson,
                markdown, hash, model, promptVersion);
        if (id == null) throw new IllegalStateException("Detail ID not returned");
        return id;
    }

    private Map<String, Object> row(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();
        var metadata = rs.getMetaData();
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            String name = metadata.getColumnLabel(i);
            Object value = rs.getObject(i);
            if ("detailed_content".equals(name) && value != null) {
                try {
                    JsonNode parsed = mapper.readTree(value.toString());
                    value = parsed;
                } catch (Exception e) {
                    throw new SQLException("Invalid detailed_content JSONB", e);
                }
            }
            result.put(name, value);
        }
        return result;
    }

    private static final String SELECT_DETAIL = """
            SELECT d.id, d.source_id, d.summary_id, d.version, d.title,
                   d.detailed_content, d.content_markdown, d.source_content_hash,
                   d.ai_model, d.prompt_version, d.approval_status, d.approved_at,
                   d.created_at, d.updated_at,
                   s.record_id, s.source_url, s.normalized_url, s.final_url,
                   s.source_title, s.content_type, s.content_hash,
                   s.content_storage_uri, s.retrieved_at
            FROM research_evidence_details d
            JOIN research_evidence_sources s ON s.id=d.source_id
            """;

    public List<Map<String, Object>> list(long recordId) {
        return jdbc.query(SELECT_DETAIL +
                " WHERE s.record_id=? ORDER BY s.id, d.version DESC", this::row, recordId);
    }

    public Map<String, Object> find(long detailId) {
        List<Map<String, Object>> rows = jdbc.query(SELECT_DETAIL + " WHERE d.id=?",
                this::row, detailId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public boolean setApproval(long detailId, String status) {
        return jdbc.update("""
                UPDATE research_evidence_details
                SET approval_status=?,
                    approved_at=CASE WHEN ?='approved' THEN CURRENT_TIMESTAMP ELSE NULL END,
                    updated_at=CURRENT_TIMESTAMP
                WHERE id=?
                """, status, status, detailId) == 1;
    }
}
