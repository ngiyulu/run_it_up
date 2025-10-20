// ============================
// Global Config
// ============================

// The API base URL (change to your backend)
const API_BASE = localStorage.getItem("apiBase") ||
    (location.origin.includes("127.0.0.1") || location.origin.includes("localhost")
        ? "http://localhost:8080"
        : location.origin);

function setApiBase(url) {
    localStorage.setItem("apiBase", url);
}

// ============================
// Auth helpers
// ============================

function token() {
    return localStorage.getItem("token");
}

function setToken(token, name) {
    localStorage.setItem("token", token);
}

function clearToken() {
    localStorage.removeItem("token");
    localStorage.removeItem("name");
}

function authHeaders(extra = {}) {
    const headers = { "Content-Type": "application/json", ...extra };
    if (token()) headers["Authorization"] = "Bearer " + token();
    return headers;
}

// ============================
// API Wrapper
// ============================

async function api(path, { method = "GET", data, headers = {}, raw = false } = {}) {
    const opts = { method, headers: authHeaders(headers) };
    if (data !== undefined && !raw) opts.body = JSON.stringify(data);

    const res = await fetch(API_BASE + path, opts);

    if (res.status === 401) {
        clearToken();
        window.location.href = "./login.html?msg=expired";
        return;
    }

    if (!res.ok) {
        const text = await res.text();
        throw new Error(text || res.statusText);
    }

    const ct = res.headers.get("content-type") || "";
    if (ct.includes("application/json")) return res.json();
    return res.text();
}

// ============================
// UI Helpers
// ============================

function requireAuth() {
    if (!token()) {
        window.location.href = "./login.html?next=" + encodeURIComponent(location.pathname);
    }
}

function logout() {
    clearToken();
    window.location.href = "./login";
}

function setStatus(el, type, msg) {
    el.className = "alert " + type;
    el.textContent = msg;
    el.classList.remove("hidden");
}

function qs(sel, root = document) {
    return root.querySelector(sel);
}

function qsa(sel, root = document) {
    return [...root.querySelectorAll(sel)];
}

function fmtMoney(n) {
    return (n ?? 0).toLocaleString(undefined, { style: "currency", currency: "USD" });
}

function todayISO() {
    return new Date().toISOString().slice(0, 10);
}

function toISODate(d) {
    return new Date(d).toISOString().slice(0, 10);
}


function validate() {
    if (!titleEl.value.trim()) throw new Error('Title is required.');
    if (!dateEl.value) throw new Error('Date is required.');
    if (!startEl.value) throw new Error('Start time is required.');
    if (!endEl.value) throw new Error('End time is required.');
    if (!zoneIdEl.value.trim()) throw new Error('Time zone is required.');
    if (!hostedByEl.value.trim()) throw new Error('Hosted By is required.');

    const amount = Number.parseFloat(amountEl.value);
    if (Number.isNaN(amount) || amount < 0) throw new Error('Amount must be a non-negative number.');

    const maxPlayers = Number.parseInt(maxPlayerEl.value, 10);
    if (Number.isNaN(maxPlayers) || maxPlayers < 10)
        throw new Error('Max players must be at least 10.');

    if (!gymIdEl.value) throw new Error('Please choose a gym.');

    if (allowGuestEl.checked) {
        const maxGuest = Number.parseInt(maxGuestEl.value || '0', 10);
        if (Number.isNaN(maxGuest) || maxGuest < 0)
            throw new Error('Max guests must be a non-negative number.');
        if (maxGuest > maxPlayers)
            throw new Error('Max guests must be less than or equal to Max players.');
    }
}
