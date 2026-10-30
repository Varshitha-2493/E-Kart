const API = {
  async request(path, options = {}) {
    const headers = {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    };
    const user = Auth.getUser();
    if (user?.id) {
      headers['X-User-Id'] = String(user.id);
    }

    const res = await fetch(path, { ...options, headers });
    const text = await res.text();
    let data = null;
    try {
      data = text ? JSON.parse(text) : null;
    } catch {
      data = text;
    }

    if (!res.ok) {
      const message =
        (data && data.error) ||
        (typeof data === 'string' ? data : null) ||
        `Request failed (${res.status})`;
      throw new Error(message);
    }
    return data;
  },

  get(path) {
    return this.request(path);
  },

  post(path, body) {
    return this.request(path, { method: 'POST', body: JSON.stringify(body) });
  },

  put(path, body) {
    return this.request(path, { method: 'PUT', body: body ? JSON.stringify(body) : null });
  },

  delete(path) {
    return this.request(path, { method: 'DELETE' });
  }
};

function formatINR(value) {
  const num = Number(value || 0);
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0
  }).format(num);
}

function toast(message, type = 'ok') {
  const existing = document.querySelector('.toast');
  if (existing) existing.remove();
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.textContent = message;
  document.body.appendChild(el);
  setTimeout(() => el.remove(), 3200);
}

function qs(id) {
  return document.getElementById(id);
}
