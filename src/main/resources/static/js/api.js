/**
 * Core API Client and Global Utilities.
 */
const API = {
    BASE_URL: '',

    getToken() {
        return localStorage.getItem('voting_auth_token');
    },

    setToken(token) {
        localStorage.setItem('voting_auth_token', token);
    },

    clearToken() {
        localStorage.removeItem('voting_auth_token');
        localStorage.removeItem('voting_user_data');
    },

    getCurrentUser() {
        const data = localStorage.getItem('voting_user_data');
        return data ? JSON.parse(data) : null;
    },

    setCurrentUser(user) {
        localStorage.setItem('voting_user_data', JSON.stringify(user));
    },

    /**
     * Executes authenticated or public HTTP requests.
     */
    async request(endpoint, options = {}) {
        const url = `${this.BASE_URL}${endpoint}`;
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        };

        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers
        };

        try {
            const response = await fetch(url, config);

            // Handle 204 No Content
            if (response.status === 204) {
                return null;
            }

            const data = await response.json().catch(() => null);

            if (!response.ok) {
                // If 401 Unauthorized, automatically clear stale credentials and show login
                if (response.status === 401) {
                    this.clearToken();
                    if (window.Auth && typeof window.Auth.updateAuthUI === 'function') {
                        window.Auth.updateAuthUI();
                        window.switchView('auth-view');
                    }
                    throw new Error(data?.message || 'Session expired. Please log in again.');
                }

                // If validation error with field list
                if (data?.errors && Array.isArray(data.errors)) {
                    const fieldMsgs = data.errors.map(e => `${e.field}: ${e.message}`).join(', ');
                    throw new Error(fieldMsgs || data.message || 'Validation error');
                }

                throw new Error(data?.message || `Request failed with status ${response.status}`);
            }

            return data;
        } catch (error) {
            console.error(`API Error [${endpoint}]:`, error);
            throw error;
        }
    }
};

/**
 * Toast Notification Helper.
 */
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast px-4 py-3 rounded-xl border flex items-center gap-3 text-sm font-medium transition-all duration-300`;

    let icon = 'fa-info-circle';
    let colors = 'bg-slate-900 text-white border-slate-700';

    if (type === 'success') {
        icon = 'fa-check-circle';
        colors = 'bg-emerald-600 text-white border-emerald-500';
    } else if (type === 'error') {
        icon = 'fa-exclamation-triangle';
        colors = 'bg-rose-600 text-white border-rose-500';
    } else if (type === 'warning') {
        icon = 'fa-exclamation-circle';
        colors = 'bg-amber-500 text-white border-amber-400';
    }

    toast.className += ` ${colors}`;

    const iconEl = document.createElement('i');
    iconEl.className = `fa-solid ${icon} text-base`;

    const textEl = document.createElement('span');
    textEl.textContent = message;

    toast.appendChild(iconEl);
    toast.appendChild(textEl);
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(1rem)';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

/**
 * Helper to format ISO dates to readable human format.
 */
function formatDate(isoString) {
    if (!isoString) return 'N/A';
    const date = new Date(isoString);
    return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

/**
 * Helper to safely escape HTML.
 */
function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
