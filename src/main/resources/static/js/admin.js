/**
 * Admin Portal Module: Election and Candidate CRUD, Status Transitions,
 * Results Publishing, and Voter Participation Turnout Analytics.
 */
const Admin = {
    selectedElectionId: null,
    editingElectionId: null,
    currentCandidates: [],

    init() {
        this.bindEvents();
    },

    bindEvents() {
        // Open Create Election Modal
        document.getElementById('btn-open-create-election')?.addEventListener('click', () => {
            this.editingElectionId = null;
            document.getElementById('modal-election-title').textContent = 'Create New Election';
            document.getElementById('form-election').reset();

            // Set default dates: start now, end in 3 days
            const now = new Date();
            const inThreeDays = new Date(now.getTime() + 3 * 24 * 60 * 60 * 1000);
            document.getElementById('election-start-time').value = this.toLocalISO(now);
            document.getElementById('election-end-time').value = this.toLocalISO(inThreeDays);

            document.getElementById('modal-election')?.classList.remove('hidden');
        });

        // Submit Election Form (Create or Edit)
        document.getElementById('form-election')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const title = document.getElementById('election-title-input').value.trim();
            const description = document.getElementById('election-desc-input').value.trim();
            const startTime = document.getElementById('election-start-time').value;
            const endTime = document.getElementById('election-end-time').value;

            if (!title || !startTime || !endTime) {
                showToast('Please fill all required fields.', 'warning');
                return;
            }

            try {
                if (this.editingElectionId) {
                    await API.request(`/api/elections/${this.editingElectionId}`, {
                        method: 'PUT',
                        body: JSON.stringify({ title, description, startTime, endTime })
                    });
                    showToast('Election updated successfully.', 'success');
                } else {
                    await API.request('/api/elections', {
                        method: 'POST',
                        body: JSON.stringify({ title, description, startTime, endTime })
                    });
                    showToast('New election created in DRAFT status.', 'success');
                }

                document.getElementById('modal-election')?.classList.add('hidden');
                this.loadAdminData();
            } catch (err) {
                showToast(err.message, 'error');
            }
        });

        // Submit Candidate Form
        document.getElementById('form-candidate')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const name = document.getElementById('cand-name-input').value.trim();
            const department = document.getElementById('cand-dept-input').value.trim();
            const position = document.getElementById('cand-pos-input').value.trim();
            const manifesto = document.getElementById('cand-manifesto-input').value.trim();

            if (!name || !manifesto) {
                showToast('Name and Manifesto are required.', 'warning');
                return;
            }

            try {
                await API.request(`/api/elections/${this.selectedElectionId}/candidates`, {
                    method: 'POST',
                    body: JSON.stringify({ name, department, position, manifesto })
                });

                showToast('Candidate nominated successfully.', 'success');
                document.getElementById('form-candidate').reset();
                this.loadElectionCandidates(this.selectedElectionId);
            } catch (err) {
                showToast(err.message, 'error');
            }
        });
    },

    toLocalISO(date) {
        const offset = date.getTimezoneOffset() * 60000;
        return new Date(date.getTime() - offset).toISOString().slice(0, 16);
    },

    async loadAdminData() {
        const tableBody = document.getElementById('admin-elections-tbody');
        if (!tableBody) return;

        tableBody.innerHTML = `
            <tr>
                <td colspan="6" class="py-12 text-center text-slate-500">
                    <i class="fa-solid fa-circle-notch fa-spin text-2xl text-indigo-600 mb-2"></i>
                    <p class="text-sm">Loading admin election database...</p>
                </td>
            </tr>
        `;

        try {
            const elections = await API.request('/api/elections');
            this.renderAdminTable(elections);
        } catch (err) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="py-8 text-center text-rose-500">
                        Failed to load: ${escapeHtml(err.message)}
                    </td>
                </tr>
            `;
        }
    },

    renderAdminTable(elections) {
        const tableBody = document.getElementById('admin-elections-tbody');
        if (!tableBody) return;

        if (elections.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="py-12 text-center text-slate-500 font-medium">
                        No elections found. Click "Create Election" to get started.
                    </td>
                </tr>
            `;
            return;
        }

        tableBody.innerHTML = '';
        elections.forEach(election => {
            const tr = document.createElement('tr');
            tr.className = 'border-b border-slate-100 hover:bg-slate-50/50 transition';

            // Status Badge
            let statusBadge = `<span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-100 text-slate-700">${election.status}</span>`;
            if (election.status === 'ACTIVE') {
                statusBadge = `<span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 badge-active-pulse">ACTIVE</span>`;
            } else if (election.status === 'UPCOMING') {
                statusBadge = `<span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-sky-100 text-sky-800">UPCOMING</span>`;
            } else if (election.status === 'CLOSED') {
                statusBadge = `<span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-200 text-slate-800">CLOSED</span>`;
            }

            // Results visibility toggle
            const visibilityBtn = `
                <button onclick="Admin.toggleResultsVisibility(${election.id}, ${!election.resultsPublished})" class="text-xs font-semibold px-2.5 py-1 rounded-lg border transition ${
                    election.resultsPublished
                        ? 'bg-emerald-50 text-emerald-700 border-emerald-300 hover:bg-emerald-100'
                        : 'bg-slate-100 text-slate-600 border-slate-200 hover:bg-slate-200'
                }">
                    <i class="fa-solid ${election.resultsPublished ? 'fa-eye' : 'fa-eye-slash'} mr-1"></i>
                    ${election.resultsPublished ? 'Published' : 'Hidden'}
                </button>
            `;

            // Transition status dropdown/options
            let nextTransition = '';
            if (election.status === 'DRAFT') {
                nextTransition = `<button onclick="Admin.changeStatus(${election.id}, 'UPCOMING')" class="text-xs font-semibold px-2 py-1 bg-sky-50 text-sky-700 hover:bg-sky-100 rounded border border-sky-200">Move to UPCOMING</button>`;
            } else if (election.status === 'UPCOMING') {
                nextTransition = `<button onclick="Admin.changeStatus(${election.id}, 'ACTIVE')" class="text-xs font-semibold px-2 py-1 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 rounded border border-emerald-200">Activate Voting</button>`;
            } else if (election.status === 'ACTIVE') {
                nextTransition = `<button onclick="Admin.changeStatus(${election.id}, 'CLOSED')" class="text-xs font-semibold px-2 py-1 bg-rose-50 text-rose-700 hover:bg-rose-100 rounded border border-rose-200">Close Election</button>`;
            } else {
                nextTransition = `<span class="text-xs text-slate-400">Finalized</span>`;
            }

            tr.innerHTML = `
                <td class="py-4 px-4">
                    <div class="font-bold text-slate-900">${escapeHtml(election.title)}</div>
                    <div class="text-xs text-slate-400">ID: ${election.id} · ${formatDate(election.startTime)} to ${formatDate(election.endTime)}</div>
                </td>
                <td class="py-4 px-3">${statusBadge}</td>
                <td class="py-4 px-3">
                    <button onclick="Admin.manageCandidates(${election.id})" class="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-indigo-50 text-indigo-700 hover:bg-indigo-100 text-xs font-semibold transition">
                        <i class="fa-solid fa-users"></i> ${election.candidateCount} Candidates
                    </button>
                </td>
                <td class="py-4 px-3">${visibilityBtn}</td>
                <td class="py-4 px-3">${nextTransition}</td>
                <td class="py-4 px-4 text-right space-x-2">
                    <button onclick="Admin.viewParticipation(${election.id})" title="Participation Turnout" class="p-2 rounded-lg text-slate-500 hover:bg-slate-100 hover:text-indigo-600 transition">
                        <i class="fa-solid fa-chart-pie"></i>
                    </button>
                    ${election.status === 'DRAFT' ? `
                        <button onclick="Admin.deleteElection(${election.id})" title="Delete DRAFT" class="p-2 rounded-lg text-slate-500 hover:bg-rose-50 hover:text-rose-600 transition">
                            <i class="fa-solid fa-trash"></i>
                        </button>
                    ` : ''}
                </td>
            `;
            tableBody.appendChild(tr);
        });
    },

    async changeStatus(id, targetStatus) {
        if (!confirm(`Are you sure you want to transition this election to ${targetStatus}?`)) return;

        try {
            await API.request(`/api/elections/${id}/status`, {
                method: 'PATCH',
                body: JSON.stringify({ status: targetStatus })
            });
            showToast(`Election status moved to ${targetStatus}.`, 'success');
            this.loadAdminData();
        } catch (err) {
            showToast(err.message, 'error');
        }
    },

    async toggleResultsVisibility(id, publish) {
        try {
            await API.request(`/api/elections/${id}/results-visibility`, {
                method: 'PATCH',
                body: JSON.stringify({ published: publish })
            });
            showToast(publish ? 'Results are now publicly visible to voters.' : 'Results are now hidden.', 'info');
            this.loadAdminData();
        } catch (err) {
            showToast(err.message, 'error');
        }
    },

    async deleteElection(id) {
        if (!confirm('Are you sure you want to delete this DRAFT election?')) return;

        try {
            await API.request(`/api/elections/${id}`, { method: 'DELETE' });
            showToast('Draft election deleted.', 'success');
            this.loadAdminData();
        } catch (err) {
            showToast(err.message, 'error');
        }
    },

    async viewParticipation(id) {
        const modal = document.getElementById('modal-participation');
        const titleEl = document.getElementById('part-election-title');
        const turnoutEl = document.getElementById('part-turnout-pct');
        const castEl = document.getElementById('part-votes-cast');
        const totalEl = document.getElementById('part-total-voters');

        modal?.classList.remove('hidden');

        try {
            const data = await API.request(`/api/elections/${id}/participation`);
            if (titleEl) titleEl.textContent = data.electionTitle;
            if (turnoutEl) turnoutEl.textContent = `${data.turnoutPercentage}%`;
            if (castEl) castEl.textContent = data.votesCast;
            if (totalEl) totalEl.textContent = data.totalRegisteredVoters;
        } catch (err) {
            showToast(err.message, 'error');
            modal?.classList.add('hidden');
        }
    },

    async manageCandidates(electionId) {
        this.selectedElectionId = electionId;
        const modal = document.getElementById('modal-manage-candidates');
        modal?.classList.remove('hidden');
        this.loadElectionCandidates(electionId);
    },

    async loadElectionCandidates(electionId) {
        const container = document.getElementById('admin-candidates-list');
        if (!container) return;

        container.innerHTML = '<div class="py-6 text-center text-slate-500"><i class="fa-solid fa-circle-notch fa-spin"></i> Loading...</div>';

        try {
            const candidates = await API.request(`/api/elections/${electionId}/candidates`);
            this.currentCandidates = candidates;

            if (candidates.length === 0) {
                container.innerHTML = '<p class="py-4 text-center text-slate-500 text-sm">No candidates nominated yet.</p>';
                return;
            }

            container.innerHTML = '';
            candidates.forEach(c => {
                const item = document.createElement('div');
                item.className = 'p-3 bg-slate-50 rounded-xl border border-slate-200 flex items-center justify-between';
                item.innerHTML = `
                    <div>
                        <div class="font-bold text-slate-900 text-sm">${escapeHtml(c.name)}</div>
                        <div class="text-xs text-slate-500">${escapeHtml(c.position || '')} · ${escapeHtml(c.department || '')}</div>
                    </div>
                    <button onclick="Admin.deleteCandidate(${c.id})" class="text-xs text-rose-500 hover:text-rose-700 p-1">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                `;
                container.appendChild(item);
            });
        } catch (err) {
            container.innerHTML = `<div class="text-rose-500 text-sm">Error: ${escapeHtml(err.message)}</div>`;
        }
    },

    async deleteCandidate(id) {
        if (!confirm('Remove this candidate nomination?')) return;
        try {
            await API.request(`/api/candidates/${id}`, { method: 'DELETE' });
            showToast('Candidate removed.', 'success');
            this.loadElectionCandidates(this.selectedElectionId);
            this.loadAdminData();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }
};

window.Admin = Admin;
