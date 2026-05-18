/**
 * Jira Handler for Index.html
 * Manages Jira integration within the main dashboard
 */

class JiraHandler {
    constructor() {
        this.config = this.loadConfig();
        this.history = this.loadHistory();
        this.init();
    }

    init() {
        this.setupTabNavigation();
        this.setupEventListeners();
        this.updateConnectionStatus();
    }

    // ============ Tab Navigation ============
    setupTabNavigation() {
        const tabButtons = document.querySelectorAll('.jira-tab-btn');
        tabButtons.forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                this.switchTab(btn.dataset.tab);
            });
        });
    }

    switchTab(tabName) {
        // Hide all tabs
        document.querySelectorAll('.jira-tab-content').forEach(tab => {
            tab.classList.add('hidden');
        });

        // Remove active state
        document.querySelectorAll('.jira-tab-btn').forEach(btn => {
            btn.classList.remove('active', 'border-blue-600', 'text-gray-700');
            btn.classList.add('border-transparent', 'text-gray-500');
        });

        // Show selected tab
        const selectedTab = document.getElementById(tabName);
        if (selectedTab) {
            selectedTab.classList.remove('hidden');
        }

        // Activate button
        const activeBtn = document.querySelector(`[data-tab="${tabName}"]`);
        if (activeBtn) {
            activeBtn.classList.add('active', 'border-blue-600', 'text-gray-700');
            activeBtn.classList.remove('border-transparent', 'text-gray-500');
        }

        // Load content for specific tabs
        if (tabName === 'jira-push') {
            this.loadTestCasesForPush();
        } else if (tabName === 'jira-history') {
            this.displayHistory();
        }
    }

    // ============ Event Listeners ============
    setupEventListeners() {
        // Configuration form
        const configForm = document.getElementById('jira-config-form');
        if (configForm) {
            configForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.saveConfiguration();
            });
        }

        // Test connection button
        const testBtn = document.getElementById('jira-test-btn');
        if (testBtn) {
            testBtn.addEventListener('click', () => this.testConnection());
        }

        // Push form
        const pushForm = document.getElementById('jira-push-form');
        if (pushForm) {
            pushForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.submitPushForm();
            });
        }
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

    async saveConfiguration() {
        const url = (document.getElementById('jira-url')?.value || '').trim();
        const email = (document.getElementById('jira-email')?.value || '').trim();
        const token = (document.getElementById('jira-token')?.value || '').trim();

        if (!url || !token) {
            this.showAlert('Please fill in Jira URL and API token', 'danger');
            return;
        }

        try {
            new URL(url);
        } catch {
            this.showAlert('Invalid Jira URL. Please include https://', 'danger');
            return;
        }

        try {
            const response = await fetch('/testmanagement/api/integrations/settings', {
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
            });
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.error || 'Failed to save configuration');
            }

            this.config = { ...this.config, url, email, connected: true, lastChecked: new Date().toISOString() };
            this.saveConfig();
            const tokenInput = document.getElementById('jira-token');
            if (tokenInput) {
                tokenInput.value = '';
                tokenInput.placeholder = 'Saved securely on server';
            }
            this.showAlert('Configuration saved securely on the server', 'success');
            this.updateConnectionStatus();
        } catch (error) {
            this.showAlert('Failed to save configuration: ' + error.message, 'danger');
        }
    }

    loadSavedConfig() {
        if (this.config.url) {
            const urlInput = document.getElementById('jira-url');
            const emailInput = document.getElementById('jira-email');
            const tokenInput = document.getElementById('jira-token');

            if (urlInput) urlInput.value = this.config.url;
            if (emailInput) emailInput.value = this.config.email;
            if (tokenInput) tokenInput.placeholder = 'Saved securely on server';
        }
    }

    // ============ Connection Testing ============
    async testConnection() {
        const url = (document.getElementById('jira-url')?.value || '').trim();

        if (!url) {
            this.showAlert('Please save Jira settings first', 'danger');
            return;
        }

        const btn = document.getElementById('jira-test-btn');
        if (!btn) return;

        btn.disabled = true;
        const originalText = btn.textContent;
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
        } catch (error) {
            this.config.connected = false;
            this.saveConfig();
            this.showAlert('Connection failed: ' + error.message, 'danger');
            this.updateConnectionStatus();
        } finally {
            btn.disabled = false;
            btn.textContent = originalText;
        }
    }

    // ============ Connection Status ============
    updateConnectionStatus() {
        const statusDot = document.getElementById('jira-status-dot');
        const statusText = document.getElementById('jira-status-text');

        if (!statusDot || !statusText) return;

        if (this.config.connected) {
            statusDot.className = 'w-3 h-3 rounded-full bg-green-500';
            statusText.className = 'text-sm font-medium text-green-600';
            statusText.textContent = '✓ Connected';
        } else {
            statusDot.className = 'w-3 h-3 rounded-full bg-red-500';
            statusText.className = 'text-sm font-medium text-red-600';
            statusText.textContent = '✗ Not Connected';
        }

        this.loadSavedConfig();
    }

    // ============ Test Cases for Push ============
    async loadTestCasesForPush() {
        const container = document.getElementById('jira-push-testcases');
        if (!container) return;

        container.innerHTML = '<p class="text-gray-500 text-sm">Loading test cases...</p>';

        try {
            // This would fetch from your backend - update the endpoint as needed
            const response = await fetch('/api/testcases/list', { method: 'GET' });
            
            if (!response.ok) {
                throw new Error('Failed to load test cases');
            }

            const testCases = await response.json();

            if (!testCases || testCases.length === 0) {
                container.innerHTML = '<p class="text-gray-500 text-sm">No test cases available</p>';
                return;
            }

            let html = '<div class="space-y-2">';
            testCases.forEach((tc, index) => {
                const id = tc.id || index;
                const name = tc.name || tc.testName || 'Unnamed Test Case';
                const appName = tc.applicationName || tc.app || 'N/A';
                
                html += `
                    <label class="flex items-center space-x-3 p-3 hover:bg-gray-50 rounded cursor-pointer">
                        <input 
                            type="checkbox" 
                            class="jira-testcase-checkbox w-4 h-4 text-blue-600 rounded" 
                            value="${id}"
                            data-name="${name}"
                        >
                        <div class="flex-1">
                            <div class="text-sm font-medium text-gray-700">${name}</div>
                            <div class="text-xs text-gray-500">${appName}</div>
                        </div>
                    </label>
                `;
            });
            html += '</div>';
            container.innerHTML = html;
        } catch (error) {
            container.innerHTML = `<p class="text-red-500 text-sm">Error: ${error.message}</p>`;
        }
    }

    // ============ Push Functionality ============
    async submitPushForm() {
        if (!this.config.connected) {
            this.showAlert('Please configure and test Jira connection first', 'danger');
            return;
        }

        const projectKey = document.getElementById('jira-push-project')?.value.trim();
        const issueType = document.getElementById('jira-push-type')?.value.trim();
        const selectedCheckboxes = document.querySelectorAll('.jira-testcase-checkbox:checked');

        if (!projectKey) {
            this.showAlert('Please enter a project key', 'danger');
            return;
        }

        if (!issueType) {
            this.showAlert('Please select an issue type', 'danger');
            return;
        }

        if (selectedCheckboxes.length === 0) {
            this.showAlert('Please select at least one test case', 'danger');
            return;
        }

        const testCaseIds = Array.from(selectedCheckboxes).map(cb => cb.value);
        const testCaseNames = Array.from(selectedCheckboxes).map(cb => cb.dataset.name);

        const btn = document.querySelector('#jira-push-form button[type="submit"]');
        const spinner = document.getElementById('jira-push-spinner');
        const btnText = document.getElementById('jira-push-btn-text');

        if (btn) btn.disabled = true;
        if (spinner) spinner.classList.remove('hidden');
        if (btnText) btnText.textContent = 'Pushing...';

        try {
            const response = await fetch('/integrations/jira/push', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    testCaseIds,
                    jiraProjectKey: projectKey,
                    issueType
                })
            });

            const result = await response.json();

            if (response.ok) {
                const successCount = result.results?.filter(r => r.status === 'SUCCESS').length || 0;
                const failedCount = (result.results?.length || 0) - successCount;

                this.addToHistory({
                    date: new Date(),
                    project: projectKey,
                    testCaseCount: testCaseIds.length,
                    testCases: testCaseNames,
                    success: successCount,
                    failed: failedCount,
                    results: result.results
                });

                const message = failedCount === 0 
                    ? `✓ Success! All ${successCount} test cases pushed to Jira`
                    : `⚠ Completed with issues: ${successCount} succeeded, ${failedCount} failed`;
                
                this.showAlert(message, failedCount === 0 ? 'success' : 'warning');
                document.getElementById('jira-push-form')?.reset();
                this.loadTestCasesForPush();
            } else {
                this.showAlert(`Error: ${result.message || 'Push failed'}`, 'danger');
            }
        } catch (error) {
            this.showAlert(`Push error: ${error.message}`, 'danger');
        } finally {
            if (btn) btn.disabled = false;
            if (spinner) spinner.classList.add('hidden');
            if (btnText) btnText.textContent = 'Push to Jira';
        }
    }

    // ============ History Management ============
    loadHistory() {
        const stored = localStorage.getItem('jiraPushHistory');
        return stored ? JSON.parse(stored) : [];
    }

    saveHistory() {
        localStorage.setItem('jiraPushHistory', JSON.stringify(this.history));
    }

    addToHistory(entry) {
        this.history.unshift(entry);
        // Keep only last 50 entries
        if (this.history.length > 50) {
            this.history = this.history.slice(0, 50);
        }
        this.saveHistory();
    }

    displayHistory() {
        const table = document.getElementById('jira-history-table');
        if (!table) return;

        if (this.history.length === 0) {
            table.innerHTML = '<tr><td colspan="6" class="px-6 py-4 text-center text-gray-500">No push history available</td></tr>';
            return;
        }

        let html = '';
        this.history.forEach((entry, index) => {
            const date = new Date(entry.date).toLocaleString();
            const success = entry.success || 0;
            const failed = entry.failed || 0;
            const total = entry.testCaseCount || (success + failed);

            const successBadge = success > 0 ? `<span style="display:inline-block; padding:4px 12px; background-color:#e3fcef; color:#36b37e; border-radius:12px; font-size:12px; font-weight:600;">${success}</span>` : '';
            const failedBadge = failed > 0 ? `<span style="display:inline-block; margin-left:8px; padding:4px 12px; background-color:#ffebe6; color:#ff5630; border-radius:12px; font-size:12px; font-weight:600;">${failed}</span>` : '';
            const statusBadge = failed === 0 
                ? '<span style="display:inline-block; padding:4px 12px; background-color:#e3fcef; color:#36b37e; border-radius:12px; font-size:12px; font-weight:600;">✓ Success</span>'
                : '<span style="display:inline-block; padding:4px 12px; background-color:#fff4d9; color:#ffab00; border-radius:12px; font-size:12px; font-weight:600;">⚠ Partial</span>';

            html += `
                <tr class="border-b border-gray-200 hover:bg-gray-50">
                    <td class="px-6 py-4 text-sm text-gray-700">${date}</td>
                    <td class="px-6 py-4 text-sm font-medium text-gray-800">${entry.project}</td>
                    <td class="px-6 py-4 text-sm text-gray-700">${total}</td>
                    <td class="px-6 py-4 text-sm">${successBadge}</td>
                    <td class="px-6 py-4 text-sm">${failedBadge}</td>
                    <td class="px-6 py-4 text-sm">${statusBadge}</td>
                </tr>
            `;
        });
        table.innerHTML = html;
    }

    // ============ UI Utilities ============
    showAlert(message, type = 'info') {
        const container = document.getElementById('jira-alert-container');
        if (!container) return;

        const alertId = 'jira-alert-' + Date.now();
        const bgColor = type === 'success' ? '#e3fcef' : type === 'danger' ? '#ffebe6' : '#dbeafe';
        const textColor = type === 'success' ? '#36b37e' : type === 'danger' ? '#ff5630' : '#0c63e4';
        const borderColor = type === 'success' ? '#36b37e' : type === 'danger' ? '#ff5630' : '#0c63e4';

        const alertDiv = document.createElement('div');
        alertDiv.id = alertId;
        alertDiv.style.cssText = `
            background-color: ${bgColor};
            color: ${textColor};
            border: 1px solid ${borderColor};
            padding: 12px 16px;
            border-radius: 6px;
            margin-bottom: 16px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        `;
        alertDiv.innerHTML = `
            <span>${message}</span>
            <button onclick="document.getElementById('${alertId}').remove()" style="background:none; border:none; color:${textColor}; cursor:pointer; font-weight:bold; font-size:18px;">×</button>
        `;

        container.appendChild(alertDiv);

        // Auto-remove after 5 seconds
        setTimeout(() => {
            const elem = document.getElementById(alertId);
            if (elem) elem.remove();
        }, 5000);
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
    window.jiraHandler = new JiraHandler();
});
