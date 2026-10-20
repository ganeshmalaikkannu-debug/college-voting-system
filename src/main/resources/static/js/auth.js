/**
 * Authentication Module: Login, Registration, Session, and Role state.
 */
const Auth = {
    init() {
        this.bindEvents();
        this.checkAuthStatus();
    },

    bindEvents() {
        // Tab switching between Login and Register
        const tabLogin = document.getElementById('tab-login');
        const tabRegister = document.getElementById('tab-register');
        const formLogin = document.getElementById('form-login-container');
        const formRegister = document.getElementById('form-register-container');

        if (tabLogin && tabRegister) {
            tabLogin.addEventListener('click', () => {
                tabLogin.className = 'flex-1 py-2.5 text-sm font-semibold rounded-lg bg-white shadow-sm text-indigo-600 transition';
                tabRegister.className = 'flex-1 py-2.5 text-sm font-semibold rounded-lg text-slate-500 hover:text-slate-800 transition';
                formLogin.classList.remove('hidden');
                formRegister.classList.add('hidden');
            });

            tabRegister.addEventListener('click', () => {
                tabRegister.className = 'flex-1 py-2.5 text-sm font-semibold rounded-lg bg-white shadow-sm text-indigo-600 transition';
                tabLogin.className = 'flex-1 py-2.5 text-sm font-semibold rounded-lg text-slate-500 hover:text-slate-800 transition';
                formRegister.classList.remove('hidden');
                formLogin.classList.add('hidden');
            });
        }

        // Demo credentials quick-fill buttons
        document.getElementById('btn-quick-voter')?.addEventListener('click', () => {
            document.getElementById('login-roll').value = '24CS001';
            document.getElementById('login-password').value = 'Student@123';
            showToast('Loaded demo voter credentials (24CS001)', 'info');
        });

        document.getElementById('btn-quick-admin')?.addEventListener('click', () => {
            document.getElementById('login-roll').value = 'ADM001';
            document.getElementById('login-password').value = 'AdminSecure@123';
            showToast('Loaded demo admin credentials (ADM001)', 'info');
        });

        // Login Form Submission
        document.getElementById('login-form')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const rollNumber = document.getElementById('login-roll').value.trim();
            const password = document.getElementById('login-password').value;
            const submitBtn = document.getElementById('login-submit-btn');

            if (!rollNumber || !password) {
                showToast('Please provide both roll number and password', 'warning');
                return;
            }

            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Authenticating...';

            try {
                const response = await API.request('/api/auth/login', {
                    method: 'POST',
                    body: JSON.stringify({ rollNumber, password })
                });

                API.setToken(response.token);
                API.setCurrentUser({
                    id: response.id,
                    rollNumber: response.rollNumber,
                    name: response.name,
                    email: response.email,
                    role: response.role
                });

                showToast(`Welcome back, ${response.name}!`, 'success');
                this.updateAuthUI();

                if (response.role === 'ADMIN') {
                    window.switchView('admin-view');
                    if (window.Admin) window.Admin.loadAdminData();
                } else {
                    window.switchView('dashboard-view');
                    if (window.Elections) window.Elections.loadElections();
                }
            } catch (err) {
                showToast(err.message || 'Login failed. Check your credentials.', 'error');
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = 'Sign In to Portal <i class="fa-solid fa-arrow-right ml-1"></i>';
            }
        });

        // Register Form Submission
        document.getElementById('register-form')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const rollNumber = document.getElementById('reg-roll').value.trim();
            const name = document.getElementById('reg-name').value.trim();
            const email = document.getElementById('reg-email').value.trim();
            const password = document.getElementById('reg-password').value;
            const submitBtn = document.getElementById('reg-submit-btn');

            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Creating Account...';

            try {
                await API.request('/api/auth/register', {
                    method: 'POST',
                    body: JSON.stringify({ rollNumber, name, email, password })
                });

                showToast('Account registered successfully! Please sign in.', 'success');
                // Switch to login tab and fill roll number
                document.getElementById('tab-login')?.click();
                document.getElementById('login-roll').value = rollNumber;
                document.getElementById('login-password').value = '';
                document.getElementById('login-password').focus();
            } catch (err) {
                showToast(err.message || 'Registration failed.', 'error');
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = 'Complete Voter Registration <i class="fa-solid fa-id-card ml-1"></i>';
            }
        });

        // Logout
        document.getElementById('nav-logout-btn')?.addEventListener('click', () => {
            API.clearToken();
            showToast('You have been safely logged out.', 'info');
            this.updateAuthUI();
            window.switchView('auth-view');
        });
    },

    async checkAuthStatus() {
        const token = API.getToken();
        if (!token) {
            this.updateAuthUI();
            window.switchView('auth-view');
            return;
        }

        try {
            const userProfile = await API.request('/api/auth/me');
            API.setCurrentUser(userProfile);
            this.updateAuthUI();

            if (userProfile.role === 'ADMIN') {
                window.switchView('admin-view');
                if (window.Admin) window.Admin.loadAdminData();
            } else {
                window.switchView('dashboard-view');
                if (window.Elections) window.Elections.loadElections();
            }
        } catch (err) {
            console.warn('Session verification failed:', err);
            API.clearToken();
            this.updateAuthUI();
            window.switchView('auth-view');
        }
    },

    updateAuthUI() {
        const user = API.getCurrentUser();
        const authBadge = document.getElementById('nav-auth-badge');
        const unauthBadge = document.getElementById('nav-unauth-badge');
        const adminLink = document.getElementById('nav-admin-link');
        const voterLink = document.getElementById('nav-voter-link');
        const userNameEl = document.getElementById('nav-user-name');
        const userRoleEl = document.getElementById('nav-user-role');

        if (user) {
            if (authBadge) authBadge.classList.remove('hidden');
            if (unauthBadge) unauthBadge.classList.add('hidden');
            if (userNameEl) userNameEl.textContent = user.name;
            if (userRoleEl) {
                userRoleEl.textContent = user.role;
                userRoleEl.className = user.role === 'ADMIN'
                    ? 'text-xs font-semibold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800'
                    : 'text-xs font-semibold px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-800';
            }

            if (user.role === 'ADMIN') {
                if (adminLink) adminLink.classList.remove('hidden');
                if (voterLink) voterLink.classList.remove('hidden');
            } else {
                if (adminLink) adminLink.classList.add('hidden');
                if (voterLink) voterLink.classList.remove('hidden');
            }
        } else {
            if (authBadge) authBadge.classList.add('hidden');
            if (unauthBadge) unauthBadge.classList.remove('hidden');
            if (adminLink) adminLink.classList.add('hidden');
            if (voterLink) voterLink.classList.add('hidden');
        }
    }
};

window.Auth = Auth;
