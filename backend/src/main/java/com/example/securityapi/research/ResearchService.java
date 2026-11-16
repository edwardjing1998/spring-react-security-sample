package com.example.securityapi.research;

import com.example.securityapi.research.ResearchDtos.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResearchService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public ResearchService(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    private String json(JsonNode value) {
        return value == null || value.isNull() ? "{}" : value.toString();
    }

    private Map<String, Object> row(ResultSet rs, int index) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        var metadata = rs.getMetaData();
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            String name = metadata.getColumnLabel(i);
            Object value = rs.getObject(i);
            if ("metadata".equals(name) || "attributes".equals(name)) {
                try {
                    value = value == null ? null : mapper.readTree(value.toString());
                } catch (JsonProcessingException ex) {
                    throw new SQLException("Invalid JSONB stored in " + name, ex);
                }
            }
            item.put(name, value);
        }
        return item;
    }

    private final RowMapper<Map<String, Object>> mapRow = this::row;

    private long insert(String sql, Object... values) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            for (int i = 0; i < values.length; i++) ps.setObject(i + 1, values[i]);
            return ps;
        }, keys);
        if (keys.getKey() == null) throw new IllegalStateException("Missing generated database ID");
        return keys.getKey().longValue();
    }

    private boolean reportExists(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM research_reports WHERE id=?)", Boolean.class, id));
    }

    private void requireReport(long id) {
        if (!reportExists(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Research report not found");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> reports(int limit, int offset) {
        return jdbc.query("SELECT * FROM research_reports ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                mapRow, limit, offset);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> report(long id) {
        var rows = jdbc.query("SELECT * FROM research_reports WHERE id=?", mapRow, id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Research report not found");
        Map<String,Object> result = new LinkedHashMap<>(rows.get(0));
        result.put("records", recordsByReport(id));
        result.put("sources", sourcesByReport(id));
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> recordsByReport(long id) {
        requireReport(id);
        return jdbc.query("SELECT * FROM research_records WHERE report_id=? ORDER BY id", mapRow, id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> sourcesByReport(long id) {
        requireReport(id);
        return jdbc.query("SELECT * FROM research_sources WHERE report_id=? ORDER BY id", mapRow, id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchRecords(String type, String category, String evidence, String tag, int limit, int offset) {
        String sql = "SELECT r.* FROM research_records r WHERE (? IS NULL OR r.record_type=?) " +
                "AND (? IS NULL OR r.category=?) AND (? IS NULL OR r.evidence_type=?) " +
                "AND (? IS NULL OR EXISTS (SELECT 1 FROM research_record_tags t " +
                "WHERE t.record_id=r.id AND lower(t.tag_value)=lower(?))) " +
                "ORDER BY r.id DESC LIMIT ? OFFSET ?";
        return jdbc.query(sql, mapRow, type, type, category, category, evidence, evidence, tag, tag, limit, offset);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> record(long id) {
        var rows = jdbc.query("SELECT * FROM research_records WHERE id=?", mapRow, id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Research record not found");
        Map<String,Object> result = new LinkedHashMap<>(rows.get(0));
        result.put("tags", jdbc.query("SELECT id,tag_type,tag_value FROM research_record_tags WHERE record_id=? ORDER BY id", mapRow, id));
        result.put("sources", jdbc.query("SELECT s.*,rs.evidence_note FROM research_sources s " +
                "JOIN research_record_sources rs ON rs.source_id=s.id WHERE rs.record_id=? ORDER BY s.id", mapRow, id));
        result.put("relations", jdbc.query("SELECT * FROM research_record_relations " +
                "WHERE source_record_id=? OR target_record_id=? ORDER BY id", mapRow, id, id));
        return result;
    }

    /** Deep-merge JSON objects; scalar/array values in the patch replace existing values. */
    private void mergeObject(ObjectNode target, JsonNode patch) {
        patch.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode incoming = entry.getValue();
            JsonNode existing = target.get(key);
            if (incoming.isObject() && existing != null && existing.isObject()) {
                mergeObject((ObjectNode) existing, incoming);
            } else {
                target.set(key, incoming.deepCopy());
            }
        });
    }

    /** Update an existing IT project without overwriting unrelated project attributes. */
    @Transactional
    public Map<String, Object> updateRecordDetails(long id, RecordDetailsPatch patch) {
        if (patch == null || patch.attributes() == null || !patch.attributes().isObject()
                || patch.attributes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "attributes must be a non-empty JSON object");
        }
        if (patch.attributes().toString().length() > 131_072) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "attributes payload is too large");
        }
        if (patch.evidenceSummary() != null && patch.evidenceSummary().length() > 10_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "evidenceSummary is too large");
        }

        // FOR UPDATE serializes concurrent patches to the same record.
        var rows = jdbc.query("SELECT id,record_type,attributes FROM research_records WHERE id=? FOR UPDATE",
                mapRow, id);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Research record not found");
        }
        Map<String, Object> current = rows.get(0);
        if (!"it_project".equals(current.get("record_type"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Deep research updates are supported only for it_project records");
        }
        ObjectNode attributes = mapper.createObjectNode();
        Object previous = current.get("attributes");
        if (previous instanceof JsonNode existing && existing.isObject()) {
            attributes = (ObjectNode) existing.deepCopy();
        }
        mergeObject(attributes, patch.attributes());
        jdbc.update("UPDATE research_records SET attributes=CAST(? AS jsonb), " +
                        "evidence_summary=COALESCE(?,evidence_summary), updated_at=CURRENT_TIMESTAMP WHERE id=?",
                attributes.toString(), patch.evidenceSummary(), id);
        return record(id);
    }

    private Long existingReportByResponseId(String responseId) {
        if (responseId == null || responseId.isBlank()) return null;
        List<Long> ids = jdbc.query("SELECT id FROM research_reports WHERE openai_response_id=?",
                (rs, row) -> rs.getLong(1), responseId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private long saveReport(ReportInput data) {
        String sql = "INSERT INTO research_reports (openai_response_id,extraction_response_id,research_question," +
                "research_level,research_category,model,extraction_model,status,answer_markdown," +
                "input_tokens,output_tokens,total_tokens,metadata) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,CAST(? AS jsonb))";
        return insert(sql, data.openaiResponseId(), data.extractionResponseId(), data.researchQuestion(),
                data.researchLevel(), data.researchCategory(), data.model(), data.extractionModel(),
                data.status() == null ? "completed" : data.status(), data.answerMarkdown(),
                data.inputTokens(), data.outputTokens(), data.totalTokens(), json(data.metadata()));
    }

    private long saveSource(long reportId, SourceInput src) {
        if (src.url() == null || !src.url().matches("https?://.+"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source URL must be http(s)");
        List<Long> existing = jdbc.query("SELECT id FROM research_sources WHERE report_id=? AND url=?",
                (rs, index) -> rs.getLong(1), reportId, src.url());
        if (!existing.isEmpty()) return existing.get(0);
        return insert("INSERT INTO research_sources (report_id,url,title,source_type,publisher,publication_date) " +
                "VALUES (?,?,?,?,?,CAST(? AS date))", reportId, src.url(), src.title(), src.sourceType(),
                src.publisher(), src.publicationDate());
    }

    private long saveRecord(long reportId, RecordInput rec) {
        if (rec.evidenceType() != null && !List.of("confirmed","job_supported","inference","unverified").contains(rec.evidenceType()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid evidence_type");
        return insert("INSERT INTO research_records (report_id,record_type,title,summary,category,subject_name," +
                "evidence_type,evidence_summary,attributes) VALUES (?,?,?,?,?,?,?,?,CAST(? AS jsonb))",
                reportId, rec.recordType(), rec.title(), rec.summary(), rec.category(), rec.subjectName(),
                rec.evidenceType(), rec.evidenceSummary(), json(rec.attributes()));
    }

    private void saveTag(long recordId, TagInput tag) {
        jdbc.update("INSERT INTO research_record_tags (record_id,tag_type,tag_value) VALUES (?,?,?) " +
                "ON CONFLICT DO NOTHING", recordId, tag.tagType(), tag.tagValue());
    }

    private void linkSource(long reportId, long recordId, long sourceId, String evidenceNote) {
        int count = jdbc.update("INSERT INTO research_record_sources (report_id,record_id,source_id,evidence_note) " +
                "SELECT ?,r.id,s.id,? FROM research_records r JOIN research_sources s " +
                "ON r.report_id=s.report_id WHERE r.id=? AND s.id=? AND r.report_id=? " +
                "ON CONFLICT DO NOTHING", reportId, evidenceNote, recordId, sourceId, reportId);
        if (count == 0) {
            boolean exists = Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM research_record_sources WHERE record_id=? AND source_id=?)",
                    Boolean.class, recordId, sourceId));
            if (!exists) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Record and source must belong to the same report");
        }
    }

    @Transactional
    public Map<String, Object> importReport(ImportRequest input) {
        Long existing = existingReportByResponseId(input.report().openaiResponseId());
        if (existing != null) return Map.of("report_id", existing, "created", false, "report", report(existing));
        long reportId = saveReport(input.report());
        Map<String, Long> sources = new LinkedHashMap<>();
        if (input.sources() != null) for (SourceInput source : input.sources())
            sources.put(source.url(), saveSource(reportId, source));
        List<Long> recordIds = new ArrayList<>();
        if (input.records() != null) for (RecordInput record : input.records()) {
            long recordId = saveRecord(reportId, record);
            recordIds.add(recordId);
            if (record.tags() != null) for (TagInput tag : record.tags()) saveTag(recordId, tag);
            if (record.sourceUrls() != null) for (String url : record.sourceUrls()) {
                Long sourceId = sources.get(url);
                if (sourceId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Record source URL absent from sources list: " + url);
                linkSource(reportId, recordId, sourceId, record.evidenceSummary());
            }
        }
        if (input.relations() != null) for (RelationInput relation : input.relations()) {
            int from = relation.fromIndex(), to = relation.toIndex();
            if (from < 0 || to < 0 || from >= recordIds.size() || to >= recordIds.size() || from == to)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid record relation indices");
            createRelation(reportId, recordIds.get(from), recordIds.get(to), relation.relationshipType(), relation.description());
        }
        return Map.of("report_id", reportId, "created", true, "record_count", recordIds.size(), "source_count", sources.size());
    }

    @Transactional
    public long addRecord(long reportId, RecordInput record) {
        requireReport(reportId);
        long id = saveRecord(reportId, record);
        if (record.tags() != null) for (TagInput tag : record.tags()) saveTag(id, tag);
        if (record.sourceUrls() != null) for (String url : record.sourceUrls()) {
            List<Long> ids = jdbc.query("SELECT id FROM research_sources WHERE report_id=? AND url=?",
                    (rs, index) -> rs.getLong(1), reportId, url);
            if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source URL missing from this report: " + url);
            linkSource(reportId, id, ids.get(0), record.evidenceSummary());
        }
        return id;
    }

    @Transactional
    public long addSource(long reportId, SourceInput source) {
        requireReport(reportId);
        return saveSource(reportId, source);
    }

    @Transactional
    public void addTag(long recordId, TagInput tag) {
        record(recordId);
        saveTag(recordId, tag);
    }

    @Transactional
    public void addSourceLink(long recordId, long sourceId, String note) {
        Map<String, Object> rec = record(recordId);
        linkSource(((Number) rec.get("report_id")).longValue(), recordId, sourceId, note);
    }

    @Transactional
    public long createRelation(long reportId, long fromId, long toId, String type, String description) {
        if (fromId == toId) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot relate record to itself");
        if (type == null || type.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Relationship type is required");
        List<Long> valid = jdbc.query("SELECT id FROM research_records WHERE report_id=? AND id IN (?,?)",
                (rs, i) -> rs.getLong(1), reportId, fromId, toId);
        if (valid.size() != 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both records must belong to report");
        var existing = jdbc.query("SELECT id FROM research_record_relations WHERE source_record_id=? AND target_record_id=? AND relationship_type=?",
                (rs, i) -> rs.getLong(1), fromId, toId, type);
        if (!existing.isEmpty()) return existing.get(0);
        return insert("INSERT INTO research_record_relations (report_id,source_record_id,target_record_id,relationship_type,description) " +
                "VALUES (?,?,?,?,?)", reportId, fromId, toId, type, description);
    }

    @Transactional
    public void deleteReport(long id) {
        requireReport(id);
        jdbc.update("DELETE FROM research_reports WHERE id=?", id);
    }
}
