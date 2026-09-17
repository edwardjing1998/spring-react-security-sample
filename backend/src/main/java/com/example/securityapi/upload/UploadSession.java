package com.example.securityapi.upload;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "upload_sessions", schema = "public")
public class UploadSession {
    @Id private UUID id;
    private Long userId;
    private String folderName;
    private Instant createdAt;
    public UUID getId() { return id; }
    public Long getUserId() { return userId; }
    public String getFolderName() { return folderName; }
}
