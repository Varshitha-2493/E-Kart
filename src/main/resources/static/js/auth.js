const Auth = {
  key: 'ekart_user',

  getUser() {
    try {
      return JSON.parse(localStorage.getItem(this.key));
    } catch {
      return null;
    }
  },

  setUser(user) {
    localStorage.setItem(this.key, JSON.stringify(user));
  },

  clear() {
    localStorage.removeItem(this.key);
  },

  requireLogin(redirectTo = '/pages/login.html') {
    const user = this.getUser();
    if (!user) {
      window.location.href = redirectTo;
      return null;
    }
    return user;
  },

  requireAdmin() {
    const user = this.requireLogin();
    if (!user) return null;
    if (user.role !== 'ADMIN') {
      toast('Admin access only', 'error');
      window.location.href = '/index.html';
      return null;
    }
    return user;
  }
};

function renderNav(active = '', adminMode = false) {
  const user = Auth.getUser();
  const nav = document.querySelector('[data-nav]');
  if (!nav) return;

  const links = adminMode
    ? [
        { href: '/pages/admin.html', label: 'Dashboard', key: 'admin' },
        { href: '/pages/admin.html#products', label: 'Products', key: 'products' },
        { href: '/pages/admin.html#orders', label: 'Orders', key: 'orders' }
      ]
    : [
        { href: '/index.html', label: 'Home', key: 'shop' },
        { href: '/index.html#catalog', label: 'Categories', key: 'categories' },
        { href: '/pages/assistant.html', label: 'AI Assistant', key: 'ai' },
        { href: '/pages/cart.html', label: 'Cart', key: 'cart' },
        { href: '/pages/orders.html', label: 'Orders', key: 'orders' }
      ];

  if (!adminMode && user?.role === 'ADMIN') {
    links.push({ href: '/pages/admin.html', label: 'Admin', key: 'admin' });
  }

  const linkHtml = links
    .map(
      (l) =>
        `<a href="${l.href}" class="${active === l.key ? 'active' : ''}">${l.label}</a>`
    )
    .join('');

  const authHtml = user
    ? `<span class="nav-user">Hi, ${user.name.split(' ')[0]}</span>
       <a href="#" id="logoutBtn">Logout</a>`
    : `<a href="/pages/login.html">Login</a>
       <a href="/pages/register.html">Register</a>`;

  nav.innerHTML = `${linkHtml}${authHtml}`;

  const logoutBtn = document.getElementById('logoutBtn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', (e) => {
      e.preventDefault();
      Auth.clear();
      window.location.href = '/index.html';
    });
  }
}
