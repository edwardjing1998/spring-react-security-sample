package com.example.securityapi.research;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class ResearchDtos {
    private ResearchDtos() {}

    public record ReportInput(
            String openaiResponseId, String extractionResponseId,
            @NotBlank String researchQuestion, String researchLevel,
            String researchCategory, String model, String extractionModel,
            String status, String answerMarkdown,
            Long inputTokens, Long outputTokens, Long totalTokens,
            JsonNode metadata) {}

    public record SourceInput(@NotBlank String url, String title, String sourceType,
                              String publisher, String publicationDate) {}

    public record TagInput(@NotBlank String tagType, @NotBlank String tagValue) {}

    public record RecordInput(
            @NotBlank String recordType, @NotBlank String title,
            String summary, String category, String subjectName,
            String evidenceType, String evidenceSummary,
            JsonNode attributes,
            List<@Valid TagInput> tags,
            List<String> sourceUrls) {}

    /** PATCH merges attributes recursively; evidenceSummary is optional. */
    public record RecordDetailsPatch(@NotNull JsonNode attributes, String evidenceSummary) {}

    /** Use zero-based record positions in the import's records list. */
    public record RelationInput(@NotNull Integer fromIndex, @NotNull Integer toIndex,
                                @NotBlank String relationshipType, String description) {}

    public record ImportRequest(@Valid @NotNull ReportInput report,
                                List<@Valid SourceInput> sources,
                                List<@Valid RecordInput> records,
                                List<@Valid RelationInput> relations) {}
}
