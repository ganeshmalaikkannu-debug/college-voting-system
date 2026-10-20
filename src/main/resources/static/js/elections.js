/**
 * Elections Module: Loads and filters elections on the voter dashboard.
 */
const Elections = {
    allElections: [],
    activeFilter: 'ALL',

    init() {
        this.bindEvents();
    },

    bindEvents() {
        // Filter tabs (All, Active, Upcoming, Closed)
        const filterBtns = document.querySelectorAll('.election-filter-btn');
        filterBtns.forEach(btn => {
            btn.addEventListener('click', (e) => {
                filterBtns.forEach(b => {
                    b.classList.remove('bg-indigo-600', 'text-white', 'shadow-sm');
                    b.classList.add('text-slate-600', 'hover:bg-slate-100');
                });
                btn.classList.add('bg-indigo-600', 'text-white', 'shadow-sm');
                btn.classList.remove('text-slate-600', 'hover:bg-slate-100');

                this.activeFilter = btn.dataset.filter;
                this.renderElections();
            });
        });

        document.getElementById('refresh-elections-btn')?.addEventListener('click', () => {
            this.loadElections();
        });
    },

    async loadElections() {
        const container = document.getElementById('elections-grid');
        if (!container) return;

        container.innerHTML = `
            <div class="col-span-full py-16 text-center">
                <i class="fa-solid fa-circle-notch fa-spin text-3xl text-indigo-600 mb-3"></i>
                <p class="text-sm font-medium text-slate-500">Loading campus elections...</p>
            </div>
        `;

        try {
            this.allElections = await API.request('/api/elections');
            this.renderElections();
        } catch (err) {
            container.innerHTML = `
                <div class="col-span-full py-12 text-center text-rose-500 bg-rose-50 rounded-2xl border border-rose-100">
                    <i class="fa-solid fa-triangle-exclamation text-2xl mb-2"></i>
                    <p class="font-medium">Failed to load elections: ${escapeHtml(err.message)}</p>
                    <button onclick="Elections.loadElections()" class="mt-3 px-4 py-1.5 text-xs font-semibold rounded-lg bg-rose-600 text-white">Retry</button>
                </div>
            `;
        }
    },

    renderElections() {
        const container = document.getElementById('elections-grid');
        if (!container) return;

        let filtered = this.allElections;
        if (this.activeFilter !== 'ALL') {
            filtered = this.allElections.filter(e => e.status === this.activeFilter);
        }

        if (filtered.length === 0) {
            container.innerHTML = `
                <div class="col-span-full py-16 text-center bg-white rounded-2xl border border-slate-200">
                    <div class="w-14 h-14 mx-auto rounded-full bg-slate-100 flex items-center justify-center text-slate-400 mb-3 text-xl">
                        <i class="fa-solid fa-box-archive"></i>
                    </div>
                    <h3 class="text-base font-semibold text-slate-800">No elections found</h3>
                    <p class="text-sm text-slate-500 mt-1">There are no elections in "${this.activeFilter}" status.</p>
                </div>
            `;
            return;
        }

        container.innerHTML = '';
        filtered.forEach(election => {
            const card = document.createElement('div');
            card.className = 'glass-panel rounded-2xl p-6 flex flex-col justify-between hover-lift shadow-sm';

            // Status Badge
            let badgeClass = 'bg-slate-100 text-slate-700';
            let badgeIcon = 'fa-clock';
            if (election.status === 'ACTIVE') {
                badgeClass = 'bg-emerald-100 text-emerald-800 border border-emerald-200 badge-active-pulse';
                badgeIcon = 'fa-check-circle';
            } else if (election.status === 'UPCOMING') {
                badgeClass = 'bg-sky-100 text-sky-800 border border-sky-200';
                badgeIcon = 'fa-calendar';
            } else if (election.status === 'CLOSED') {
                badgeClass = 'bg-slate-200 text-slate-800';
                badgeIcon = 'fa-lock';
            }

            // Has Voted Badge
            let votedBadge = '';
            if (election.hasVoted === true) {
                votedBadge = `
                    <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                        <i class="fa-solid fa-circle-check text-emerald-500"></i> Vote Recorded
                    </span>
                `;
            }

            // Primary Action Button
            let actionBtn = '';
            const user = API.getCurrentUser();
            const isVoter = user && user.role === 'VOTER';

            if (election.status === 'ACTIVE') {
                if (election.hasVoted === true) {
                    actionBtn = `
                        <button disabled class="w-full py-2.5 px-4 rounded-xl text-sm font-semibold bg-slate-100 text-slate-400 cursor-not-allowed">
                            <i class="fa-solid fa-check mr-1.5"></i> You Already Voted
                        </button>
                    `;
                } else if (isVoter) {
                    actionBtn = `
                        <button onclick="Ballot.openBallot(${election.id})" class="w-full py-2.5 px-4 rounded-xl text-sm font-semibold bg-indigo-600 hover:bg-indigo-700 text-white shadow-md shadow-indigo-200 transition">
                            <i class="fa-solid fa-check-to-slot mr-1.5"></i> Cast Your Ballot
                        </button>
                    `;
                } else {
                    actionBtn = `
                        <button onclick="Ballot.openBallot(${election.id})" class="w-full py-2.5 px-4 rounded-xl text-sm font-semibold bg-slate-800 hover:bg-slate-900 text-white transition">
                            <i class="fa-solid fa-eye mr-1.5"></i> View Candidates
                        </button>
                    `;
                }
            } else if (election.status === 'UPCOMING') {
                actionBtn = `
                    <button onclick="Ballot.openBallot(${election.id})" class="w-full py-2.5 px-4 rounded-xl text-sm font-semibold bg-slate-100 hover:bg-slate-200 text-slate-700 transition">
                        <i class="fa-solid fa-users mr-1.5"></i> View Candidates (${election.candidateCount})
                    </button>
                `;
            } else if (election.status === 'CLOSED') {
                actionBtn = `
                    <button onclick="Ballot.openResults(${election.id})" class="w-full py-2.5 px-4 rounded-xl text-sm font-semibold ${election.resultsPublished ? 'bg-indigo-600 text-white hover:bg-indigo-700' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'} transition">
                        <i class="fa-solid fa-chart-simple mr-1.5"></i> ${election.resultsPublished ? 'View Official Results' : 'Results Pending'}
                    </button>
                `;
            }

            // Results button if published while active
            let resultsSecondary = '';
            if (election.resultsPublished && election.status !== 'CLOSED') {
                resultsSecondary = `
                    <button onclick="Ballot.openResults(${election.id})" class="mt-2 w-full py-2 px-3 rounded-lg text-xs font-semibold text-indigo-600 hover:bg-indigo-50 border border-indigo-200 transition">
                        <i class="fa-solid fa-chart-column mr-1"></i> Live Results Available
                    </button>
                `;
            }

            card.innerHTML = `
                <div>
                    <div class="flex items-center justify-between gap-2 mb-3">
                        <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold ${badgeClass}">
                            <i class="fa-solid ${badgeIcon}"></i> ${election.status}
                        </span>
                        ${votedBadge}
                    </div>

                    <h3 class="text-lg font-bold text-slate-900 mb-1.5 leading-snug">${escapeHtml(election.title)}</h3>
                    <p class="text-sm text-slate-600 line-clamp-2 mb-4">${escapeHtml(election.description || 'No description provided.')}</p>

                    <div class="space-y-1.5 text-xs text-slate-500 mb-6 bg-slate-50/70 p-3 rounded-xl border border-slate-100">
                        <div class="flex items-center justify-between">
                            <span><i class="fa-solid fa-users text-slate-400 mr-1.5"></i> Candidates:</span>
                            <span class="font-semibold text-slate-700">${election.candidateCount}</span>
                        </div>
                        <div class="flex items-center justify-between">
                            <span><i class="fa-solid fa-clock text-slate-400 mr-1.5"></i> Start:</span>
                            <span class="font-medium text-slate-700">${formatDate(election.startTime)}</span>
                        </div>
                        <div class="flex items-center justify-between">
                            <span><i class="fa-solid fa-hourglass-end text-slate-400 mr-1.5"></i> End:</span>
                            <span class="font-medium text-slate-700">${formatDate(election.endTime)}</span>
                        </div>
                    </div>
                </div>

                <div>
                    ${actionBtn}
                    ${resultsSecondary}
                </div>
            `;

            container.appendChild(card);
        });
    }
};

window.Elections = Elections;
