document.addEventListener('DOMContentLoaded', async () => {
  renderNav('orders');
  const user = Auth.requireLogin();
  if (!user) return;

  try {
    const orders = await API.get(`/api/orders/${user.id}`);
    const wrap = qs('ordersList');
    if (!orders.length) {
      wrap.innerHTML = '<div class="empty panel">No orders yet.</div>';
      return;
    }

    wrap.innerHTML = orders
      .map(
        (o) => `
      <article class="panel" style="margin-bottom:1rem">
        <div class="section-head" style="margin-bottom:0.6rem">
          <div>
            <h2 style="font-size:1.25rem">Order #${o.id}</h2>
            <p>Placed ${new Date(o.createdAt).toLocaleString()} · ETA ${o.deliveryDate}</p>
          </div>
          <span class="badge">${o.status}</span>
        </div>
        <p class="muted">${o.shippingAddress}</p>
        <table class="table">
          <thead><tr><th>Item</th><th>Qty</th><th>Price</th><th>Total</th></tr></thead>
          <tbody>
            ${o.items
              .map(
                (i) => `<tr>
              <td>${i.productName}</td>
              <td>${i.quantity}</td>
              <td>${formatINR(i.priceAtPurchase)}</td>
              <td>${formatINR(i.total)}</td>
            </tr>`
              )
              .join('')}
          </tbody>
        </table>
        <strong>Total: ${formatINR(o.totalAmount)}</strong>
      </article>`
      )
      .join('');
  } catch (err) {
    toast(err.message, 'error');
  }
});
