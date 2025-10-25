'use strict';

document.addEventListener('DOMContentLoaded', async () => {
    requireAuth();



    // Header actions
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) logoutBtn.addEventListener('click', e => { e.preventDefault(); logout(); });

    // Tabs
    const tabs = Array.from(document.querySelectorAll('.tab'));
    const panels = Array.from(document.querySelectorAll('.tabpanel'));
    tabs.forEach(t => t.addEventListener('click', () => {
        tabs.forEach(x => x.classList.remove('active'));
        panels.forEach(p => p.classList.remove('active'));
        t.classList.add('active');
        const target = document.querySelector(`.tabpanel[data-panel="${t.dataset.tab}"]`);
        if (target) target.classList.add('active');
    }));

    const statusEl = document.getElementById('status');
    const params = new URLSearchParams(location.search);
    const userId = params.get('id');

    if (!userId) {
        setStatus(statusEl, 'error', 'Missing user id.');
        return;
    }

    // Load user details
    try {
        setStatus(statusEl, 'success', 'Loading user…');
        const u = await api(`/admin/api/v1/user/retrieve/${encodeURIComponent(userId)}`, { method: 'GET' });

        const avatar = document.getElementById('avatar');
        const fullName = document.getElementById('fullName');
        const title = document.getElementById('title');
        const uid = document.getElementById('uid');
        const email = document.getElementById('email');
        const phone = document.getElementById('phone');
        const credit = document.getElementById('credit');
        const phoneVerified = document.getElementById('phoneVerified');
        const dob = document.getElementById('dob');
        const skill = document.getElementById('skill');
        const joined = document.getElementById('joined');
        const levelBadge = document.getElementById('levelBadge');
        const enabledBadge = document.getElementById('enabledBadge');

        const waiverEmpty = document.getElementById('waiverEmpty');
        const waiverLinkRow = document.getElementById('waiverLinkRow');
        const waiverLink = document.getElementById('waiverLink');
        const waiverSignedRow = document.getElementById('waiverSignedRow');
        const waiverSignedAt = document.getElementById('waiverSignedAt');
        const waiverNotesRow = document.getElementById('waiverNotesRow');
        const waiverNotes = document.getElementById('waiverNotes');

        // Render core fields
        if (avatar) {
            avatar.src = u.imageUrl || u.photoUrl || '';
            avatar.onerror = () => { avatar.style.display = 'none'; };
        }

        const name = [u.firstName || u.first, u.lastName || u.last].filter(Boolean).join(' ') || u.name || '—';
        if (fullName) fullName.textContent = name;
        if (title) title.textContent = `User • ${name}`;

        if (uid) uid.textContent = u.id || u.userId || userId;
        if (email) email.textContent = u.email || '—';

        if (credit) credit.textContent = fmtMoney(u.credit || '—');

        const phoneVal = u.phoneNumber || u.phone || '—';
        if (phone) phone.textContent = phoneVal;
        const isVerified = (typeof u.verifiedPhone === 'boolean') ? u.verifiedPhone : !!u.phoneVerified;
        if (phoneVerified) phoneVerified.classList.toggle('hidden', !isVerified);

        if (dob) dob.textContent = prettyDate(u.dob);
        if (skill) skill.textContent = u.skillLevel || u.level || '—';
        if (joined) joined.textContent = prettyDateTime(u.createdAt);

        if (u.skillLevel && levelBadge) {
            levelBadge.textContent = String(u.skillLevel);
            levelBadge.classList.remove('hidden');
        }
        if (typeof u.enabled === 'boolean' && enabledBadge) {
            enabledBadge.textContent = u.enabled ? 'Enabled' : 'Disabled';
            enabledBadge.classList.remove('hidden');
        }

        // Waiver bits
        const waiverUrl = u.waiverUrl || u.waiver?.url;
        const signedAt = u.waiverAuthorizedAt || u.waiverSignedAt || u.waiver?.approvedAt;
        const notesVal = u.internalNotes || u.waiver?.note;

        if (waiverUrl) {
            if (waiverEmpty) waiverEmpty.classList.add('hidden');
            if (waiverLinkRow) waiverLinkRow.classList.remove('hidden');
            if (waiverLink) waiverLink.href = waiverUrl;

            if (signedAt && waiverSignedRow && waiverSignedAt) {
                waiverSignedRow.classList.remove('hidden');
                waiverSignedAt.textContent = prettyDateTime(signedAt);
            }
            if (notesVal && waiverNotesRow && waiverNotes) {
                waiverNotesRow.classList.remove('hidden');
                waiverNotes.textContent = notesVal;
            }
        } else {
            if (waiverLinkRow) waiverLinkRow.classList.add('hidden');
            if (waiverSignedRow) waiverSignedRow.classList.add('hidden');
            if (waiverNotesRow) waiverNotesRow.classList.add('hidden');
        }

        setStatus(statusEl, 'success', 'User loaded.');
    } catch (e) {
        console.error(e);
        setStatus(statusEl, 'error', 'Failed to load user.');
    }

    // Run Sessions tab
    const startDateEl = document.getElementById('startDate');
    const endDateEl = document.getElementById('endDate');
    const searchBtn = document.getElementById('searchBtn');
    const listEl = document.getElementById('sessionsList');
    const countBadge = document.getElementById('countBadge');

    // Force-open native date picker on click/focus (with graceful fallback)
    function attachDatePicker(el) {
        if (!el) return;
        const open = () => {
            if (typeof el.showPicker === 'function') {
                el.showPicker();              // Modern Chrome/Edge
            } else {
                el.focus(); el.click();       // Fallback for Safari/Firefox
            }
        };
        el.addEventListener('click', open);
        el.addEventListener('focus', open);
    }

    attachDatePicker(startDateEl);
    attachDatePicker(endDateEl);

    // Defaults: last 90 days -> today
    const todayIso = new Date().toISOString().split('T')[0];
    const ninetyAgo = (() => {
        const d = new Date(); d.setDate(d.getDate() - 90);
        return d.toISOString().split('T')[0];
    })();
    if (startDateEl) startDateEl.value = ninetyAgo;
    if (endDateEl) endDateEl.value = '';

    if (searchBtn) {
        searchBtn.addEventListener('click', async () => {
            const start = startDateEl?.value || '';
            const end = endDateEl?.value || '';

            if (!start) {
                setStatus(statusEl, 'error', 'Start date is required.');
                return;
            }

            // 🧭 Validation: End date cannot be earlier than start date
            if (end) {
                const startDate = new Date(start);
                const endDate = new Date(end);
                if (endDate < startDate) {
                    setStatus(statusEl, 'error', 'End date cannot be earlier than start date.');
                    return;
                }
            }

            await loadSessions(start, end);
        });
    }

    async function loadSessions(start, end) {
        try {
            setStatus(statusEl, 'success', 'Loading sessions…');
            if (listEl) listEl.innerHTML = `<div class="muted" style="text-align:center;">Loading…</div>`;
            const query = `/admin/api/v1/run-session/by-range?start=${encodeURIComponent(start)}${end ? `&end=${encodeURIComponent(end)}` : ''}`;
            const all = await api(query, { method: 'GET' });

            const mine = (Array.isArray(all) ? all : []).filter(s => participatedByUser(s, userId));
            renderSessions(mine);
            setStatus(statusEl, 'success', `Loaded ${mine.length} session(s).`);
        } catch (e) {
            console.error(e);
            if (listEl) listEl.innerHTML = `<div class="muted" style="text-align:center;">Failed to load sessions.</div>`;
            setStatus(statusEl, 'error', 'Failed to load sessions.');
        }
    }

    function participatedByUser(s, uid) {
        if (!s || !uid) return false;
        if (Array.isArray(s.bookingList) && s.bookingList.some(b => (b.userId || '') === uid)) return true;
        if (Array.isArray(s.bookings)) {
            if (s.bookings.some(b => (b.userId || '') === uid)) return true;
            if (s.bookings.some(b => (b.user && (b.user.userId || b.user.id)) === uid)) return true;
        }
        if (Array.isArray(s.players) && s.players.some(p => (p.id || p.userId) === uid)) return true;
        return false;
    }

    function renderSessions(list) {
        if (countBadge) countBadge.textContent = `${list.length} session${list.length === 1 ? '' : 's'}`;
        if (!Array.isArray(list) || list.length === 0) {
            if (listEl) listEl.innerHTML = `<div class="muted" style="text-align:center;">No sessions found for this user in the selected range.</div>`;
            return;
        }

        list.sort((a, b) => cmpDate(a.date, b.date) || cmpTime(a.startTime, b.startTime));
        if (listEl) listEl.innerHTML = '';

        list.forEach(s => {
            const row = document.createElement('div');
            row.className = 'rowline';

            const when = document.createElement('div');
            when.className = 'nowrap';
            when.innerHTML = `<div>${prettyDateOnly(s.date)}</div><div class="muted">${prettyTime(s.startTime)} – ${prettyTime(s.endTime)}</div>`;

            const gym = document.createElement('div');
            gym.className = 'grow';
            const addr = formatGymAddress(s.gym?.address || {});
            gym.innerHTML = `<div style="font-weight:600;">${escapeHtml(s.gym?.title || s.gym?.name || '—')}</div>
                       <div class="muted">${escapeHtml(addr)}</div>`;

            const status = document.createElement('div');
            status.innerHTML = `<span class="badge">${escapeHtml(s.status || '—')}</span>`;

            const amount = document.createElement('div');
            amount.className = 'nowrap';
            amount.textContent = (typeof s.amount === 'number') ? fmtMoney(s.amount) : '—';

            if (s.id) {
                row.style.cursor = 'pointer';
                row.title = 'Open session';
                row.addEventListener('click', () => {
                    window.open(`/admin/runsessions/edit?id=${encodeURIComponent(s.id)}`, '_blank', 'noopener');
                });
            }

            row.append(when, gym, status, amount);
            if (listEl) listEl.appendChild(row);
        });
    }

    // ---------- Helpers ----------
    function prettyDate(val) {
        if (!val) return '—';
        const d = new Date(val);
        if (isNaN(d.getTime())) {
            if (/^\d{4}-\d{2}-\d{2}$/.test(String(val))) return String(val);
            return String(val);
        }
        return d.toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' });
    }
    function prettyDateOnly(val) { return prettyDate(val); }

    function prettyDateTime(val) {
        if (!val) return '—';
        const d = new Date(val);
        if (isNaN(d.getTime())) return String(val);
        return d.toLocaleString([], { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    }
    function prettyTime(t) {
        if (!t) return '—';
        const m = String(t).match(/^(\d{1,2}):(\d{2})(?::\d{2})?$/);
        if (!m) return String(t);
        const d = new Date(); d.setHours(+m[1], +m[2], 0, 0);
        return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
    function cmpDate(a, b) {
        const da = new Date(a).getTime();
        const db = new Date(b).getTime();
        if (!isNaN(da) && !isNaN(db)) return da - db;
        return String(a).localeCompare(String(b));
    }
    function cmpTime(a, b) {
        return String(a).localeCompare(String(b));
    }
    function formatGymAddress(a) {
        if (!a) return '';
        const zip = a.zip || a.zipCode;
        return [a.line1, a.line2, a.city, a.state, zip].filter(Boolean).join(', ');
    }
    function fmtMoney(n) {
        try { return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' }).format(n); }
        catch { return `$${(n ?? 0).toFixed(2)}`; }
    }
    function escapeHtml(s) { return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
});
