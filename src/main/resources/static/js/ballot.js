/**
 * Ballot and Voting Workflow Module.
 */
const Ballot = {
    currentElection: null,
    currentCandidates: [],
    selectedCandidateId: null,

    init() {
        this.bindEvents();
    },

    bindEvents() {
        // Confirmation Modal Confirm Button
        document.getElementById('btn-confirm-vote')?.addEventListener('click', async () => {
            await this.submitBallot();
        });

        // Confirmation Modal Cancel Button
        document.getElementById('btn-cancel-vote')?.addEventListener('click', () => {
            document.getElementById('modal-vote-confirm')?.classList.add('hidden');
        });

        // Back to Dashboard Buttons
        document.getElementById('btn-back-dashboard')?.addEventListener('click', () => {
            window.switchView('dashboard-view');
            Elections.loadElections();
        });

        document.getElementById('btn-success-dashboard')?.addEventListener('click', () => {
            window.switchView('dashboard-view');
            Elections.loadElections();
        });

        document.getElementById('btn-results-dashboard')?.addEventListener('click', () => {
            window.switchView('dashboard-view');
            Elections.loadElections();
        });
    },

    async openBallot(electionId) {
        window.switchView('ballot-view');
        this.selectedCandidateId = null;

        const titleEl = document.getElementById('ballot-election-title');
        const descEl = document.getElementById('ballot-election-desc');
        const badgeEl = document.getElementById('ballot-election-badge');
        const candidatesContainer = document.getElementById('ballot-candidates-grid');
        const actionContainer = document.getElementById('ballot-action-bar');

        if (titleEl) titleEl.textContent = 'Loading Election...';
        if (candidatesContainer) {
            candidatesContainer.innerHTML = `
                <div class="col-span-full py-16 text-center text-slate-500">
                    <i class="fa-solid fa-circle-notch fa-spin text-3xl text-indigo-600 mb-3"></i>
                    <p>Loading candidate nominations...</p>
                </div>
            `;
        }

        try {
            const [election, candidates] = await Promise.all([
                API.request(`/api/elections/${electionId}`),
                API.request(`/api/elections/${electionId}/candidates`)
            ]);

            this.currentElection = election;
            this.currentCandidates = candidates;

            if (titleEl) titleEl.textContent = election.title;
            if (descEl) descEl.textContent = election.description || 'Cast your official ballot below.';
            if (badgeEl) {
                badgeEl.textContent = election.status;
                badgeEl.className = election.status === 'ACTIVE'
                    ? 'px-3 py-1 text-xs font-semibold rounded-full bg-emerald-100 text-emerald-800'
                    : 'px-3 py-1 text-xs font-semibold rounded-full bg-slate-100 text-slate-700';
            }

            this.renderCandidateCards();

            // Render bottom action button
            const user = API.getCurrentUser();
            const canVote = user && user.role === 'VOTER' && election.status === 'ACTIVE' && !election.hasVoted;

            if (canVote) {
                actionContainer.innerHTML = `
                    <button id="btn-review-vote" onclick="Ballot.reviewVote()" class="py-3 px-8 rounded-xl text-sm font-semibold bg-indigo-600 hover:bg-indigo-700 text-white shadow-lg shadow-indigo-200 transition flex items-center gap-2">
                        <i class="fa-solid fa-check-to-slot"></i> Review & Cast Ballot
                    </button>
                `;
            } else if (election.hasVoted) {
                actionContainer.innerHTML = `
                    <div class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-50 text-emerald-700 border border-emerald-200 text-sm font-semibold">
                        <i class="fa-solid fa-circle-check text-emerald-500"></i> You have already voted in this election.
                    </div>
                `;
            } else if (election.status !== 'ACTIVE') {
                actionContainer.innerHTML = `
                    <div class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-100 text-slate-600 text-sm font-medium">
                        <i class="fa-solid fa-lock"></i> Voting is currently ${election.status}.
                    </div>
                `;
            } else {
                actionContainer.innerHTML = `
                    <div class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-amber-50 text-amber-700 border border-amber-200 text-sm font-medium">
                        <i class="fa-solid fa-shield-halved"></i> Administrator view (Auditing mode).
                    </div>
                `;
            }

        } catch (err) {
            showToast('Failed to load election details: ' + err.message, 'error');
            window.switchView('dashboard-view');
        }
    },

    renderCandidateCards() {
        const container = document.getElementById('ballot-candidates-grid');
        if (!container) return;

        if (this.currentCandidates.length === 0) {
            container.innerHTML = `
                <div class="col-span-full py-12 text-center text-slate-500 bg-white rounded-2xl border border-slate-200">
                    <p class="font-medium">No candidates have been nominated for this election yet.</p>
                </div>
            `;
            return;
        }

        container.innerHTML = '';
        const user = API.getCurrentUser();
        const isVoterActive = user && user.role === 'VOTER' && this.currentElection?.status === 'ACTIVE' && !this.currentElection?.hasVoted;

        this.currentCandidates.forEach(candidate => {
            const cardWrapper = document.createElement('label');
            cardWrapper.className = 'block cursor-pointer';

            const initials = candidate.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();

            cardWrapper.innerHTML = `
                <input type="radio" name="candidateSelection" value="${candidate.id}" class="sr-only candidate-radio" ${!isVoterActive ? 'disabled' : ''}>
                <div class="candidate-card glass-panel rounded-2xl p-6 border-2 border-slate-200 hover:border-indigo-300 transition-all flex flex-col justify-between h-full hover-lift">
                    <div>
                        <div class="flex items-center gap-4 mb-4">
                            <div class="w-14 h-14 rounded-2xl bg-gradient-to-tr from-indigo-500 to-sky-400 flex items-center justify-center text-white font-bold text-lg shadow-md shadow-indigo-100">
                                ${initials}
                            </div>
                            <div>
                                <h4 class="text-lg font-bold text-slate-900 leading-snug">${escapeHtml(candidate.name)}</h4>
                                <span class="inline-block mt-0.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-100">
                                    ${escapeHtml(candidate.department || 'General')}
                                </span>
                            </div>
                        </div>

                        <div class="mb-4">
                            <span class="text-xs font-semibold uppercase tracking-wider text-slate-400">Position</span>
                            <p class="text-sm font-semibold text-slate-700">${escapeHtml(candidate.position || 'Representative')}</p>
                        </div>

                        <div class="mb-4">
                            <span class="text-xs font-semibold uppercase tracking-wider text-slate-400">Key Manifesto</span>
                            <p class="text-sm text-slate-600 line-clamp-3 mt-1 leading-relaxed bg-slate-50/70 p-3 rounded-xl border border-slate-100">
                                "${escapeHtml(candidate.manifesto)}"
                            </p>
                        </div>
                    </div>

                    <div class="pt-4 border-t border-slate-100 flex items-center justify-between">
                        <button type="button" onclick="Ballot.viewManifesto(${candidate.id}, event)" class="text-xs font-semibold text-indigo-600 hover:text-indigo-800">
                            Read Full Manifesto <i class="fa-solid fa-arrow-up-right-from-square ml-1"></i>
                        </button>
                        ${isVoterActive ? `
                            <div class="w-6 h-6 rounded-full border-2 border-slate-300 flex items-center justify-center text-indigo-600 radio-indicator">
                                <i class="fa-solid fa-check text-xs opacity-0"></i>
                            </div>
                        ` : ''}
                    </div>
                </div>
            `;

            // Radio change event
            const radio = cardWrapper.querySelector('input[type="radio"]');
            if (radio) {
                radio.addEventListener('change', () => {
                    this.selectedCandidateId = candidate.id;
                    document.querySelectorAll('.radio-indicator i').forEach(i => i.classList.add('opacity-0'));
                    cardWrapper.querySelector('.radio-indicator i')?.classList.remove('opacity-0');
                });
            }

            container.appendChild(cardWrapper);
        });
    },

    viewManifesto(candidateId, event) {
        if (event) event.stopPropagation();
        const candidate = this.currentCandidates.find(c => c.id === candidateId);
        if (!candidate) return;

        const modal = document.getElementById('modal-manifesto');
        const nameEl = document.getElementById('manifesto-candidate-name');
        const deptEl = document.getElementById('manifesto-candidate-dept');
        const textEl = document.getElementById('manifesto-full-text');

        if (nameEl) nameEl.textContent = candidate.name;
        if (deptEl) deptEl.textContent = `${candidate.position || 'Candidate'} · ${candidate.department || 'Department'}`;
        if (textEl) textEl.textContent = candidate.manifesto;

        modal?.classList.remove('hidden');
    },

    reviewVote() {
        if (!this.selectedCandidateId) {
            showToast('Please select a candidate before proceeding.', 'warning');
            return;
        }

        const candidate = this.currentCandidates.find(c => c.id === this.selectedCandidateId);
        if (!candidate) return;

        const modal = document.getElementById('modal-vote-confirm');
        const candNameEl = document.getElementById('confirm-candidate-name');
        const electionNameEl = document.getElementById('confirm-election-name');

        if (candNameEl) candNameEl.textContent = candidate.name;
        if (electionNameEl) electionNameEl.textContent = this.currentElection.title;

        modal?.classList.remove('hidden');
    },

    async submitBallot() {
        if (!this.selectedCandidateId || !this.currentElection) return;

        const confirmBtn = document.getElementById('btn-confirm-vote');
        const cancelBtn = document.getElementById('btn-cancel-vote');

        // CRITICAL VIVA REQUIREMENT: Disable button immediately to prevent double-click race conditions
        confirmBtn.disabled = true;
        cancelBtn.disabled = true;
        confirmBtn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Encrypting & Casting...';

        try {
            const response = await API.request(`/api/elections/${this.currentElection.id}/votes`, {
                method: 'POST',
                body: JSON.stringify({ candidateId: this.selectedCandidateId })
            });

            document.getElementById('modal-vote-confirm')?.classList.add('hidden');
            showToast('Vote successfully cast and cryptographically recorded!', 'success');

            // Render Neutral Vote-Recorded Screen
            this.renderVoteSuccess(response);

        } catch (err) {
            showToast(err.message || 'Failed to record vote.', 'error');
            document.getElementById('modal-vote-confirm')?.classList.add('hidden');
        } finally {
            confirmBtn.disabled = false;
            cancelBtn.disabled = false;
            confirmBtn.innerHTML = 'Yes, Confirm & Cast Ballot';
        }
    },

    renderVoteSuccess(response) {
        window.switchView('vote-success-view');

        const electionTitleEl = document.getElementById('success-election-title');
        const castTimeEl = document.getElementById('success-cast-time');
        const receiptHashEl = document.getElementById('success-receipt-hash');

        if (electionTitleEl) electionTitleEl.textContent = this.currentElection.title;
        if (castTimeEl) castTimeEl.textContent = formatDate(response.castTime);

        // Generate visual receipt pseudo-hash
        if (receiptHashEl) {
            receiptHashEl.textContent = 'BLT-' + Math.random().toString(36).substring(2, 10).toUpperCase() + '-' + Date.now().toString(36).toUpperCase();
        }
    },

    async openResults(electionId) {
        window.switchView('results-view');

        const titleEl = document.getElementById('results-election-title');
        const totalVotesEl = document.getElementById('results-total-votes');
        const statusBadgeEl = document.getElementById('results-status-badge');
        const listContainer = document.getElementById('results-candidates-list');

        if (titleEl) titleEl.textContent = 'Loading Election Results...';
        if (listContainer) {
            listContainer.innerHTML = `
                <div class="py-16 text-center text-slate-500">
                    <i class="fa-solid fa-circle-notch fa-spin text-3xl text-indigo-600 mb-3"></i>
                    <p>Computing verified ballots...</p>
                </div>
            `;
        }

        try {
            const data = await API.request(`/api/elections/${electionId}/results`);

            if (titleEl) titleEl.textContent = data.electionTitle;
            if (totalVotesEl) totalVotesEl.textContent = `${data.totalVotesCast} Verified Votes Cast`;
            if (statusBadgeEl) {
                statusBadgeEl.textContent = data.resultsPublished ? 'Official Certified Results' : 'Embargoed / Preliminary (Admin Only)';
                statusBadgeEl.className = data.resultsPublished
                    ? 'px-3 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800'
                    : 'px-3 py-1 rounded-full text-xs font-semibold bg-amber-100 text-amber-800';
            }

            if (data.candidateResults.length === 0) {
                listContainer.innerHTML = '<p class="text-center py-8 text-slate-500">No votes recorded yet.</p>';
                return;
            }

            listContainer.innerHTML = '';
            const winner = data.candidateResults[0];

            data.candidateResults.forEach((c, index) => {
                const isLeader = index === 0 && c.voteCount > 0;
                const row = document.createElement('div');
                row.className = `p-5 rounded-2xl border transition-all ${isLeader ? 'bg-indigo-50/60 border-indigo-200' : 'bg-white border-slate-200'}`;

                row.innerHTML = `
                    <div class="flex items-center justify-between mb-2">
                        <div class="flex items-center gap-3">
                            <span class="w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs ${isLeader ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-600'}">
                                ${index + 1}
                            </span>
                            <div>
                                <h4 class="font-bold text-slate-900">${escapeHtml(c.candidateName)}</h4>
                                <span class="text-xs text-slate-500">${escapeHtml(c.position || 'Candidate')} · ${escapeHtml(c.department || '')}</span>
                            </div>
                        </div>
                        <div class="text-right">
                            <span class="text-lg font-bold text-slate-900">${c.voteCount}</span>
                            <span class="text-xs font-semibold text-indigo-600 ml-1">(${c.percentage}%)</span>
                        </div>
                    </div>
                    <div class="w-full h-3 rounded-full bg-slate-100 overflow-hidden">
                        <div class="progress-bar-fill h-full rounded-full ${isLeader ? 'bg-indigo-600' : 'bg-slate-400'}" style="width: ${c.percentage}%"></div>
                    </div>
                `;
                listContainer.appendChild(row);
            });

        } catch (err) {
            showToast(err.message, 'error');
            if (listContainer) {
                listContainer.innerHTML = `
                    <div class="py-12 text-center text-slate-500">
                        <i class="fa-solid fa-lock text-3xl text-slate-400 mb-2"></i>
                        <p class="font-medium">${escapeHtml(err.message)}</p>
                    </div>
                `;
            }
        }
    }
};

window.Ballot = Ballot;
