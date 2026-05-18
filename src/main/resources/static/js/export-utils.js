/**
 * Export Utilities for QA Platform
 * Provides functionality to export test cases in various formats
 * 
 * ✔ MANDATORY TABLE RULES:
 * - Each test step must be on a separate row
 * - The expected result for each step must be on the same row as its step
 * - If there are preconditions, list them as the first test steps with "Pre-condition" prefix
 * - Login steps should come after pre-condition
 * - Expected results must use "should" consistently
 * - Columns remain in the same order
 */

class ExportUtils {
    /**
     * Creates a properly formatted Excel file with new flattened structure
     */
    static createExcelBlob(testCases, appName) {
        const worksheet = this.createWorksheet(testCases, appName);
        const workbook = this.createWorkbook(worksheet);
        return workbook;
    }

    /**
     * Flatten a single test case into multiple rows (one per step)
     * @param {Object} testCase - The test case to flatten
     * @param {number} tcIndex - The index of the test case (for ID generation)
     * @returns {Array} Array of row objects
     */
    static flattenTestCaseToRows(testCase, tcIndex) {
        const rows = [];
        const tcId = `TC${String(tcIndex + 1).padStart(3, '0')}`;
        const title = testCase.title || 'Untitled Test Case';
        
        let stepIndex = 0;

        // Step 1: Add preconditions as first step (if exists)
        const existingSteps = Array.isArray(testCase.steps) ? testCase.steps : [];
        const firstStepText = existingSteps.length > 0
            ? (typeof existingSteps[0] === 'string' ? existingSteps[0] : (existingSteps[0].action || existingSteps[0].text || ''))
            : '';
        const firstStepIsPrecondition = firstStepText.toLowerCase().includes('pre-condition');

        if (testCase.preconditions && !firstStepIsPrecondition) {
            const preconditionText = typeof testCase.preconditions === 'string' 
                ? testCase.preconditions 
                : testCase.preconditions.join('; ');
            
            // Add "Pre-condition: " prefix if not already present
            const preconditionStep = preconditionText.includes('Pre-condition:') 
                ? preconditionText 
                : `Pre-condition: ${preconditionText}`;
            
            rows.push({
                tcId: tcId,
                title: title,
                step: preconditionStep,
                expectedResult: testCase.preconditionExpected || 'Pre-condition should be met',
                labels: '',
                automationState: 'Not Automated',
                testCaseStatus: '',
                createdInSprint: ''
            });
            stepIndex++;
        }

        // Step 2: Add regular steps
        if (existingSteps.length > 0) {
            existingSteps.forEach((step, idx) => {
                const stepText = typeof step === 'string' 
                    ? step 
                    : (step.action || step.text || step);
                
                let expectedResult = '';
                
                if (typeof step === 'object') {
                    expectedResult = step.expectedResult || '';
                } else if (testCase.expectedResults && testCase.expectedResults[idx]) {
                    expectedResult = testCase.expectedResults[idx];
                } else {
                    expectedResult = 'Should execute successfully';
                }

                if (!expectedResult && String(stepText).toLowerCase().includes('pre-condition')) {
                    expectedResult = 'System should allow the test to start only when the pre-condition is met';
                }

                // Ensure expected result contains "should"
                if (expectedResult && !expectedResult.toLowerCase().includes('should')) {
                    expectedResult = `${expectedResult} should be verified`;
                }

                rows.push({
                    tcId: tcId,
                    title: stepIndex === 0 ? title : '', // Only first step gets the title
                    step: stepText,
                    expectedResult: expectedResult,
                    labels: '',
                    automationState: 'Not Automated',
                    testCaseStatus: '',
                    createdInSprint: ''
                });
                stepIndex++;
            });
        } else {
            // If no steps, add at least one row for the test case
            rows.push({
                tcId: tcId,
                title: title,
                step: 'Execute test case',
                expectedResult: 'Test should execute successfully',
                labels: '',
                automationState: 'Not Automated',
                testCaseStatus: '',
                createdInSprint: ''
            });
        }

        return rows;
    }

    /**
     * Creates Excel worksheet HTML with flattened structure
     */
    static createWorksheet(testCases, appName) {
        let html = `
        <!DOCTYPE html>
        <html xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet" xmlns:o="urn:schemas-microsoft-com:office:office">
        <head>
            <meta charset="UTF-8">
            <style>
                table { border-collapse: collapse; width: 100%; }
                th { background-color: #4472C4; color: white; padding: 12px; text-align: left; font-weight: bold; border: 1px solid #333; font-size: 12px; }
                td { padding: 10px; border: 1px solid #ddd; font-size: 11px; }
                tr:nth-child(even) { background-color: #f9f9f9; }
                .title-row { font-weight: bold; background-color: #E7F0FF; }
                .automation-state { text-align: center; font-weight: 600; background-color: #FFE7E7; }
            </style>
        </head>
        <body>
        <table>
            <thead>
                <tr>
                    <th style="width: 150px;">Test Case Title</th>
                    <th style="width: 250px;">Test Steps</th>
                    <th style="width: 200px;">Expected Result</th>
                    <th style="width: 100px;">Labels</th>
                    <th style="width: 120px;">Automation State</th>
                    <th style="width: 100px;">Test Case Status</th>
                    <th style="width: 100px;">Created in Sprint</th>
                </tr>
            </thead>
            <tbody>
        `;

        // Flatten all test cases into rows
        const allRows = [];
        testCases.forEach((tc, idx) => {
            const rows = this.flattenTestCaseToRows(tc, idx);
            allRows.push(...rows);
        });

        // Generate HTML for all rows
        allRows.forEach((row, idx) => {
            const isTitleRow = row.title !== '';
            const titleCellClass = isTitleRow ? 'title-row' : '';
            
            html += `
                <tr>
                    <td class="${titleCellClass}">${this.escapeHtml(row.title)}</td>
                    <td>${this.escapeHtml(row.step)}</td>
                    <td>${this.escapeHtml(row.expectedResult)}</td>
                    <td>${this.escapeHtml(row.labels)}</td>
                    <td class="automation-state">${this.escapeHtml(row.automationState)}</td>
                    <td>${this.escapeHtml(row.testCaseStatus)}</td>
                    <td>${this.escapeHtml(row.createdInSprint)}</td>
                </tr>
            `;
        });

        html += `
            </tbody>
        </table>
        </body>
        </html>
        `;

        return html;
    }

    /**
     * Creates workbook (for future enhancement to create true Excel format)
     */
    static createWorkbook(worksheet) {
        return new Blob([worksheet], { type: 'application/vnd.ms-excel;charset=utf-8;' });
    }

    /**
     * Escape HTML special characters
     */
    static escapeHtml(text) {
        if (!text) return '';
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
     * Generate CSV content with flattened structure
     */
    static generateCSV(testCases) {
        let csvContent = '"Test Case Title","Test Steps","Expected Result","Labels","Automation State","Test Case Status","Created in Sprint"\n';
        
        // Flatten all test cases into rows
        const allRows = [];
        testCases.forEach((tc, idx) => {
            const rows = this.flattenTestCaseToRows(tc, idx);
            allRows.push(...rows);
        });

        // Generate CSV content
        allRows.forEach(row => {
            const cells = [
                this.escapeCSV(row.title),
                this.escapeCSV(row.step),
                this.escapeCSV(row.expectedResult),
                this.escapeCSV(row.labels),
                this.escapeCSV(row.automationState),
                this.escapeCSV(row.testCaseStatus),
                this.escapeCSV(row.createdInSprint)
            ];
            
            csvContent += cells.map(cell => `"${cell}"`).join(',') + '\n';
        });
        
        return csvContent;
    }

    /**
     * Escape CSV values (handle quotes and newlines)
     */
    static escapeCSV(text) {
        if (!text) return '';
        // Replace quotes with double quotes and handle newlines
        return String(text)
            .replace(/"/g, '""')
            .replace(/\n/g, ' ')
            .replace(/\r/g, '');
    }

    /**
     * Download file
     */
    static downloadFile(content, filename, mimeType = 'text/plain') {
        const blob = typeof content === 'string' 
            ? new Blob([content], { type: mimeType + ';charset=utf-8;' })
            : content;
        
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', filename);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }

    /**
     * Generate timestamp for filename
     */
    static getTimestamp() {
        return new Date().toISOString().split('T')[0];
    }

    /**
     * Export test cases to Excel with new format
     */
    static exportToExcel(testCases, appName = 'App', module = '') {
        const timestamp = this.getTimestamp();
        const filename = `TestCases_${appName}${module ? '_' + module : ''}_${timestamp}.xls`;
        const blob = this.createExcelBlob(testCases, appName);
        this.downloadFile(blob, filename, 'application/vnd.ms-excel');
    }

    /**
     * Export test cases to CSV with new format
     */
    static exportToCSV(testCases, appName = 'App', module = '') {
        const timestamp = this.getTimestamp();
        const filename = `TestCases_${appName}${module ? '_' + module : ''}_${timestamp}.csv`;
        const csvContent = this.generateCSV(testCases);
        this.downloadFile(csvContent, filename, 'text/csv');
    }
}

/**
 * Global function for export button - Uses the global allTestCases from HTML
 */
function exportTableToExcel() {
    // Get the global allTestCases variable from the HTML page
    if (typeof allTestCases === 'undefined' || !Array.isArray(allTestCases)) {
        alert('No test cases to export');
        return;
    }

    if (allTestCases.length === 0) {
        alert('No test cases to export');
        return;
    }

    try {
        // Call ExportUtils static method
        ExportUtils.exportToExcel(allTestCases, 'QA-App', 'TestCases');
    } catch (error) {
        console.error('Export error:', error);
        alert('Failed to export test cases: ' + error.message);
    }
}
