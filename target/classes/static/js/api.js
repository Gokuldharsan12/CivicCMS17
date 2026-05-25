/* ── API helpers ───────────────────────────────────────────── */
const API = (() => {
  const BASE = '';   // same origin

  function getToken() { return localStorage.getItem('token') || ''; }

  function headers(json = true) {
    const h = { Authorization: 'Bearer ' + getToken() };
    if (json) h['Content-Type'] = 'application/json';
    return h;
  }

  async function request(method, url, body, multipart = false) {
    const opts = { method, headers: multipart ? { Authorization: 'Bearer ' + getToken() } : headers(!multipart) };
    if (body) opts.body = multipart ? body : JSON.stringify(body);
    const res = await fetch(BASE + url, opts);
    const text = await res.text();
    let data;
    try { data = text ? JSON.parse(text) : {}; } catch { data = { error: text }; }
    if (!res.ok) throw { status: res.status, message: data.error || 'Request failed', data };
    return data;
  }

  return {
    get:           (url)           => request('GET',    url),
    post:          (url, body)     => request('POST',   url, body),
    patch:         (url, body)     => request('PATCH',  url, body),
    postForm:      (url, formData) => request('POST',   url, formData, true),
    getToken,
    setSession:    (token, name, email, role) => {
      localStorage.setItem('token', token);
      localStorage.setItem('name',  name);
      localStorage.setItem('email', email);
      localStorage.setItem('role',  role);
    },
    clearSession: () => {
      ['token','name','email','role'].forEach(k => localStorage.removeItem(k));
    },
    isAdmin:    () => localStorage.getItem('role') === 'ADMIN',
    isDeptHead: () => localStorage.getItem('role') === 'DEPT_HEAD',
    isLogged:   () => !!localStorage.getItem('token'),
    getName:    () => localStorage.getItem('name') || '',
    getEmail:   () => localStorage.getItem('email') || '',
    getRole:    () => localStorage.getItem('role') || '',
  };
})();

/* ── UI helpers ────────────────────────────────────────────── */
function showSpinner()  { document.getElementById('spinner')?.classList.remove('hidden'); }
function hideSpinner()  { document.getElementById('spinner')?.classList.add('hidden'); }

function showAlert(id, msg, type = 'error') {
  const el = document.getElementById(id);
  if (!el) return;
  el.className = 'alert alert-' + type;
  el.textContent = msg;
  el.style.display = 'flex';
}
function hideAlert(id) {
  const el = document.getElementById(id);
  if (el) el.style.display = 'none';
}

function priorityBadge(p) {
  const map = { LOW:'badge-low', MEDIUM:'badge-medium', HIGH:'badge-high', CRITICAL:'badge-critical' };
  return `<span class="badge ${map[p] || ''}">${p}</span>`;
}
function statusBadge(s) {
  return `<span class="badge badge-${s?.toLowerCase()}">${s?.replace('_',' ')}</span>`;
}
function fmtDate(dt) {
  if (!dt) return '—';
  return new Date(dt).toLocaleString('en-IN', { dateStyle:'medium', timeStyle:'short' });
}
