let allProducts = [];
let lastGoalResult = null;

function showTab(name) {
  document.querySelectorAll('[data-tab-panel]').forEach((el) => el.classList.add('hidden'));
  document.querySelectorAll('.tab').forEach((el) => el.classList.remove('active'));
  qs(`panel-${name}`)?.classList.remove('hidden');
  document.querySelector(`[data-tab="${name}"]`)?.classList.add('active');
}

function parseSpecs(product) {
  const text = product.specs || product.description || '';
  const parts = text.split('|').map((p) => p.trim()).filter(Boolean);
  if (parts.length) return parts.slice(0, 4);
  return [
    `Stock: ${product.stock}`,
    `Rating: ${product.averageRating ?? '—'}`,
    product.category?.name || 'General'
  ];
}

function renderCompareCards(products, recommendedId, points) {
  const grid = qs('compareCards');
  grid.innerHTML = products
    .map((p) => {
      const suggested = p.id === recommendedId;
      const specs = parseSpecs(p);
      return `
      <article class="compare-card ${suggested ? 'suggested' : ''}">
        ${suggested ? '<span class="compare-badge">Suggested</span>' : ''}
        <div class="compare-media" style="background-image:linear-gradient(0deg,rgba(215,228,248,.65),rgba(215,228,248,.65)),url('${p.imageUrl || ''}')">${p.name}</div>
        <div class="compare-body">
          <h3>${p.name}</h3>
          <div class="price">${formatINR(p.price)}</div>
          <ul class="spec-list">
            ${specs.map((s) => `<li>${s}</li>`).join('')}
            <li><strong>Rating</strong> ${p.averageRating ?? '—'} ★</li>
          </ul>
        </div>
      </article>`;
    })
    .join('');

  if (points?.length) {
    // points already shown in reason box; keep cards clean
  }
}

async function loadProductOptions() {
  allProducts = await API.get('/api/products');
  const options =
    '<option value="">Select product</option>' +
    allProducts.map((p) => `<option value="${p.id}">${p.name} (${formatINR(p.price)})</option>`).join('');
  [qs('compareA'), qs('compareB'), qs('compareC')].forEach((s) => {
    if (s) s.innerHTML = options;
  });
  if (qs('summaryProduct')) {
    qs('summaryProduct').innerHTML = allProducts
      .map((p) => `<option value="${p.id}">${p.name}</option>`)
      .join('');
  }
}

function renderGoalResult(result) {
  lastGoalResult = result;
  qs('goalResultTitle').textContent = 'Suggested cart for your goal';
  qs('goalResultSub').textContent = result.aiNotes || '';
  qs('goalCount').textContent = `${result.items.length} items`;
  qs('goalItems').innerHTML = result.items
    .map(
      (i) => `
      <div class="cart-row">
        <div><strong>${i.productName}</strong></div>
        <div class="qty">x${i.quantity}</div>
        <div class="why">${i.reason || ''}</div>
        <div class="amt">${formatINR(i.lineTotal)}</div>
      </div>`
    )
    .join('');

  qs('goalFooter').classList.remove('hidden');
  const ok = result.withinBudget && result.items.every((i) => i.inStock);
  qs('goalVerified').textContent = ok
    ? '✓ Verified: all in stock · within budget'
    : 'Checked against stock and budget';
  qs('goalVerified').className = `pill ${ok ? 'pill-ok' : 'pill-count'}`;
  qs('goalTotalText').textContent =
    `Total ${formatINR(result.total)} of ${formatINR(result.budget)}`;
}

document.addEventListener('DOMContentLoaded', async () => {
  renderNav('ai');
  try {
    await loadProductOptions();
  } catch (err) {
    toast(err.message, 'error');
  }

  document.querySelectorAll('[data-tab]').forEach((btn) => {
    btn.addEventListener('click', () => showTab(btn.dataset.tab));
  });
  showTab('goal');

  qs('goalForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      const result = await API.post('/api/ai/goal-to-cart', {
        goal: qs('goalInput').value,
        budget: Number(qs('budgetInput').value),
        userId: Auth.getUser()?.id || null,
        addToCart: false
      });
      renderGoalResult(result);
      toast('Cart suggestion ready');
    } catch (err) {
      toast(err.message, 'error');
    }
  });

  qs('addGoalCartBtn')?.addEventListener('click', async () => {
    const user = Auth.requireLogin();
    if (!user || !lastGoalResult) return;
    try {
      const result = await API.post('/api/ai/goal-to-cart', {
        goal: lastGoalResult.goal,
        budget: lastGoalResult.budget,
        userId: user.id,
        addToCart: true
      });
      renderGoalResult(result);
      toast('Added verified items to cart');
    } catch (err) {
      toast(err.message, 'error');
    }
  });

  qs('compareForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const ids = [qs('compareA').value, qs('compareB').value, qs('compareC').value]
      .filter(Boolean)
      .map(Number);
    try {
      const result = await API.post('/api/ai/compare', {
        productIds: ids,
        preference: qs('preferenceInput').value
      });
      const selected = allProducts.filter((p) => ids.includes(p.id));
      renderCompareCards(selected, result.recommendedProductId, result.comparisonPoints);
      qs('compareExplain').classList.remove('hidden');
      const points = (result.comparisonPoints || []).join(' ');
      qs('compareReason').textContent =
        `${result.reasoning}${points ? ' ' + points : ''} Suggestion: ${result.recommendedProductName}.`;
    } catch (err) {
      toast(err.message, 'error');
    }
  });

  qs('summaryForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      const productId = Number(qs('summaryProduct').value);
      const result = await API.get(`/api/ai/review-summary/${productId}`);
      qs('summaryResult').classList.remove('hidden');
      qs('summaryTitle').textContent =
        `AI review summary · based on ${result.reviewCount} reviews`;
      qs('summaryOverallText').textContent = result.overall;
      qs('summaryLikesList').innerHTML = result.likes.map((x) => `<li>${x}</li>`).join('');
      qs('summaryComplaintsList').innerHTML = result.complaints
        .map((x) => `<li>${x}</li>`)
        .join('');
    } catch (err) {
      toast(err.message, 'error');
    }
  });
});
