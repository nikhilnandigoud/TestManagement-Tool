/**
 * Router Service
 * Manages application routing with hash-based navigation
 */
class RouterService {
    constructor() {
        this.routes = new Map();
        this.currentRoute = null;
        this.listeners = [];
    }

    /**
     * Register a route with its handler
     * @param {string} path - Route path (e.g., 'dashboard', 'file-extraction')
     * @param {Function} handler - Async handler function
     */
    register(path, handler) {
        this.routes.set(path, handler);
    }

    /**
     * Navigate to a route
     * @param {string} path - Route path
     * @param {Object} params - Route parameters
     */
    async navigate(path, params = {}) {
        if (this.routes.has(path)) {
            this.currentRoute = path;
            const handler = this.routes.get(path);
            await handler(params);
            this.notifyListeners(path, params);
        } else {
            console.warn(`Route not found: ${path}`);
            // Fallback to dashboard
            if (this.routes.has('dashboard')) {
                this.navigate('dashboard');
            }
        }
    }

    /**
     * Subscribe to route changes
     * @param {Function} callback - Called when route changes
     */
    subscribe(callback) {
        this.listeners.push(callback);
    }

    /**
     * Notify all listeners of route change
     */
    notifyListeners(path, params) {
        this.listeners.forEach(callback => callback(path, params));
    }

    /**
     * Get current route
     */
    getCurrentRoute() {
        return this.currentRoute;
    }

    /**
     * Go back to previous route (simple implementation)
     */
    back() {
        window.history.back();
    }

    /**
     * Programmatically set hash
     */
    push(path, params = {}) {
        const queryString = new URLSearchParams(params).toString();
        const hash = queryString ? `#${path}?${queryString}` : `#${path}`;
        window.location.hash = hash;
    }
}

// Global router instance
const routerService = new RouterService();
