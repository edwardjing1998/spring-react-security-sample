package com.example.securityapi.research;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request shapes accepted from the Node.js evidence-summarization response. */
public final class ResearchEvidenceDtos {
    private ResearchEvidenceDtos() {}

    public record SaveSummaryRequest(
            @NotBlank @JsonAlias("source_url") String sourceUrl,
            @JsonAlias("final_url") String finalUrl,
            @JsonAlias("source_title") String sourceTitle,
            @JsonAlias("content_type") String contentType,
            @JsonAlias("content_hash") String contentHash,
            @JsonAlias("content_storage_uri") String contentStorageUri,
            @JsonAlias("retrieved_at") String retrievedAt,
            @NotBlank String summary,
            JsonNode technologies,
            @JsonAlias("key_findings") JsonNode keyFindings,
            JsonNode limitations,
            @JsonAlias("related_pages") JsonNode relatedPages,
            @JsonAlias("related_videos") JsonNode relatedVideos,
            @JsonAlias({"model", "ai_model"}) String aiModel,
            @JsonAlias("prompt_version") String promptVersion
    ) {}

    public record ApprovalRequest(@NotNull String status) {}
}
