document.addEventListener('DOMContentLoaded', () => {
    requireAuth();

    /* ---------- Tabs ---------- */
    const tabs = [...document.querySelectorAll('.tab')];
    const panels = [...document.querySelectorAll('.tabpanel')];
    tabs.forEach(t => t.addEventListener('click', () => {
        tabs.forEach(x => x.classList.remove('active'));
        panels.forEach(p => p.classList.remove('active'));
        t.classList.add('active');
        document.querySelector(`.tabpanel[data-panel="${t.dataset.tab}"]`).classList.add('active');
    }));

    /* ---------- DOM refs ---------- */
    const pageTitle = document.getElementById('pageTitle');
    const form = document.getElementById('runForm');
    const statusEl = document.getElementById('status');

    const dateEl = document.getElementById('date');
    const startEl = document.getElementById('startTime');
    const endEl = document.getElementById('endTime');
    const zoneEl = document.getElementById('zoneId');

    const titleEl = document.getElementById('title');
    const amountEl = document.getElementById('amount');
    const maxPlayerEl = document.getElementById('maxPlayer');
    const allowGuestEl = document.getElementById('allowGuest');
    const maxGuestEl = document.getElementById('maxGuest');
    const privateRunEl = document.getElementById('privateRun');
    const notesEl = document.getElementById('notes');
    const descEl = document.getElementById('description');

    const gymIdEl = document.getElementById('gymId');

    const saveBtn = document.getElementById('saveBtn');
    const resetBtn = document.getElementById('resetBtn');
    const cancelBtn = document.getElementById('cancelBtn');

    const chooseGymBtn = document.getElementById('chooseGymBtn');

    const playersList = document.getElementById('playersList');
    const waitList = document.getElementById('waitList');

    document.getElementById('logoutBtn').addEventListener('click', e => { e.preventDefault(); logout(); });

    /* ---------- Defaults & pickers ---------- */
    dateEl.value = new Date().toISOString().split('T')[0];
    zoneEl.value = 'America/Chicago';

    [dateEl, startEl, endEl].forEach(el => {
        el.addEventListener('click', () => el.showPicker?.());
        el.addEventListener('focus', () => el.showPicker?.());
    });

    /* ---------- Guest constraints ---------- */
    const guestHelp = document.getElementById('guestHelp');

    function syncGuest() {
        const on = allowGuestEl.checked;
        maxGuestEl.disabled = !on;
        guestHelp.classList.toggle('hidden', on);
        maxGuestEl.classList.toggle('muted', !on);
        if (!on) maxGuestEl.value = '';
    }
    function syncMaxGuestCap() {
        const mp = parseInt(maxPlayerEl.value || '0', 10);
        maxGuestEl.max = Math.max(0, mp);
        const mg = parseInt(maxGuestEl.value || '0', 10);
        if (!Number.isNaN(mg) && mg > mp) maxGuestEl.value = String(mp);
    }

    allowGuestEl.addEventListener('change', () => { syncGuest(); syncMaxGuestCap(); });
    maxPlayerEl.addEventListener('input', () => {
        const v = parseInt(maxPlayerEl.value || '0', 10);
        if (!Number.isNaN(v) && v < 10) maxPlayerEl.value = '10';
        syncMaxGuestCap();
    });
    syncGuest(); syncMaxGuestCap();

    /* ---------- Create vs Edit detection ---------- */
    const params = new URLSearchParams(location.search);
    const sessionId = params.get('id');   // if present -> edit mode

    if (sessionId) {
        pageTitle.textContent = 'Edit Run Session';
        saveBtn.textContent = 'Update';
        setFormEnabled(false);
        setStatus(statusEl, 'success', 'Loading session…');
        loadSession(sessionId).then(() => {
            setStatus(statusEl, 'success', 'Session loaded.');
        }).catch(err => {
            console.error(err);
            setStatus(statusEl, 'error', 'Failed to load session.');
            // Disable Choose Gym + bottom buttons on error
        });
    }

    /* ---------- Submit (Create or Update) ---------- */
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            const payload = collectPayload();
            setStatus(statusEl, 'success', sessionId ? 'Updating session…' : 'Creating run session…');
            if (sessionId) {
                await api(`/admin/api/v1/run-session/${encodeURIComponent(sessionId)}`, { method: 'PUT', data: payload });
                setStatus(statusEl, 'success', 'Session updated.');
            } else {
                await api('/admin/api/v1/run-session/create', { method: 'POST', data: payload });
                setStatus(statusEl, 'success', 'Session created.');
            }
            window.location.href = '/admin/runsessions';
        } catch (err) {
            console.error(err);
            setStatus(statusEl, 'error', err.message || (sessionId ? 'Failed to update.' : 'Failed to create.'));
        }
    });

    /* ---------- Reset / Cancel ---------- */
    resetBtn.addEventListener('click', () => {
        if (sessionId && original) {
            fillForm(original);
        } else {
            form.reset();
            setSelectedGym(null);
            dateEl.value = new Date().toISOString().split('T')[0];
            zoneEl.value = 'America/Chicago';
            syncGuest(); syncMaxGuestCap();
        }
    });
    cancelBtn.addEventListener('click', () => history.back());

    /* ---------- Data helpers ---------- */
    let original = null;

    async function loadSession(id) {
        const s = await api(`/admin/api/v1/run-session/${encodeURIComponent(id)}`, { method: 'GET' });
        original = s;
        fillForm(s);
        renderPlayers(s);
        renderWaitlist(s);
    }

    function fillForm(s) {
        const get = (v, d='') => (v === undefined || v === null) ? d : v;

        titleEl.value = get(s.title);
        amountEl.value = get(s.amount, 0);

        dateEl.value = normalizeDate(get(s.date, dateEl.value));
        startEl.value = normalizeTime(get(s.startTime, ''));
        endEl.value = normalizeTime(get(s.endTime, ''));

        maxPlayerEl.value = get(s.maxPlayer, 10);
        allowGuestEl.checked = !!s.allowGuest;
        maxGuestEl.value = get(s.maxGuest, 0);

        privateRunEl.checked = !!s.privateRun;
        notesEl.value = get(s.notes);
        descEl.value = get(s.description);
        zoneEl.value = get(s.zoneId, zoneEl.value);

        if (s.gym && (s.gym.id || s.gym.title)) {
            setSelectedGym({
                id: s.gym.id || s.gymId,
                title: s.gym.title,
                image: s.gym.imageUrl || s.gym.image,
                address: s.gym.address
            });
        } else if (s.gymId) {
            gymIdEl.value = s.gymId; // minimal fallback
        }

        syncGuest(); syncMaxGuestCap();
    }

    function renderPlayers(s) {
        const list = s.players || [];
        if (!Array.isArray(list) || list.length === 0) {
            playersList.innerHTML = `<div style="text-align:center;color:gray;">No players.</div>`;
            return;
        }
        playersList.innerHTML = '';
        list.forEach(p => {
            const row = document.createElement('div');
            row.className = 'rowline';
            const avatar = document.createElement('img');
            avatar.src = p.photoUrl || p.avatar || '';
            avatar.alt = p.name || p.fullName || p.displayName || 'Player';
            Object.assign(avatar.style, { width:'36px', height:'36px', borderRadius:'50%', objectFit:'cover' });
            avatar.onerror = () => { avatar.style.display = 'none'; };
            const name = document.createElement('div');
            name.textContent = p.name || p.fullName || p.displayName || (p.firstName && p.lastName ? `${p.firstName} ${p.lastName}` : '—');
            name.style.marginLeft = '10px';
            const skill = document.createElement('div');
            skill.textContent = p.skill || p.level || '';
            skill.style.marginLeft = 'auto'; skill.style.opacity = .7;
            row.append(avatar, name, skill);
            playersList.appendChild(row);
        });
    }

    function renderWaitlist(s) {
        const list = s.waitList || [];
        if (!Array.isArray(list) || list.length === 0) {
            waitList.innerHTML = `<div style="text-align:center;color:gray;">No players.</div>`;
            return;
        }
        waitList.innerHTML = '';
        list.forEach(p => {
            const row = document.createElement('div');
            row.className = 'rowline';
            const avatar = document.createElement('img');
            avatar.src = p.photoUrl || p.avatar || '';
            avatar.alt = p.name || 'Player';
            Object.assign(avatar.style, { width:'36px', height:'36px', borderRadius:'50%', objectFit:'cover' });
            avatar.onerror = () => { avatar.style.display = 'none'; };
            const name = document.createElement('div');
            name.textContent = p.name || p.fullName || p.displayName || '—';
            name.style.marginLeft = '10px';
            const skill = document.createElement('div');
            skill.textContent = p.skill || p.level || '';
            skill.style.marginLeft = 'auto'; skill.style.opacity = .7;
            row.append(avatar, name, skill);
            waitList.appendChild(row);
        });
    }

    /* ---------- Submit payload ---------- */
    function collectPayload() {
        const title = titleEl.value.trim();
        const date = dateEl.value;
        const startTime = startEl.value;
        const endTime = endEl.value;
        const amount = parseFloat(amountEl.value);
        const maxPlayer = parseInt(maxPlayerEl.value, 10);
        const allowGuest = allowGuestEl.checked;
        const maxGuest = allowGuest ? parseInt(maxGuestEl.value || '0', 10) : 0;
        const privateRun = privateRunEl.checked;
        const notes = (notesEl.value || '').trim();
        const description = (descEl.value || '').trim();
        const zoneId = zoneEl.value;
        const gymId = gymIdEl.value;

        if (!title) throw new Error('Title is required.');
        if (!date) throw new Error('Date is required.');
        if (!startTime) throw new Error('Start time is required.');
        if (!endTime) throw new Error('End time is required.');
        if (Number.isNaN(amount) || amount < 0) throw new Error('Amount must be a non-negative number.');
        if (Number.isNaN(maxPlayer) || maxPlayer < 10) throw new Error('Max players must be at least 10.');
        if (!zoneId) throw new Error('Time zone is required.');
        if (!gymId) throw new Error('Please choose a gym.');
        if (allowGuest) {
            if (Number.isNaN(maxGuest) || maxGuest < 0) throw new Error('Max guests must be a non-negative number.');
            if (maxGuest > maxPlayer) throw new Error('Max guests must be ≤ Max players.');
        }

        return {
            gymId,
            date,
            startTime,
            endTime,
            zoneId,
            allowGuest,
            notes,
            privateRun,
            description,
            amount,
            maxPlayer,
            title,
            maxGuest
        };
    }

    /* ---------- UI helpers ---------- */
    function setFormEnabled(enabled) {
        // Disable all form fields
        [...form.elements].forEach(el => el.disabled = !enabled);
        // Explicitly control primary action buttons
        saveBtn.disabled = !enabled;
        resetBtn.disabled = !enabled;
        cancelBtn.disabled = !enabled;
        // Explicitly control Choose Gym (in case it’s not part of form.elements in some browsers)
        if (chooseGymBtn) chooseGymBtn.disabled = !enabled;
    }

    function normalizeDate(d) {
        if (!d) return '';
        if (/^\d{4}-\d{2}-\d{2}$/.test(d)) return d;
        const dt = new Date(d);
        if (isNaN(dt.getTime())) return '';
        return dt.toISOString().split('T')[0];
    }

    function normalizeTime(t) {
        if (!t) return '';
        const m = String(t).match(/^(\d{1,2}):(\d{2})(?::\d{2})?$/);
        if (!m) return '';
        const hh = String(m[1]).padStart(2,'0');
        const mm = m[2];
        return `${hh}:${mm}`;
    }

    /* ---------- Gym Picker Modal + helpers ---------- */
    (function gymModalInit() {
        const modal = document.getElementById('gymModal');
        const closeBtn = document.getElementById('gymCloseBtn');
        const searchEl = document.getElementById('gymSearch');
        const gymList = document.getElementById('gymList');
        const gymEmpty = document.getElementById('gymEmpty');

        async function loadGyms() {
            try {
                let gyms = await api('/admin/api/v1/gym/list');
                const q = (searchEl.value || '').trim().toLowerCase();
                if (q) gyms = gyms.filter(g => (g.title || '').toLowerCase().includes(q));
                renderGyms(gyms);
            } catch (e) {
                console.error(e);
                renderGyms([]);
            }
        }

        function renderGyms(gyms) {
            gymList.innerHTML = '';
            if (!Array.isArray(gyms) || gyms.length === 0) {
                gymEmpty.classList.remove('hidden');
                return;
            }
            gymEmpty.classList.add('hidden');
            gyms.sort((a, b) => (a.title || '').localeCompare(b.title || '', undefined, { sensitivity: 'base' }));
            gyms.forEach(g => {
                const card = document.createElement('div');
                card.className = 'gym-card';
                const img = document.createElement('img');
                img.className = 'gym-thumb';
                img.alt = g.title || 'Gym';
                const pic = g.imageUrl || g.image;
                if (pic) img.src = pic; else img.style.display = 'none';
                const addr = formatGymAddress(g);
                const body = document.createElement('div');
                body.style.flex = '1 1 auto';
                body.innerHTML = `<div style="font-weight:600;">${g.title || '—'}</div><div style="color:#9ca3af;font-size:12px;">${addr}</div>`;
                const btn = document.createElement('button');
                btn.className = 'primary';
                btn.textContent = 'Select';
                btn.onclick = () => { setSelectedGym({ id: g.id, title: g.title, image: pic || '', address: g.address, line1: g.address?.line1, line2: g.address?.line2, city: g.address?.city, state: g.address?.state, zipCode: g.address?.zip || g.address?.zipCode }); closeGymPicker(); };
                card.append(img, body, btn);
                gymList.appendChild(card);
            });
        }

        function openGymPicker() {
            const modal = document.getElementById('gymModal');
            modal.style.display = 'flex';
            modal.setAttribute('aria-hidden', 'false');
            loadGyms();
        }

        function closeGymPicker() {
            const modal = document.getElementById('gymModal');
            modal.style.display = 'none';
            modal.setAttribute('aria-hidden', 'true');
            gymList.innerHTML = '';
            searchEl.value = '';
            gymEmpty.classList.add('hidden');
        }

        // Bindings
        chooseGymBtn.addEventListener('click', openGymPicker);
        closeBtn.addEventListener('click', closeGymPicker);
        searchEl.addEventListener('input', () => loadGyms());
    })();

    function setSelectedGym(g) {
        const empty = document.getElementById('gymEmptyState');
        const picked = document.getElementById('gymPickedState');
        const idEl = document.getElementById('gymId');
        const titleEl = document.getElementById('gymTitleText');
        const addrEl = document.getElementById('gymAddrText');
        const thumbEl = document.getElementById('gymThumb');

        if (!g) {
            idEl.value = '';
            titleEl.textContent = '—';
            addrEl.textContent = '—';
            thumbEl.src = '';
            thumbEl.style.display = 'none';
            picked.classList.add('hidden');
            empty.classList.remove('hidden');
            return;
        }
        idEl.value = g.id || '';
        titleEl.textContent = g.title || '—';
        addrEl.textContent = formatGymAddress(g);
        if (g.image) {
            thumbEl.src = g.image;
            thumbEl.style.display = '';
            thumbEl.onerror = () => { thumbEl.style.display = 'none'; };
        } else {
            thumbEl.style.display = 'none';
        }
        empty.classList.add('hidden');
        picked.classList.remove('hidden');
    }

    function formatGymAddress(g) {
        console.error(g)
        return [g.line1, g.line2, g.city, g.state, g.zipCode].filter(Boolean).join(', ') || 'No address';
    }
});
