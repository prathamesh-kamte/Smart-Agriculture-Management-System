const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Helper to construct headers with optional JWT Authorization token.
 */
function getHeaders() {
  const headers = {
    'Content-Type': 'application/json',
  };
  const token = localStorage.getItem('token');
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

/**
 * Handle HTTP response and throw errors for bad responses.
 */
async function handleResponse(response) {
  if (response.status === 204) {
    return null;
  }
  
  const contentType = response.headers.get('content-type');
  let data;
  if (contentType && contentType.includes('application/json')) {
    data = await response.json();
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    // If token expired or unauthorized, clear storage
    if (response.status === 401 || response.status === 403) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.dispatchEvent(new Event('auth-change'));
    }
    const errorMessage = data && typeof data === 'object' ? (data.message || JSON.stringify(data)) : data;
    throw new Error(errorMessage || `Request failed with status ${response.status}`);
  }
  
  return data;
}

export const api = {
  // --- AUTH ---
  async login(email, password) {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    const data = await handleResponse(response);
    if (data && data.accessToken) {
      localStorage.setItem('token', data.accessToken);
      localStorage.setItem('user', JSON.stringify({
        email: data.email,
        fullName: data.fullName,
        role: data.role
      }));
      window.dispatchEvent(new Event('auth-change'));
    }
    return data;
  },

  async register(fullName, email, password) {
    const response = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fullName, email, password, role: 'FARMER' }),
    });
    return await handleResponse(response);
  },

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.dispatchEvent(new Event('auth-change'));
  },

  isAuthenticated() {
    return !!localStorage.getItem('token');
  },

  getCurrentUser() {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  },

  // --- DASHBOARD ---
  async getDashboardStats() {
    const response = await fetch(`${API_BASE_URL}/dashboard/stats`, {
      method: 'GET',
      headers: getHeaders(),
    });
    return await handleResponse(response);
  },

  // --- CROPS ---
  async getCrops({ status = '', season = '', page = 0, size = 50 } = {}) {
    let url = `${API_BASE_URL}/crops?page=${page}&size=${size}&sort=id,desc`;
    if (status) url += `&status=${status}`;
    if (season) url += `&season=${season}`;
    
    const response = await fetch(url, {
      method: 'GET',
      headers: getHeaders(),
    });
    return await handleResponse(response);
  },

  async createCrop(cropData) {
    const response = await fetch(`${API_BASE_URL}/crops`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(cropData),
    });
    return await handleResponse(response);
  },

  async updateCrop(id, cropData) {
    const response = await fetch(`${API_BASE_URL}/crops/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(cropData),
    });
    return await handleResponse(response);
  },

  async updateCropStatus(id, status) {
    const response = await fetch(`${API_BASE_URL}/crops/${id}/status?status=${status}`, {
      method: 'PATCH',
      headers: getHeaders(),
    });
    return await handleResponse(response);
  },

  async deleteCrop(id) {
    const response = await fetch(`${API_BASE_URL}/crops/${id}`, {
      method: 'DELETE',
      headers: getHeaders(),
    });
    return await handleResponse(response);
  },

  // --- EXPENSES ---
  async getExpenses({ category = '', fromDate = '', toDate = '', page = 0, size = 50 } = {}) {
    let url = `${API_BASE_URL}/expenses?page=${page}&size=${size}&sort=expenseDate,desc`;
    if (category) url += `&category=${category}`;
    if (fromDate) url += `&fromDate=${fromDate}`;
    if (toDate) url += `&toDate=${toDate}`;

    const response = await fetch(url, {
      method: 'GET',
      headers: getHeaders(),
    });
    return await handleResponse(response);
  },

  async createExpense(expenseData) {
    const response = await fetch(`${API_BASE_URL}/expenses`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(expenseData),
    });
    return await handleResponse(response);
  },

  async updateExpense(id, expenseData) {
    const response = await fetch(`${API_BASE_URL}/expenses/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(expenseData),
    });
    return await handleResponse(response);
  },

  async deleteExpense(id) {
    const response = await fetch(`${API_BASE_URL}/expenses/${id}`, {
      method: 'DELETE',
      headers: getHeaders(),
    });
    return await handleResponse(response);
  },

  async getExpensesByCrop(cropId) {
    const response = await fetch(`${API_BASE_URL}/expenses/crop/${cropId}`, {
      method: 'GET',
      headers: getHeaders(),
    });
    return await handleResponse(response);
  },

  // --- ADVISORIES ---
  async getActiveAdvisories({ severity = '', page = 0, size = 20 } = {}) {
    let url = `${API_BASE_URL}/advisories/active?page=${page}&size=${size}&sort=generatedAt,desc`;
    if (severity) url += `&severity=${severity}`;

    const response = await fetch(url, {
      method: 'GET',
      headers: getHeaders(),
    });
    return await handleResponse(response);
  },

  async generateAdvisories() {
    const response = await fetch(`${API_BASE_URL}/advisories/generate`, {
      method: 'POST',
      headers: getHeaders(),
    });
    return await handleResponse(response);
  },

  async acknowledgeAdvisory(id) {
    const response = await fetch(`${API_BASE_URL}/advisories/${id}/acknowledge`, {
      method: 'PATCH',
      headers: getHeaders(),
    });
    return await handleResponse(response);
  },

  // --- USER PROFILE ---
  async getProfile() {
    const response = await fetch(`${API_BASE_URL}/users/me`, {
      method: 'GET',
      headers: getHeaders(),
    });
    return await handleResponse(response);
  },

  async updateProfile(profileData) {
    const response = await fetch(`${API_BASE_URL}/users/me`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(profileData),
    });
    return await handleResponse(response);
  }
};
