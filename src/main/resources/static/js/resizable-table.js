/**
 * Resizable Table Utility
 * Adds column resizing functionality to HTML tables
 */

class ResizableTable {
    constructor(tableSelector) {
        this.table = document.querySelector(tableSelector);
        if (!this.table) return;
        
        this.headers = Array.from(this.table.querySelectorAll('th'));
        this.storageKey = 'tableColumnWidths';
        this.init();
    }

    init() {
        // Load saved column widths
        this.loadColumnWidths();
        
        // Add resize handles to headers
        this.headers.forEach((header, index) => {
            if (index < this.headers.length - 1) { // Don't add resizer to last column
                this.addResizeHandle(header, index);
            }
        });
    }

    addResizeHandle(header, index) {
        const resizer = document.createElement('div');
        resizer.className = 'column-resizer';
        resizer.style.position = 'absolute';
        resizer.style.right = '0';
        resizer.style.top = '0';
        resizer.style.bottom = '0';
        resizer.style.width = '4px';
        resizer.style.background = 'transparent';
        resizer.style.cursor = 'col-resize';
        resizer.style.userSelect = 'none';
        
        header.style.position = 'relative';
        header.appendChild(resizer);

        let isResizing = false;
        let startX = 0;
        let startWidth = 0;

        resizer.addEventListener('mousedown', (e) => {
            isResizing = true;
            startX = e.clientX;
            startWidth = header.offsetWidth;
            resizer.classList.add('active');

            const handleMouseMove = (e) => {
                if (!isResizing) return;
                const diff = e.clientX - startX;
                const newWidth = Math.max(50, startWidth + diff); // Min width 50px
                header.style.width = newWidth + 'px';

                // Also update all cells in this column
                const cells = this.table.querySelectorAll(`td:nth-child(${index + 1})`);
                cells.forEach(cell => {
                    cell.style.width = newWidth + 'px';
                });
            };

            const handleMouseUp = () => {
                isResizing = false;
                resizer.classList.remove('active');
                document.removeEventListener('mousemove', handleMouseMove);
                document.removeEventListener('mouseup', handleMouseUp);
                
                // Save column widths
                this.saveColumnWidths();
            };

            document.addEventListener('mousemove', handleMouseMove);
            document.addEventListener('mouseup', handleMouseUp);
        });
    }

    saveColumnWidths() {
        const widths = this.headers.map(h => h.offsetWidth);
        localStorage.setItem(this.storageKey, JSON.stringify(widths));
    }

    loadColumnWidths() {
        try {
            const saved = localStorage.getItem(this.storageKey);
            if (saved) {
                const widths = JSON.parse(saved);
                this.headers.forEach((header, index) => {
                    if (widths[index]) {
                        header.style.width = widths[index] + 'px';
                        
                        // Update cells in this column
                        const cells = this.table.querySelectorAll(`td:nth-child(${index + 1})`);
                        cells.forEach(cell => {
                            cell.style.width = widths[index] + 'px';
                        });
                    }
                });
            }
        } catch (e) {
            console.error('Error loading column widths:', e);
        }
    }
}

// Initialize resizable tables when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    const tables = document.querySelectorAll('.resizable-table');
    tables.forEach((table, index) => {
        new ResizableTable(`.resizable-table:nth-of-type(${index + 1})`);
    });
});
