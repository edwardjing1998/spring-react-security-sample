package com.example.securityapi.upload;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {
    List<UploadSession> findByUserIdOrderByCreatedAtDesc(Long userId);
}
