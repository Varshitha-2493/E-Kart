document.addEventListener('DOMContentLoaded', () => {
  renderNav('');

  qs('loginForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      const data = await API.post('/api/auth/login', {
        email: qs('email').value,
        password: qs('password').value
      });
      Auth.setUser({
        id: data.id,
        name: data.name,
        email: data.email,
        role: data.role
      });
      toast('Welcome back');
      window.location.href = data.role === 'ADMIN' ? '/pages/admin.html' : '/index.html';
    } catch (err) {
      toast(err.message, 'error');
    }
  });

  qs('registerForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      const data = await API.post('/api/auth/register', {
        name: qs('name').value,
        email: qs('email').value,
        password: qs('password').value
      });
      Auth.setUser({
        id: data.id,
        name: data.name,
        email: data.email,
        role: data.role
      });
      toast('Account created');
      window.location.href = '/index.html';
    } catch (err) {
      toast(err.message, 'error');
    }
  });
});
