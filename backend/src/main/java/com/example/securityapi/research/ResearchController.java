package com.example.securityapi.research;

import com.example.securityapi.research.ResearchDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** All research endpoints are admin-only until per-user ownership is implemented. */
@RestController
@RequestMapping("/api/research")
@PreAuthorize("hasRole('ADMIN')")
public class ResearchController {
    private final ResearchService service;

    public ResearchController(ResearchService service) { this.service = service; }

    @PostMapping("/reports/import")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> importReport(@Valid @RequestBody ImportRequest body) {
        return service.importReport(body);
    }

    @GetMapping("/reports")
    public List<Map<String, Object>> reports(@RequestParam(defaultValue="50") int limit,
                                              @RequestParam(defaultValue="0") int offset) {
        checkPage(limit, offset);
        return service.reports(limit, offset);
    }

    @GetMapping("/reports/{id}")
    public Map<String, Object> report(@PathVariable long id) { return service.report(id); }

    @DeleteMapping("/reports/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.deleteReport(id); }

    @GetMapping("/reports/{id}/records")
    public List<Map<String, Object>> recordsByReport(@PathVariable long id) {
        return service.recordsByReport(id);
    }

    @PostMapping("/reports/{id}/records")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createRecord(@PathVariable long id, @Valid @RequestBody RecordInput body) {
        return Map.of("id", service.addRecord(id, body));
    }

    @GetMapping("/reports/{id}/sources")
    public List<Map<String, Object>> sourcesByReport(@PathVariable long id) {
        return service.sourcesByReport(id);
    }

    @PostMapping("/reports/{id}/sources")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createSource(@PathVariable long id, @Valid @RequestBody SourceInput body) {
        return Map.of("id", service.addSource(id, body));
    }

    @GetMapping("/records")
    public List<Map<String, Object>> records(
            @RequestParam(required=false) String type,
            @RequestParam(required=false) String category,
            @RequestParam(required=false) String evidence,
            @RequestParam(required=false) String tag,
            @RequestParam(defaultValue="50") int limit,
            @RequestParam(defaultValue="0") int offset) {
        checkPage(limit, offset);
        return service.searchRecords(type, category, evidence, tag, limit, offset);
    }

    @GetMapping("/records/{id}")
    public Map<String, Object> record(@PathVariable long id) { return service.record(id); }

    /** Merge new technical research details into an existing IT project. */
    @PatchMapping("/records/{id}")
    public Map<String, Object> patchRecord(@PathVariable long id,
                                           @Valid @RequestBody RecordDetailsPatch body) {
        return service.updateRecordDetails(id, body);
    }

    @PostMapping("/records/{id}/tags")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void tag(@PathVariable long id, @Valid @RequestBody TagInput body) {
        service.addTag(id, body);
    }

    public record SourceLink(long sourceId, String evidenceNote) {}

    @PostMapping("/records/{id}/sources")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void linkSource(@PathVariable long id, @RequestBody SourceLink body) {
        service.addSourceLink(id, body.sourceId(), body.evidenceNote());
    }

    public record RelationRequest(long reportId, long sourceRecordId, long targetRecordId,
                                  String relationshipType, String description) {}

    @PostMapping("/relations")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> relate(@RequestBody RelationRequest body) {
        return Map.of("id", service.createRelation(body.reportId(), body.sourceRecordId(),
                body.targetRecordId(), body.relationshipType(), body.description()));
    }

    private void checkPage(int limit, int offset) {
        if (limit < 1 || limit > 200 || offset < 0)
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "limit must be 1..200 and offset >= 0");
    }
}
