// App State
let currentUser = null;
let authToken = null;
let refreshToken = null;
let currentSection = 'dashboard';
let appData = {
    vehicles: [],
    parts: [],
    tags: [],
    maintenance: []
};

// API Configuration
const API_BASE = '/api/v1';

// Utility Functions
function showLoading() {
    document.getElementById('loading').classList.remove('hidden');
}

function hideLoading() {
    document.getElementById('loading').classList.add('hidden');
}

function showToast(message, type = 'success', title = '') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    toast.innerHTML = `
        <div class="toast-header">
            <span class="toast-title">${title || (type === 'error' ? 'Error' : type === 'success' ? 'Success' : 'Info')}</span>
            <button class="toast-close" onclick="this.parentElement.parentElement.remove()">×</button>
        </div>
        <div class="toast-message">${message}</div>
    `;
    
    container.appendChild(toast);
    
    // Auto remove after 5 seconds
    setTimeout(() => {
        if (toast.parentElement) {
            toast.remove();
        }
    }, 5000);
}

function formatCurrency(cents, currency = 'EUR') {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: currency
    }).format(cents / 100);
}

function formatDate(dateString) {
    return new Date(dateString).toLocaleDateString();
}

// API Functions
async function apiCall(endpoint, options = {}) {
    const url = `${API_BASE}${endpoint}`;
    const config = {
        headers: {
            'Content-Type': 'application/json',
            ...options.headers
        },
        ...options
    };
    
    if (authToken && !endpoint.includes('/auth/')) {
        config.headers.Authorization = `Bearer ${authToken}`;
    }
    
    try {
        const response = await fetch(url, config);
        
        if (!response.ok) {
            // Try to parse error message if there's content
            let errorMessage = 'API request failed';
            try {
                const errorData = await response.json();
                errorMessage = errorData.message || errorMessage;
            } catch {
                // If parsing fails, use default message
            }
            throw new Error(errorMessage);
        }
        
        // Handle responses with no content (like 204 No Content)
        if (response.status === 204 || !response.headers.get('content-type')?.includes('application/json')) {
            return null;
        }
        
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

// Authentication Functions
async function login(email, password) {
    try {
        showLoading();
        const response = await apiCall('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
        
        authToken = response.accessToken;
        refreshToken = response.refreshToken;
        currentUser = response.user;
        
        // Store tokens
        localStorage.setItem('authToken', authToken);
        localStorage.setItem('refreshToken', refreshToken);
        localStorage.setItem('currentUser', JSON.stringify(currentUser));
        
        showApp();
        showToast('Logged in successfully!');
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
}

async function register(email, password, name) {
    try {
        showLoading();
        const response = await apiCall('/auth/register', {
            method: 'POST',
            body: JSON.stringify({ email, password, name })
        });
        
        authToken = response.accessToken;
        refreshToken = response.refreshToken;
        currentUser = response.user;
        
        // Store tokens
        localStorage.setItem('authToken', authToken);
        localStorage.setItem('refreshToken', refreshToken);
        localStorage.setItem('currentUser', JSON.stringify(currentUser));
        
        showApp();
        showToast('Account created successfully!');
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
}

function logout() {
    authToken = null;
    refreshToken = null;
    currentUser = null;
    
    localStorage.removeItem('authToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('currentUser');
    
    showAuth();
    showToast('Logged out successfully!');
}

// UI Functions
function showAuth() {
    document.getElementById('auth-container').classList.remove('hidden');
    document.getElementById('app-container').classList.add('hidden');
    document.getElementById('navbar').classList.add('hidden');
}

function showApp() {
    document.getElementById('auth-container').classList.add('hidden');
    document.getElementById('app-container').classList.remove('hidden');
    document.getElementById('navbar').classList.remove('hidden');
    
    // Update user name
    document.getElementById('user-name').textContent = currentUser?.name || currentUser?.email || 'User';
    
    // Load initial data
    loadDashboardData();
    showSection('dashboard');
}

function showLogin() {
    document.getElementById('login-form').classList.remove('hidden');
    document.getElementById('register-form').classList.add('hidden');
}

function showRegister() {
    document.getElementById('login-form').classList.add('hidden');
    document.getElementById('register-form').classList.remove('hidden');
}

function showSection(sectionName) {
    // Hide all sections
    document.querySelectorAll('.content-section').forEach(section => {
        section.classList.add('hidden');
    });
    
    // Show target section
    document.getElementById(`${sectionName}-section`).classList.remove('hidden');
    
    // Update navigation
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active');
    });
    document.querySelector(`[href="#${sectionName}"]`).classList.add('active');
    
    currentSection = sectionName;
    
    // Load section data
    switch (sectionName) {
        case 'vehicles':
            loadVehicles();
            break;
        case 'parts':
            loadParts();
            loadTags(); // For filter dropdown
            break;
        case 'maintenance':
            loadMaintenance();
            loadVehicles(); // For filter dropdown
            break;
        case 'tags':
            loadTags();
            break;
    }
}

// Modal Functions
function showModal(content) {
    document.getElementById('modal-content').innerHTML = content;
    document.getElementById('modal-overlay').classList.remove('hidden');
}

function hideModal() {
    document.getElementById('modal-overlay').classList.add('hidden');
}

// Dashboard Functions
async function loadDashboardData() {
    try {
        const [vehicles, parts, tags] = await Promise.all([
            apiCall('/vehicles'),
            apiCall('/parts'),
            apiCall('/tags')
        ]);
        
        // Update counts
        document.getElementById('vehicles-count').textContent = vehicles.totalCount || vehicles.data?.length || 0;
        document.getElementById('parts-count').textContent = parts.totalCount || parts.data?.length || 0;
        document.getElementById('tags-count').textContent = tags.length || 0;
        
        // Load recent maintenance
        if (vehicles.data?.length > 0) {
            const maintenancePromises = vehicles.data.slice(0, 3).map(vehicle =>
                apiCall(`/vehicles/${vehicle.id}/maintenance?pageSize=5`)
            );
            const maintenanceResults = await Promise.all(maintenancePromises);
            const allMaintenance = maintenanceResults.flatMap(result => result.data || []);
            
            document.getElementById('maintenance-count').textContent = allMaintenance.length;
            renderRecentMaintenance(allMaintenance.slice(0, 5));
        }
        
    } catch (error) {
        showToast('Failed to load dashboard data', 'error');
    }
}

function renderRecentMaintenance(maintenance) {
    const container = document.getElementById('recent-maintenance');
    
    if (maintenance.length === 0) {
        container.innerHTML = '<p class="empty-state">No recent maintenance records</p>';
        return;
    }
    
    container.innerHTML = maintenance.map(record => `
        <div class="maintenance-item">
            <span>${record.title}</span>
            <span class="maintenance-date">${formatDate(record.happenedAt)}</span>
        </div>
    `).join('');
}

// Vehicle Functions
async function loadVehicles() {
    try {
        showLoading();
        const response = await apiCall('/vehicles');
        appData.vehicles = response.data || response;
        renderVehicles();
        
        // Update maintenance filter dropdown
        const filterSelect = document.getElementById('maintenance-vehicle-filter');
        if (filterSelect) {
            filterSelect.innerHTML = '<option value="">All Vehicles</option>' +
                appData.vehicles.map(vehicle => 
                    `<option value="${vehicle.id}">${vehicle.manufacturer} ${vehicle.model} (${vehicle.licensePlate || 'No plate'})</option>`
                ).join('');
        }
    } catch (error) {
        showToast('Failed to load vehicles', 'error');
    } finally {
        hideLoading();
    }
}

function renderVehicles() {
    const container = document.getElementById('vehicles-grid');
    
    if (appData.vehicles.length === 0) {
        container.innerHTML = '<p class="empty-state">No vehicles added yet</p>';
        return;
    }
    
    container.innerHTML = appData.vehicles.map(vehicle => `
        <div class="vehicle-card">
            <div class="card-header">
                <div class="card-title">${vehicle.manufacturer} ${vehicle.model}</div>
                <div class="card-actions">
                    <button class="btn btn-sm" onclick="editVehicle('${vehicle.id}')">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-error" onclick="deleteVehicle('${vehicle.id}')">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
            <div class="card-meta">
                ${vehicle.licensePlate ? `<div class="meta-item"><i class="fas fa-id-card"></i> ${vehicle.licensePlate}</div>` : ''}
                ${vehicle.vin ? `<div class="meta-item"><i class="fas fa-barcode"></i> ${vehicle.vin}</div>` : ''}
                ${vehicle.buildYear ? `<div class="meta-item"><i class="fas fa-calendar"></i> ${vehicle.buildYear}</div>` : ''}
                ${vehicle.fuelType ? `<div class="meta-item"><i class="fas fa-gas-pump"></i> ${vehicle.fuelType}</div>` : ''}
                ${vehicle.currentOdo ? `<div class="meta-item"><i class="fas fa-tachometer-alt"></i> ${vehicle.currentOdo.toLocaleString()} ${vehicle.odoUnit}</div>` : ''}
            </div>
        </div>
    `).join('');
}

function showAddVehicleModal() {
    const content = `
        <div class="modal-header">
            <h3>Add Vehicle</h3>
            <button class="modal-close" onclick="hideModal()">×</button>
        </div>
        <div class="modal-body">
            <form id="add-vehicle-form">
                <div class="form-group">
                    <label for="vehicle-manufacturer">Manufacturer *</label>
                    <input type="text" id="vehicle-manufacturer" required>
                </div>
                <div class="form-group">
                    <label for="vehicle-model">Model *</label>
                    <input type="text" id="vehicle-model" required>
                </div>
                <div class="form-group">
                    <label for="vehicle-license-plate">License Plate</label>
                    <input type="text" id="vehicle-license-plate">
                </div>
                <div class="form-group">
                    <label for="vehicle-vin">VIN</label>
                    <input type="text" id="vehicle-vin">
                </div>
                <div class="form-group">
                    <label for="vehicle-build-year">Build Year</label>
                    <input type="number" id="vehicle-build-year" min="1900" max="2030">
                </div>
                <div class="form-group">
                    <label for="vehicle-fuel-type">Fuel Type</label>
                    <input type="text" id="vehicle-fuel-type" placeholder="e.g., Gasoline, Diesel, Electric">
                </div>
                <div class="form-group">
                    <label for="vehicle-odo-unit">Odometer Unit</label>
                    <select id="vehicle-odo-unit">
                        <option value="KM">Kilometers</option>
                        <option value="HOURS">Hours</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="vehicle-current-odo">Current Odometer Reading</label>
                    <input type="number" id="vehicle-current-odo" min="0">
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button class="btn" onclick="hideModal()">Cancel</button>
            <button class="btn btn-primary" onclick="addVehicle()">Add Vehicle</button>
        </div>
    `;
    showModal(content);
}

async function addVehicle() {
    const form = document.getElementById('add-vehicle-form');
    const formData = new FormData(form);
    
    const vehicleData = {
        manufacturer: document.getElementById('vehicle-manufacturer').value,
        model: document.getElementById('vehicle-model').value,
        licensePlate: document.getElementById('vehicle-license-plate').value || null,
        vin: document.getElementById('vehicle-vin').value || null,
        buildYear: document.getElementById('vehicle-build-year').value ? parseInt(document.getElementById('vehicle-build-year').value) : null,
        fuelType: document.getElementById('vehicle-fuel-type').value || null,
        odoUnit: document.getElementById('vehicle-odo-unit').value,
        currentOdo: document.getElementById('vehicle-current-odo').value ? parseInt(document.getElementById('vehicle-current-odo').value) : null
    };
    
    try {
        showLoading();
        await apiCall('/vehicles', {
            method: 'POST',
            body: JSON.stringify(vehicleData)
        });
        
        hideModal();
        loadVehicles();
        showToast('Vehicle added successfully!');
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
}

function showEditVehicleModal(vehicleId) {
    const vehicle = appData.vehicles.find(v => v.id === vehicleId);
    if (!vehicle) {
        showToast('Vehicle not found', 'error');
        return;
    }
    
    const content = `
        <div class="modal-header">
            <h3>Edit Vehicle</h3>
            <button class="modal-close" onclick="hideModal()">×</button>
        </div>
        <div class="modal-body">
            <form id="edit-vehicle-form">
                <div class="form-group">
                    <label for="edit-vehicle-manufacturer">Manufacturer *</label>
                    <input type="text" id="edit-vehicle-manufacturer" required value="${vehicle.manufacturer}">
                </div>
                <div class="form-group">
                    <label for="edit-vehicle-model">Model *</label>
                    <input type="text" id="edit-vehicle-model" required value="${vehicle.model}">
                </div>
                <div class="form-group">
                    <label for="edit-vehicle-license-plate">License Plate</label>
                    <input type="text" id="edit-vehicle-license-plate" value="${vehicle.licensePlate || ''}">
                </div>
                <div class="form-group">
                    <label for="edit-vehicle-vin">VIN</label>
                    <input type="text" id="edit-vehicle-vin" value="${vehicle.vin || ''}">
                </div>
                <div class="form-group">
                    <label for="edit-vehicle-build-year">Build Year</label>
                    <input type="number" id="edit-vehicle-build-year" min="1900" max="2030" value="${vehicle.buildYear || ''}">
                </div>
                <div class="form-group">
                    <label for="edit-vehicle-fuel-type">Fuel Type</label>
                    <select id="edit-vehicle-fuel-type">
                        <option value="">Select fuel type</option>
                        <option value="Gasoline" ${vehicle.fuelType === 'Gasoline' ? 'selected' : ''}>Gasoline</option>
                        <option value="Diesel" ${vehicle.fuelType === 'Diesel' ? 'selected' : ''}>Diesel</option>
                        <option value="Electric" ${vehicle.fuelType === 'Electric' ? 'selected' : ''}>Electric</option>
                        <option value="Hybrid" ${vehicle.fuelType === 'Hybrid' ? 'selected' : ''}>Hybrid</option>
                        <option value="LPG" ${vehicle.fuelType === 'LPG' ? 'selected' : ''}>LPG</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="edit-vehicle-odo-unit">Odometer Unit</label>
                    <select id="edit-vehicle-odo-unit">
                        <option value="KM" ${vehicle.odoUnit === 'KM' ? 'selected' : ''}>Kilometers</option>
                        <option value="MI" ${vehicle.odoUnit === 'MI' ? 'selected' : ''}>Miles</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="edit-vehicle-current-odo">Current Odometer Reading</label>
                    <input type="number" id="edit-vehicle-current-odo" min="0" value="${vehicle.currentOdo || ''}">
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button class="btn" onclick="hideModal()">Cancel</button>
            <button class="btn btn-primary" onclick="updateVehicle('${vehicleId}')">Update Vehicle</button>
        </div>
    `;
    showModal(content);
}

async function updateVehicle(vehicleId) {
    const vehicleData = {
        manufacturer: document.getElementById('edit-vehicle-manufacturer').value,
        model: document.getElementById('edit-vehicle-model').value,
        licensePlate: document.getElementById('edit-vehicle-license-plate').value || null,
        vin: document.getElementById('edit-vehicle-vin').value || null,
        buildYear: document.getElementById('edit-vehicle-build-year').value ? parseInt(document.getElementById('edit-vehicle-build-year').value) : null,
        fuelType: document.getElementById('edit-vehicle-fuel-type').value || null,
        odoUnit: document.getElementById('edit-vehicle-odo-unit').value,
        currentOdo: document.getElementById('edit-vehicle-current-odo').value ? parseInt(document.getElementById('edit-vehicle-current-odo').value) : null
    };
    
    try {
        showLoading();
        await apiCall(`/vehicles/${vehicleId}`, {
            method: 'PUT',
            body: JSON.stringify(vehicleData)
        });
        
        hideModal();
        loadVehicles();
        showToast('Vehicle updated successfully!');
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
}

function editVehicle(vehicleId) {
    showEditVehicleModal(vehicleId);
}

async function deleteVehicle(vehicleId) {
    if (!confirm('Are you sure you want to delete this vehicle? This action cannot be undone.')) {
        return;
    }
    
    try {
        showLoading();
        await apiCall(`/vehicles/${vehicleId}`, { method: 'DELETE' });
        loadVehicles();
        showToast('Vehicle deleted successfully!');
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
}

// Parts Functions
async function loadParts() {
    try {
        showLoading();
        const response = await apiCall('/parts');
        appData.parts = response.data || response;
        renderParts();
    } catch (error) {
        showToast('Failed to load parts', 'error');
    } finally {
        hideLoading();
    }
}

function renderParts() {
    const container = document.getElementById('parts-grid');
    
    if (appData.parts.length === 0) {
        container.innerHTML = '<p class="empty-state">No parts added yet</p>';
        return;
    }
    
    container.innerHTML = appData.parts.map(part => `
        <div class="part-card">
            <div class="card-header">
                <div class="card-title">${part.name}</div>
                <div class="card-actions">
                    <button class="btn btn-sm" onclick="editPart('${part.id}')">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-error" onclick="deletePart('${part.id}')">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
            <div class="card-meta">
                <div class="meta-item">
                    <i class="fas fa-dollar-sign"></i>
                    ${formatCurrency(part.priceCents, part.currency)}
                </div>
                ${part.description ? `<div class="meta-item"><i class="fas fa-info-circle"></i> ${part.description}</div>` : ''}
                ${part.url ? `<div class="meta-item"><i class="fas fa-link"></i> <a href="${part.url}" target="_blank">Supplier Link</a></div>` : ''}
            </div>
            ${part.tags && part.tags.length > 0 ? `
                <div class="card-tags">
                    ${part.tags.map(tag => `<span class="tag tag-colored" style="--color: ${tag.color || '#2563eb'}">${tag.name}</span>`).join('')}
                </div>
            ` : ''}
        </div>
    `).join('');
}

function showAddPartModal() {
    const content = `
        <div class="modal-header">
            <h3>Add Part</h3>
            <button class="modal-close" onclick="hideModal()">×</button>
        </div>
        <div class="modal-body">
            <form id="add-part-form">
                <div class="form-group">
                    <label for="part-name">Part Name *</label>
                    <input type="text" id="part-name" required>
                </div>
                <div class="form-group">
                    <label for="part-description">Description</label>
                    <textarea id="part-description" rows="3"></textarea>
                </div>
                <div class="form-group">
                    <label for="part-url">Supplier URL</label>
                    <input type="url" id="part-url">
                </div>
                <div class="form-group">
                    <label for="part-tags">Tags</label>
                    <select id="part-tags" multiple>
                        ${appData.tags.map(tag => `<option value="${tag.id}">${tag.name}</option>`).join('')}
                    </select>
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button class="btn" onclick="hideModal()">Cancel</button>
            <button class="btn btn-primary" onclick="addPart()">Add Part</button>
        </div>
    `;
    showModal(content);
}

async function addPart() {
    const partData = {
        name: document.getElementById('part-name').value,
        description: document.getElementById('part-description').value || null,
        url: document.getElementById('part-url').value || null,
        tagIds: Array.from(document.getElementById('part-tags').selectedOptions).map(option => option.value)
    };
    
    try {
        showLoading();
        await apiCall('/parts', {
            method: 'POST',
            body: JSON.stringify(partData)
        });
        
        hideModal();
        loadParts();
        showToast('Part added successfully!');
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
}

async function deletePart(partId) {
    if (!confirm('Are you sure you want to delete this part?')) {
        return;
    }
    
    try {
        showLoading();
        await apiCall(`/parts/${partId}`, { method: 'DELETE' });
        loadParts();
        showToast('Part deleted successfully!');
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
}

function editPart(partId) {
    const part = appData.parts.find(p => p.id === partId);
    if (!part) {
        showToast('Part not found', 'error');
        return;
    }
    showEditPartModal(part);
}

function showEditPartModal(part) {
    const content = `
        <div class="modal-header">
            <h3>Edit Part</h3>
            <button class="modal-close" onclick="hideModal()">×</button>
        </div>
        <div class="modal-body">
            <form id="edit-part-form">
                <div class="form-group">
                    <label for="edit-part-name">Part Name *</label>
                    <input type="text" id="edit-part-name" required value="${part.name}">
                </div>
                <div class="form-group">
                    <label for="edit-part-description">Description</label>
                    <textarea id="edit-part-description" rows="3">${part.description || ''}</textarea>
                </div>
                <div class="form-group">
                    <label for="edit-part-url">Supplier URL</label>
                    <input type="url" id="edit-part-url" value="${part.url || ''}">
                </div>
                <div class="form-group">
                    <label for="edit-part-tags">Tags</label>
                    <select id="edit-part-tags" multiple>
                        ${appData.tags.map(tag => `<option value="${tag.id}" ${part.tags && part.tags.some(t => t.id === tag.id) ? 'selected' : ''}>${tag.name}</option>`).join('')}
                    </select>
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button class="btn" onclick="hideModal()">Cancel</button>
            <button class="btn btn-primary" onclick="updatePart('${part.id}')">Update Part</button>
        </div>
    `;
    showModal(content);
}

async function updatePart(partId) {
    const partData = {
        name: document.getElementById('edit-part-name').value,
        description: document.getElementById('edit-part-description').value || null,
        url: document.getElementById('edit-part-url').value || null,
        tagIds: Array.from(document.getElementById('edit-part-tags').selectedOptions).map(option => option.value)
    };
    
    try {
        showLoading();
        await apiCall(`/parts/${partId}`, {
            method: 'PUT',
            body: JSON.stringify(partData)
        });
        
        hideModal();
        loadParts();
        showToast('Part updated successfully!');
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
}

// Tags Functions
async function loadTags() {
    try {
        const response = await apiCall('/tags');
        appData.tags = response;
        renderTags();
        
        // Update parts tag filter
        const filterSelect = document.getElementById('parts-tag-filter');
        if (filterSelect) {
            filterSelect.innerHTML = '<option value="">All Tags</option>' +
                appData.tags.map(tag => `<option value="${tag.id}">${tag.name}</option>`).join('');
        }
    } catch (error) {
        showToast('Failed to load tags', 'error');
    }
}

function renderTags() {
    const container = document.getElementById('tags-grid');
    
    if (appData.tags.length === 0) {
        container.innerHTML = '<p class="empty-state">No tags created yet</p>';
        return;
    }
    
    container.innerHTML = appData.tags.map(tag => `
        <div class="tag-card">
            <div class="card-header">
                <div class="card-title">
                    <span class="tag tag-colored" style="--color: ${tag.color || '#2563eb'}">${tag.name}</span>
                </div>
                <div class="card-actions">
                    <button class="btn btn-sm" onclick="editTag('${tag.id}')">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-error" onclick="deleteTag('${tag.id}')">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
            <div class="card-meta">
                <div class="meta-item">
                    <i class="fas fa-calendar"></i>
                    Created ${formatDate(tag.createdAt)}
                </div>
            </div>
        </div>
    `).join('');
}

function showAddTagModal() {
    const content = `
        <div class="modal-header">
            <h3>Add Tag</h3>
            <button class="modal-close" onclick="hideModal()">×</button>
        </div>
        <div class="modal-body">
            <form id="add-tag-form">
                <div class="form-group">
                    <label for="tag-name">Tag Name *</label>
                    <input type="text" id="tag-name" required>
                </div>
                <div class="form-group">
                    <label for="tag-color">Color</label>
                    <input type="color" id="tag-color" value="#2563eb">
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button class="btn" onclick="hideModal()">Cancel</button>
            <button class="btn btn-primary" onclick="addTag()">Add Tag</button>
        </div>
    `;
    showModal(content);
}

async function addTag() {
    const tagData = {
        name: document.getElementById('tag-name').value,
        color: document.getElementById('tag-color').value
    };
    
    try {
        showLoading();
        await apiCall('/tags', {
            method: 'POST',
            body: JSON.stringify(tagData)
        });
        
        hideModal();
        loadTags();
        showToast('Tag added successfully!');
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
}

async function deleteTag(tagId) {
    if (!confirm('Are you sure you want to delete this tag?')) {
        return;
    }
    
    try {
        showLoading();
        await apiCall(`/tags/${tagId}`, { method: 'DELETE' });
        loadTags();
        showToast('Tag deleted successfully!');
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
}

function editTag(tagId) {
    const tag = appData.tags.find(t => t.id === tagId);
    if (!tag) {
        showToast('Tag not found', 'error');
        return;
    }
    showEditTagModal(tag);
}

function showEditTagModal(tag) {
    const content = `
        <div class="modal-header">
            <h3>Edit Tag</h3>
            <button class="modal-close" onclick="hideModal()">×</button>
        </div>
        <div class="modal-body">
            <form id="edit-tag-form">
                <div class="form-group">
                    <label for="edit-tag-name">Tag Name *</label>
                    <input type="text" id="edit-tag-name" required value="${tag.name}">
                </div>
                <div class="form-group">
                    <label for="edit-tag-color">Color</label>
                    <input type="color" id="edit-tag-color" value="${tag.color || '#2563eb'}">
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button class="btn" onclick="hideModal()">Cancel</button>
            <button class="btn btn-primary" onclick="updateTag('${tag.id}')">Update Tag</button>
        </div>
    `;
    showModal(content);
}

async function updateTag(tagId) {
    const tagData = {
        name: document.getElementById('edit-tag-name').value,
        color: document.getElementById('edit-tag-color').value
    };
    
    try {
        showLoading();
        await apiCall(`/tags/${tagId}`, {
            method: 'PUT',
            body: JSON.stringify(tagData)
        });
        
        hideModal();
        loadTags();
        showToast('Tag updated successfully!');
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
}

// Maintenance Functions
async function loadMaintenance() {
    try {
        showLoading();
        if (appData.vehicles.length === 0) {
            await loadVehicles();
        }
        
        const maintenancePromises = appData.vehicles.map(vehicle =>
            apiCall(`/vehicles/${vehicle.id}/maintenance`)
        );
        const maintenanceResults = await Promise.all(maintenancePromises);
        
        appData.maintenance = maintenanceResults.flatMap((result, index) =>
            (result.data || []).map(record => ({
                ...record,
                vehicleName: `${appData.vehicles[index].manufacturer} ${appData.vehicles[index].model}`
            }))
        );
        
        renderMaintenance();
    } catch (error) {
        showToast('Failed to load maintenance records', 'error');
    } finally {
        hideLoading();
    }
}

function renderMaintenance() {
    const container = document.getElementById('maintenance-list');
    
    if (appData.maintenance.length === 0) {
        container.innerHTML = '<p class="empty-state">No maintenance records yet</p>';
        return;
    }
    
    container.innerHTML = appData.maintenance.map(record => `
        <div class="maintenance-record" id="maintenance-${record.id}">
            <div class="maintenance-header" onclick="toggleMaintenanceDetails('${record.id}')">
                <div class="maintenance-title-container">
                    <button class="maintenance-toggle-btn" title="Expand/Collapse">
                        <i class="fas fa-chevron-down"></i>
                    </button>
                    <div class="maintenance-title">${record.title}</div>
                </div>
                <div class="maintenance-date">${formatDate(record.happenedAt)}</div>
                <div class="maintenance-actions" onclick="event.stopPropagation()">
                    <button class="btn btn-sm" onclick="editMaintenance('${record.id}')" title="Edit">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-error" onclick="deleteMaintenance('${record.id}')" title="Delete">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
            <div class="maintenance-details" id="details-${record.id}">
                <div class="meta-item">
                    <i class="fas fa-car"></i>
                    ${record.vehicleName}
                </div>
                ${record.odoReading ? `
                    <div class="meta-item">
                        <i class="fas fa-tachometer-alt"></i>
                        ${record.odoReading.toLocaleString()} ${record.odoUnit || 'KM'}
                    </div>
                ` : ''}
                ${record.notes ? `
                    <div class="meta-item">
                        <i class="fas fa-sticky-note"></i>
                        ${record.notes}
                    </div>
                ` : ''}
            </div>
            ${record.items && record.items.length > 0 ? `
                <div class="maintenance-items" id="items-${record.id}">
                    <h4>Parts Used:</h4>
                    <div class="item-list">
                        ${record.items.map(item => `
                            <div class="maintenance-item">
                                <span>${item.part?.name || 'Unknown Part'}</span>
                                <span>Qty: ${item.quantity} ${item.unit || ''}</span>
                            </div>
                        `).join('')}
                    </div>
                </div>
            ` : ''}
        </div>
    `).join('');
}

function toggleMaintenanceDetails(recordId) {
    const record = document.getElementById(`maintenance-${recordId}`);
    const details = document.getElementById(`details-${recordId}`);
    const items = document.getElementById(`items-${recordId}`);
    const toggleBtn = record.querySelector('.maintenance-toggle-btn i');
    
    const isCollapsed = record.classList.contains('collapsed');
    
    if (isCollapsed) {
        // Expand
        record.classList.remove('collapsed');
        details.style.display = 'grid';
        if (items) items.style.display = 'block';
        toggleBtn.classList.remove('fa-chevron-right');
        toggleBtn.classList.add('fa-chevron-down');
    } else {
        // Collapse
        record.classList.add('collapsed');
        details.style.display = 'none';
        if (items) items.style.display = 'none';
        toggleBtn.classList.remove('fa-chevron-down');
        toggleBtn.classList.add('fa-chevron-right');
    }
}

function showAddMaintenanceModal() {
    if (appData.vehicles.length === 0) {
        showToast('Please add a vehicle first', 'warning');
        return;
    }
    
    // Load parts if not already loaded
    if (appData.parts.length === 0) {
        loadParts().then(() => {
            showMaintenanceModal();
        });
    } else {
        showMaintenanceModal();
    }
}

function showMaintenanceModal() {
    if (appData.vehicles.length === 0) {
        showToast('Please add a vehicle first', 'warning');
        return;
    }
    
    const content = `
        <div class="modal-header">
            <h3>Add Maintenance Record</h3>
            <button class="modal-close" onclick="hideModal()">×</button>
        </div>
        <div class="modal-body">
            <form id="add-maintenance-form">
                <div class="form-group">
                    <label for="maintenance-vehicle">Vehicle *</label>
                    <select id="maintenance-vehicle" required>
                        <option value="">Select a vehicle</option>
                        ${appData.vehicles.map(vehicle => 
                            `<option value="${vehicle.id}">${vehicle.manufacturer} ${vehicle.model} (${vehicle.licensePlate || 'No plate'})</option>`
                        ).join('')}
                    </select>
                </div>
                <div class="form-group">
                    <label for="maintenance-title">Title *</label>
                    <input type="text" id="maintenance-title" required placeholder="e.g., Oil Change, Brake Pads">
                </div>
                <div class="form-group">
                    <label for="maintenance-date">Date *</label>
                    <input type="date" id="maintenance-date" required>
                </div>
                <div class="form-group">
                    <label for="maintenance-odo">Odometer Reading</label>
                    <input type="number" id="maintenance-odo" min="0">
                </div>
                <div class="form-group">
                    <label for="maintenance-notes">Notes</label>
                    <textarea id="maintenance-notes" rows="3"></textarea>
                </div>
                <div class="form-group">
                    <label>Parts Used</label>
                    <div id="maintenance-parts-container">
                    </div>
                    <button type="button" class="btn btn-sm" onclick="addMaintenancePart()">
                        <i class="fas fa-plus"></i> Add Part
                    </button>
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button class="btn" onclick="hideModal()">Cancel</button>
            <button class="btn btn-primary" onclick="addMaintenance()">Add Record</button>
        </div>
    `;
    showModal(content);
    
    // Set today's date as default
    document.getElementById('maintenance-date').valueAsDate = new Date();
}

async function addMaintenance() {
    const vehicleId = document.getElementById('maintenance-vehicle').value;
    const maintenanceData = {
        title: document.getElementById('maintenance-title').value,
        happenedAt: document.getElementById('maintenance-date').value,
        odoReading: document.getElementById('maintenance-odo').value ? parseInt(document.getElementById('maintenance-odo').value) : null,
        notes: document.getElementById('maintenance-notes').value || null,
        items: []
    };
    
    // Collect parts data
    const partSelects = document.querySelectorAll('#maintenance-parts-container .part-select');
    partSelects.forEach((select, index) => {
        const partId = select.value;
        const quantity = document.querySelector(`.part-quantity[data-index="${index}"]`)?.value;
        const unit = document.querySelector(`.part-unit[data-index="${index}"]`)?.value;
        
        if (partId && quantity && parseFloat(quantity) > 0) {
            maintenanceData.items.push({
                partId: partId,
                quantity: parseFloat(quantity),
                unit: unit || null
            });
        }
    });
    
    try {
        showLoading();
        await apiCall(`/vehicles/${vehicleId}/maintenance`, {
            method: 'POST',
            body: JSON.stringify(maintenanceData)
        });
        
        hideModal();
        loadMaintenance();
        showToast('Maintenance record added successfully!');
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
}

function showEditMaintenanceModal(maintenanceId) {
    const record = appData.maintenance.find(m => m.id === maintenanceId);
    if (!record) {
        showToast('Maintenance record not found', 'error');
        return;
    }
    
    // Load parts if not already loaded
    if (appData.parts.length === 0) {
        loadParts().then(() => {
            showEditMaintenanceModalContent(record);
        });
    } else {
        showEditMaintenanceModalContent(record);
    }
}

function showEditMaintenanceModalContent(record) {
    const content = `
        <div class="modal-header">
            <h3>Edit Maintenance Record</h3>
            <button class="modal-close" onclick="hideModal()">×</button>
        </div>
        <div class="modal-body">
            <form id="edit-maintenance-form">
                <div class="form-group">
                    <label for="edit-maintenance-vehicle">Vehicle *</label>
                    <select id="edit-maintenance-vehicle" required>
                        ${appData.vehicles.map(vehicle => 
                            `<option value="${vehicle.id}" ${vehicle.id === record.vehicleId ? 'selected' : ''}>${vehicle.manufacturer} ${vehicle.model} (${vehicle.licensePlate || 'No plate'})</option>`
                        ).join('')}
                    </select>
                </div>
                <div class="form-group">
                    <label for="edit-maintenance-title">Title *</label>
                    <input type="text" id="edit-maintenance-title" required placeholder="e.g., Oil Change, Brake Pads" value="${record.title}">
                </div>
                <div class="form-group">
                    <label for="edit-maintenance-date">Date *</label>
                    <input type="date" id="edit-maintenance-date" required value="${record.happenedAt}">
                </div>
                <div class="form-group">
                    <label for="edit-maintenance-odo">Odometer Reading</label>
                    <input type="number" id="edit-maintenance-odo" min="0" value="${record.odoReading || ''}">
                </div>
                <div class="form-group">
                    <label for="edit-maintenance-notes">Notes</label>
                    <textarea id="edit-maintenance-notes" rows="3">${record.notes || ''}</textarea>
                </div>
                <div class="form-group">
                    <label>Parts Used</label>
                    <div id="edit-maintenance-parts-container">
                        ${(record.items || []).map((item, index) => `
                            <div class="maintenance-part-item" data-index="${index}">
                                <select class="part-select" data-index="${index}">
                                    <option value="">Select a part</option>
                                    ${appData.parts.map(part => 
                                        `<option value="${part.id}" ${part.id === item.partId ? 'selected' : ''}>${part.name}</option>`
                                    ).join('')}
                                </select>
                                <input type="number" class="part-quantity" data-index="${index}" placeholder="Qty" min="0" step="0.1" value="${item.quantity}">
                                <input type="text" class="part-unit" data-index="${index}" placeholder="Unit" value="${item.unit || ''}">
                                <button type="button" class="btn btn-sm btn-error" onclick="removeMaintenancePart(${index})">
                                    <i class="fas fa-trash"></i>
                                </button>
                            </div>
                        `).join('')}
                    </div>
                    <button type="button" class="btn btn-sm" onclick="addMaintenancePart()">
                        <i class="fas fa-plus"></i> Add Part
                    </button>
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button class="btn" onclick="hideModal()">Cancel</button>
            <button class="btn btn-primary" onclick="updateMaintenance('${record.id}')">Update Record</button>
        </div>
    `;
    showModal(content);
}

async function updateMaintenance(maintenanceId) {
    const maintenanceData = {
        title: document.getElementById('edit-maintenance-title').value,
        happenedAt: document.getElementById('edit-maintenance-date').value,
        odoReading: document.getElementById('edit-maintenance-odo').value ? parseInt(document.getElementById('edit-maintenance-odo').value) : null,
        notes: document.getElementById('edit-maintenance-notes').value || null,
        items: []
    };
    
    // Collect parts data
    const partSelects = document.querySelectorAll('#edit-maintenance-parts-container .part-select');
    partSelects.forEach((select, index) => {
        const partId = select.value;
        const quantity = document.querySelector(`.part-quantity[data-index="${index}"]`)?.value;
        const unit = document.querySelector(`.part-unit[data-index="${index}"]`)?.value;
        
        if (partId && quantity && parseFloat(quantity) > 0) {
            maintenanceData.items.push({
                partId: partId,
                quantity: parseFloat(quantity),
                unit: unit || null
            });
        }
    });
    
    try {
        showLoading();
        await apiCall(`/maintenance/${maintenanceId}`, {
            method: 'PUT',
            body: JSON.stringify(maintenanceData)
        });
        
        hideModal();
        loadMaintenance();
        showToast('Maintenance record updated successfully!');
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
}

function editMaintenance(maintenanceId) {
    showEditMaintenanceModal(maintenanceId);
}

async function deleteMaintenance(maintenanceId) {
    if (!confirm('Are you sure you want to delete this maintenance record? This action cannot be undone.')) {
        return;
    }
    
    try {
        showLoading();
        await apiCall(`/maintenance/${maintenanceId}`, { method: 'DELETE' });
        loadMaintenance();
        showToast('Maintenance record deleted successfully!');
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
}

function addMaintenancePart() {
    const container = document.getElementById('maintenance-parts-container') || document.getElementById('edit-maintenance-parts-container');
    if (!container) return;
    
    const existingParts = container.querySelectorAll('.maintenance-part-item');
    const index = existingParts.length;
    
    const partItem = document.createElement('div');
    partItem.className = 'maintenance-part-item';
    partItem.setAttribute('data-index', index);
    
    partItem.innerHTML = `
        <select class="part-select" data-index="${index}">
            <option value="">Select a part</option>
            ${appData.parts.map(part => 
                `<option value="${part.id}">${part.name}</option>`
            ).join('')}
        </select>
        <input type="number" class="part-quantity" data-index="${index}" placeholder="Qty" min="0" step="0.1">
        <input type="text" class="part-unit" data-index="${index}" placeholder="Unit">
        <button type="button" class="btn btn-sm btn-error" onclick="removeMaintenancePart(${index})">
            <i class="fas fa-trash"></i>
        </button>
    `;
    
    container.appendChild(partItem);
}

function removeMaintenancePart(index) {
    const partItem = document.querySelector(`.maintenance-part-item[data-index="${index}"]`);
    if (partItem) {
        partItem.remove();
    }
}

// Event Listeners
document.addEventListener('DOMContentLoaded', function() {
    // Check for stored auth token
    const storedToken = localStorage.getItem('authToken');
    const storedUser = localStorage.getItem('currentUser');
    
    if (storedToken && storedUser) {
        authToken = storedToken;
        refreshToken = localStorage.getItem('refreshToken');
        currentUser = JSON.parse(storedUser);
        showApp();
    } else {
        showAuth();
    }
    
    // Auth form handlers
    document.getElementById('login-form').addEventListener('submit', function(e) {
        e.preventDefault();
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;
        login(email, password);
    });
    
    document.getElementById('register-form').addEventListener('submit', function(e) {
        e.preventDefault();
        const email = document.getElementById('register-email').value;
        const password = document.getElementById('register-password').value;
        const name = document.getElementById('register-name').value;
        register(email, password, name);
    });
    
    // Navigation
    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const section = this.getAttribute('href').substring(1);
            showSection(section);
        });
    });
    
    // Modal overlay click to close
    document.getElementById('modal-overlay').addEventListener('click', function(e) {
        if (e.target === this) {
            hideModal();
        }
    });
    
    // Search and filter functionality
    const partsSearch = document.getElementById('parts-search');
    if (partsSearch) {
        partsSearch.addEventListener('input', function() {
            filterParts();
        });
    }
    
    const partsTagFilter = document.getElementById('parts-tag-filter');
    if (partsTagFilter) {
        partsTagFilter.addEventListener('change', function() {
            filterParts();
        });
    }
    
    const maintenanceVehicleFilter = document.getElementById('maintenance-vehicle-filter');
    if (maintenanceVehicleFilter) {
        maintenanceVehicleFilter.addEventListener('change', function() {
            filterMaintenance();
        });
    }
});

function filterParts() {
    const searchTerm = document.getElementById('parts-search').value.toLowerCase();
    const selectedTag = document.getElementById('parts-tag-filter').value;
    
    const filteredParts = appData.parts.filter(part => {
        const matchesSearch = part.name.toLowerCase().includes(searchTerm) ||
                            (part.description && part.description.toLowerCase().includes(searchTerm));
        const matchesTag = !selectedTag || (part.tags && part.tags.some(tag => tag.id === selectedTag));
        
        return matchesSearch && matchesTag;
    });
    
    // Temporarily update the parts data for rendering
    const originalParts = appData.parts;
    appData.parts = filteredParts;
    renderParts();
    appData.parts = originalParts;
}

function filterMaintenance() {
    const selectedVehicle = document.getElementById('maintenance-vehicle-filter').value;
    
    const filteredMaintenance = selectedVehicle 
        ? appData.maintenance.filter(record => record.vehicleId === selectedVehicle)
        : appData.maintenance;
    
    // Temporarily update the maintenance data for rendering
    const originalMaintenance = appData.maintenance;
    appData.maintenance = filteredMaintenance;
    renderMaintenance();
    appData.maintenance = originalMaintenance;
}