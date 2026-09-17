package com.example.securityapi.upload;

import com.example.securityapi.user.AppUser;
import com.example.securityapi.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class UploadReviewController {
    private final UserRepository users;
    private final UploadSessionRepository sessions;
    private final UserUploadedFileRepository files;

    public UploadReviewController(UserRepository users, UploadSessionRepository sessions,
                                  UserUploadedFileRepository files) {
        this.users = users; this.sessions = sessions; this.files = files;
    }

    @GetMapping("/users")
    public List<UserSummary> users() {
        return users.findAll().stream().map(u -> new UserSummary(u.getId(), u.getName(), u.getEmail(), u.getRole().name())).toList();
    }

    @GetMapping("/users/{userId}/upload-sessions")
    public List<SessionSummary> sessions(@PathVariable Long userId, Authentication auth) {
        authorize(userId, auth);
        return sessions.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(s -> new SessionSummary(s.getId(), s.getFolderName())).toList();
    }

    @GetMapping("/users/{userId}/upload-sessions/{sessionId}/folders")
    public Map<String, Object> folder(@PathVariable Long userId, @PathVariable UUID sessionId, Authentication auth) {
        authorize(userId, auth);
        if (!sessions.findById(sessionId).filter(s -> userId.equals(s.getUserId())).isPresent())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload session not found");
        var images = files.findByUserIdAndUploadSessionIdOrderByRelativePath(userId, sessionId).stream()
                .map(f -> Map.of("id", f.getId(), "fileId", f.getFileId(), "name", f.getOriginalFileName(),
                        "relativePath", f.getRelativePath(), "contentType", f.getContentType() == null ? "" : f.getContentType(),
                        "sizeBytes", f.getFileSizeBytes(), "blobUrl", f.getBlobUrl(), "status", f.getStatus()))
                .toList();
        return Map.of("userId", userId, "sessionId", sessionId, "images", images);
    }

    private void authorize(Long userId, Authentication auth) {
        AppUser current = users.findByEmailIgnoreCase(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!current.getId().equals(userId) && current.getRole() != com.example.securityapi.user.Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    public record UserSummary(Long id, String name, String email, String role) {}
    public record SessionSummary(UUID id, String folderName) {}
}
