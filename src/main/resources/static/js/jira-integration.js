/**
 * Jira Integration Module
 * Handles configuration, connection, and test case synchronization with Jira
 */

class JiraIntegration {
    constructor() {
        this.config = this.loadConfig();
        this.pushHistory = this.loadHistory();
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.updateConnectionStatus();
        this.loadTestCases();
        this.loadPushHistory();
    }

    setupEventListeners() {
        // Tab navigation
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.addEventListener('click', (e) => this.switchTab(e.target.dataset.tab));
        });

        // Configuration form
        document.getElementById('jiraConfigForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.saveConfiguration();
        });

        document.getElementById('testConnectionBtn').addEventListener('click', () => {
            this.testConnection();
        });

        // Push form
        document.getElementById('pushForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.pushTestCases();
        });
    }

    // ============ Configuration Management ============

    loadConfig() {
        const stored = sessionStorage.getItem('jiraConfig');
        return stored ? JSON.parse(stored) : {
            url: '',
            email: '',
            connected: false,
            lastChecked: null
        };
    }

    saveConfig() {
        const safeConfig = {
            url: this.config.url || '',
            email: this.config.email || '',
            connected: Boolean(this.config.connected),
            lastChecked: this.config.lastChecked || null
        };
        sessionStorage.setItem('jiraConfig', JSON.stringify(safeConfig));
    }

    saveConfiguration() {
        const url = document.getElementById('jiraUrl').value.trim();
        const email = document.getElementById('jiraEmail').value.trim();
        const token = document.getElementById('jiraToken').value.trim();

        if (!url || !email || !token) {
            this.showAlert('Please fill in all required fields', 'danger');
            return;
        }

        // Validate URL format
        try {
            new URL(url);
        } catch {
            this.showAlert('Invalid Jira URL. Please include https://', 'danger');
            return;
        }

        fetch('/testmanagement/api/integrations/settings', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-User-Id': this.getUserId()
            },
            body: JSON.stringify({
                jiraWorkspaceUrl: url,
                jiraAuthMethod: 'PAT',
                jiraToken: token
            })
        }).then(async response => {
            const result = await response.json();
            if (!response.ok) {
                throw new Error(result.error || 'Failed to save configuration');
            }
            this.config = { ...this.config, url, email, connected: true, lastChecked: new Date().toISOString() };
            this.saveConfig();
            document.getElementById('jiraToken').value = '';
            document.getElementById('jiraToken').placeholder = 'Saved securely on server';
            this.showAlert('Configuration saved securely on the server', 'success');
            this.updateConnectionStatus();
        }).catch(error => {
            this.showAlert('Configuration error: ' + error.message, 'danger');
        });
    }

    loadSavedConfig() {
        if (this.config.url) {
            document.getElementById('jiraUrl').value = this.config.url;
            document.getElementById('jiraEmail').value = this.config.email;
            document.getElementById('jiraToken').placeholder = 'Saved securely on server';
        }
    }

    // ============ Connection Testing ============

    async testConnection() {
        const url = document.getElementById('jiraUrl').value.trim();

        if (!url) {
            this.showAlert('Please save Jira settings first', 'danger');
            return;
        }

        const btn = document.getElementById('testConnectionBtn');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner inline-block mr-2"></span>Testing...';

        try {
            const response = await fetch('/testmanagement/api/integrations/test-connections', {
                method: 'POST',
                headers: { 'X-User-Id': this.getUserId() }
            });
            const result = await response.json();
            if (!response.ok || !result.connections?.jira) {
                throw new Error(result.error || 'Jira connection failed');
            }
            this.config.connected = true;
            this.config.lastChecked = new Date().toISOString();
            this.saveConfig();
            this.showAlert('Connection successful', 'success');
            this.updateConnectionStatus();
            this.loadProjects();
        } catch (error) {
            this.showAlert('Connection error: ' + error.message, 'danger');
        } finally {
            btn.disabled = false;
            btn.innerHTML = 'Test Connection';
        }
    }

    async loadProjects() {
        if (!this.config.connected) return;

        const container = document.getElementById('projectsContainer');
        container.innerHTML = '<div class="flex items-center space-x-2"><span class="spinner"></span><span>Loading projects...</span></div>';

        try {
            // This would call your backend endpoint to fetch projects
            // const response = await fetch('/integrations/jira/projects');
            // const projects = await response.json();
            
            // For now, show placeholder
            container.innerHTML = `
                <div class="alert alert-info">
                    <strong>Note:</strong> Projects will be loaded once you configure and test your Jira connection.
                </div>
            `;
        } catch (error) {
            container.innerHTML = `<div class="alert alert-danger">Error loading projects: ${error.message}</div>`;
        }
    }

    // ============ Test Cases ============

    async loadTestCases() {
        const container = document.getElementById('testCasesList');
        
        try {
            // Fetch test cases from your backend
            const response = await fetch('/api/testcases/list');
            const testCases = await response.json();

            if (!testCases || testCases.length === 0) {
                container.innerHTML = '<p class="text-gray-500 text-sm">No test cases available</p>';
                return;
            }

            let html = '<div class="space-y-2">';
            testCases.forEach((tc, index) => {
                html += `
                    <label class="flex items-center space-x-2 p-2 hover:bg-gray-50 rounded">
                        <input 
                            type="checkbox" 
                            class="testcase-checkbox w-4 h-4 text-blue-600" 
                            value="${tc.id || index}"
                            data-name="${tc.name || 'Test Case'}"
                        >
                        <span class="text-sm text-gray-700">${tc.name || 'Unnamed'}</span>
                        <span class="text-xs text-gray-500">(${tc.applicationName || 'N/A'})</span>
                    </label>
                `;
            });
            html += '</div>';
            container.innerHTML = html;
        } catch (error) {
            container.innerHTML = `<p class="text-red-500 text-sm">Error loading test cases: ${error.message}</p>`;
        }
    }

    // ============ Push Functionality ============

    async pushTestCases() {
        const projectKey = document.getElementById('projectKey').value;
        const issueType = document.getElementById('issueType').value;
        const selectedCheckboxes = document.querySelectorAll('.testcase-checkbox:checked');

        if (!projectKey || !issueType) {
            this.showAlert('Please select project and issue type', 'danger');
            return;
        }

        if (selectedCheckboxes.length === 0) {
            this.showAlert('Please select at least one test case', 'danger');
            return;
        }

        if (!this.config.connected) {
            this.showAlert('Please configure and test Jira connection first', 'danger');
            return;
        }

        const testCaseIds = Array.from(selectedCheckboxes).map(cb => cb.value);
        const btn = document.querySelector('#pushForm button[type="submit"]');
        const spinner = document.getElementById('pushSpinner');
        const btnText = document.getElementById('pushBtnText');

        btn.disabled = true;
        btnText.textContent = 'Pushing...';
        spinner.classList.remove('hidden');

        try {
            const response = await fetch('/integrations/jira/push', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    testCaseIds,
                    jiraProjectKey: projectKey,
                    issueType
                })
            });

            const result = await response.json();

            if (response.ok) {
                const successCount = result.results.filter(r => r.status === 'SUCCESS').length;
                const failedCount = result.results.length - successCount;

                this.addToHistory({
                    date: new Date(),
                    project: projectKey,
                    testCaseCount: testCaseIds.length,
                    success: successCount,
                    failed: failedCount,
                    results: result.results
                });

                this.showAlert(
                    `✓ Push successful: ${successCount} passed, ${failedCount} failed`,
                    failedCount === 0 ? 'success' : 'warning'
                );

                document.getElementById('pushForm').reset();
                this.loadPushHistory();
            } else {
                this.showAlert(`Error: ${result.message || 'Push failed'}`, 'danger');
            }
        } catch (error) {
            this.showAlert(`Push error: ${error.message}`, 'danger');
        } finally {
            btn.disabled = false;
            btnText.textContent = 'Push to Jira';
            spinner.classList.add('hidden');
        }
    }

    // ============ History Management ============

    loadHistory() {
        const stored = localStorage.getItem('jiraPushHistory');
        return stored ? JSON.parse(stored) : [];
    }

    saveHistory() {
        localStorage.setItem('jiraPushHistory', JSON.stringify(this.pushHistory));
    }

    addToHistory(entry) {
        this.pushHistory.unshift(entry);
        // Keep only last 50 entries
        if (this.pushHistory.length > 50) {
            this.pushHistory = this.pushHistory.slice(0, 50);
        }
        this.saveHistory();
    }

    loadPushHistory() {
        const table = document.getElementById('historyTable');
        
        if (this.pushHistory.length === 0) {
            table.innerHTML = '<tr><td colspan="7" class="px-6 py-4 text-center text-gray-500">No push history available</td></tr>';
            return;
        }

        let html = '';
        this.pushHistory.forEach(entry => {
            const date = new Date(entry.date).toLocaleString();
            const success = entry.success || 0;
            const failed = entry.failed || 0;
            const successBadge = success > 0 ? `<span class="badge badge-success">${success}</span>` : '';
            const failedBadge = failed > 0 ? `<span class="badge badge-danger ml-2">${failed}</span>` : '';
            const statusBadge = failed === 0 ? 
                '<span class="badge badge-success">✓ Success</span>' : 
                '<span class="badge badge-warning">⚠ Partial</span>';

            html += `
                <tr>
                    <td class="px-6 py-4">${date}</td>
                    <td class="px-6 py-4 font-medium">${entry.project}</td>
                    <td class="px-6 py-4">${entry.testCaseCount}</td>
                    <td class="px-6 py-4">${successBadge}</td>
                    <td class="px-6 py-4">${failedBadge}</td>
                    <td class="px-6 py-4">${statusBadge}</td>
                    <td class="px-6 py-4">
                        <button class="text-blue-600 hover:text-blue-800 text-sm font-medium" onclick="jiraApp.showDetails(${this.pushHistory.indexOf(entry)})">
                            View
                        </button>
                    </td>
                </tr>
            `;
        });
        table.innerHTML = html;
    }

    showDetails(index) {
        const entry = this.pushHistory[index];
        if (!entry || !entry.results) return;

        let detailsHtml = '<div class="space-y-2">';
        entry.results.forEach(result => {
            const badge = result.status === 'SUCCESS' 
                ? '<span class="badge badge-success">✓</span>' 
                : '<span class="badge badge-danger">✗</span>';
            detailsHtml += `
                <div class="text-sm">
                    ${badge}
                    <span class="font-medium">${result.testCaseName || result.name || 'Test Case'}</span>
                    ${result.jiraKey ? `<span class="text-blue-600">${result.jiraKey}</span>` : ''}
                    ${result.message ? `<span class="text-gray-600 ml-2">${result.message}</span>` : ''}
                </div>
            `;
        });
        detailsHtml += '</div>';

        this.showModal('Push Results', detailsHtml);
    }

    // ============ UI Utilities ============

    switchTab(tabName) {
        // Hide all tabs
        document.querySelectorAll('.tab-content').forEach(tab => {
            tab.classList.add('hidden');
        });

        // Remove active state from all buttons
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.remove('active', 'border-blue-600', 'text-gray-700');
            btn.classList.add('border-transparent', 'text-gray-500');
        });

        // Show selected tab
        document.getElementById(tabName).classList.remove('hidden');

        // Add active state to clicked button
        event.target.classList.add('active', 'border-blue-600', 'text-gray-700');
        event.target.classList.remove('border-transparent', 'text-gray-500');
    }

    showAlert(message, type = 'info') {
        const container = document.getElementById('alertContainer');
        const alertId = 'alert-' + Date.now();
        
        const alertDiv = document.createElement('div');
        alertDiv.id = alertId;
        alertDiv.className = `alert alert-${type} mb-4 flex items-center justify-between`;
        alertDiv.innerHTML = `
            <span>${message}</span>
            <button class="ml-4 text-gray-500 hover:text-gray-700" onclick="document.getElementById('${alertId}').remove()">
                ×
            </button>
        `;
        
        container.appendChild(alertDiv);

        // Auto-remove after 5 seconds
        setTimeout(() => {
            const elem = document.getElementById(alertId);
            if (elem) elem.remove();
        }, 5000);
    }

    showModal(title, content) {
        // Simple modal - you can enhance this with a proper modal library
        alert(`${title}\n\n${content}`);
    }

    getUserId() {
        const user = sessionStorage.getItem('user');
        if (user) {
            try {
                return JSON.parse(user).username || 'anonymous';
            } catch {
                return user;
            }
        }
        return 'anonymous';
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.jiraApp = new JiraIntegration();
});
