let cachedProducts = [];

async function loadAdminProducts() {
  cachedProducts = await API.get('/api/products');
  const categories = await API.get('/api/categories');
  qs('productCategory').innerHTML = categories
    .map((c) => `<option value="${c.id}">${c.name}</option>`)
    .join('');

  qs('adminProducts').innerHTML = cachedProducts
    .map(
      (p) => `<tr>
      <td>${p.id}</td>
      <td>${p.name}</td>
      <td>${formatINR(p.price)}</td>
      <td>${p.stock}</td>
      <td>
        <button class="btn btn-outline btn-sm" data-edit="${p.id}">Edit</button>
        <button class="btn btn-danger btn-sm" data-del="${p.id}">Delete</button>
      </td>
    </tr>`
    )
    .join('');

  qs('adminProducts').querySelectorAll('[data-del]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      try {
        await API.delete(`/api/products/${btn.dataset.del}`);
        toast('Deleted');
        await loadAdminProducts();
      } catch (err) {
        toast(err.message, 'error');
      }
    });
  });

  qs('adminProducts').querySelectorAll('[data-edit]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const p = cachedProducts.find((x) => String(x.id) === btn.dataset.edit);
      if (!p) return;
      qs('productId').value = p.id;
      qs('productName').value = p.name;
      qs('productDesc').value = p.description || '';
      qs('productSpecs').value = p.specs || '';
      qs('productPrice').value = p.price;
      qs('productStock').value = p.stock;
      qs('productImage').value = p.imageUrl || '';
      qs('productCategory').value = p.category?.id || '';
    });
  });
}

async function loadAdminOrders() {
  const orders = await API.get('/api/orders/admin/all');
  qs('adminOrders').innerHTML = orders
    .map(
      (o) => `<tr>
      <td>#${o.id}</td>
      <td>User ${o.userId}</td>
      <td>${formatINR(o.totalAmount)}</td>
      <td>
        <select data-status="${o.id}">
          ${['PLACED', 'PACKED', 'SHIPPED', 'DELIVERED', 'CANCELLED']
            .map((s) => `<option value="${s}" ${s === o.status ? 'selected' : ''}>${s}</option>`)
            .join('')}
        </select>
      </td>
      <td><button class="btn btn-primary btn-sm" data-save-status="${o.id}">Save</button></td>
    </tr>`
    )
    .join('');

  qs('adminOrders').querySelectorAll('[data-save-status]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const id = btn.dataset.saveStatus;
      const status = qs('adminOrders').querySelector(`[data-status="${id}"]`).value;
      try {
        await API.put(`/api/orders/admin/${id}/status`, { status });
        toast('Status updated');
      } catch (err) {
        toast(err.message, 'error');
      }
    });
  });
}

async function loadInsights() {
  const data = await API.get('/api/ai/admin/insights');
  qs('insightsBox').classList.remove('hidden');
  qs('kpiRevenue').textContent = formatINR(data.totalRevenue);
  qs('kpiOrders').textContent = data.totalOrders;
  qs('kpiLow').textContent = data.lowStockCount;
  qs('kpiPending').textContent = data.pendingOrders;
  qs('insightSummary').textContent = data.aiSummary;

  const maxSale = Math.max(
    1,
    ...(data.salesByCategory || []).map((c) => Number(c.amount || 0))
  );
  qs('salesBars').innerHTML = (data.salesByCategory || []).length
    ? data.salesByCategory
        .map(
          (c) => `
      <div class="bar-row">
        <div class="bar-label"><span>${c.category}</span><span>${formatINR(c.amount)}</span></div>
        <div class="bar-track"><div class="bar-fill" style="width:${(Number(c.amount) / maxSale) * 100}%"></div></div>
      </div>`
        )
        .join('')
    : '<p class="muted">No sales data yet. Place a few orders first.</p>';

  qs('bestSellers').innerHTML = (data.topProducts || []).length
    ? data.topProducts
        .map((p) => `<li><span>${p.name}</span><span>${p.count} sold</span></li>`)
        .join('')
    : '<li class="muted">No sales yet</li>';

  qs('lowStockList').innerHTML = (data.lowStockItems || []).length
    ? data.lowStockItems
        .map((p) => `<li><span>${p.name}</span><strong>${p.count} left</strong></li>`)
        .join('')
    : '<li class="muted">Stock looks healthy</li>';
}

document.addEventListener('DOMContentLoaded', async () => {
  renderNav('admin', true);
  if (!Auth.requireAdmin()) return;

  try {
    await loadAdminProducts();
    await loadAdminOrders();
    await loadInsights().catch(() => {
      /* AI may fail without key; products/orders still load */
    });
  } catch (err) {
    toast(err.message, 'error');
  }

  qs('productForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
      name: qs('productName').value,
      description: qs('productDesc').value,
      specs: qs('productSpecs').value,
      price: Number(qs('productPrice').value),
      stock: Number(qs('productStock').value),
      imageUrl: qs('productImage').value,
      category: { id: Number(qs('productCategory').value) }
    };
    const id = qs('productId').value;
    try {
      if (id) await API.put(`/api/products/${id}`, payload);
      else await API.post('/api/products', payload);
      toast(id ? 'Product updated' : 'Product created');
      qs('productForm').reset();
      qs('productId').value = '';
      await loadAdminProducts();
    } catch (err) {
      toast(err.message, 'error');
    }
  });

  qs('insightsBtn')?.addEventListener('click', async () => {
    try {
      await loadInsights();
      toast('Insights ready');
    } catch (err) {
      toast(err.message, 'error');
    }
  });

  qs('resetProduct')?.addEventListener('click', () => {
    qs('productForm').reset();
    qs('productId').value = '';
  });
});
