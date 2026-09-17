package com.example.securityapi.upload;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserUploadedFileRepository extends JpaRepository<UserUploadedFile, Long> {
    List<UserUploadedFile> findByUserIdAndUploadSessionIdOrderByRelativePath(Long userId, UUID sessionId);
}
