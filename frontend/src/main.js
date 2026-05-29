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
    } else if (panelName === 'tasks') {
      await loadTasksData();
    } else if (panelName === 'analytics') {
      await loadAnalyticsData();
    } else if (panelName === 'market') {
      await loadMarketData();
    } else if (panelName === 'disease') {
      // Just fetch crops for the select dropdown
      await loadDiseaseScannerInitialData();
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

async function loadWeatherWidget(city = 'Pune') {
  try {
    const current = await api.getCurrentWeather(city);
    document.getElementById('weather-desc').textContent = current.description;
    
    let badgesHtml = '';
    if (current.isRainy) badgesHtml += `<span class="badge" style="background:#2196F3; color:white;">Rain</span>`;
    if (current.isFrosty) badgesHtml += `<span class="badge" style="background:#E0E0E0; color:#333;">Frost</span>`;
    if (current.temperature > 40) badgesHtml += `<span class="badge" style="background:#F44336; color:white;">Heat</span>`;

    document.getElementById('weather-current-stats').innerHTML = `
      <div style="background: var(--colors-surface-elevated-dark); padding: 0.5rem 1rem; border-radius: var(--rounded-md);">
        <span style="font-size: 1.5rem; font-weight: 700; color: var(--colors-primary);">${current.temperature}°C</span>
      </div>
      <div style="background: var(--colors-surface-elevated-dark); padding: 0.5rem 1rem; border-radius: var(--rounded-md);">
        <span style="color: var(--colors-muted); font-size: 0.85rem;">Humidity</span><br>
        <span style="font-weight: 600;">${current.humidity}%</span>
      </div>
      <div style="background: var(--colors-surface-elevated-dark); padding: 0.5rem 1rem; border-radius: var(--rounded-md);">
        <span style="color: var(--colors-muted); font-size: 0.85rem;">Wind</span><br>
        <span style="font-weight: 600;">${current.windSpeed} km/h</span>
      </div>
      ${badgesHtml ? `<div style="display:flex; gap:0.5rem; align-items:center;">${badgesHtml}</div>` : ''}
    `;

    const forecast = await api.getWeatherForecast(city);
    document.getElementById('weather-forecast-row').innerHTML = forecast.map(day => `
      <div style="background: var(--colors-canvas-dark); border: 1px solid var(--colors-hairline-on-dark); border-radius: var(--rounded-md); padding: 0.75rem; text-align: center; min-width: 90px;">
        <div style="font-size: 0.8rem; color: var(--colors-muted); margin-bottom: 0.25rem;">${new Date(day.date).toLocaleDateString(undefined, {weekday: 'short', month: 'short', day: 'numeric'})}</div>
        <div style="font-weight: 700; font-size: 1.1rem; color: var(--colors-on-dark);">${day.temperature}°C</div>
        <div style="font-size: 0.75rem; margin-top: 0.25rem; color: var(--colors-primary); text-transform: capitalize;">${day.description}</div>
      </div>
    `).join('');
  } catch (error) {
    document.getElementById('weather-desc').textContent = 'Failed to load weather.';
  }
}

async function loadDashboardMarketPrices() {
  const tbody = document.getElementById('dashboard-market-prices-tbody');
  try {
    const prices = await api.getAllMarketPrices();
    if (!prices || prices.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--colors-muted); padding: 1rem;">No market prices found.</td></tr>`;
      return;
    }
    // Show only top 5 in dashboard
    tbody.innerHTML = prices.slice(0, 5).map(p => `
      <tr>
        <td><strong>${p.cropName}</strong></td>
        <td>${p.market}, ${p.state}</td>
        <td>₹${p.minPrice}</td>
        <td>₹${p.maxPrice}</td>
        <td style="color: var(--colors-trading-up); font-weight: 600;">₹${p.modalPrice}</td>
      </tr>
    `).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--colors-trading-down); padding: 1rem;">Error loading prices</td></tr>`;
  }
}

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

  // Weather and Market
  const cityInput = document.getElementById('weather-city-input').value || 'Pune';
  loadWeatherWidget(cityInput);
  loadDashboardMarketPrices();

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
            <div class="crop-meta-row" style="margin-top: 0.5rem; padding-top: 0.5rem; border-top: 1px solid var(--colors-hairline-on-dark);">
              <span class="crop-meta-label" style="color:var(--colors-on-dark); font-weight:600;">Profit/Loss</span>
              <span class="crop-meta-val binance-plex" style="color: ${crop.profitLoss > 0 ? 'var(--colors-trading-up)' : (crop.profitLoss < 0 ? 'var(--colors-trading-down)' : 'gray')}; font-weight:700;">
                ₹${crop.profitLoss || 0}
              </span>
            </div>
            
            <p style="font-size:0.85rem; color:var(--colors-muted); background:var(--colors-canvas-dark); padding:0.6rem; border-radius:var(--rounded-md); margin-top:1rem; border:1px solid var(--colors-hairline-on-dark);">
              ${notesSnippet}
            </p>
          </div>

          <div class="crop-actions" style="flex-wrap: wrap;">
            ${actionButtons}
            <button class="btn-secondary btn-photos" data-id="${crop.id}" style="padding:0 0.5rem; height:32px; font-size:0.8rem;">Photos</button>
            <button class="btn-secondary btn-disease" data-id="${crop.id}" style="padding:0 0.5rem; height:32px; font-size:0.8rem;">Scan</button>
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

    // Bind Photos click
    container.querySelectorAll('.btn-photos').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = Number(e.currentTarget.dataset.id);
        document.getElementById('photo-crop-id').value = id;
        loadPhotosGrid(id);
        openModal('modal-photos');
      });
    });

    // Bind Disease click
    container.querySelectorAll('.btn-disease').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = Number(e.currentTarget.dataset.id);
        navigateToPanel('disease');
        // Wait a tick for the panel to load then set the dropdown
        setTimeout(() => {
          const select = document.getElementById('disease-crop-select');
          if (select) select.value = id;
        }, 300);
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
        <div style="display:flex; flex-direction:column; gap:0.5rem; min-width:140px;">
          <button class="btn-acknowledge" data-adv-id="${adv.id}">Acknowledge</button>
          <button class="btn-secondary btn-convert-task" data-adv-id="${adv.id}" style="padding: 0.25rem 0.75rem; font-size: 0.8rem; height: 32px;">Convert to Task</button>
        </div>
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

    // Bind convert to task clicks
    container.querySelectorAll('.btn-convert-task').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const id = Number(e.currentTarget.dataset.advId);
        try {
          await api.createTaskFromAdvisory(id);
          showToast('Task created from advisory');
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

// 6. Tasks
async function loadTasksData() {
  const loader = document.getElementById('tasks-loading');
  if (loader) loader.style.display = 'flex';
  
  const cols = {
    PENDING: document.getElementById('tasks-col-pending'),
    IN_PROGRESS: document.getElementById('tasks-col-in-progress'),
    COMPLETED: document.getElementById('tasks-col-completed')
  };
  
  Object.values(cols).forEach(col => col.innerHTML = '');

  try {
    const tasksPage = await api.getTasks();
    const tasks = tasksPage.content || [];
    
    if (loader) loader.style.display = 'none';
    
    tasks.forEach(task => {
      const col = cols[task.status] || cols.PENDING;
      const priorityColors = {
        LOW: 'gray', MEDIUM: 'blue', HIGH: 'orange', URGENT: 'red'
      };
      
      col.innerHTML += `
        <div class="elevated-container" style="padding: 0.75rem; border-left: 4px solid ${priorityColors[task.priority]};">
          <div class="flex justify-between align-center">
            <h4 style="font-size: 0.95rem; margin-bottom: 0.25rem;">${task.title}</h4>
            <span class="badge" style="font-size:0.6rem; padding:0.1rem 0.3rem;">${task.priority}</span>
          </div>
          ${task.description ? `<p style="font-size: 0.8rem; color: var(--colors-muted);">${task.description}</p>` : ''}
          <div style="font-size: 0.75rem; margin-top: 0.5rem; color: var(--colors-primary);">
            ${task.cropName ? `Crop: ${task.cropName}` : ''}
          </div>
          <div style="font-size: 0.75rem; margin-top: 0.25rem; color: var(--colors-muted);">
            Due: ${new Date(task.dueDate).toLocaleString()}
          </div>
          <div class="flex gap-1" style="margin-top: 0.75rem;">
            ${task.status !== 'COMPLETED' ? `<button class="btn-secondary btn-task-complete" data-id="${task.id}" style="padding: 0.2rem 0.5rem; font-size: 0.75rem; height: auto;">Complete</button>` : ''}
            <button class="btn-logout btn-task-delete" data-id="${task.id}" style="padding: 0.2rem 0.5rem; font-size: 0.75rem; height: auto; border:none; color: var(--colors-trading-down);">Delete</button>
          </div>
        </div>
      `;
    });
    
    // Bind task complete
    document.querySelectorAll('.btn-task-complete').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        try {
          await api.completeTask(e.currentTarget.dataset.id);
          showToast('Task marked completed');
          loadTasksData();
        } catch(err) {
          showToast(err.message, 'error');
        }
      });
    });
    
    // Bind task delete
    document.querySelectorAll('.btn-task-delete').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        if(confirm('Delete this task?')) {
          try {
            await api.deleteTask(e.currentTarget.dataset.id);
            showToast('Task deleted');
            loadTasksData();
          } catch(err) {
            showToast(err.message, 'error');
          }
        }
      });
    });
    
  } catch (err) {
    if (loader) loader.style.display = 'none';
    showToast(err.message, 'error');
  }
}

// 7. Analytics
async function loadAnalyticsData() {
  const loader = document.getElementById('analytics-loading');
  if (loader) loader.style.display = 'flex';
  
  try {
    const list = await api.getYieldAnalytics();
    if (loader) loader.style.display = 'none';
    
    const tbody = document.getElementById('analytics-tbody');
    tbody.innerHTML = list.map(item => {
      const isProfit = item.profitLoss > 0;
      const plColor = item.profitLoss === 0 ? 'gray' : (isProfit ? 'var(--colors-trading-up)' : 'var(--colors-trading-down)');
      return `
        <tr>
          <td><strong>${item.cropName}</strong></td>
          <td>${item.season}</td>
          <td>${item.expectedYieldKg || 0} kg</td>
          <td>${item.actualYieldKg || 0} kg</td>
          <td>${item.efficiencyPercentage || 0}%</td>
          <td>₹${item.totalRevenue || 0}</td>
          <td>₹${item.totalExpenses || 0}</td>
          <td style="color: ${plColor}; font-weight: bold;">₹${item.profitLoss || 0}</td>
          <td><span class="badge" style="background: ${plColor}; color: white;">${item.status}</span></td>
        </tr>
      `;
    }).join('');
    
    // Custom SVG Bar chart for Profit/Loss
    const chartWrapper = document.getElementById('analytics-chart-wrapper');
    if (list.length === 0) {
      chartWrapper.innerHTML = `<div style="text-align:center; color:var(--colors-muted); padding: 2rem;">No data for chart.</div>`;
      return;
    }
    
    const w = 800; const h = 300;
    const padding = { top: 20, right: 20, bottom: 40, left: 60 };
    const chartW = w - padding.left - padding.right;
    const chartH = h - padding.top - padding.bottom;
    
    const maxVal = Math.max(...list.map(i => Math.abs(i.profitLoss || 0)), 100);
    const scale = (chartH / 2) / maxVal;
    
    const barW = Math.min(chartW / list.length * 0.6, 50);
    
    let svgBars = '';
    let svgLabels = '';
    
    list.forEach((item, idx) => {
      const pl = item.profitLoss || 0;
      const bH = Math.abs(pl) * scale;
      const x = padding.left + (chartW / list.length) * idx + ((chartW / list.length) - barW) / 2;
      const y = pl >= 0 ? (padding.top + chartH/2 - bH) : (padding.top + chartH/2);
      const color = pl >= 0 ? 'var(--colors-trading-up)' : 'var(--colors-trading-down)';
      
      svgBars += `<rect x="${x}" y="${y}" width="${barW}" height="${bH}" fill="${color}" rx="2" />`;
      svgLabels += `<text x="${x + barW/2}" y="${padding.top + chartH + 20}" text-anchor="middle" font-size="12" fill="var(--colors-muted)">${item.cropName.substring(0, 10)}</text>`;
    });
    
    // Zero line
    const zeroY = padding.top + chartH/2;
    const zeroLine = `<line x1="${padding.left}" y1="${zeroY}" x2="${w - padding.right}" y2="${zeroY}" stroke="var(--colors-hairline-on-dark)" stroke-width="2" />`;
    
    chartWrapper.innerHTML = `
      <svg width="100%" height="100%" viewBox="0 0 ${w} ${h}" preserveAspectRatio="xMidYMid meet">
        ${zeroLine}
        ${svgBars}
        ${svgLabels}
      </svg>
    `;
    
  } catch(err) {
    if (loader) loader.style.display = 'none';
    showToast(err.message, 'error');
  }
}

// 8. Market Prices
async function loadMarketData(cropName = '') {
  try {
    let prices = [];
    if (cropName) {
      prices = await api.getMarketPricesByCrop(cropName);
    } else {
      prices = await api.getAllMarketPrices();
    }
    
    const tbody = document.getElementById('market-full-tbody');
    if (!prices || prices.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--colors-muted); padding: 1rem;">No market prices found.</td></tr>`;
      return;
    }
    
    tbody.innerHTML = prices.map(p => `
      <tr>
        <td><strong>${p.cropName}</strong></td>
        <td>${p.market}</td>
        <td>${p.state}</td>
        <td>₹${p.minPrice}</td>
        <td>₹${p.maxPrice}</td>
        <td style="color: var(--colors-trading-up); font-weight: 600;">₹${p.modalPrice}</td>
        <td>${p.priceDate}</td>
      </tr>
    `).join('');
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// 9. Disease Scanner Initial
async function loadDiseaseScannerInitialData() {
  try {
    const cropsRes = await api.getCrops();
    const select = document.getElementById('disease-crop-select');
    select.innerHTML = '<option value="">No specific plot</option>' + 
      (cropsRes.content || []).map(c => `<option value="${c.id}">${c.cropName}</option>`).join('');
      
    document.getElementById('disease-scan-result').style.display = 'none';
    document.getElementById('form-disease-scan').reset();
  } catch(err) {
    showToast('Failed to load crops for scanner', 'error');
  }
}

// 10. Load Photos Grid
async function loadPhotosGrid(cropId) {
  const grid = document.getElementById('photos-grid');
  grid.innerHTML = '<div style="color:var(--colors-muted); grid-column:1/-1; text-align:center;">Loading...</div>';
  
  try {
    const photos = await api.getCropPhotos(cropId);
    if (!photos || photos.length === 0) {
      grid.innerHTML = '<div style="color:var(--colors-muted); grid-column:1/-1; text-align:center;">No photos uploaded yet.</div>';
      return;
    }
    
    grid.innerHTML = photos.map(photo => `
      <div style="background:var(--colors-canvas-dark); border-radius:var(--rounded-md); overflow:hidden; position:relative;">
        <img src="${photo.photoUrl}" alt="${photo.description}" style="width:100%; height:150px; object-fit:cover; display:block;">
        <div style="padding:0.5rem;">
          <p style="font-size:0.8rem; margin-bottom:0.25rem;">${photo.description || 'No description'}</p>
          <p style="font-size:0.65rem; color:var(--colors-muted);">${new Date(photo.uploadedAt).toLocaleString()}</p>
        </div>
        <button class="btn-logout delete-photo-btn" data-id="${photo.id}" style="position:absolute; top:0.25rem; right:0.25rem; width:24px; height:24px; padding:0; border-radius:50%; background:rgba(0,0,0,0.5); color:white; border:none;">&times;</button>
      </div>
    `).join('');
    
    // Bind delete photo click
    grid.querySelectorAll('.delete-photo-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        if(confirm('Delete this photo?')) {
          try {
            await api.deleteCropPhoto(cropId, e.currentTarget.dataset.id);
            showToast('Photo deleted');
            loadPhotosGrid(cropId);
          } catch(err) {
            showToast(err.message, 'error');
          }
        }
      });
    });
  } catch(err) {
    grid.innerHTML = `<div style="color:var(--colors-trading-down); grid-column:1/-1; text-align:center;">${err.message}</div>`;
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
    
    document.getElementById('crop-expected-yield').value = crop.expectedYieldKg || '';
    document.getElementById('crop-actual-yield').value = crop.actualYieldKg || '';
    document.getElementById('crop-selling-price').value = crop.sellingPricePerKg || '';
    
    statusGroup.style.display = 'block';
    document.getElementById('crop-status').value = crop.status;
    document.getElementById('crop-actual-yield-group').style.display = crop.status === 'HARVESTED' ? 'block' : 'none';
  } else {
    title.textContent = 'Configure New Plot';
    document.getElementById('crop-id-field').value = '';
    document.getElementById('crop-expected-yield').value = '';
    document.getElementById('crop-actual-yield').value = '';
    document.getElementById('crop-selling-price').value = '';
    statusGroup.style.display = 'none';
    document.getElementById('crop-actual-yield-group').style.display = 'none';
  }

  // Setup actual yield toggle listener
  document.getElementById('crop-status').onchange = (e) => {
    document.getElementById('crop-actual-yield-group').style.display = e.target.value === 'HARVESTED' ? 'block' : 'none';
  };

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
      notes: document.getElementById('crop-notes').value || null,
      expectedYieldKg: document.getElementById('crop-expected-yield').value ? parseFloat(document.getElementById('crop-expected-yield').value) : null,
      actualYieldKg: document.getElementById('crop-actual-yield').value ? parseFloat(document.getElementById('crop-actual-yield').value) : null,
      sellingPricePerKg: document.getElementById('crop-selling-price').value ? parseFloat(document.getElementById('crop-selling-price').value) : null
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

  // Weather Search
  document.getElementById('btn-search-weather').addEventListener('click', () => {
    const city = document.getElementById('weather-city-input').value;
    if (city) loadWeatherWidget(city);
  });

  // Market Search
  document.getElementById('btn-search-market').addEventListener('click', () => {
    const cropName = document.getElementById('market-search-input').value;
    loadMarketData(cropName);
  });

  // Language Switcher
  const langSelect = document.getElementById('lang-switcher');
  langSelect.value = localStorage.getItem('sag_lang') || 'en';
  langSelect.addEventListener('change', async (e) => {
    const lang = e.target.value;
    localStorage.setItem('sag_lang', lang);
    showToast('Language preference saved', 'info');
    try {
      if (api.isAuthenticated()) {
        const user = api.getCurrentUser();
        if (user) {
          await api.updateProfile({ fullName: user.fullName, email: user.email, preferredLanguage: lang });
        }
      }
      location.reload(); // Reload to apply i18n
    } catch(err) {}
  });

  // Task Modal Trigger
  document.getElementById('btn-add-task').addEventListener('click', async () => {
    document.getElementById('form-task').reset();
    document.getElementById('task-due-date').value = new Date().toISOString().slice(0,16);
    
    // Load crops for select
    const cropsRes = await api.getCrops();
    const select = document.getElementById('task-crop-select');
    select.innerHTML = '<option value="">None</option>' + 
      (cropsRes.content || []).map(c => `<option value="${c.id}">${c.cropName}</option>`).join('');
      
    openModal('modal-task');
  });

  // Form Submit: Task
  document.getElementById('form-task').addEventListener('submit', async (e) => {
    e.preventDefault();
    const taskData = {
      title: document.getElementById('task-title').value,
      description: document.getElementById('task-desc').value,
      priority: document.getElementById('task-priority').value,
      dueDate: document.getElementById('task-due-date').value,
      cropId: document.getElementById('task-crop-select').value ? Number(document.getElementById('task-crop-select').value) : null
    };
    try {
      await api.createTask(taskData);
      showToast('Task created successfully');
      closeModal('modal-task');
      loadTasksData();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  // Form Submit: Disease Scan
  document.getElementById('form-disease-scan').addEventListener('submit', async (e) => {
    e.preventDefault();
    const fileInput = document.getElementById('disease-photo');
    if (!fileInput.files || fileInput.files.length === 0) return;
    
    const cropId = document.getElementById('disease-crop-select').value;
    
    showToast('Uploading photo for AI analysis...', 'info');
    try {
      const res = await api.analyzeDisease(cropId || null, fileInput.files[0]);
      
      const confValue = parseFloat(res.confidence);
      let confColor = 'var(--colors-trading-up)';
      if (confValue < 40) confColor = 'var(--colors-trading-down)';
      else if (confValue < 70) confColor = 'orange';

      const resultsDiv = document.getElementById('disease-scan-result');
      resultsDiv.style.display = 'block';
      resultsDiv.innerHTML = `
        <div class="flex justify-between align-center" style="margin-bottom:1rem;">
          <h3 style="font-size:1.2rem;">${res.diseaseName}</h3>
          <span class="badge badge-${res.severity.toLowerCase()}">${res.severity}</span>
        </div>
        <div style="margin-bottom:1rem;">
          <div style="font-size:0.8rem; color:var(--colors-muted); margin-bottom:0.25rem;">AI Confidence</div>
          <div style="width:100%; height:8px; background:var(--colors-canvas-dark); border-radius:4px; overflow:hidden;">
            <div style="width:${res.confidence}; height:100%; background:${confColor};"></div>
          </div>
          <div style="font-size:0.75rem; text-align:right; color:${confColor}; margin-top:0.25rem;">${res.confidence}</div>
        </div>
        <p style="font-size:0.9rem; color:var(--colors-muted); line-height:1.5; margin-bottom:1.5rem;">${res.description}</p>
        
        <div style="display:grid; grid-template-columns:1fr 1fr; gap:1.5rem;">
          <div>
            <h4 style="font-size:0.9rem; margin-bottom:0.5rem; color:var(--colors-primary);">Recommended Treatments</h4>
            <ul style="font-size:0.85rem; color:var(--colors-muted); padding-left:1.2rem; line-height:1.5;">
              ${(res.treatments || []).map(t => `<li style="margin-bottom:0.25rem;">${t}</li>`).join('')}
            </ul>
          </div>
          <div>
            <h4 style="font-size:0.9rem; margin-bottom:0.5rem; color:var(--colors-on-dark);">Preventive Measures</h4>
            <ul style="font-size:0.85rem; color:var(--colors-muted); padding-left:1.2rem; line-height:1.5;">
              ${(res.preventions || []).map(p => `<li style="margin-bottom:0.25rem;">${p}</li>`).join('')}
            </ul>
          </div>
        </div>
      `;
      showToast('Analysis complete');
    } catch(err) {
      showToast(err.message, 'error');
    }
  });

  // Form Submit: Photo Upload
  document.getElementById('form-photo-upload').addEventListener('submit', async (e) => {
    e.preventDefault();
    const cropId = document.getElementById('photo-crop-id').value;
    const file = document.getElementById('photo-file').files[0];
    const desc = document.getElementById('photo-desc').value;
    
    if(!file || !cropId) return;
    
    try {
      showToast('Uploading photo...', 'info');
      await api.uploadCropPhoto(cropId, file, desc);
      showToast('Photo uploaded successfully');
      document.getElementById('form-photo-upload').reset();
      
      // Reload photos grid
      await loadPhotosGrid(cropId);
    } catch(err) {
      showToast(err.message, 'error');
    }
  });

  syncAuthViews();
});
