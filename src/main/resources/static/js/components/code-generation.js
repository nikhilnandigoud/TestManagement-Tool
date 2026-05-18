/**
 * Code Generation Component
 * Generate automation code from scenarios
 */
class CodeGenerationComponent {
    constructor() {
        this.element = null;
    }

    async render(container) {
        this.element = container;
        const html = `
            <div class="page-header">
                <h1 class="page-title">💻 Code Generation</h1>
                <p class="page-description">Generate automation code for Selenium and Playwright</p>
            </div>

            <div class="card">
                <div class="card-title">Generate Automation Code</div>
                <form id="codeForm">
                    <div class="form-group">
                        <label class="form-label">Scenario Description *</label>
                        <textarea id="scenarioText" class="form-control" placeholder="Describe your test scenario..." required></textarea>
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label class="form-label">Framework</label>
                            <select id="framework" class="form-control">
                                <option value="SELENIUM">Selenium WebDriver</option>
                                <option value="PLAYWRIGHT">Playwright</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Language</label>
                            <select id="language" class="form-control">
                                <option value="JAVA">Java</option>
                                <option value="PYTHON">Python</option>
                                <option value="TYPESCRIPT">TypeScript</option>
                            </select>
                        </div>
                    </div>
                    <button type="submit" class="btn btn-primary">Generate Code</button>
                </form>
            </div>

            <div id="codeResult"></div>

            <div class="grid grid-2 mt-20">
                <div class="card">
                    <div class="card-title">Selenium Features</div>
                    <ul style="margin-left: 20px; color: var(--text-secondary); font-size: 14px;">
                        <li>Cross-browser support</li>
                        <li>Desktop automation</li>
                        <li>Robust element handling</li>
                    </ul>
                </div>
                <div class="card">
                    <div class="card-title">Playwright Features</div>
                    <ul style="margin-left: 20px; color: var(--text-secondary); font-size: 14px;">
                        <li>Multi-browser testing</li>
                        <li>Better performance</li>
                        <li>Modern API design</li>
                    </ul>
                </div>
            </div>
        `;
        this.element.innerHTML = html;

        document.getElementById('codeForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleCodeGeneration();
        });
    }

    async handleCodeGeneration() {
        const resultDiv = document.getElementById('codeResult');
        const payload = {
            scenarioText: document.getElementById('scenarioText').value,
            framework: document.getElementById('framework').value,
            language: document.getElementById('language').value,
            outputType: 'skeleton'
        };

        try {
            resultDiv.innerHTML = '<div class="alert alert-info">🔄 Generating code...</div>';
            const response = await fetch('/testmanagement/agents/code', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const data = await response.json();
            
            if (response.ok) {
                resultDiv.innerHTML = `
                    <div class="alert alert-success">✓ Code generated successfully</div>
                    <div class="card">
                        <div class="card-title">Generated Code (${payload.language})</div>
                        <pre style="background: var(--light-bg); padding: 15px; border-radius: 6px; overflow-x: auto; max-height: 500px; font-size: 12px;">${escapeHtml(data.code || 'No code generated')}</pre>
                        <div class="btn-group mt-20">
                            <button class="btn btn-outline btn-sm" onclick="navigator.clipboard.writeText(document.querySelector('pre').textContent)">Copy Code</button>
                        </div>
                    </div>
                `;
            } else {
                resultDiv.innerHTML = `<div class="alert alert-danger">✗ Error: ${data.message}</div>`;
            }
        } catch (error) {
            resultDiv.innerHTML = `<div class="alert alert-danger">✗ Error: ${error.message}</div>`;
        }
    }
}

const codeGeneration = new CodeGenerationComponent();
