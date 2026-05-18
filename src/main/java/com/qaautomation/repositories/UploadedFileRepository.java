package com.qaautomation.repositories;

import com.qaautomation.models.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, String> {
    Optional<UploadedFile> findByOriginalFilename(String filename);
    List<UploadedFile> findByStatus(UploadedFile.ExtractionStatus status);
}
