async function loadCart() {
  const user = Auth.requireLogin();
  if (!user) return;

  const items = await API.get(`/api/cart/${user.id}`);
  const body = qs('cartBody');
  if (!items.length) {
    body.innerHTML = '<tr><td colspan="5" class="empty">Your cart is empty.</td></tr>';
    qs('cartTotal').textContent = formatINR(0);
    return;
  }

  let total = 0;
  body.innerHTML = items
    .map((item) => {
      total += Number(item.total || 0);
      return `
      <tr>
        <td>${item.productName}</td>
        <td>${formatINR(item.price)}</td>
        <td>
          <input type="number" min="1" value="${item.quantity}" data-qty="${item.productId}" style="width:70px">
        </td>
        <td>${formatINR(item.total)}</td>
        <td>
          <button class="btn btn-secondary btn-sm" data-update="${item.productId}">Update</button>
          <button class="btn btn-danger btn-sm" data-remove="${item.productId}">Remove</button>
        </td>
      </tr>`;
    })
    .join('');

  qs('cartTotal').textContent = formatINR(total);

  body.querySelectorAll('[data-update]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const id = btn.dataset.update;
      const qty = Number(body.querySelector(`[data-qty="${id}"]`).value);
      try {
        await API.put(`/api/cart/${user.id}/${id}?quantity=${qty}`);
        toast('Cart updated');
        await loadCart();
      } catch (err) {
        toast(err.message, 'error');
      }
    });
  });

  body.querySelectorAll('[data-remove]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      try {
        await API.delete(`/api/cart/${user.id}/${btn.dataset.remove}`);
        toast('Removed');
        await loadCart();
      } catch (err) {
        toast(err.message, 'error');
      }
    });
  });
}

document.addEventListener('DOMContentLoaded', async () => {
  renderNav('cart');
  try {
    await loadCart();
  } catch (err) {
    toast(err.message, 'error');
  }

  qs('checkoutForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const user = Auth.getUser();
    try {
      const order = await API.post(`/api/orders/${user.id}/checkout`, {
        shippingAddress: qs('addressInput').value
      });
      toast(`Order #${order.id} placed`);
      window.location.href = '/pages/orders.html';
    } catch (err) {
      toast(err.message, 'error');
    }
  });
});
