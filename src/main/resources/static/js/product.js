function getProductId() {
  return Number(new URLSearchParams(window.location.search).get('id'));
}

async function addToCart(productId, quantity = 1) {
  const user = Auth.requireLogin();
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

async function loadProductPage() {
  const id = getProductId();
  if (!id) {
    toast('Product not found', 'error');
    return;
  }

  const [product, reviews] = await Promise.all([
    API.get(`/api/products/${id}`),
    API.get(`/api/reviews/product/${id}`)
  ]);

  qs('productName').textContent = product.name;
  qs('productMeta').textContent =
    `${product.averageRating ?? '—'} ★ · ${reviews.length} reviews`;
  qs('productPrice').textContent = formatINR(product.price);
  qs('productDesc').textContent = product.description || '';
  qs('productSpecs').textContent = product.specs || 'No specs listed';

  const media = qs('productImage');
  if (product.imageUrl) {
    media.style.backgroundImage =
      `linear-gradient(0deg, rgba(215,228,248,.35), rgba(215,228,248,.35)), url('${product.imageUrl}')`;
    media.textContent = '';
  } else {
    media.textContent = product.name;
  }

  qs('compareLink').href = `/pages/assistant.html`;

  const list = qs('reviewList');
  if (!reviews.length) {
    list.innerHTML = '<div class="card muted">No reviews yet. Be the first.</div>';
  } else {
    list.innerHTML = reviews
      .map(
        (r) => `
      <div class="card" style="margin-bottom:0.7rem;padding:0.9rem">
        <strong>${r.userName}</strong> · ★ ${r.rating}
        <p class="muted" style="margin:0.35rem 0 0">${r.comment}</p>
      </div>`
      )
      .join('');
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  renderNav('shop');
  try {
    await loadProductPage();
  } catch (err) {
    toast(err.message, 'error');
  }

  qs('addCartBtn')?.addEventListener('click', async () => {
    await addToCart(getProductId(), Number(qs('qtyInput').value || 1));
  });

  qs('summaryBtn')?.addEventListener('click', async () => {
    try {
      const data = await API.get(`/api/ai/review-summary/${getProductId()}`);
      qs('summaryBox').classList.remove('hidden');
      qs('summaryHeading').textContent =
        `AI review summary · based on ${data.reviewCount} reviews`;
      qs('summaryOverall').textContent = data.overall;
      qs('summaryLikes').innerHTML = data.likes.map((x) => `<li>${x}</li>`).join('');
      qs('summaryComplaints').innerHTML = data.complaints
        .map((x) => `<li>${x}</li>`)
        .join('');
      qs('summaryBox').scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (err) {
      toast(err.message, 'error');
    }
  });

  qs('reviewForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const user = Auth.requireLogin();
    if (!user) return;
    try {
      await API.post(`/api/reviews/product/${getProductId()}`, {
        userId: user.id,
        rating: Number(qs('ratingInput').value),
        comment: qs('commentInput').value
      });
      toast('Review submitted');
      qs('commentInput').value = '';
      await loadProductPage();
    } catch (err) {
      toast(err.message, 'error');
    }
  });
});
