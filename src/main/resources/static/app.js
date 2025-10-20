/* ===========================================================
   RunItUp Admin – Shared JS Helpers
   =========================================================== */

console.log("RunItUp Admin JS loaded");

const API_HEADERS = {
    'Content-Type': 'application/json'
};

/* -------------------------------
   Unified API Wrapper
--------------------------------*/
async function api(path, options = {}) {
    const token = localStorage.getItem('token') || '';
    const method = options.method || 'GET';
    const headers = Object.assign({}, API_HEADERS, options.headers || {});
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const init = { method, headers };
    if (options.data) {
        if (options.data instanceof FormData) {
            // If FormData, override headers (browser sets correct boundary)
            delete headers['Content-Type'];
            init.body = options.data;
        } else {
            init.body = JSON.stringify(options.data);
        }
    }

    const resp = await fetch(path, init);
    if (resp.status === 401) {
        console.warn('Unauthorized → redirecting to login');
        localStorage.removeItem('token');
        window.location.href = '/admin/login?msg=expired';
        return;
    }

    if (!resp.ok) {
        const text = await resp.text().catch(() => '');
        throw new Error(text || `HTTP ${resp.status}`);
    }

    const contentType = resp.headers.get('content-type') || '';
    if (contentType.includes('application/json')) return resp.json();
    return resp.text();
}

/* -------------------------------
   Authentication helpers
--------------------------------*/
function requireAuth() {
    const token = localStorage.getItem('token');
    if (!token) {
        console.warn("No JWT found → redirecting to login");
        window.location.href = '/admin/login';
    }
}

function logout() {
    localStorage.removeItem('token');
    window.location.href = '/admin/login';
}

/* -------------------------------
   Status / Alert handling
--------------------------------*/
function setStatus(el, type, msg) {
    if (!el) return;
    el.classList.remove('hidden', 'success', 'error');
    el.classList.add(type);
    el.textContent = msg || '';
    // auto-hide after 5s
    if (type !== 'success') return;
    setTimeout(() => {
        el.classList.add('hidden');
    }, 5000);
}

/* -------------------------------
   Money formatting
--------------------------------*/
function fmtMoney(value, currency = 'USD') {
    const num = parseFloat(value || 0);
    return num.toLocaleString('en-US', {
        style: 'currency',
        currency
    });
}

/* -------------------------------
   Loading Overlay
--------------------------------*/
function showOverlay(msg = 'Loading...') {
    let overlay = document.getElementById('progressOverlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'progressOverlay';
        overlay.style.cssText = `
      position: fixed; inset: 0; background: rgba(0,0,0,.55);
      display: flex; align-items: center; justify-content: center;
      z-index: 9999; color: white; font-size: 18px; font-weight: 600;
    `;
        overlay.innerHTML = `<div style="text-align:center;">
      <div class="spinner" style="
        width: 40px; height: 40px; border: 4px solid rgba(255,255,255,0.3);
        border-top: 4px solid #3b82f6; border-radius: 50%; 
        margin: 0 auto 10px auto; animation: spin 1s linear infinite;">
      </div>
      <div>${msg}</div>
    </div>`;
        document.body.appendChild(overlay);
    } else {
        overlay.querySelector('div:last-child').textContent = msg;
        overlay.style.display = 'flex';
    }
}

function hideOverlay() {
    const overlay = document.getElementById('progressOverlay');
    if (overlay) overlay.style.display = 'none';
}

/* Spinner animation */
const style = document.createElement('style');
style.innerHTML = `
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}`;
document.head.appendChild(style);

/* -------------------------------
   Utility – Query Params
--------------------------------*/
function getParam(name) {
    return new URLSearchParams(window.location.search).get(name);
}

/* -------------------------------
   Helper – Validate email/phone
--------------------------------*/
function isValidEmail(email) {
    return /^[^@]+@[^@]+\.[^@]+$/.test(email);
}

function isValidPhone(phone) {
    return /^\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}$/.test(phone);
}

/* -------------------------------
   Hook overlay into form submits
--------------------------------*/
document.addEventListener('submit', (e) => {
    const btn = e.submitter;
    if (btn && btn.classList.contains('primary')) {
        showOverlay(btn.textContent.includes('Update') ? 'Updating...' : 'Saving...');
    }
});
window.addEventListener('load', hideOverlay);
window.addEventListener('pageshow', hideOverlay);

/* -------------------------------
   Export globally
--------------------------------*/
window.api = api;
window.setStatus = setStatus;
window.requireAuth = requireAuth;
window.logout = logout;
window.fmtMoney = fmtMoney;
window.showOverlay = showOverlay;
window.hideOverlay = hideOverlay;
window.getParam = getParam;
window.isValidEmail = isValidEmail;
window.isValidPhone = isValidPhone;
