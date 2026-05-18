/**
 * Jira-Style QA Platform App
 * Main application router and initialization
 */

class JiraQAApp {
    constructor() {
        this.currentRoute = 'test-cases';
        this.components = {
            'test-cases': null,
            'jira-integration': null,
            'reports': null
        };
        this.init();
    }

    async init() {
        console.log('Initializing Jira-Style QA Platform');
        this.setupRouting();
        this.loadTestCasesComponent();
    }

    setupRouting() {
        const sidebarLinks = document.querySelectorAll('.sidebar-link');
        
        sidebarLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const route = link.getAttribute('data-route');
                this.navigateTo(route);
            });
        });
    }

    navigateTo(route) {
        console.log('Navigating to:', route);
        
        // Update active link
        document.querySelectorAll('.sidebar-link').forEach(link => {
            link.classList.remove('active', 'text-gray-700');
            link.classList.add('text-gray-500');
        });
        
        const activeLink = document.querySelector(`[data-route="${route}"]`);
        if (activeLink) {
            activeLink.classList.add('active', 'text-gray-700');
            activeLink.classList.remove('text-gray-500');
        }

        // Load content
        this.currentRoute = route;
        this.loadContent(route);
    }

    loadContent(route) {
        const contentArea = document.querySelector('app-content');
        
        switch(route) {
            case 'test-cases':
                this.loadTestCasesComponent();
                break;
            case 'jira-integration':
                this.loadJiraIntegration();
                break;
            case 'reports':
                this.loadReports();
                break;
            default:
                this.loadTestCasesComponent();
        }
    }

    loadTestCasesComponent() {
        const contentArea = document.querySelector('app-content');
        contentArea.innerHTML = `<div id="test-cases-container"></div>`;

        // Render the unified component (it handles all tabs internally)
        const container = contentArea.querySelector('#test-cases-container');
        const component = new UnifiedTestCasesComponent();
        component.render(container);
    }

    loadJiraIntegration() {
        const contentArea = document.querySelector('app-content');
        contentArea.innerHTML = `
            <div>
                <div class="mb-6">
                    <h1 class="text-3xl font-bold text-gray-800">Jira Integration</h1>
                    <p class="text-gray-500 mt-1">Connect your QA Platform to Jira to sync test cases and track automation progress.</p>
                </div>

                <!-- Connection Status -->
                <div class="bg-white p-6 rounded-lg shadow-md border border-gray-200 mb-8">
                    <h2 class="text-xl font-semibold text-gray-800 mb-4">Connection Status</h2>
                    <div class="flex items-center space-x-3 p-3 bg-red-50 border border-red-200 rounded-md">
                        <svg class="w-6 h-6 text-red-500" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"></path></svg>
                        <p class="text-sm text-red-700">Not Connected. Please configure your Jira credentials.</p>
                    </div>
                </div>

                <!-- Configuration Form -->
                <div class="bg-white p-6 rounded-lg shadow-md border border-gray-200">
                    <h2 class="text-xl font-semibold text-gray-800 mb-6">Jira Server Configuration</h2>
                    
                    <form class="space-y-6" id="jira-form">
                        <div>
                            <label class="block text-sm font-medium text-gray-700 mb-2">Jira Server URL</label>
                            <input type="url" placeholder="https://yourcompany.atlassian.net" class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500">
                        </div>

                        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div>
                                <label class="block text-sm font-medium text-gray-700 mb-2">Email (Jira Username)</label>
                                <input type="email" placeholder="your.name@company.com" class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500">
                            </div>
                            <div>
                                <label class="block text-sm font-medium text-gray-700 mb-2">API Token</label>
                                <input type="password" placeholder="Enter your generated API Token" class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500">
                            </div>
                        </div>

                        <div class="pt-6 border-t border-gray-100 flex justify-end">
                            <button type="submit" class="px-6 py-3 bg-indigo-600 text-white font-medium rounded-md shadow-sm hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition duration-150">
                                Connect to Jira
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        `;
    }

    loadReports() {
        const contentArea = document.querySelector('app-content');
        contentArea.innerHTML = `
            <div>
                <div class="mb-6">
                    <h1 class="text-3xl font-bold text-gray-800">Execution Summary</h1>
                    <p class="text-gray-500 mt-1">View the history and performance metrics for all test runs.</p>
                </div>
                <div class="bg-white p-6 rounded-lg shadow-md border border-gray-200">
                    <p class="text-gray-500">Reports and analytics coming soon...</p>
                </div>
            </div>
        `;
    }
}

// Initialize app when DOM is ready
// NOTE: Disabled - new HTML handles routing with inline JavaScript
// document.addEventListener('DOMContentLoaded', () => {
//     window.qaApp = new JiraQAApp();
// });
