package com.qaautomation.services;

import com.qaautomation.models.Attachment;
import com.qaautomation.models.ChatMessage;
import com.qaautomation.repositories.AttachmentRepository;
import com.qaautomation.dto.FileUploadResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;

@Slf4j
@Service
@Transactional
public class FileUploadService {
    
    @Autowired
    private AttachmentRepository attachmentRepository;
    
    @Value("${file.upload.dir}")
    private String uploadDirConfig;
    
    private String uploadDir;
    
    @PostConstruct
    public void init() {
        // Resolve upload directory with fallback to user's home directory
        if (uploadDirConfig != null && !uploadDirConfig.trim().isEmpty()) {
            uploadDir = uploadDirConfig;
        } else {
            // Use user's temp directory as fallback
            uploadDir = System.getProperty("user.home") + File.separator + ".qa-automation-uploads";
        }
        
        // Convert to absolute path to avoid Tomcat context path issues
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
        uploadDir = uploadPath.toString();
        
        log.info("Upload directory configured: {}", uploadDir);
    }
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
        "pdf", "docx", "doc", "txt", "jpg", "jpeg", "png", "gif", "pgf", "xlsx", "xls"
    ));
    
    private static final Map<String, String> CATEGORY_MAP = new HashMap<String, String>() {{
        put("pdf", "DOCUMENT");
        put("docx", "DOCUMENT");
        put("doc", "DOCUMENT");
        put("txt", "DOCUMENT");
        put("xlsx", "SPREADSHEET");
        put("xls", "SPREADSHEET");
        put("jpg", "IMAGE");
        put("jpeg", "IMAGE");
        put("png", "IMAGE");
        put("gif", "IMAGE");
        put("pgf", "IMAGE");
    }};

    /**
     * Upload file for a chat message
     */
    public FileUploadResponseDTO uploadFile(MultipartFile file, ChatMessage chatMessage) {
        try {
            // Validate file
            validateFile(file);
            
            // Get file extension
            String fileName = file.getOriginalFilename();
            String fileExtension = getFileExtension(fileName).toLowerCase();
            
            // Create uploads directory if not exists
            Path uploadDirPath = Paths.get(uploadDir);
            if (!Files.exists(uploadDirPath)) {
                Files.createDirectories(uploadDirPath);
            }
            
            // Generate unique file name and construct path safely
            String uniqueFileName = UUID.randomUUID() + "_" + fileName;
            Path filePath = uploadDirPath.resolve(uniqueFileName);
            
            // Save file to disk
            file.transferTo(filePath.toFile());
            
            // Create attachment entity
            String category = CATEGORY_MAP.getOrDefault(fileExtension, "DOCUMENT");
            
            Attachment attachment = Attachment.builder()
                .id(UUID.randomUUID())
                .chatMessage(chatMessage)
                .originalFileName(fileName)
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .fileType(fileExtension)
                .category(category)
                .build();
            
            // Extract text if possible (for documents/images)
            String extractedText = extractTextFromFile(file, fileExtension);
            if (extractedText != null) {
                attachment.setExtractedText(extractedText);
            }
            
            // Save to database
            attachment = attachmentRepository.save(attachment);
            
            log.info("File uploaded successfully: {} ({})", fileName, category);
            
            return FileUploadResponseDTO.builder()
                .id(attachment.getId().toString())
                .fileName(fileName)
                .fileType(fileExtension)
                .fileSize(file.getSize())
                .category(category)
                .message("File uploaded successfully")
                .success(true)
                .build();
                
        } catch (IOException e) {
            log.error("Error uploading file", e);
            return FileUploadResponseDTO.builder()
                .message("Failed to upload file: " + e.getMessage())
                .success(false)
                .build();
        } catch (IllegalArgumentException e) {
            log.warn("File validation failed", e);
            return FileUploadResponseDTO.builder()
                .message(e.getMessage())
                .success(false)
                .build();
        }
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }
        
        String fileExtension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * Extract text from file (basic implementation)
     */
    private String extractTextFromFile(MultipartFile file, String fileExtension) {
        try {
            if ("txt".equals(fileExtension)) {
                return new String(file.getBytes());
            }
            // For other file types, basic extraction or metadata
            // In a real scenario, you'd use libraries like PDFBox, Tika, etc.
            return "File extracted: " + file.getOriginalFilename();
        } catch (Exception e) {
            log.warn("Could not extract text from file", e);
            return null;
        }
    }

    /**
     * Delete file
     */
    public boolean deleteFile(String attachmentId) {
        try {
            UUID id = UUID.fromString(attachmentId);
            Optional<Attachment> attachment = attachmentRepository.findById(id);
            
            if (attachment.isPresent()) {
                Path filePath = Paths.get(attachment.get().getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
                attachmentRepository.deleteById(id);
                log.info("File deleted successfully: {}", filePath);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error deleting file", e);
            return false;
        }
    }

    /**
     * Get file by ID
     */
    public Optional<Attachment> getAttachment(String attachmentId) {
        try {
            UUID id = UUID.fromString(attachmentId);
            return attachmentRepository.findById(id);
        } catch (Exception e) {
            log.error("Error fetching attachment", e);
            return Optional.empty();
        }
    }
}
