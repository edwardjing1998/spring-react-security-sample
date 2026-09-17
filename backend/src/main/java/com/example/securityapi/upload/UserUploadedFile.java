package com.example.securityapi.upload;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "user_uploaded_files", schema = "public")
public class UserUploadedFile {
    @Id private Long id;
    private Long userId;
    private UUID uploadSessionId;
    private UUID fileId;
    private String originalFileName;
    private String relativePath;
    private String contentType;
    private Long fileSizeBytes;
    private String blobUrl;
    private String status;
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public UUID getUploadSessionId() { return uploadSessionId; }
    public UUID getFileId() { return fileId; }
    public String getOriginalFileName() { return originalFileName; }
    public String getRelativePath() { return relativePath; }
    public String getContentType() { return contentType; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public String getBlobUrl() { return blobUrl; }
    public String getStatus() { return status; }
}
