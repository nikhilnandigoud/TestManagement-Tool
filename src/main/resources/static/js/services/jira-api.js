/**
 * Jira API Helper
 * Provides utility functions for making authenticated requests to Jira
 */

class JiraAPI {
    /**
     * Test Jira connection
     * @param {string} baseUrl - Jira base URL
     * @param {string} email - Jira email/username
     * @param {string} token - Jira API token
     * @returns {Promise<Object>} User details if successful
     */
    static async testConnection(baseUrl, email, token) {
        try {
            const response = await this.makeRequest('/rest/api/3/myself', 'GET', baseUrl, email, token);
            if (response.ok) {
                return await response.json();
            }
            throw new Error('Connection failed: ' + response.statusText);
        } catch (error) {
            throw new Error('Failed to connect to Jira: ' + error.message);
        }
    }

    /**
     * Fetch list of projects
     * @param {string} baseUrl - Jira base URL
     * @param {string} email - Jira email/username
     * @param {string} token - Jira API token
     * @returns {Promise<Array>} Array of projects
     */
    static async getProjects(baseUrl, email, token) {
        try {
            const response = await this.makeRequest(
                '/rest/api/3/project?type=software',
                'GET',
                baseUrl,
                email,
                token
            );
            if (response.ok) {
                const data = await response.json();
                return data.values || data || [];
            }
            throw new Error('Failed to fetch projects: ' + response.statusText);
        } catch (error) {
            throw new Error('Error fetching projects: ' + error.message);
        }
    }

    /**
     * Fetch issue types for a project
     * @param {string} baseUrl - Jira base URL
     * @param {string} projectKey - Jira project key
     * @param {string} email - Jira email/username
     * @param {string} token - Jira API token
     * @returns {Promise<Array>} Array of issue types
     */
    static async getIssueTypes(baseUrl, projectKey, email, token) {
        try {
            const response = await this.makeRequest(
                `/rest/api/3/project/${projectKey}/issuetypes`,
                'GET',
                baseUrl,
                email,
                token
            );
            if (response.ok) {
                return await response.json();
            }
            throw new Error('Failed to fetch issue types: ' + response.statusText);
        } catch (error) {
            throw new Error('Error fetching issue types: ' + error.message);
        }
    }

    /**
     * Create an issue in Jira
     * @param {string} baseUrl - Jira base URL
     * @param {string} email - Jira email/username
     * @param {string} token - Jira API token
     * @param {Object} issueData - Issue data object
     * @returns {Promise<Object>} Created issue details
     */
    static async createIssue(baseUrl, email, token, issueData) {
        try {
            const response = await this.makeRequest(
                '/rest/api/3/issue',
                'POST',
                baseUrl,
                email,
                token,
                issueData
            );
            
            if (response.ok) {
                return await response.json();
            }
            
            const errorData = await response.json();
            throw new Error('Failed to create issue: ' + (errorData.errorMessages?.join(', ') || response.statusText));
        } catch (error) {
            throw new Error('Error creating issue: ' + error.message);
        }
    }

    /**
     * Bulk create issues
     * @param {string} baseUrl - Jira base URL
     * @param {string} email - Jira email/username
     * @param {string} token - Jira API token
     * @param {Array} issues - Array of issue data objects
     * @returns {Promise<Array>} Array of created issues
     */
    static async createBulkIssues(baseUrl, email, token, issues) {
        const results = [];
        
        for (const issue of issues) {
            try {
                const result = await this.createIssue(baseUrl, email, token, issue);
                results.push({
                    status: 'SUCCESS',
                    issueKey: result.key,
                    issueId: result.id,
                    testCaseName: issue.fields.summary,
                    message: 'Issue created successfully'
                });
            } catch (error) {
                results.push({
                    status: 'FAILED',
                    testCaseName: issue.fields.summary,
                    message: error.message
                });
            }
        }
        
        return results;
    }

    /**
     * Update an issue
     * @param {string} baseUrl - Jira base URL
     * @param {string} issueKey - Jira issue key
     * @param {string} email - Jira email/username
     * @param {string} token - Jira API token
     * @param {Object} updateData - Data to update
     * @returns {Promise<void>}
     */
    static async updateIssue(baseUrl, issueKey, email, token, updateData) {
        try {
            const response = await this.makeRequest(
                `/rest/api/3/issue/${issueKey}`,
                'PUT',
                baseUrl,
                email,
                token,
                updateData
            );
            
            if (!response.ok) {
                throw new Error('Failed to update issue: ' + response.statusText);
            }
        } catch (error) {
            throw new Error('Error updating issue: ' + error.message);
        }
    }

    /**
     * Get issue details
     * @param {string} baseUrl - Jira base URL
     * @param {string} issueKey - Jira issue key
     * @param {string} email - Jira email/username
     * @param {string} token - Jira API token
     * @returns {Promise<Object>} Issue details
     */
    static async getIssue(baseUrl, issueKey, email, token) {
        try {
            const response = await this.makeRequest(
                `/rest/api/3/issue/${issueKey}`,
                'GET',
                baseUrl,
                email,
                token
            );
            
            if (response.ok) {
                return await response.json();
            }
            throw new Error('Failed to fetch issue: ' + response.statusText);
        } catch (error) {
            throw new Error('Error fetching issue: ' + error.message);
        }
    }

    /**
     * Make authenticated HTTP request to Jira API
     * @private
     */
    static async makeRequest(endpoint, method = 'GET', baseUrl, email, token, body = null) {
        const auth = btoa(`${email}:${token}`);
        const url = `${baseUrl}${endpoint}`;

        const options = {
            method,
            headers: {
                'Authorization': `Basic ${auth}`,
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        };

        if (body) {
            options.body = JSON.stringify(body);
        }

        return fetch(url, options);
    }

    /**
     * Format test case data to Jira issue format
     * @param {Object} testCase - Test case object
     * @param {string} projectKey - Jira project key
     * @param {string} issueType - Type of issue
     * @returns {Object} Formatted issue data
     */
    static formatTestCaseAsIssue(testCase, projectKey, issueType = 'Test') {
        return {
            fields: {
                project: {
                    key: projectKey
                },
                summary: testCase.name || 'Test Case',
                description: {
                    type: 'doc',
                    version: 1,
                    content: [
                        {
                            type: 'paragraph',
                            content: [
                                {
                                    type: 'text',
                                    text: testCase.description || testCase.steps || 'No description'
                                }
                            ]
                        }
                    ]
                },
                issuetype: {
                    name: issueType
                },
                labels: testCase.tags || [],
                priority: this.mapPriority(testCase.priority)
            }
        };
    }

    /**
     * Map test case priority to Jira priority
     * @private
     */
    static mapPriority(priority) {
        const priorityMap = {
            'high': 'High',
            'medium': 'Medium',
            'low': 'Low',
            '1': 'High',
            '2': 'Medium',
            '3': 'Low'
        };
        
        return {
            name: priorityMap[priority?.toLowerCase()] || 'Medium'
        };
    }
}

// Export for use in modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = JiraAPI;
}
