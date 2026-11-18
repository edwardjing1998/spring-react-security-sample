package com.example.securityapi.research;

import com.example.securityapi.research.ResearchEvidenceDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Admin-only persistence API; React should reach this through authenticated jira-api. */
@RestController
@RequestMapping("/api/research")
@PreAuthorize("hasRole('ADMIN')")
public class ResearchEvidenceController {
    private final ResearchEvidenceService service;

    public ResearchEvidenceController(ResearchEvidenceService service) {
        this.service = service;
    }

    @PostMapping("/records/{recordId}/evidence-summaries")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> save(@PathVariable long recordId,
                                     @Valid @RequestBody SaveSummaryRequest body) {
        return service.save(recordId, body);
    }

    @GetMapping("/records/{recordId}/evidence-summaries")
    public List<Map<String, Object>> list(@PathVariable long recordId) {
        return service.list(recordId);
    }

    @GetMapping("/evidence-summaries/{summaryId}")
    public Map<String, Object> get(@PathVariable long summaryId) {
        return service.get(summaryId);
    }

    @PatchMapping("/evidence-summaries/{summaryId}/approval")
    public Map<String, Object> approval(@PathVariable long summaryId,
                                         @Valid @RequestBody ApprovalRequest body) {
        return service.approve(summaryId, body);
    }
}
