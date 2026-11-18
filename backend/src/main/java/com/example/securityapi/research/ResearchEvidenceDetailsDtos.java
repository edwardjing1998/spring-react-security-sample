package com.example.securityapi.research;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Requests for versioned, source-grounded detailed research. */
public final class ResearchEvidenceDetailsDtos {
    private ResearchEvidenceDetailsDtos() {}

    public record SaveDetailRequest(
            @NotNull @JsonAlias("source_id") Long sourceId,
            @JsonAlias("summary_id") Long summaryId,
            @NotBlank @Size(max = 500) String title,
            @NotNull @JsonAlias("detailed_content") JsonNode detailedContent,
            @NotBlank @Size(max = 2000000) @JsonAlias("content_markdown") String contentMarkdown,
            @JsonAlias("source_content_hash") String sourceContentHash,
            @JsonAlias({"model", "ai_model"}) String aiModel,
            @JsonAlias("prompt_version") String promptVersion
    ) {}

    public record ApprovalRequest(@NotBlank String status) {}
}
