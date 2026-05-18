package com.qaautomation.controllers;

import com.qaautomation.services.FileUploadService;
import com.qaautomation.models.ChatMessage;
import com.qaautomation.models.Conversation;
import com.qaautomation.dto.FileUploadResponseDTO;
import com.qaautomation.repositories.ChatMessageRepository;
import com.qaautomation.repositories.ConversationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/chat/files")
@CrossOrigin(origins = "*")
public class FileUploadController {
    
    @Autowired
    private FileUploadService fileUploadService;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ConversationRepository conversationRepository;
    
    /**
     * Upload file for a chat message
     * POST /api/v1/chat/files/upload?messageId={messageId}
     */
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponseDTO> uploadFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "messageId", required = false) String messageId
    ) {
        try {
            log.info("Uploading file: {}", file.getOriginalFilename());
            
            ChatMessage chatMessage = null;
            if (messageId != null && !messageId.isEmpty()) {
                UUID id = UUID.fromString(messageId);
                chatMessage = chatMessageRepository.findById(id).orElse(null);

                if (chatMessage == null) {
                    Optional<Conversation> conversation = conversationRepository.findById(id);
                    if (conversation.isPresent()) {
                        chatMessage = ChatMessage.builder()
                            .id(UUID.randomUUID())
                            .conversation(conversation.get())
                            .role("user")
                            .content("[File upload]")
                            .messageType("FILE")
                            .build();
                        chatMessage = chatMessageRepository.save(chatMessage);
                    }
                }
            }

            if (chatMessage == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(FileUploadResponseDTO.builder()
                        .message("A valid chat message or conversation ID is required for file upload")
                        .success(false)
                        .build());
            }
            
            FileUploadResponseDTO response = fileUploadService.uploadFile(file, chatMessage);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            log.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(FileUploadResponseDTO.builder()
                    .message("File upload failed: " + e.getMessage())
                    .success(false)
                    .build());
        }
    }
    
    /**
     * Delete attachment
     * DELETE /api/v1/chat/files/{attachmentId}
     */
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<?> deleteFile(@PathVariable String attachmentId) {
        try {
            log.info("Deleting attachment: {}", attachmentId);
            boolean deleted = fileUploadService.deleteFile(attachmentId);
            
            if (deleted) {
                return ResponseEntity.ok(Map.of("success", true, "message", "File deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "File not found"));
            }
        } catch (Exception e) {
            log.error("Error deleting file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Delete failed: " + e.getMessage()));
        }
    }
}
