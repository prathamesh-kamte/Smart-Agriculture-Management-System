import './style.css';
import { api } from './api.js';

// --- GLOBAL STATE ---
let activePanel = 'dashboard';
let cropsList = [];

// --- TOAST NOTIFICATIONS ---
function showToast(message, type = 'success') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  
  let iconSvg = '';
  if (type === 'success') {
    iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--colors-trading-up)" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>`;
  } else if (type === 'error') {
    iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--colors-trading-down)" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>`;
  } else {
    iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--colors-primary)" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="8" y1="12" x2="12" y2="12"></line></svg>`;
  }

  toast.innerHTML = `
    ${iconSvg}
    <span>${message}</span>
  `;

  container.appendChild(toast);

  // Auto remove
  setTimeout(() => {
    toast.style.animation = 'slideIn 0.2s ease reverse forwards';
    toast.addEventListener('animationend', () => toast.remove());
  }, 3500);
}

// --- MODAL CONTROLLERS ---
function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.add('active');
  }
}

function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.remove('active');
  }
}

// --- VIEW ROUTING ---
function syncAuthViews() {
  const viewAuth = document.getElementById('view-auth');
  const viewApp = document.getElementById('view-app');
  const loader = document.getElementById('app-loader');

  if (loader) loader.style.display = 'none';

  if (api.isAuthenticated()) {
    if (viewAuth) viewAuth.style.display = 'none';
    if (viewApp) viewApp.style.display = 'flex';
    
    // Bind profile sidebar badge details
    const user = api.getCurrentUser();
    if (user) {
      document.getElementById('display-user-name').textContent = user.fullName;
      document.getElementById('display-user-role').textContent = user.role || 'FARMER';
      
      const initials = user.fullName
        .split(' ')
        .map(n => n[0])
        .slice(0, 2)
        .join('')
        .toUpperCase();
      document.getElementById('avatar-letters').textContent = initials || 'US';
    }
    
    navigateToPanel(activePanel);
  } else {
    if (viewApp) viewApp.style.display = 'none';
    if (viewAuth) viewAuth.style.display = 'flex';
  }
}

async function navigateToPanel(panelName) {
  activePanel = panelName;
  
  // Highlight sidebar link
  document.querySelectorAll('.sidebar-link').forEach(link => {
    if (link.dataset.panel === panelName) {
      link.classList.add('active');
    } else {
      link.classList.remove('active');
    }
  });

  // Switch panels
  document.querySelectorAll('.app-panel').forEach(panel => {
    if (panel.id === `panel-${panelName}`) {
      panel.classList.add('active');
    } else {
      panel.classList.remove('active');
    }
  });

  try {
    if (panelName === 'dashboard') {
      await loadDashboardData();
    } else if (panelName === 'crops') {
      await loadCropsData();
    } else if (panelName === 'expenses') {
      await loadExpensesData();
    } else if (panelName === 'advisories') {
      await loadAdvisoriesData();
    } else if (panelName === 'profile') {
      await loadProfileData();
    }
  } catch (error) {
    showToast(error.message, 'error');
  }
}

// --- DYNAMIC SVG CHART RENDERER (BINANCE YELLOW BARS) ---
function renderExpenseChart(trends = []) {
  const container = document.getElementById('expense-chart-wrapper');
  if (!container) return;

  if (!trends || trends.length === 0) {
    container.innerHTML = `
      <div style="display:flex; align-items:center; justify-content:center; height:100%; color:var(--colors-muted); font-size:0.9rem;">
        No investment trend records available.
      </div>
    `;
    return;
  }

  const width = 600;
  const height = 280;
  const paddingLeft = 50;
  const paddingRight = 20;
  const paddingTop = 25;
  const paddingBottom = 35;

  const chartWidth = width - paddingLeft - paddingRight;
  const chartHeight = height - paddingTop - paddingBottom;

  const maxAmount = Math.max(...trends.map(t => t.totalAmount || 0), 1000);
  const roundedMax = Math.ceil(maxAmount / 1000) * 1000;

  const steps = 4;
  let gridLinesHtml = '';
  let yAxisLabelsHtml = '';
  for (let i = 0; i <= steps; i++) {
    const yVal = paddingTop + (chartHeight / steps) * i;
    const amountVal = roundedMax - (roundedMax / steps) * i;
    gridLinesHtml += `<line class="chart-grid-line" x1="${paddingLeft}" y1="${yVal}" x2="${width - paddingRight}" y2="${yVal}" />`;
    yAxisLabelsHtml += `<text class="chart-text" x="${paddingLeft - 8}" y="${yVal + 3}" text-anchor="end">$${amountVal}</text>`;
  }

  const barCount = trends.length;
  const barSpacing = chartWidth / barCount;
  const barWidth = Math.min(barSpacing * 0.55, 38);
  let barsHtml = '';
  let xAxisLabelsHtml = '';

  trends.forEach((item, index) => {
    const amount = item.totalAmount || 0;
    const barHeight = (amount / roundedMax) * chartHeight;
    const xVal = paddingLeft + (index * barSpacing) + (barSpacing - barWidth) / 2;
    const yVal = height - paddingBottom - barHeight;

    barsHtml += `
      <g>
        <rect class="chart-bar" x="${xVal}" y="${yVal}" width="${barWidth}" height="${barHeight}" rx="2">
          <title>${item.month}: $${amount.toFixed(2)}</title>
        </rect>
        <text class="chart-text" x="${xVal + barWidth/2}" y="${yVal - 6}" text-anchor="middle" font-weight="600" font-size="10" fill="var(--colors-body-on-dark)">$${Math.round(amount)}</text>
      </g>
    `;

    xAxisLabelsHtml += `
      <text class="chart-text" x="${xVal + barWidth/2}" y="${height - paddingBottom + 18}" text-anchor="middle">
        ${item.month}
      </text>
    `;
  });

  container.innerHTML = `
    <svg class="svg-chart" viewBox="0 0 ${width} ${height}" preserveAspectRatio="xMidYMid meet">
      <g>${gridLinesHtml}</g>
      <g>${yAxisLabelsHtml}</g>
      <g>${barsHtml}</g>
      <g>${xAxisLabelsHtml}</g>
    </svg>
  `;
}

// --- PANEL LOADER FUNCTIONS ---

// 1. Dashboard
async function loadDashboardData() {
  const stats = await api.getDashboardStats();
  
  // Set stat values (Tabular Plex format)
  document.getElementById('stat-total-crops').textContent = stats.totalCrops;
  document.getElementById('stat-active-crops').textContent = stats.activeCrops;
  document.getElementById('stat-monthly-expenses').textContent = stats.totalExpensesThisMonth 
    ? `$${Number(stats.totalExpensesThisMonth).toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})}` 
    : '$0.00';
  
  const pendingCount = stats.pendingAdvisories;
  document.getElementById('stat-pending-advisories').textContent = pendingCount;
  
  const advBadge = document.getElementById('advisory-badge-count');
  if (pendingCount > 0) {
    advBadge.style.display = 'inline-block';
    advBadge.textContent = pendingCount;
  } else {
    advBadge.style.display = 'none';
  }

  // Render SVG chart
  renderExpenseChart(stats.monthlyExpenseTrend);

  // Load top active advisories
  const advPage = await api.getActiveAdvisories({ size: 3 });
  const quickAdvList = document.getElementById('quick-advisories-list');
  if (!advPage.content || advPage.content.length === 0) {
    quickAdvList.innerHTML = `<div style="color:var(--colors-muted); text-align:center; padding:1.5rem 0; font-size:0.9rem;">No active advisory logs.</div>`;
  } else {
    quickAdvList.innerHTML = advPage.content.map(adv => `
      <div class="advisory-card severity-${adv.severity.toLowerCase()}" style="padding: 1rem; margin-bottom: 0.4rem; border-radius: var(--rounded-md);">
        <div class="advisory-content">
          <div class="advisory-header-row">
            <span class="badge badge-${adv.severity.toLowerCase()}">${adv.severity}</span>
            <span class="advisory-title">${adv.title}</span>
          </div>
          <p style="font-size:0.85rem; color:var(--colors-body-on-dark); line-height:1.4; margin-bottom: 0.5rem;">${adv.message}</p>
          <div class="flex justify-between align-center">
            <span style="font-size:0.75rem; color:var(--colors-primary); font-weight:600;">${adv.cropName || 'Global'}</span>
            <button class="btn-acknowledge" data-adv-id="${adv.id}">Acknowledge</button>
          </div>
        </div>
      </div>
    `).join('');
    
    // Bind quick acknowledge clicks
    quickAdvList.querySelectorAll('.btn-acknowledge').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const advId = e.currentTarget.dataset.advId;
        try {
          await api.acknowledgeAdvisory(advId);
          showToast('Advisory marked acknowledged');
          loadDashboardData();
        } catch (err) {
          showToast(err.message, 'error');
        }
      });
    });
  }
}

// 2. Crops Inventory
async function loadCropsData() {
  const container = document.getElementById('crops-container');
  const loader = document.getElementById('crops-loading');
  
  if (container) container.innerHTML = '';
  if (loader) loader.style.display = 'flex';

  const status = document.getElementById('filter-crop-status').value;
  const season = document.getElementById('filter-crop-season').value;

  try {
    const response = await api.getCrops({ status, season });
    cropsList = response.content || [];

    if (loader) loader.style.display = 'none';

    if (cropsList.length === 0) {
      container.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 3rem 2rem; color: var(--colors-muted);" class="glass-card">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-bottom:0.75rem; opacity:0.5;"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
          <h3 style="color:var(--colors-on-dark);">No crop plots found</h3>
          <p style="margin-top:0.25rem; font-size:0.85rem;">Create a new plot to start tracking.</p>
        </div>
      `;
      return;
    }

    container.innerHTML = cropsList.map(crop => {
      const notesSnippet = crop.notes ? crop.notes : 'No extra notes recorded.';
      const plantingDate = new Date(crop.plantingDate).toLocaleDateString();
      const harvestDate = crop.expectedHarvestDate ? new Date(crop.expectedHarvestDate).toLocaleDateString() : 'N/A';
      
      // Secondary and trading indicators matching Binance specification
      let actionButtons = '';
      if (crop.status === 'PLANTED') {
        actionButtons = `<button class="btn-secondary transition-status" data-id="${crop.id}" data-next="GROWING">Growing</button>`;
      } else if (crop.status === 'GROWING') {
        actionButtons = `<button class="btn-secondary transition-status" data-id="${crop.id}" data-next="READY_FOR_HARVEST">Ready</button>`;
      } else if (crop.status === 'READY_FOR_HARVEST') {
        actionButtons = `<button class="btn-trading-up transition-status" data-id="${crop.id}" data-next="HARVESTED">Harvest Plot</button>`;
      }

      return `
        <div class="glass-card flex flex-col justify-between">
          <div>
            <div class="crop-card-header flex justify-between align-center">
              <div>
                <h4 class="crop-title">${crop.cropName}</h4>
                <span class="crop-type">${crop.cropType}</span>
              </div>
              <span class="badge badge-${crop.status.toLowerCase()}">${crop.status.replace(/_/g, ' ')}</span>
            </div>
            
            <div class="crop-meta-row">
              <span class="crop-meta-label">Season</span>
              <span class="badge badge-${crop.season.toLowerCase()}">${crop.season}</span>
            </div>
            <div class="crop-meta-row">
              <span class="crop-meta-label">Plot Size</span>
              <span class="crop-meta-val binance-plex">${crop.areaInAcres} Acres</span>
            </div>
            <div class="crop-meta-row">
              <span class="crop-meta-label">Planted</span>
              <span class="crop-meta-val binance-plex">${plantingDate}</span>
            </div>
            <div class="crop-meta-row">
              <span class="crop-meta-label">Harvest Target</span>
              <span class="crop-meta-val binance-plex">${harvestDate}</span>
            </div>
            
            <p style="font-size:0.85rem; color:var(--colors-muted); background:var(--colors-canvas-dark); padding:0.6rem; border-radius:var(--rounded-md); margin-top:1rem; border:1px solid var(--colors-hairline-on-dark);">
              ${notesSnippet}
            </p>
          </div>

          <div class="crop-actions">
            ${actionButtons}
            <button class="btn-secondary edit-crop" data-id="${crop.id}" style="padding:0; width:32px; height:32px; flex-grow:0;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 20h9M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg></button>
            <button class="btn-logout delete-crop" data-id="${crop.id}" style="padding:0; width:32px; height:32px; flex-grow:0; border-color:transparent;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg></button>
          </div>
        </div>
      `;
    }).join('');

    // Bind edit click
    container.querySelectorAll('.edit-crop').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = Number(e.currentTarget.dataset.id);
        const crop = cropsList.find(c => c.id === id);
        if (crop) {
          showCropModal(crop);
        }
      });
    });

    // Bind delete click
    container.querySelectorAll('.delete-crop').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const id = Number(e.currentTarget.dataset.id);
        if (confirm('Delete this crop plot and all related capital records?')) {
          try {
            await api.deleteCrop(id);
            showToast('Crop plot deleted');
            loadCropsData();
          } catch (err) {
            showToast(err.message, 'error');
          }
        }
      });
    });

    // Bind status transitions
    container.querySelectorAll('.transition-status').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const id = Number(e.currentTarget.dataset.id);
        const nextStatus = e.currentTarget.dataset.next;
        try {
          await api.updateCropStatus(id, nextStatus);
          showToast(`Transitioned to ${nextStatus.replace(/_/g, ' ')}`);
          loadCropsData();
        } catch (err) {
          showToast(err.message, 'error');
        }
      });
    });

  } catch (error) {
    if (loader) loader.style.display = 'none';
    showToast(error.message, 'error');
  }
}

// 3. Expense Tracker
async function loadExpensesData() {
  const tbody = document.getElementById('expenses-tbody');
  const cropSummaryList = document.getElementById('expenses-crop-summary-list');
  const category = document.getElementById('filter-expense-category').value;

  if (tbody) tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding:1.5rem 0;"><div class="loader" style="width:20px; height:20px; margin:0 auto;"></div></td></tr>`;
  if (cropSummaryList) cropSummaryList.innerHTML = `<div style="text-align:center; padding:1rem 0;"><div class="loader" style="width:16px; height:16px; margin:0 auto;"></div></div>`;

  try {
    const response = await api.getExpenses({ category });
    const expenses = response.content || [];

    if (cropsList.length === 0) {
      const cropsRes = await api.getCrops();
      cropsList = cropsRes.content || [];
    }

    // Render list table
    if (expenses.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding:2rem 0; color:var(--colors-muted);">No transaction logs recorded.</td></tr>`;
    } else {
      tbody.innerHTML = expenses.map(exp => `
        <tr data-id="${exp.id}">
          <td class="binance-plex">${new Date(exp.expenseDate).toLocaleDateString()}</td>
          <td>
            <strong>${exp.description}</strong>
            ${exp.receiptReference ? `<br><span style="font-size:0.7rem; color:var(--colors-muted); font-family:var(--font-mono);">Invoice: ${exp.receiptReference}</span>` : ''}
          </td>
          <td>${exp.cropName || 'Plot N/A'}</td>
          <td><span class="badge" style="background:var(--colors-surface-elevated-dark); color:var(--colors-body-on-dark);">${exp.category}</span></td>
          <td class="expense-amount">$${exp.amount.toFixed(2)}</td>
          <td style="text-align: right;">
            <button class="action-icon-btn delete delete-expense" data-id="${exp.id}">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
            </button>
          </td>
        </tr>
      `).join('');

      tbody.querySelectorAll('.delete-expense').forEach(btn => {
        btn.addEventListener('click', async (e) => {
          const id = Number(e.currentTarget.dataset.id);
          if (confirm('Delete this transaction log?')) {
            try {
              await api.deleteExpense(id);
              showToast('Transaction deleted');
              loadExpensesData();
            } catch (err) {
              showToast(err.message, 'error');
            }
          }
        });
      });
    }

    // Render crop summaries
    if (cropsList.length === 0) {
      cropSummaryList.innerHTML = `<div style="color:var(--colors-muted); font-size:0.85rem; text-align:center; padding:1rem 0;">No active plots.</div>`;
    } else {
      const summaries = await Promise.all(cropsList.map(async (crop) => {
        try {
          const total = await fetchTotalByCrop(crop.id);
          return { cropName: crop.cropName, status: crop.status, total: total };
        } catch {
          return { cropName: crop.cropName, status: crop.status, total: 0 };
        }
      }));

      cropSummaryList.innerHTML = summaries.map(sum => `
        <div class="flex justify-between align-center" style="padding:0.6rem 0.75rem; background:var(--colors-canvas-dark); border-radius:var(--rounded-md); border:1px solid var(--colors-hairline-on-dark);">
          <div>
            <strong style="font-size:0.85rem; color:var(--colors-on-dark);">${sum.cropName}</strong>
            <div style="font-size:0.7rem; color:var(--colors-muted); text-transform:capitalize;">${sum.status.toLowerCase()}</div>
          </div>
          <span class="binance-plex" style="font-weight:600; color:var(--colors-trading-up); font-size:0.95rem;">$${Number(sum.total).toFixed(2)}</span>
        </div>
      `).join('');
    }

  } catch (error) {
    showToast(error.message, 'error');
  }
}

// Wrapper for fetching crop total
async function fetchTotalByCrop(cropId) {
  const response = await fetch(`http://localhost:8080/api/expenses/crop/${cropId}/total`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  });
  if (!response.ok) return 0;
  const valText = await response.text();
  return parseFloat(valText) || 0;
}

// 4. Advisory Center
async function loadAdvisoriesData() {
  const container = document.getElementById('advisories-container');
  if (container) container.innerHTML = `<div style="grid-column: 1/-1; text-align:center; padding:3rem 0;"><div class="loader"></div></div>`;

  const severity = document.getElementById('filter-advisory-severity').value;

  try {
    const page = await api.getActiveAdvisories({ severity });
    const advisories = page.content || [];

    if (advisories.length === 0) {
      container.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 3rem 2rem; color: var(--colors-muted);" class="glass-card">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-bottom:0.75rem; opacity:0.5;"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          <h3 style="color:var(--colors-on-dark);">All advisories clear</h3>
          <p style="margin-top:0.25rem; font-size:0.85rem;">Rule engine generated no warnings for your ledger.</p>
        </div>
      `;
      return;
    }

    container.innerHTML = advisories.map(adv => `
      <div class="advisory-card severity-${adv.severity.toLowerCase()}">
        <div style="display:flex; gap:0.75rem; flex-grow:1; align-items:flex-start;">
          <div class="advisory-indicator">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
          </div>
          <div class="advisory-content">
            <div class="advisory-header-row">
              <span class="badge badge-${adv.severity.toLowerCase()}">${adv.severity}</span>
              ${adv.category ? `<span class="badge" style="background:var(--colors-surface-elevated-dark); color:var(--colors-body-on-dark);">${adv.category}</span>` : ''}
              <h4 class="advisory-title">${adv.title}</h4>
            </div>
            <p class="advisory-msg">${adv.message}</p>
            <div class="flex align-center gap-1">
              <span style="font-size:0.75rem; font-weight:600; color:var(--colors-primary);">${adv.cropName ? `Plot: ${adv.cropName}` : 'Global System'}</span>
              <span class="advisory-time binance-plex">• Generated ${new Date(adv.generatedAt).toLocaleString()}</span>
            </div>
          </div>
        </div>
        <button class="btn-acknowledge" data-adv-id="${adv.id}">Acknowledge</button>
      </div>
    `).join('');

    // Bind acknowledge clicks
    container.querySelectorAll('.btn-acknowledge').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const id = Number(e.currentTarget.dataset.advId);
        try {
          await api.acknowledgeAdvisory(id);
          showToast('Advisory acknowledged');
          loadAdvisoriesData();
        } catch (err) {
          showToast(err.message, 'error');
        }
      });
    });

  } catch (error) {
    showToast(error.message, 'error');
  }
}

// 5. User Profile
async function loadProfileData() {
  const profileForm = document.getElementById('form-profile');
  if (profileForm) profileForm.style.opacity = '0.5';

  try {
    const user = await api.getProfile();
    
    document.getElementById('profile-name').value = user.fullName;
    document.getElementById('profile-email').value = user.email;
    document.getElementById('profile-role').value = user.role || 'FARMER';
    
    if (profileForm) profileForm.style.opacity = '1';
  } catch (error) {
    showToast(error.message, 'error');
  }
}

// --- FORM MODALS ---

function showCropModal(crop = null) {
  const title = document.getElementById('crop-modal-title');
  const form = document.getElementById('form-crop');
  const statusGroup = document.getElementById('crop-status-field-group');

  form.reset();

  if (crop) {
    title.textContent = 'Configure Crop Plot';
    document.getElementById('crop-id-field').value = crop.id;
    document.getElementById('crop-name').value = crop.cropName;
    document.getElementById('crop-type').value = crop.cropType;
    document.getElementById('crop-season').value = crop.season;
    document.getElementById('crop-area').value = crop.areaInAcres;
    document.getElementById('crop-planting-date').value = crop.plantingDate;
    document.getElementById('crop-harvest-date').value = crop.expectedHarvestDate || '';
    
    statusGroup.style.display = 'block';
    document.getElementById('crop-status').value = crop.status;
  } else {
    title.textContent = 'Configure New Plot';
    document.getElementById('crop-id-field').value = '';
    statusGroup.style.display = 'none';
  }

  openModal('modal-crop');
}

async function showExpenseModal() {
  const select = document.getElementById('expense-crop-select');
  select.innerHTML = '<option value="" disabled selected>Loading Active plots...</option>';

  try {
    const cropsRes = await api.getCrops();
    const crops = cropsRes.content || [];
    cropsList = crops;

    if (crops.length === 0) {
      showToast('Create a Crop Plot before logging transaction allocations.', 'info');
      navigateToPanel('crops');
      return;
    }

    select.innerHTML = '<option value="" disabled selected>Select plot...</option>' + 
      crops.map(c => `<option value="${c.id}">${c.cropName} (${c.cropType})</option>`).join('');

    document.getElementById('expense-date').value = new Date().toISOString().split('T')[0];
    
    openModal('modal-expense');
  } catch (error) {
    showToast(error.message, 'error');
  }
}

// --- EVENT LISTENERS ---
document.addEventListener('DOMContentLoaded', () => {
  
  window.addEventListener('auth-change', syncAuthViews);

  // Toggle auth sections
  document.getElementById('link-to-register').addEventListener('click', (e) => {
    e.preventDefault();
    document.getElementById('auth-login-section').style.display = 'none';
    document.getElementById('auth-register-section').style.display = 'block';
  });

  document.getElementById('link-to-login').addEventListener('click', (e) => {
    e.preventDefault();
    document.getElementById('auth-register-section').style.display = 'none';
    document.getElementById('auth-login-section').style.display = 'block';
  });

  // Login Submit
  document.getElementById('form-login').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;
    
    try {
      await api.login(email, password);
      showToast('Authentication success');
    } catch (err) {
      showToast(err.message || 'Login failed. Verify credentials.', 'error');
    }
  });

  // Register Submit
  document.getElementById('form-register').addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = document.getElementById('register-name').value;
    const email = document.getElementById('register-email').value;
    const password = document.getElementById('register-password').value;

    try {
      await api.register(name, email, password);
      showToast('Registration success. Please sign in.', 'success');
      document.getElementById('form-register').reset();
      document.getElementById('link-to-login').click();
    } catch (err) {
      showToast(err.message || 'Registration failed.', 'error');
    }
  });

  // Logout Button
  document.getElementById('btn-logout-sidebar').addEventListener('click', () => {
    api.logout();
    showToast('Signed out');
  });

  // Tab navigation
  document.querySelectorAll('.sidebar-link').forEach(link => {
    link.addEventListener('click', (e) => {
      const panel = e.currentTarget.dataset.panel;
      if (panel) navigateToPanel(panel);
    });
  });

  // Modals close buttons
  document.querySelectorAll('[data-close]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const modal = e.currentTarget.closest('.modal-overlay');
      if (modal) modal.classList.remove('active');
    });
  });

  // Quick Action Buttons
  document.getElementById('btn-quick-add-crop').addEventListener('click', () => showCropModal());
  document.getElementById('btn-add-crop').addEventListener('click', () => showCropModal());
  
  document.getElementById('btn-add-expense').addEventListener('click', showExpenseModal);
  
  document.getElementById('btn-quick-generate-adv').addEventListener('click', async () => {
    try {
      showToast('Processing rule parameters...', 'info');
      await api.generateAdvisories();
      showToast('Advisories processed successfully');
      if (activePanel === 'dashboard') {
        loadDashboardData();
      } else if (activePanel === 'advisories') {
        loadAdvisoriesData();
      }
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  document.getElementById('btn-generate-advisories').addEventListener('click', async () => {
    try {
      showToast('Processing rule parameters...', 'info');
      await api.generateAdvisories();
      showToast('Advisories processed');
      loadAdvisoriesData();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  // Form Submit: Add/Edit Crop
  document.getElementById('form-crop').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('crop-id-field').value;
    const cropData = {
      cropName: document.getElementById('crop-name').value,
      cropType: document.getElementById('crop-type').value,
      season: document.getElementById('crop-season').value,
      areaInAcres: parseFloat(document.getElementById('crop-area').value),
      plantingDate: document.getElementById('crop-planting-date').value,
      expectedHarvestDate: document.getElementById('crop-harvest-date').value || null,
      notes: document.getElementById('crop-notes').value || null
    };

    if (id) {
      cropData.status = document.getElementById('crop-status').value;
      try {
        await api.updateCrop(id, cropData);
        showToast('Plot settings updated');
        closeModal('modal-crop');
        loadCropsData();
      } catch (err) {
        showToast(err.message, 'error');
      }
    } else {
      try {
        await api.createCrop(cropData);
        showToast('New plot configured');
        closeModal('modal-crop');
        loadCropsData();
      } catch (err) {
        showToast(err.message, 'error');
      }
    }
  });

  // Form Submit: Log Expense
  document.getElementById('form-expense').addEventListener('submit', async (e) => {
    e.preventDefault();
    const expenseData = {
      cropId: Number(document.getElementById('expense-crop-select').value),
      description: document.getElementById('expense-desc').value,
      category: document.getElementById('expense-category').value,
      amount: parseFloat(document.getElementById('expense-amount').value),
      expenseDate: document.getElementById('expense-date').value,
      receiptReference: document.getElementById('expense-receipt').value || null
    };

    try {
      await api.createExpense(expenseData);
      showToast('Transaction allocation logged');
      closeModal('modal-expense');
      
      if (activePanel === 'expenses') {
        loadExpensesData();
      } else if (activePanel === 'dashboard') {
        loadDashboardData();
      }
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  // Form Submit: Update Profile
  document.getElementById('form-profile').addEventListener('submit', async (e) => {
    e.preventDefault();
    const fullName = document.getElementById('profile-name').value;
    const email = document.getElementById('profile-email').value;

    try {
      await api.updateProfile({ fullName, email });
      showToast('Farmer profile updated');
      
      const user = api.getCurrentUser();
      if (user) {
        user.fullName = fullName;
        localStorage.setItem('user', JSON.stringify(user));
      }
      syncAuthViews();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  // Filter Changes
  document.getElementById('filter-crop-status').addEventListener('change', loadCropsData);
  document.getElementById('filter-crop-season').addEventListener('change', loadCropsData);
  document.getElementById('filter-expense-category').addEventListener('change', loadExpensesData);
  document.getElementById('filter-advisory-severity').addEventListener('change', loadAdvisoriesData);

  document.getElementById('view-all-advisories-link').addEventListener('click', (e) => {
    e.preventDefault();
    navigateToPanel('advisories');
  });

  syncAuthViews();
});
