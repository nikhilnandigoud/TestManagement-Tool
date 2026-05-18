/**
 * Chat Interface Handler
 * Manages chat messaging with backend AI agent
 */

class ChatHandler {
    constructor() {
        this.messagesArea = document.getElementById('chatMessagesArea');
        this.chatInput = document.getElementById('chatInput');
        this.chatSendBtn = document.getElementById('chatSendBtn');
        this.fileUploadBtn = document.getElementById('fileUploadBtn');
        this.fileInput = document.getElementById('fileInput');
        this.guideCheckboxes = {};
        this.conversationId = null;
        this.messages = [];
        this.uploadedFiles = []; // Track uploaded files
        
        if (!this.messagesArea || !this.chatInput || !this.chatSendBtn) {
            console.error('Chat elements not found');
            return;
        }
        
        this.initializeEventListeners();
        this.initializeUserGuides();
        this.initializeChat();
    }

    initializeEventListeners() {
        // Send button click
        this.chatSendBtn.addEventListener('click', () => this.sendMessage());
        
        // Enter key in input
        this.chatInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });

        // File upload button click
        if (this.fileUploadBtn && this.fileInput) {
            this.fileUploadBtn.addEventListener('click', () => this.fileInput.click());
            this.fileInput.addEventListener('change', () => this.handleFileSelect());
        }

        // Focus on input when page loads
        this.chatInput.focus();
    }

    initializeUserGuides() {
        const guides = [
            { id: 'bestPractices', name: 'Best Practices', enabled: true },
            { id: 'dan', name: 'DAN', enabled: false },
            { id: 'dmdedt', name: 'DMDEDT', enabled: false }
        ];

        const guidesContainer = document.getElementById('userGuidesCheckboxes');
        if (!guidesContainer) {
            console.warn('User guides container not found');
            return;
        }

        guidesContainer.innerHTML = '';
        
        const label = document.createElement('div');
        label.style.cssText = 'font-weight: 600; font-size: 12px; color: #6b7280; margin-bottom: 8px; text-transform: uppercase;';
        label.textContent = 'Active Guides';
        guidesContainer.appendChild(label);

        guides.forEach(guide => {
            const checkboxWrapper = document.createElement('div');
            checkboxWrapper.style.cssText = 'display: flex; align-items: center; margin-bottom: 6px; gap: 8px;';
            
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.id = `guide-${guide.id}`;
            checkbox.checked = guide.enabled;
            checkbox.style.cssText = 'cursor: pointer; width: 16px; height: 16px;';
            
            const labelEl = document.createElement('label');
            labelEl.htmlFor = `guide-${guide.id}`;
            labelEl.textContent = guide.name;
            labelEl.style.cssText = 'cursor: pointer; font-size: 13px; color: #374151; user-select: none;';
            
            checkboxWrapper.appendChild(checkbox);
            checkboxWrapper.appendChild(labelEl);
            guidesContainer.appendChild(checkboxWrapper);
            
            this.guideCheckboxes[guide.id] = checkbox;
            
            checkbox.addEventListener('change', () => {
                console.log(`Guide ${guide.name} ${checkbox.checked ? 'enabled' : 'disabled'}`);
            });
        });
    }

    async initializeChat() {
        try {
            // Start a new chat conversation
            const response = await fetch('/testmanagement/chat/start', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({})
            });

            if (response.ok) {
                const data = await response.json();
                console.log('API Response:', data);
                
                // Get conversation ID (might be in different fields)
                this.conversationId = data.conversationId || data.id || data.conversation_id || '';
                
                console.log('Final Conversation ID:', this.conversationId);
                
                if (!this.conversationId) {
                    console.warn('Conversation ID is empty');
                }
                
                this.addSystemMessage('Hello! I am your QA & Testing Agent. I can help you generate test cases, analyze scenarios, and provide QA guidance. What would you like to work on today?');
            } else {
                console.error('Chat initialization failed:', response.status, response.statusText);
                const errorText = await response.text();
                console.error('Error response:', errorText);
                this.addSystemMessage('Chat initialization failed. Status: ' + response.status);
            }
        } catch (error) {
            console.error('Error initializing chat:', error);
            this.addSystemMessage('Unable to connect to chat service. Error: ' + error.message);
        }
    }

    getSelectedGuides() {
        const selected = [];
        Object.entries(this.guideCheckboxes).forEach(([key, checkbox]) => {
            if (checkbox.checked) {
                selected.push(key);
            }
        });
        return selected;
    }

    /**
     * Handle file selection from input
     */
    async handleFileSelect() {
        const files = Array.from(this.fileInput.files);
        if (files.length === 0) return;

        for (const file of files) {
            await this.uploadFile(file);
        }

        // Clear file input
        this.fileInput.value = '';
    }

    /**
     * Upload a single file
     */
    async uploadFile(file) {
        try {
            // Validate file
            const maxSize = 10 * 1024 * 1024; // 10MB
            const allowedTypes = ['pdf', 'docx', 'doc', 'txt', 'jpg', 'jpeg', 'png', 'gif', 'pgf', 'xlsx', 'xls'];
            const fileExtension = file.name.split('.').pop().toLowerCase();

            if (!allowedTypes.includes(fileExtension)) {
                this.addSystemMessage(`Error: File type .${fileExtension} not allowed. Allowed types: ${allowedTypes.join(', ')}`);
                return;
            }

            if (file.size > maxSize) {
                this.addSystemMessage(`Error: File ${file.name} exceeds 10MB limit`);
                return;
            }

            // Check for duplicates
            if (this.uploadedFiles.some(f => f.name === file.name)) {
                this.addSystemMessage(`File ${file.name} already uploaded`);
                return;
            }

            // Show upload progress
            const formData = new FormData();
            formData.append('file', file);
            if (this.conversationId) {
                formData.append('messageId', this.conversationId);
            }

            const response = await fetch('/testmanagement/chat/files/upload', {
                method: 'POST',
                body: formData
            });

            const data = await response.json();

            if (data.success) {
                // Add to uploaded files
                this.uploadedFiles.push({
                    id: data.id,
                    name: data.fileName,
                    fileType: data.fileType,
                    fileSize: data.fileSize,
                    category: data.category
                });

                console.log(`File uploaded: ${data.fileName}`);
                this.updateUploadedFilesDisplay();
                this.addSystemMessage(`✓ File "${data.fileName}" attached successfully`);
            } else {
                this.addSystemMessage(`Error uploading ${file.name}: ${data.message}`);
            }
        } catch (error) {
            console.error('Error uploading file:', error);
            this.addSystemMessage(`Failed to upload file: ${error.message}`);
        }
    }

    /**
     * Remove uploaded file
     */
    async removeFile(fileId, fileName) {
        try {
            const response = await fetch(`/testmanagement/chat/files/${fileId}`, {
                method: 'DELETE'
            });

            const data = await response.json();

            if (data.success) {
                this.uploadedFiles = this.uploadedFiles.filter(f => f.id !== fileId);
                this.updateUploadedFilesDisplay();
                this.addSystemMessage(`File "${fileName}" removed`);
            }
        } catch (error) {
            console.error('Error removing file:', error);
            this.addSystemMessage(`Failed to remove file: ${error.message}`);
        }
    }

    /**
     * Update uploaded files display
     */
    updateUploadedFilesDisplay() {
        const container = document.getElementById('uploadedFilesContainer');
        const list = document.getElementById('uploadedFilesList');

        if (!container || !list) return;

        if (this.uploadedFiles.length === 0) {
            container.style.display = 'none';
            return;
        }

        container.style.display = 'block';
        list.innerHTML = '';

        this.uploadedFiles.forEach(file => {
            const fileItem = document.createElement('div');
            fileItem.style.cssText = 'display: flex; justify-content: space-between; align-items: center; padding: 6px 8px; background: white; border-radius: 4px; font-size: 12px;';
            
            const icon = this.getFileIcon(file.fileType);
            const size = (file.fileSize / 1024).toFixed(1);

            fileItem.innerHTML = `
                <span style="flex: 1;">
                    ${icon} <strong>${file.name}</strong>
                    <span style="color: #9ca3af; font-size: 11px;">(${size} KB)</span>
                </span>
                <button 
                    onclick="chatHandler.removeFile('${file.id}', '${file.name}')"
                    style="background: #fee2e2; color: #991b1b; border: none; padding: 3px 8px; border-radius: 3px; cursor: pointer; font-size: 11px; font-weight: 600;"
                >
                    ✕
                </button>
            `;

            list.appendChild(fileItem);
        });
    }

    /**
     * Get file icon based on type
     */
    getFileIcon(fileType) {
        const icons = {
            'pdf': '📄',
            'docx': '📋',
            'doc': '📋',
            'txt': '📝',
            'jpg': '🖼️',
            'jpeg': '🖼️',
            'png': '🖼️',
            'gif': '🖼️',
            'pgf': '🖼️',
            'xlsx': '📊',
            'xls': '📊'
        };
        return icons[fileType] || '📎';
    }

    async sendMessage() {
        const message = this.chatInput.value.trim();
        
        if (!message && this.uploadedFiles.length === 0) {
            console.warn('Empty message and no files');
            return;
        }

        if (!this.conversationId) {
            console.warn('No conversation ID');
            this.addSystemMessage('Chat not initialized. Please refresh the page.');
            return;
        }

        // Clear input
        this.chatInput.value = '';
        this.chatInput.focus();
        
        // Add user message
        if (message) {
            this.addUserMessage(message);
        }

        // Show file info if only files
        if (!message && this.uploadedFiles.length > 0) {
            this.addUserMessage(`📎 Attached ${this.uploadedFiles.length} file(s) for analysis`);
        }
        
        // Show loading indicator
        const loadingIndicator = this.addLoadingMessage();

        try {
            const selectedGuides = this.getSelectedGuides();
            const attachmentIds = this.uploadedFiles.map(f => f.id);

            const requestBody = {
                conversationId: this.conversationId,
                message: message,
                guides: selectedGuides,
                attachmentIds: attachmentIds
            };

            console.log('Sending message:', requestBody);

            const response = await fetch('/testmanagement/chat/message', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requestBody)
            });

            if (response.ok) {
                const data = await response.json();
                
                // Remove loading indicator
                if (loadingIndicator) loadingIndicator.remove();
                
                // Add assistant message. Backend uses "content"; keep "response" for compatibility.
                const assistantContent = data.content || data.response || data.message;
                if (assistantContent) {
                    this.addAssistantMessage(assistantContent, data);
                }

                if (data.contextSummary || (data.processingSteps && data.processingSteps.length > 0)) {
                    this.addResponseDetails(data);
                }
                
                // If test cases were generated, show them
                if (data.testCases && data.testCases.length > 0) {
                    this.showGeneratedTestCases(data.testCases);
                }

                // Clear uploaded files after successful send
                this.uploadedFiles = [];
                this.updateUploadedFilesDisplay();
            } else {
                if (loadingIndicator) loadingIndicator.remove();
                console.error('Error response:', response.status);
                this.addSystemMessage('Error processing your message. Please try again.');
            }
        } catch (error) {
            console.error('Error sending message:', error);
            if (loadingIndicator) loadingIndicator.remove();
            this.addSystemMessage('Failed to send message. Please check your connection.');
        }
    }

    addUserMessage(content) {
        const messageDiv = document.createElement('div');
        messageDiv.style.cssText = 'display: flex; justify-content: flex-end; margin-bottom: 12px;';
        messageDiv.innerHTML = `
            <div style="background-color: #0052cc; color: white; padding: 10px 14px; border-radius: 8px; max-width: 70%; word-wrap: break-word;">
                ${this.escapeHtml(content)}
            </div>
        `;
        this.messagesArea.appendChild(messageDiv);
        this.messagesArea.scrollTop = this.messagesArea.scrollHeight;
    }

    addAssistantMessage(content, responseData = null) {
        const messageDiv = document.createElement('div');
        messageDiv.style.cssText = 'display: flex; justify-content: flex-start; margin-bottom: 12px;';
        const messageId = responseData && responseData.id ? responseData.id : '';
        messageDiv.innerHTML = `
            <div style="background-color: #f3f4f6; color: #1f2937; padding: 10px 14px; border-radius: 8px; max-width: 70%; word-wrap: break-word;">
                ${this.escapeHtml(content)}
                ${messageId ? this.renderActionToolbar(messageId) : ''}
            </div>
        `;
        this.messagesArea.appendChild(messageDiv);
        this.messagesArea.scrollTop = this.messagesArea.scrollHeight;
    }

    renderActionToolbar(messageId) {
        return `
            <div style="display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; padding-top: 10px; border-top: 1px solid #d1d5db;">
                <button onclick="chatHandler.runResponseAction('regenerate')" style="padding: 5px 9px; border: 1px solid #c7d2fe; background: #eef2ff; color: #3730a3; border-radius: 5px; cursor: pointer; font-size: 12px;">Regenerate</button>
                <button onclick="chatHandler.runResponseAction('modify')" style="padding: 5px 9px; border: 1px solid #c7d2fe; background: #eef2ff; color: #3730a3; border-radius: 5px; cursor: pointer; font-size: 12px;">Modify</button>
                <button onclick="chatHandler.runResponseAction('add-more')" style="padding: 5px 9px; border: 1px solid #bfdbfe; background: #eff6ff; color: #1d4ed8; border-radius: 5px; cursor: pointer; font-size: 12px;">Add More</button>
                <button onclick="chatHandler.runResponseAction('merge')" style="padding: 5px 9px; border: 1px solid #a7f3d0; background: #ecfdf5; color: #047857; border-radius: 5px; cursor: pointer; font-size: 12px;">Merge</button>
                <button onclick="chatHandler.deleteAssistantMessage('${this.escapeHtml(messageId)}')" style="padding: 5px 9px; border: 1px solid #fecaca; background: #fef2f2; color: #b91c1c; border-radius: 5px; cursor: pointer; font-size: 12px; margin-left: auto;">Delete</button>
            </div>
        `;
    }

    async runResponseAction(actionType) {
        if (!this.conversationId) return;

        const actionMessages = {
            'regenerate': 'Regenerate the previous test cases with a different approach.',
            'modify': 'Modify the previous test cases to improve coverage and clarity.',
            'add-more': 'Add more edge and negative test cases.',
            'merge': 'Merge duplicate or overlapping test cases.'
        };

        try {
            const response = await fetch(`/testmanagement/chat/${this.conversationId}/${actionType}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: actionMessages[actionType] || actionType })
            });

            if (!response.ok) {
                throw new Error(`Action failed: ${response.status}`);
            }

            const data = await response.json();
            const content = data.content || data.response || data.message || `${actionType} completed`;
            this.addAssistantMessage(content, data);
            if (data.contextSummary || (data.processingSteps && data.processingSteps.length > 0)) {
                this.addResponseDetails(data);
            }
            if (data.testCases && data.testCases.length > 0) {
                this.showGeneratedTestCases(data.testCases);
            }
        } catch (error) {
            console.error('Error running response action:', error);
            this.addSystemMessage(`Failed to run ${actionType}: ${error.message}`);
        }
    }

    deleteAssistantMessage(messageId) {
        const button = event && event.target ? event.target : null;
        const wrapper = button ? button.closest('div[style*="justify-content: flex-start"]') : null;
        if (wrapper) {
            wrapper.remove();
        }
        this.addSystemMessage('Response removed from this view.');
    }

    addResponseDetails(data) {
        const detailsDiv = document.createElement('div');
        detailsDiv.style.cssText = 'display: flex; justify-content: flex-start; margin-bottom: 12px;';

        const steps = Array.isArray(data.processingSteps)
            ? data.processingSteps.map(step => `<li>${this.escapeHtml(step)}</li>`).join('')
            : '';

        detailsDiv.innerHTML = `
            <div style="background-color: #eef2ff; color: #374151; padding: 10px 14px; border-radius: 8px; max-width: 70%; font-size: 13px; line-height: 1.5;">
                ${data.contextSummary ? `<div><strong>${this.escapeHtml(data.contextSummary)}</strong></div>` : ''}
                <div style="margin-top: 4px; color: #6b7280;">
                    Guides: ${data.guidesUsed || 0} &nbsp; Files: ${data.filesUsed || 0} &nbsp; Vector hits: ${data.vectorHits || 0}
                </div>
                ${steps ? `<ul style="margin: 8px 0 0 18px; padding: 0;">${steps}</ul>` : ''}
            </div>
        `;

        this.messagesArea.appendChild(detailsDiv);
        this.messagesArea.scrollTop = this.messagesArea.scrollHeight;
    }

    addSystemMessage(content) {
        const messageDiv = document.createElement('div');
        messageDiv.style.cssText = 'display: flex; justify-content: center; margin-bottom: 12px;';
        messageDiv.innerHTML = `
            <div style="background-color: #e5e7eb; color: #6b7280; padding: 10px 14px; border-radius: 8px; max-width: 70%; text-align: center; font-size: 14px; word-wrap: break-word;">
                ${this.escapeHtml(content)}
            </div>
        `;
        this.messagesArea.appendChild(messageDiv);
        this.messagesArea.scrollTop = this.messagesArea.scrollHeight;
    }

    addLoadingMessage() {
        const messageDiv = document.createElement('div');
        messageDiv.style.cssText = 'display: flex; justify-content: flex-start; margin-bottom: 12px;';
        messageDiv.innerHTML = `
            <div style="background-color: #f3f4f6; padding: 10px 14px; border-radius: 8px;">
                <div style="display: flex; gap: 4px;">
                    <div style="width: 8px; height: 8px; background-color: #9ca3af; border-radius: 50%; animation: pulse 1.4s infinite;"></div>
                    <div style="width: 8px; height: 8px; background-color: #9ca3af; border-radius: 50%; animation: pulse 1.4s infinite; animation-delay: 0.2s;"></div>
                    <div style="width: 8px; height: 8px; background-color: #9ca3af; border-radius: 50%; animation: pulse 1.4s infinite; animation-delay: 0.4s;"></div>
                </div>
            </div>
        `;
        this.messagesArea.appendChild(messageDiv);
        this.messagesArea.scrollTop = this.messagesArea.scrollHeight;
        return messageDiv;
    }

    showGeneratedTestCases(testCases) {
        const caseDiv = document.createElement('div');
        caseDiv.style.cssText = 'margin: 0 0 12px 0; padding: 12px; background-color: #f8fafc; border-left: 4px solid #0052cc; border-radius: 6px; max-width: 76%;';
        
        let html = '<strong>Generated Test Cases:</strong><div style="display: grid; gap: 10px; margin-top: 10px;">';
        testCases.forEach(tc => {
            const steps = Array.isArray(tc.steps)
                ? tc.steps.slice(0, 5).map(step => `<li>${this.escapeHtml(String(step))}</li>`).join('')
                : '';
            html += `
                <div style="background: white; border: 1px solid #e5e7eb; border-radius: 6px; padding: 10px;">
                    <div style="font-weight: 700; color: #111827; margin-bottom: 6px;">${this.escapeHtml(tc.title || 'Untitled test case')}</div>
                    <div style="font-size: 12px; color: #6b7280; margin-bottom: 6px;">Status: ${this.escapeHtml(tc.status || 'Generated')} &nbsp; Version: ${tc.version || 1}</div>
                    ${steps ? `<ol style="margin: 0 0 8px 18px; padding: 0; font-size: 13px; color: #374151;">${steps}</ol>` : ''}
                    ${tc.expectedResults ? `<div style="font-size: 13px; color: #374151;"><strong>Expected:</strong> ${this.escapeHtml(tc.expectedResults)}</div>` : ''}
                </div>
            `;
        });
        html += '</div>';
        
        caseDiv.innerHTML = html;
        this.messagesArea.appendChild(caseDiv);
        this.messagesArea.scrollTop = this.messagesArea.scrollHeight;
    }

    escapeHtml(text) {
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.replace(/[&<>"']/g, m => map[m]);
    }
}

// Add pulse animation
const style = document.createElement('style');
style.textContent = `
    @keyframes pulse {
        0%, 100% { opacity: 1; }
        50% { opacity: 0.5; }
    }
`;
document.head.appendChild(style);

// Initialize chat when page loads
let chatHandler = null;

function initializeChat() {
    const chatContainer = document.getElementById('chatMessagesArea');
    if (chatContainer && !chatHandler) {
        console.log('Initializing ChatHandler');
        chatHandler = new ChatHandler();
    }
}

// Try to initialize immediately
setTimeout(() => {
    initializeChat();
}, 100);

// Also initialize on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
    setTimeout(() => {
        initializeChat();
    }, 100);
});

// Initialize when chat link is clicked
document.addEventListener('DOMContentLoaded', () => {
    const chatLink = document.getElementById('nav-chat');
    if (chatLink) {
        chatLink.addEventListener('click', () => {
            setTimeout(() => initializeChat(), 100);
        });
    }
});
