package com.example.securityapi.research;

import com.example.securityapi.research.ResearchEvidenceDetailsDtos.ApprovalRequest;
import com.example.securityapi.research.ResearchEvidenceDetailsDtos.SaveDetailRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Admin-only persistence API; call from jira-api, never directly from a public guest browser. */
@RestController
@RequestMapping("/api/research")
@PreAuthorize("hasRole('ADMIN')")
public class ResearchEvidenceDetailsController {
    private final ResearchEvidenceDetailsService service;

    public ResearchEvidenceDetailsController(ResearchEvidenceDetailsService service) {
        this.service = service;
    }

    @PostMapping("/records/{recordId}/evidence-details")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> save(@PathVariable long recordId,
                                    @Valid @RequestBody SaveDetailRequest body) {
        return service.save(recordId, body);
    }

    @GetMapping("/records/{recordId}/evidence-details")
    public List<Map<String, Object>> list(@PathVariable long recordId) {
        return service.list(recordId);
    }

    @GetMapping("/evidence-details/{detailId}")
    public Map<String, Object> get(@PathVariable long detailId) {
        return service.get(detailId);
    }

    @PatchMapping("/evidence-details/{detailId}/approval")
    public Map<String, Object> approve(@PathVariable long detailId,
                                       @Valid @RequestBody ApprovalRequest body) {
        return service.approve(detailId, body);
    }
}
