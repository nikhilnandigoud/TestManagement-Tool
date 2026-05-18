/**
 * Code Viewer Component
 * Displays generated automation code with syntax highlighting
 * Supports multiple files and language-specific formatting
 */
class CodeViewerComponent {
    constructor(artifact, framework, language, testCases) {
        this.artifact = artifact;
        this.framework = framework;
        this.language = language;
        this.testCases = testCases;
        this.currentFileIndex = 0;
        this.element = null;
    }

    /**
     * Render the code viewer in a container
     */
    render(container) {
        this.element = container;
        
        const files = this.artifact.files || [];
        const code = this.artifact.code || '';
        const hasMultipleFiles = files.length > 1;
        const hasCode = code.length > 0;

        console.log('Code Viewer - artifact:', this.artifact);
        console.log('Code Viewer - artifact.files:', files.length, 'files:', files);
        console.log('Code Viewer - artifact.code length:', code.length);

        let html = `
            <div class="code-viewer-container" style="background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                <!-- Header with Framework/Language Info -->
                <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px 8px 0 0;">
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <h2 style="margin: 0 0 5px 0; font-size: 18px; font-weight: 700;">💻 Generated Code</h2>
                            <p style="margin: 0; font-size: 13px; opacity: 0.9;">
                                <strong>${this.framework}</strong> • <strong>${this.language}</strong> • ${this.testCases.length} Test Case(s)
                            </p>
                        </div>
                        <div>
                            <button id="closeViewerBtn" style="background: rgba(255,255,255,0.2); border: 1px solid rgba(255,255,255,0.4); color: white; padding: 6px 12px; border-radius: 4px; cursor: pointer; font-size: 12px; font-weight: 600;">✕ Close</button>
                        </div>
                    </div>
                </div>

                <!-- Controls Bar -->
                <div style="background: #f8f9fa; border-bottom: 1px solid #e9ecef; padding: 12px 15px; display: flex; justify-content: space-between; align-items: center;">
                    <div style="display: flex; gap: 8px; flex: 1;">
                        <button id="copyAllBtn" class="btn-code-action" style="padding: 6px 12px; background: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 12px; font-weight: 600;">📋 Copy All</button>
                        <button id="downloadFileBtn" class="btn-code-action" style="padding: 6px 12px; background: #28a745; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 12px; font-weight: 600;">💾 Download File</button>
                        ${hasMultipleFiles ? `<button id="downloadAllBtn" class="btn-code-action" style="padding: 6px 12px; background: #6f42c1; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 12px; font-weight: 600;">📦 Download All</button>` : ''}
                    </div>
                    <div style="display: flex; gap: 10px; align-items: center;">
                        <span style="font-size: 11px; color: #666; font-weight: 500;">${files.length > 0 ? files.length : '1'} File(s)</span>
                    </div>
                </div>

                <!-- File Tabs -->
                ${hasMultipleFiles ? `
                    <div style="background: #f8f9fa; border-bottom: 1px solid #e9ecef; padding: 0; display: flex; overflow-x: auto;">
                        ${files.map((file, idx) => `
                            <button class="file-tab" data-file-index="${idx}" style="
                                padding: 12px 16px;
                                border: none;
                                background: ${idx === 0 ? 'white' : '#f8f9fa'};
                                color: ${idx === 0 ? '#333' : '#666'};
                                cursor: pointer;
                                font-size: 12px;
                                font-weight: ${idx === 0 ? '600' : '500'};
                                border-bottom: ${idx === 0 ? '2px solid #007bff' : 'none'};
                                white-space: nowrap;
                                transition: all 0.2s ease;
                            ">
                                📄 ${this.extractFileName(file.filePath)}
                            </button>
                        `).join('')}
                    </div>
                ` : ''}

                <!-- Code Content -->
                <div style="background: #f5f5f5; padding: 20px; min-height: 400px; max-height: 600px; overflow-y: auto; font-family: 'Courier New', monospace;">
                    <pre id="codeContent" style="
                        background: white;
                        border: 1px solid #ddd;
                        border-radius: 4px;
                        padding: 16px;
                        margin: 0;
                        font-size: 12px;
                        line-height: 1.6;
                        color: #333;
                        overflow-x: auto;
                    "></pre>
                </div>

                <!-- Footer -->
                <div style="background: #f8f9fa; border-top: 1px solid #e9ecef; padding: 12px 15px; font-size: 11px; color: #666;">
                    <span id="fileNameFooter">${files.length > 0 ? files[0]?.filePath : 'Generated Code'}</span> • 
                    <span id="lineCount">0 lines</span> • 
                    <span>Confidence: ${(this.artifact.confidence * 100).toFixed(0)}%</span>
                    ${this.artifact.requiresHumanReview ? '<span style="color: #ff6b6b; font-weight: 600;">⚠️ Requires Review</span>' : ''}
                </div>
            </div>
        `;

        this.element.innerHTML = html;
        this.setupEventListeners();
        
        // Display code - either from files array or from code property
        if (files.length > 0) {
            this.displayCode(0);
        } else if (code) {
            this.displayCodeFromString(code);
        } else {
            document.getElementById('codeContent').textContent = 'No code available';
        }
    }

    setupEventListeners() {
        // File tabs
        document.querySelectorAll('.file-tab').forEach(tab => {
            tab.addEventListener('click', (e) => {
                const fileIndex = parseInt(e.target.getAttribute('data-file-index'));
                this.switchFile(fileIndex);
            });
        });

        // Action buttons
        document.getElementById('copyAllBtn')?.addEventListener('click', () => this.copyCodeToClipboard());
        document.getElementById('downloadFileBtn')?.addEventListener('click', () => this.downloadCurrentFile());
        document.getElementById('downloadAllBtn')?.addEventListener('click', () => this.downloadAllFiles());
        document.getElementById('closeViewerBtn')?.addEventListener('click', () => this.closeViewer());
    }

    /**
     * Switch to a different file
     */
    switchFile(fileIndex) {
        // Update tab styling
        document.querySelectorAll('.file-tab').forEach((tab, idx) => {
            if (idx === fileIndex) {
                tab.style.background = 'white';
                tab.style.color = '#333';
                tab.style.fontWeight = '600';
                tab.style.borderBottom = '2px solid #007bff';
            } else {
                tab.style.background = '#f8f9fa';
                tab.style.color = '#666';
                tab.style.fontWeight = '500';
                tab.style.borderBottom = 'none';
            }
        });

        this.currentFileIndex = fileIndex;
        this.displayCode(fileIndex);
    }

    /**
     * Display code with syntax highlighting
     */
    displayCode(fileIndex) {
        const files = this.artifact.files || [];
        if (fileIndex >= files.length) return;

        const file = files[fileIndex];
        const codeContent = document.getElementById('codeContent');
        const lineCount = document.getElementById('lineCount');

        // Get code (will be escaped in applySyntaxHighlighting)
        const code = file.content || '';
        
        // Apply syntax highlighting (which includes HTML escaping internally)
        const highlightedCode = this.applySyntaxHighlighting(code, this.language);
        
        codeContent.innerHTML = highlightedCode;
        lineCount.textContent = code.split('\n').length + ' lines';

        // Update footer with current file
        const fileNameFooter = document.getElementById('fileNameFooter');
        if (fileNameFooter) {
            fileNameFooter.textContent = file.filePath;
        }
    }

    /**
     * Display code from string (when no files array)
     */
    displayCodeFromString(code) {
        const codeContent = document.getElementById('codeContent');
        const lineCount = document.getElementById('lineCount');

        if (!code || code === null || code === undefined) {
            codeContent.textContent = 'No code available';
            lineCount.textContent = '0 lines';
            return;
        }

        // Apply syntax highlighting (which includes HTML escaping internally)
        const highlightedCode = this.applySyntaxHighlighting(code, this.language);
        
        codeContent.innerHTML = highlightedCode;
        const lines = code.split('\n').length;
        lineCount.textContent = lines + ' lines';
    }

    /**
     * Apply syntax highlighting based on language
     */
    applySyntaxHighlighting(code, language) {
        // Simple syntax highlighting using token-based approach
        // This avoids double-escaping by only escaping unprocessed code
        
        const keywords = ['class', 'function', 'public', 'private', 'protected', 'static', 'void', 'String', 'int', 
                         'boolean', 'return', 'if', 'else', 'for', 'while', 'try', 'catch', 'finally', 'throw',
                         'new', 'this', 'super', 'import', 'package', 'interface', 'extends', 'implements',
                         'def', 'async', 'await', 'const', 'let', 'var', 'export', 'import', 'from'];

        let result = '';
        let i = 0;
        
        while (i < code.length) {
            // Check for strings (single, double, backtick quotes)
            if ((code[i] === '"' || code[i] === "'" || code[i] === '`') && i > 0) {
                const quote = code[i];
                let stringContent = quote;
                i++;
                while (i < code.length && code[i] !== quote) {
                    stringContent += code[i];
                    i++;
                }
                if (i < code.length) stringContent += quote;
                result += `<span style="color: #008000;">${this.escapeHtml(stringContent)}</span>`;
                i++;
            }
            // Check for line comments
            else if (code[i] === '/' && code[i + 1] === '/') {
                let comment = '';
                while (i < code.length && code[i] !== '\n') {
                    comment += code[i];
                    i++;
                }
                result += `<span style="color: #666666; font-style: italic;">${this.escapeHtml(comment)}</span>`;
            }
            // Check for block comments
            else if (code[i] === '/' && code[i + 1] === '*') {
                let comment = '';
                while (i < code.length - 1 && !(code[i] === '*' && code[i + 1] === '/')) {
                    comment += code[i];
                    i++;
                }
                if (i < code.length - 1) {
                    comment += '*/';
                    i += 2;
                }
                result += `<span style="color: #666666; font-style: italic;">${this.escapeHtml(comment)}</span>`;
            }
            // Check for keywords
            else if (/[a-zA-Z_$]/.test(code[i])) {
                let word = '';
                while (i < code.length && /[a-zA-Z0-9_$]/.test(code[i])) {
                    word += code[i];
                    i++;
                }
                if (keywords.includes(word)) {
                    result += `<span style="color: #0066cc; font-weight: 600;">${word}</span>`;
                } else {
                    result += this.escapeHtml(word);
                }
            }
            // Check for numbers
            else if (/\d/.test(code[i])) {
                let number = '';
                while (i < code.length && /[\d.]/.test(code[i])) {
                    number += code[i];
                    i++;
                }
                result += `<span style="color: #d73a49;">${number}</span>`;
            }
            // Regular character
            else {
                result += this.escapeHtml(code[i]);
                i++;
            }
        }
        
        return result;
    }

    /**
     * Copy current code to clipboard
     */
    copyCodeToClipboard() {
        const files = this.artifact.files || [];
        let codeText = '';

        if (files.length > 0) {
            const file = files[this.currentFileIndex];
            codeText = file?.content || '';
        } else {
            codeText = this.artifact.code || '';
        }

        if (!codeText) return;

        navigator.clipboard.writeText(codeText).then(() => {
            const btn = document.getElementById('copyAllBtn');
            const originalText = btn.textContent;
            btn.textContent = '✓ Copied!';
            btn.style.background = '#28a745';
            setTimeout(() => {
                btn.textContent = originalText;
                btn.style.background = '#007bff';
            }, 2000);
        }).catch(err => {
            alert('Failed to copy: ' + err);
        });
    }

    /**
     * Download current file
     */
    downloadCurrentFile() {
        const files = this.artifact.files || [];
        let content = '';
        let fileName = `GeneratedCode_${this.framework}_${this.language}.java`;

        if (files.length > 0) {
            const file = files[this.currentFileIndex];
            content = file?.content || '';
            fileName = this.extractFileName(file?.filePath) || fileName;
        } else {
            content = this.artifact.code || '';
        }

        if (!content) return;

        const element = document.createElement('a');
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(content));
        element.setAttribute('download', fileName);
        element.style.display = 'none';
        document.body.appendChild(element);
        element.click();
        document.body.removeChild(element);
    }

    /**
     * Download all files as merged text
     */
    downloadAllFiles() {
        const files = this.artifact.files || [];
        let mergedContent = '';

        if (files.length > 0) {
            files.forEach((file, idx) => {
                mergedContent += `\n${'='.repeat(60)}\n`;
                mergedContent += `FILE: ${file.filePath}\n`;
                mergedContent += `${'='.repeat(60)}\n\n`;
                mergedContent += file.content;
                mergedContent += '\n\n';
            });
        } else {
            mergedContent = this.artifact.code || '';
        }

        const element = document.createElement('a');
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(mergedContent));
        element.setAttribute('download', `GeneratedCode_${this.framework}_${this.language}.txt`);
        element.style.display = 'none';
        document.body.appendChild(element);
        element.click();
        document.body.removeChild(element);
    }

    /**
     * Extract filename from full path
     */
    extractFileName(filePath) {
        if (!filePath) {
            return 'code.txt';
        }
        return filePath.split('/').pop().split('\\').pop();
    }

    /**
     * Escape HTML special characters
     */
    escapeHtml(text) {
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return String(text).replace(/[&<>"']/g, m => map[m]);
    }

    /**
     * Close the viewer
     */
    closeViewer() {
        if (this.element) {
            this.element.innerHTML = '';
        }
    }
}
