package com.qaautomation.repositories;

import com.qaautomation.models.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    List<Attachment> findByChatMessageId(UUID chatMessageId);
    List<Attachment> findByFileType(String fileType);
}
