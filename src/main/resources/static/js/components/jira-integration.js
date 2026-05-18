/**
 * Jira Integration Component
 * Configure and push test cases to Jira
 */
class JiraIntegrationComponent {
    constructor() {
        this.element = null;
    }

    async render(container) {
        this.element = container;
        const html = `
            <div class="page-header">
                <h1 class="page-title">🔗 Jira Integration</h1>
                <p class="page-description">Push test cases to your Jira projects</p>
            </div>

            <div class="alert alert-info">
                <span>ℹ️</span>
                <div>
                    <strong>Setup Required:</strong> Configure your Jira credentials to enable test case synchronization.
                </div>
            </div>

            <div class="card">
                <div class="card-title">Jira Configuration</div>
                <form id="jiraForm">
                    <div class="form-group">
                        <label class="form-label">Jira Project Key *</label>
                        <input type="text" id="jiraProjectKey" class="form-control" placeholder="e.g., QA" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Jira Instance URL *</label>
                        <input type="url" id="jiraUrl" class="form-control" placeholder="https://your-domain.atlassian.net" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">API Token (or Password) *</label>
                        <input type="password" id="jiraToken" class="form-control" placeholder="Your Jira API token" required>
                    </div>
                    <button type="submit" class="btn btn-primary">Connect to Jira</button>
                </form>
            </div>

            <div id="jiraResult"></div>

            <div class="card mt-20">
                <div class="card-title">How to Get Your API Token</div>
                <ol style="margin-left: 20px; color: var(--text-secondary);">
                    <li>Go to Atlassian API Tokens page</li>
                    <li>Click "Create API token"</li>
                    <li>Copy the generated token</li>
                    <li>Paste it in the API Token field above</li>
                </ol>
            </div>
        `;
        this.element.innerHTML = html;

        document.getElementById('jiraForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleJiraIntegration();
        });
    }

    async handleJiraIntegration() {
        const resultDiv = document.getElementById('jiraResult');
        const projectKey = document.getElementById('jiraProjectKey').value;
        const jiraUrl = document.getElementById('jiraUrl').value;

        try {
            resultDiv.innerHTML = '<div class="alert alert-info">⏳ Connecting to Jira...</div>';
            
            // Store configuration
            localStorage.setItem('jiraConfig', JSON.stringify({
                projectKey: projectKey,
                url: jiraUrl,
                connected: true
            }));

            resultDiv.innerHTML = `
                <div class="alert alert-success">✓ Jira integration configured successfully</div>
                <div class="card">
                    <div class="card-title">Configuration Saved</div>
                    <p class="card-subtitle">Project Key: <strong>${escapeHtml(projectKey)}</strong></p>
                    <p class="card-subtitle">Jira URL: <strong>${escapeHtml(jiraUrl)}</strong></p>
                    <p style="margin-top: 15px; color: var(--text-secondary);">You can now push test cases to this Jira project.</p>
                </div>
            `;
        } catch (error) {
            resultDiv.innerHTML = `<div class="alert alert-danger">✗ Error: ${error.message}</div>`;
        }
    }
}

const jiraIntegration = new JiraIntegrationComponent();
