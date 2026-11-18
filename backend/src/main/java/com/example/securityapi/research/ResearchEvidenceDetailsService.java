package com.example.securityapi.research;

import com.example.securityapi.research.ResearchEvidenceDetailsDtos.ApprovalRequest;
import com.example.securityapi.research.ResearchEvidenceDetailsDtos.SaveDetailRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResearchEvidenceDetailsService {
    private final ResearchEvidenceDetailsRepository repository;

    public ResearchEvidenceDetailsService(ResearchEvidenceDetailsRepository repository) {
        this.repository = repository;
    }

    private void requireRecord(long recordId) {
        if (recordId <= 0 || !repository.recordExists(recordId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Research record not found");
        }
    }

    @Transactional
    public Map<String, Object> save(long recordId, SaveDetailRequest input) {
        requireRecord(recordId);
        if (input.sourceId() == null || input.sourceId() <= 0 ||
                !repository.sourceBelongsToRecord(input.sourceId(), recordId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "source_id is not associated with this research record");
        }
        if (input.summaryId() != null && (input.summaryId() <= 0 ||
                !repository.approvedSummaryBelongsToSource(input.summaryId(), input.sourceId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "summary_id must reference an approved summary for this source");
        }
        if (input.title() == null || input.title().isBlank() || input.title().length() > 500 ||
                input.contentMarkdown() == null || input.contentMarkdown().isBlank() ||
                input.contentMarkdown().length() > 2_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid title or content_markdown");
        }
        if (input.detailedContent() == null || !input.detailedContent().isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "detailed_content must be a JSON object");
        }
        if (input.sourceContentHash() != null &&
                !input.sourceContentHash().matches("(?i)[0-9a-f]{64}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "source_content_hash must be SHA-256 hex");
        }
        repository.lockSource(input.sourceId());
        int version = repository.nextVersion(input.sourceId());
        long detailId = repository.insert(input.sourceId(), input.summaryId(), version,
                input.title(), input.detailedContent().toString(), input.contentMarkdown(),
                input.sourceContentHash(), input.aiModel(), input.promptVersion());
        return Map.of("record_id", recordId, "source_id", input.sourceId(),
                "detail_id", detailId, "version", version,
                "approval_status", "draft", "saved", true);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(long recordId) {
        requireRecord(recordId);
        return repository.list(recordId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(long detailId) {
        if (detailId <= 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Detail not found");
        Map<String, Object> detail = repository.find(detailId);
        if (detail == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Detail not found");
        return detail;
    }

    @Transactional
    public Map<String, Object> approve(long detailId, ApprovalRequest input) {
        if (input.status() == null ||
                !List.of("draft", "reviewed", "approved", "rejected").contains(input.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid approval status");
        }
        if (!repository.setApproval(detailId, input.status())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Detail not found");
        }
        return get(detailId);
    }
}
