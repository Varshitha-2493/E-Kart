async function loadCategories() {
  const categories = await API.get('/api/categories');
  const select = qs('categoryFilter');
  if (!select) return categories;
  select.innerHTML =
    '<option value="">All categories</option>' +
    categories.map((c) => `<option value="${c.id}">${c.name}</option>`).join('');
  return categories;
}

function productCard(p) {
  const img = p.imageUrl || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600';
  return `
    <article class="product">
      <a href="/pages/product.html?id=${p.id}" class="product-img" style="background-image:url('${img}')"></a>
      <div class="product-body">
        <h3><a href="/pages/product.html?id=${p.id}">${p.name}</a></h3>
        <div class="meta">${p.category?.name || 'General'} · ★ ${p.averageRating ?? '—'}</div>
        <div class="price">${formatINR(p.price)}</div>
        <div class="meta">Stock: ${p.stock}</div>
        <div class="product-actions">
          <button class="btn btn-primary btn-sm" data-add="${p.id}">Add</button>
          <a class="btn btn-secondary btn-sm" href="/pages/product.html?id=${p.id}">View</a>
        </div>
      </div>
    </article>`;
}

async function loadProducts() {
  const params = new URLSearchParams();
  const q = qs('searchInput')?.value?.trim();
  const categoryId = qs('categoryFilter')?.value;
  const minPrice = qs('minPrice')?.value;
  const maxPrice = qs('maxPrice')?.value;
  const minRating = qs('minRating')?.value;
  const inStockOnly = qs('inStockOnly')?.checked;

  if (q) params.set('q', q);
  if (categoryId) params.set('categoryId', categoryId);
  if (minPrice) params.set('minPrice', minPrice);
  if (maxPrice) params.set('maxPrice', maxPrice);
  if (minRating) params.set('minRating', minRating);
  if (inStockOnly) params.set('inStockOnly', 'true');

  const products = await API.get(`/api/products/filter?${params.toString()}`);
  const grid = qs('productGrid');
  if (!products.length) {
    grid.innerHTML = '<div class="empty panel">No products match your filters.</div>';
    return;
  }
  grid.innerHTML = products.map(productCard).join('');
  grid.querySelectorAll('[data-add]').forEach((btn) => {
    btn.addEventListener('click', () => addToCart(Number(btn.dataset.add)));
  });
}

async function addToCart(productId, quantity = 1) {
  const user = Auth.requireLogin('/pages/login.html');
  if (!user) return;
  try {
    await API.post(
      `/api/cart/add?userId=${user.id}&productId=${productId}&quantity=${quantity}`,
      {}
    );
    toast('Added to cart');
  } catch (err) {
    toast(err.message, 'error');
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  renderNav('shop');
  try {
    await loadCategories();
    await loadProducts();
  } catch (err) {
    toast(err.message, 'error');
  }

  qs('applyFilters')?.addEventListener('click', () =>
    loadProducts().catch((e) => toast(e.message, 'error'))
  );
  qs('searchInput')?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') loadProducts().catch((err) => toast(err.message, 'error'));
  });
});
