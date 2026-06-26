// ==================== STATE & GLOBALS ====================
const API_BASE = '/api/v1';

let state = {
    token: localStorage.getItem('token') || '',
    user: null,
    cart: [],
    currentView: '',
    charts: {
        status: null,
        topProducts: null
    }
};

// ==================== INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

function initApp() {
    setupTime();
    setupEventListeners();
    
    if (state.token) {
        loadUserProfile();
    } else {
        showView('auth');
    }
}

// System clock in top-bar
function setupTime() {
    const timeEl = document.getElementById('system-time');
    setInterval(() => {
        const now = new Date();
        timeEl.innerHTML = `<i class="fa-regular fa-clock"></i> ${now.toLocaleTimeString()}`;
    }, 1000);
}

// ==================== ROUTING / VIEW NAVIGATION ====================
function showView(viewName) {
    state.currentView = viewName;
    
    // Auth vs Dashboard Sections
    const authSection = document.getElementById('auth-section');
    const dashboardSection = document.getElementById('dashboard-section');
    
    if (viewName === 'auth') {
        authSection.classList.remove('hidden');
        dashboardSection.classList.add('hidden');
        return;
    }
    
    authSection.classList.add('hidden');
    dashboardSection.classList.remove('hidden');
    
    // Hide all view sub-panels
    document.querySelectorAll('.dashboard-view').forEach(panel => {
        panel.classList.add('hidden');
    });
    
    // Remove active state from nav items
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
    });
    
    // Show selected panel
    const targetPanel = document.getElementById(`view-${viewName}`);
    if (targetPanel) {
        targetPanel.classList.remove('hidden');
    }
    
    // Highlight sidebar nav item
    const navBtn = document.getElementById(`nav-btn-${viewName}`);
    if (navBtn) {
        navBtn.classList.add('active');
    }
    
    // Update view title in header
    const titleMap = {
        'analytics': 'Dashboard Analytics',
        'inventory': 'Inventory Management',
        'orders': 'Customer Orders',
        'store': 'Product Storefront',
        'myorders': 'My Orders',
        'profile': 'Profile Settings'
    };
    document.getElementById('view-title').innerText = titleMap[viewName] || 'Dashboard';
    
    // Load fresh data for the selected view
    loadViewData(viewName);
}

function loadViewData(viewName) {
    switch (viewName) {
        case 'analytics':
            loadAnalyticsData();
            break;
        case 'inventory':
            loadInventoryData();
            break;
        case 'orders':
            loadOrdersData();
            break;
        case 'store':
            loadStorefrontData();
            break;
        case 'myorders':
            loadMyOrdersData();
            break;
        case 'profile':
            fillProfileForm();
            break;
    }
}

// ==================== API FETCH UTILITY ====================
async function apiFetch(endpoint, options = {}) {
    const url = `${API_BASE}${endpoint}`;
    
    // Build Headers
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };
    if (state.token) {
        headers['Authorization'] = `Bearer ${state.token}`;
    }
    
    const config = {
        ...options,
        headers
    };
    
    try {
        const response = await fetch(url, config);
        
        // Handle unauthorized token
        if (response.status === 401 || response.status === 403) {
            handleLogout();
            showToast('Session expired or access denied. Please login again.', 'error');
            throw new Error('Unauthorized');
        }
        
        // Check for success status
        if (!response.ok) {
            let errorMsg = 'API request failed';
            try {
                const errData = await response.json();
                if (errData.message) {
                    errorMsg = errData.message;
                } else if (errData && typeof errData === 'object') {
                    const messages = Object.values(errData);
                    if (messages.length > 0) {
                        errorMsg = messages.join(', ');
                    }
                }
            } catch (e) {}
            throw new Error(errorMsg);
        }
        
        // Return JSON or raw blob (e.g. for pdf)
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/pdf')) {
            return await response.blob();
        }
        
        if (response.status === 204) {
            return null; // No Content
        }
        
        return await response.json();
    } catch (err) {
        console.error(`Fetch Error: ${url}`, err);
        throw err;
    }
}

// ==================== AUTH SERVICES ====================
async function handleLogin(email, password) {
    try {
        const data = await apiFetch('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
        
        state.token = data.token;
        localStorage.setItem('token', data.token);
        showToast('Login successful!', 'success');
        
        await loadUserProfile();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleRegister(username, email, password, phone) {
    try {
        await apiFetch('/auth/register', {
            method: 'POST',
            body: JSON.stringify({ username, email, password, phone })
        });
        
        showToast('Account created successfully! Logging you in...', 'success');
        await handleLogin(email, password);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function loadUserProfile() {
    try {
        const profile = await apiFetch('/users/me');
        state.user = profile;
        
        // UI updates based on user role
        document.getElementById('user-display-name').innerText = profile.username;
        document.getElementById('user-display-role').innerText = profile.role;
        document.getElementById('user-avatar-char').innerText = profile.username.substring(0,1).toUpperCase();
        
        const roleBadge = document.getElementById('user-display-role');
        roleBadge.className = 'badge';
        roleBadge.classList.add(`badge-${profile.role.toLowerCase()}`);
        
        // Show role-specific sidebar nav elements
        if (profile.role === 'ADMIN' || profile.role === 'STAFF') {
            document.getElementById('nav-group-admin').classList.remove('hidden');
            document.getElementById('nav-group-customer').classList.add('hidden');
            showView('analytics');
        } else {
            document.getElementById('nav-group-admin').classList.add('hidden');
            document.getElementById('nav-group-customer').classList.remove('hidden');
            showView('store');
        }
    } catch (err) {
        handleLogout();
    }
}

function handleLogout() {
    state.token = '';
    state.user = null;
    state.cart = [];
    localStorage.removeItem('token');
    
    // Destroy charts
    if (state.charts.status) { state.charts.status.destroy(); state.charts.status = null; }
    if (state.charts.topProducts) { state.charts.topProducts.destroy(); state.charts.topProducts = null; }
    
    updateCartBadge();
    showView('auth');
}

// ==================== ANALYTICS DASHBOARD ====================
async function loadAnalyticsData() {
    try {
        const [summary, topProducts, statusOrders] = await Promise.all([
            apiFetch('/analytics/summary'),
            apiFetch('/analytics/top-products'),
            apiFetch('/analytics/orders-by-status')
        ]);
        
        // Update stats cards
        document.getElementById('stat-revenue').innerText = `₹${summary.totalRevenue.toLocaleString('en-IN', {minimumFractionDigits: 2})}`;
        document.getElementById('stat-orders').innerText = summary.totalOrders;
        document.getElementById('stat-products').innerText = summary.activeProducts;
        document.getElementById('stat-users').innerText = summary.totalCustomers;
        
        // Render Charts
        renderStatusChart(statusOrders);
        renderTopProductsChart(topProducts);
    } catch (err) {
        showToast('Failed to load dashboard analytics data', 'error');
    }
}

function renderStatusChart(statusData) {
    const ctx = document.getElementById('orders-status-chart').getContext('2d');
    
    if (state.charts.status) {
        state.charts.status.destroy();
    }
    
    const labels = statusData.map(item => item.status);
    const data = statusData.map(item => item.count);
    
    state.charts.status = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: ['#f59e0b', '#0ea5e9', '#10b981', '#ef4444'],
                borderWidth: 1,
                borderColor: 'rgba(255,255,255,0.08)'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { color: '#9ca3af', font: { family: 'Plus Jakarta Sans' } }
                }
            }
        }
    });
}

function renderTopProductsChart(productData) {
    const ctx = document.getElementById('top-products-chart').getContext('2d');
    
    if (state.charts.topProducts) {
        state.charts.topProducts.destroy();
    }
    
    const labels = productData.map(item => item.productName);
    const revenue = productData.map(item => item.totalRevenue);
    
    state.charts.topProducts = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Revenue (₹)',
                data: revenue,
                backgroundColor: 'rgba(139, 92, 246, 0.75)',
                hoverBackgroundColor: '#8b5cf6',
                borderWidth: 0,
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    ticks: { color: '#9ca3af', font: { family: 'Plus Jakarta Sans' } },
                    grid: { color: 'rgba(255,255,255,0.03)' }
                },
                y: {
                    ticks: { color: '#9ca3af', font: { family: 'Plus Jakarta Sans' } },
                    grid: { color: 'rgba(255,255,255,0.03)' }
                }
            },
            plugins: {
                legend: { display: false }
            }
        }
    });
}

// ==================== INVENTORY SERVICES ====================
async function loadInventoryData(query = '') {
    const tableBody = document.getElementById('inventory-table-body');
    tableBody.innerHTML = `<tr><td colspan="6" class="text-center">Loading inventory...</td></tr>`;
    
    try {
        const response = await apiFetch('/products?size=100');
        let products = response.content;
        
        if (query) {
            products = products.filter(p => p.name.toLowerCase().includes(query.toLowerCase()));
        }
        
        tableBody.innerHTML = '';
        if (products.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="6" class="text-center">No products found.</td></tr>`;
            return;
        }
        
        products.forEach(p => {
            const tr = document.createElement('tr');
            
            // Highlight low stock (<= 10)
            const lowStockClass = p.stockQty <= 10 ? 'badge-cancelled' : 'badge-delivered';
            
            tr.innerHTML = `
                <td>${p.id}</td>
                <td><strong>${p.name}</strong></td>
                <td>${p.category}</td>
                <td>₹${p.price.toFixed(2)}</td>
                <td><span class="badge ${lowStockClass}">${p.stockQty} Units</span></td>
                <td>
                    <div class="actions-cell-buttons">
                        <button class="btn btn-secondary btn-table-action" onclick="openStockModal(${p.id}, '${p.name.replace(/'/g, "\\'")}', ${p.stockQty})" title="Adjust Stock">
                            <i class="fa-solid fa-boxes-stacked"></i>
                        </button>
                        <button class="btn btn-primary btn-table-action" onclick="openEditProductModal(${p.id})" title="Edit Details">
                            <i class="fa-solid fa-pen-to-square"></i>
                        </button>
                        <button class="btn btn-danger btn-table-action" onclick="deleteProduct(${p.id})" title="Delete Product">
                            <i class="fa-solid fa-trash-can"></i>
                        </button>
                    </div>
                </td>
            `;
            tableBody.appendChild(tr);
        });
    } catch (err) {
        showToast('Failed to retrieve inventory data', 'error');
    }
}

async function deleteProduct(id) {
    if (!confirm('Are you sure you want to delete this product?')) return;
    try {
        await apiFetch(`/products/${id}`, { method: 'DELETE' });
        showToast('Product successfully deleted', 'success');
        loadInventoryData();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// Product add/edit operations
function openAddProductModal() {
    document.getElementById('product-form').reset();
    document.getElementById('product-id-hidden').value = '';
    document.getElementById('modal-product-title').innerText = 'Add New Product';
    
    // Enable stock input for new product
    document.getElementById('product-stock').parentElement.classList.remove('hidden');
    document.getElementById('product-stock').required = true;
    
    openModal('product');
}

async function openEditProductModal(id) {
    try {
        const product = await apiFetch(`/products/${id}`);
        document.getElementById('product-id-hidden').value = product.id;
        document.getElementById('product-name').value = product.name;
        document.getElementById('product-category').value = product.category;
        document.getElementById('product-price').value = product.price;
        document.getElementById('product-description').value = product.description;
        document.getElementById('modal-product-title').innerText = 'Edit Product Details';
        
        // Hide stock input for edit (must adjust stock via dedicated modal)
        document.getElementById('product-stock').parentElement.classList.add('hidden');
        document.getElementById('product-stock').required = false;
        
        openModal('product');
    } catch (err) {
        showToast('Failed to fetch product details', 'error');
    }
}

async function saveProduct(e) {
    e.preventDefault();
    const id = document.getElementById('product-id-hidden').value;
    const name = document.getElementById('product-name').value;
    const category = document.getElementById('product-category').value;
    const price = parseFloat(document.getElementById('product-price').value);
    const description = document.getElementById('product-description').value;
    
    const body = { name, category, price, description };
    
    let method = 'POST';
    let url = '/products';
    
    if (id) {
        method = 'PUT';
        url = `/products/${id}`;
    } else {
        body.stockQty = parseInt(document.getElementById('product-stock').value) || 0;
    }
    
    try {
        await apiFetch(url, {
            method,
            body: JSON.stringify(body)
        });
        showToast(id ? 'Product details updated' : 'Product successfully added', 'success');
        closeModal('product');
        loadInventoryData();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// Stock Adjustment operations
function openStockModal(productId, productName, currentQty) {
    document.getElementById('stock-product-id-hidden').value = productId;
    document.getElementById('stock-product-name').innerText = productName;
    document.getElementById('stock-current-qty').innerText = currentQty;
    document.getElementById('stock-adjust-qty').value = '';
    openModal('stock');
}

async function handleStockAdjust(e) {
    e.preventDefault();
    const productId = document.getElementById('stock-product-id-hidden').value;
    const qty = parseInt(document.getElementById('stock-adjust-qty').value);
    
    try {
        await apiFetch(`/products/${productId}/stock`, {
            method: 'PATCH',
            body: JSON.stringify({ quantity: qty })
        });
        showToast('Stock level adjusted successfully', 'success');
        closeModal('stock');
        loadInventoryData();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function getCategoryIcon(category) {
    if (!category) return 'fa-box';
    const cat = category.toLowerCase();
    if (cat.includes('electr')) return 'fa-laptop';
    if (cat.includes('foot')) return 'fa-shoe-prints';
    if (cat.includes('kitchen') || cat.includes('home')) return 'fa-mug-hot';
    if (cat.includes('furnit') || cat.includes('office') || cat.includes('chair')) return 'fa-chair';
    return 'fa-box';
}

// ==================== STOREFRONT & CART SERVICES (Customer) ====================
async function loadStorefrontData(query = '') {
    const grid = document.getElementById('store-products-grid');
    grid.innerHTML = `<div class="text-center width-full" style="grid-column: 1/-1;">Loading product catalog...</div>`;
    
    try {
        const response = await apiFetch('/products?size=100');
        let products = response.content;
        
        if (query) {
            products = products.filter(p => p.name.toLowerCase().includes(query.toLowerCase()));
        }
        
        grid.innerHTML = '';
        if (products.length === 0) {
            grid.innerHTML = `<div class="text-center width-full" style="grid-column: 1/-1;">No products found in the catalog.</div>`;
            return;
        }
        
        products.forEach(p => {
            const card = document.createElement('div');
            card.className = 'product-card';
            
            // Check current cart qty
            const cartItem = state.cart.find(item => item.product.id === p.id);
            const initialQty = cartItem ? cartItem.quantity : 1;
            
            const isLowStock = p.stockQty > 0 && p.stockQty <= 5;
            let stockBadgeClass = 'badge-delivered';
            let stockText = `In Stock (${p.stockQty})`;
            
            if (p.stockQty <= 0) {
                stockBadgeClass = 'badge-cancelled';
                stockText = 'Out of Stock';
            } else if (isLowStock) {
                stockBadgeClass = 'badge-pending';
                stockText = `Only ${p.stockQty} left!`;
            }
            
            card.innerHTML = `
                <div class="product-image-container">
                    <i class="fa-solid ${getCategoryIcon(p.category)} category-large-icon"></i>
                </div>
                <div class="product-card-top">
                    <div class="product-meta">
                        <span class="product-category">${p.category}</span>
                        <span class="badge ${stockBadgeClass}">
                            ${stockText}
                        </span>
                    </div>
                    <h4>${p.name}</h4>
                    <p>${p.description || 'No description provided.'}</p>
                </div>
                <div class="product-card-bottom">
                    <div class="product-price-row">
                        <span class="product-price">₹${p.price.toFixed(2)}</span>
                        ${p.stockQty > 0 ? `
                            <div class="qty-adjuster">
                                <button class="btn-qty" onclick="adjustStorefrontQty(this, -1)">-</button>
                                <span class="qty-val" id="qty-val-${p.id}">${initialQty}</span>
                                <button class="btn-qty" onclick="adjustStorefrontQty(this, 1, ${p.stockQty})">+</button>
                            </div>
                        ` : ''}
                    </div>
                    ${p.stockQty > 0 ? `
                        <button class="btn btn-primary btn-add-to-cart" onclick="addToCart(${p.id}, '${p.name.replace(/'/g, "\\'")}', ${p.price})">
                            <i class="fa-solid fa-cart-plus"></i> Add to Cart
                        </button>
                    ` : `
                        <button class="btn btn-secondary btn-add-to-cart" disabled>Unavailable</button>
                    `}
                </div>
            `;
            grid.appendChild(card);
        });
    } catch (err) {
        showToast('Failed to load store products', 'error');
    }
}

function adjustStorefrontQty(btn, change, max = 999) {
    const qtySpan = btn.parentElement.querySelector('.qty-val');
    let current = parseInt(qtySpan.innerText) || 1;
    let next = current + change;
    if (next < 1) next = 1;
    if (next > max) {
        next = max;
        showToast(`Cannot select more than available stock (${max})`, 'info');
    }
    qtySpan.innerText = next;
}

function addToCart(productId, name, price) {
    const qtyVal = parseInt(document.getElementById(`qty-val-${productId}`).innerText) || 1;
    
    // Check if item is already in cart
    const existingIndex = state.cart.findIndex(item => item.product.id === productId);
    if (existingIndex > -1) {
        state.cart[existingIndex].quantity = qtyVal;
    } else {
        state.cart.push({
            product: { id: productId, name, price },
            quantity: qtyVal
        });
    }
    
    updateCartBadge();
    showToast(`Added ${qtyVal}x "${name}" to your cart`, 'success');
}

function updateCartBadge() {
    const badge = document.getElementById('cart-count-badge');
    const totalItems = state.cart.reduce((sum, item) => sum + item.quantity, 0);
    badge.innerText = totalItems;
}

function showCartModal() {
    const tableBody = document.getElementById('cart-table-body');
    tableBody.innerHTML = '';
    
    if (state.cart.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="5" class="text-center">Your cart is empty.</td></tr>`;
        document.getElementById('cart-total-qty').innerText = '0';
        document.getElementById('cart-total-amt').innerText = '₹0.00';
        document.getElementById('checkout-form').classList.add('hidden');
        openModal('cart');
        return;
    }
    
    document.getElementById('checkout-form').classList.remove('hidden');
    
    let totalQty = 0;
    let totalAmt = 0;
    
    state.cart.forEach((item, index) => {
        const subtotal = item.product.price * item.quantity;
        totalQty += item.quantity;
        totalAmt += subtotal;
        
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong>${item.product.name}</strong></td>
            <td>₹${item.product.price.toFixed(2)}</td>
            <td>
                <div class="qty-adjuster">
                    <button class="btn-qty" onclick="adjustCartItemQty(${index}, -1)">-</button>
                    <span class="qty-val">${item.quantity}</span>
                    <button class="btn-qty" onclick="adjustCartItemQty(${index}, 1)">+</button>
                </div>
            </td>
            <td>₹${subtotal.toFixed(2)}</td>
            <td>
                <button class="btn btn-danger btn-table-action" onclick="removeCartItem(${index})">
                    <i class="fa-solid fa-xmark"></i>
                </button>
            </td>
        `;
        tableBody.appendChild(tr);
    });
    
    document.getElementById('cart-total-qty').innerText = totalQty;
    document.getElementById('cart-total-amt').innerText = `₹${totalAmt.toFixed(2)}`;
    
    openModal('cart');
}

function adjustCartItemQty(index, change) {
    state.cart[index].quantity += change;
    if (state.cart[index].quantity < 1) {
        removeCartItem(index);
    } else {
        updateCartBadge();
        showCartModal();
    }
}

function removeCartItem(index) {
    state.cart.splice(index, 1);
    updateCartBadge();
    showCartModal();
}

async function handleCheckout(e) {
    e.preventDefault();
    if (state.cart.length === 0) return;
    
    const shippingAddress = document.getElementById('checkout-address').value;
    const notes = document.getElementById('checkout-notes').value;
    
    const items = state.cart.map(item => ({
        productId: item.product.id,
        quantity: item.quantity
    }));
    
    try {
        const orderRequest = {
            shippingAddress,
            notes,
            items
        };
        
        await apiFetch('/orders', {
            method: 'POST',
            body: JSON.stringify(orderRequest)
        });
        
        showToast('Your order has been placed successfully!', 'success');
        state.cart = [];
        updateCartBadge();
        closeModal('cart');
        
        // Show Customer orders
        showView('myorders');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==================== ORDER MANAGEMENT SERVICES (Customer & Admin/Staff) ====================
async function loadOrdersData() {
    const tableBody = document.getElementById('orders-table-body');
    tableBody.innerHTML = `<tr><td colspan="6" class="text-center">Loading orders...</td></tr>`;
    
    const filterStatus = document.getElementById('filter-order-status').value;
    let endpoint = '/orders?size=100';
    if (filterStatus) {
        endpoint += `&status=${filterStatus}`;
    }
    
    try {
        const data = await apiFetch(endpoint);
        const orders = data.content;
        
        tableBody.innerHTML = '';
        if (orders.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="6" class="text-center">No orders found.</td></tr>`;
            return;
        }
        
        orders.forEach(o => {
            const dateStr = new Date(o.placedAt).toLocaleString();
            const statusBadgeClass = `badge-${o.status.toLowerCase()}`;
            
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>#${o.id}</td>
                <td>${dateStr}</td>
                <td>${o.userEmail}</td>
                <td>₹${o.totalAmount.toFixed(2)}</td>
                <td><span class="badge ${statusBadgeClass}">${o.status}</span></td>
                <td>
                    <div class="actions-cell-buttons">
                        <button class="btn btn-secondary btn-table-action" onclick="viewOrderDetails(${o.id})" title="Details">
                            <i class="fa-solid fa-eye"></i>
                        </button>
                        ${o.status === 'PENDING' ? `
                            <button class="btn btn-primary btn-table-action" onclick="updateOrderStatus(${o.id}, 'CONFIRMED')" title="Accept Order" style="background: var(--success-grad); border-color: transparent;">
                                <i class="fa-solid fa-circle-check"></i>
                            </button>
                        ` : ''}
                        ${o.status === 'CONFIRMED' ? `
                            <button class="btn btn-primary btn-table-action" onclick="updateOrderStatus(${o.id}, 'PROCESSING')" title="Process Order" style="background: var(--primary-grad); border-color: transparent;">
                                <i class="fa-solid fa-gears"></i>
                            </button>
                        ` : ''}
                        ${o.status === 'PROCESSING' ? `
                            <button class="btn btn-accent btn-table-action" onclick="updateOrderStatus(${o.id}, 'SHIPPED')" title="Ship Order" style="background: var(--accent-grad); border-color: transparent;">
                                <i class="fa-solid fa-truck"></i>
                            </button>
                        ` : ''}
                        ${o.status === 'SHIPPED' ? `
                            <button class="btn btn-primary btn-table-action" onclick="updateOrderStatus(${o.id}, 'DELIVERED')" title="Deliver Order" style="background: var(--success-grad); border-color: transparent;">
                                <i class="fa-solid fa-box-open"></i>
                            </button>
                        ` : ''}
                        ${(o.status === 'PENDING' || o.status === 'CONFIRMED') ? `
                            <button class="btn btn-danger btn-table-action" onclick="cancelOrder(${o.id})" title="Cancel Order">
                                <i class="fa-solid fa-ban"></i>
                            </button>
                        ` : ''}
                        <button class="btn btn-secondary btn-table-action" onclick="downloadInvoice(${o.id})" title="Download Invoice">
                            <i class="fa-solid fa-file-pdf"></i>
                        </button>
                    </div>
                </td>
            `;
            tableBody.appendChild(tr);
        });
    } catch (err) {
        showToast('Failed to load orders list', 'error');
    }
}

async function loadMyOrdersData() {
    const tableBody = document.getElementById('myorders-table-body');
    tableBody.innerHTML = `<tr><td colspan="5" class="text-center">Loading orders...</td></tr>`;
    
    try {
        const data = await apiFetch('/orders/my?size=100');
        const orders = data.content;
        
        tableBody.innerHTML = '';
        if (orders.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="5" class="text-center">You have not placed any orders yet.</td></tr>`;
            return;
        }
        
        orders.forEach(o => {
            const dateStr = new Date(o.placedAt).toLocaleString();
            const statusBadgeClass = `badge-${o.status.toLowerCase()}`;
            
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>#${o.id}</td>
                <td>${dateStr}</td>
                <td>₹${o.totalAmount.toFixed(2)}</td>
                <td><span class="badge ${statusBadgeClass}">${o.status}</span></td>
                <td>
                    <div class="actions-cell-buttons">
                        <button class="btn btn-secondary btn-table-action" onclick="viewOrderDetails(${o.id})" title="View Details">
                            <i class="fa-solid fa-eye"></i>
                        </button>
                        ${o.status === 'PENDING' ? `
                            <button class="btn btn-danger btn-table-action" onclick="cancelOrder(${o.id})" title="Cancel Order">
                                <i class="fa-solid fa-ban"></i>
                            </button>
                        ` : ''}
                        <button class="btn btn-secondary btn-table-action" onclick="downloadInvoice(${o.id})" title="Download Invoice">
                            <i class="fa-solid fa-file-pdf"></i>
                        </button>
                    </div>
                </td>
            `;
            tableBody.appendChild(tr);
        });
    } catch (err) {
        showToast('Failed to retrieve your order history', 'error');
    }
}

async function updateOrderStatus(orderId, nextStatus) {
    try {
        await apiFetch(`/orders/${orderId}/status`, {
            method: 'PATCH',
            body: JSON.stringify({ status: nextStatus })
        });
        showToast(`Order status updated to ${nextStatus}`, 'success');
        loadOrdersData();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function cancelOrder(orderId) {
    if (!confirm('Are you sure you want to cancel this order?')) return;
    try {
        await apiFetch(`/orders/${orderId}/cancel`, {
            method: 'PATCH'
        });
        showToast('Order cancelled successfully', 'success');
        if (state.user.role === 'CUSTOMER') {
            loadMyOrdersData();
        } else {
            loadOrdersData();
        }
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function downloadInvoice(orderId) {
    try {
        showToast('Generating invoice PDF...', 'info');
        const blob = await apiFetch(`/orders/${orderId}/invoice`);
        const link = document.createElement('a');
        link.href = window.URL.createObjectURL(blob);
        link.download = `invoice-${orderId}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        showToast('Invoice downloaded successfully', 'success');
    } catch (err) {
        showToast('Failed to download invoice', 'error');
    }
}

async function viewOrderDetails(orderId) {
    try {
        const order = await apiFetch(`/orders/${orderId}`);
        
        document.getElementById('detail-order-id').innerText = order.id;
        document.getElementById('detail-order-date').innerText = new Date(order.placedAt).toLocaleString();
        document.getElementById('detail-order-customer').innerText = order.userEmail;
        document.getElementById('detail-order-address').innerText = order.shippingAddress;
        document.getElementById('detail-order-notes').innerText = order.notes || 'N/A';
        
        const statusBadge = document.getElementById('detail-order-status');
        statusBadge.innerText = order.status;
        statusBadge.className = 'badge';
        statusBadge.classList.add(`badge-${order.status.toLowerCase()}`);
        
        document.getElementById('detail-order-total').innerText = `₹${order.totalAmount.toFixed(2)}`;
        
        const itemsBody = document.getElementById('order-items-table-body');
        itemsBody.innerHTML = '';
        
        order.orderItems.forEach(item => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><strong>${item.productName}</strong></td>
                <td>₹${item.unitPrice.toFixed(2)}</td>
                <td>${item.quantity}</td>
                <td>₹${item.subtotal.toFixed(2)}</td>
            `;
            itemsBody.appendChild(tr);
        });
        
        openModal('order-details');
    } catch (err) {
        showToast('Failed to retrieve order details', 'error');
    }
}

// ==================== PROFILE SERVICES ====================
function fillProfileForm() {
    if (!state.user) return;
    document.getElementById('profile-username').value = state.user.username;
    document.getElementById('profile-email').value = state.user.email;
    document.getElementById('profile-phone').value = state.user.phone || '';
    document.getElementById('profile-role').value = state.user.role;
}

async function updateProfile(e) {
    e.preventDefault();
    const username = document.getElementById('profile-username').value;
    const phone = document.getElementById('profile-phone').value;
    
    try {
        const updatedProfile = await apiFetch('/users/me', {
            method: 'PATCH',
            body: JSON.stringify({ username, phone })
        });
        
        state.user = updatedProfile;
        document.getElementById('user-display-name').innerText = updatedProfile.username;
        showToast('Profile settings saved successfully', 'success');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==================== EVENT LISTENERS & MODAL UTILS ====================
function setupEventListeners() {
    // Auth Switch Link Toggles
    document.getElementById('link-go-register').addEventListener('click', (e) => {
        e.preventDefault();
        document.getElementById('login-form').classList.add('hidden');
        document.getElementById('register-form').classList.remove('hidden');
        document.getElementById('toggle-to-register').classList.add('hidden');
        document.getElementById('toggle-to-login').classList.remove('hidden');
        document.getElementById('auth-subtitle').innerText = 'Create your account to start managing orders.';
    });

    document.getElementById('link-go-login').addEventListener('click', (e) => {
        e.preventDefault();
        document.getElementById('login-form').classList.remove('hidden');
        document.getElementById('register-form').classList.add('hidden');
        document.getElementById('toggle-to-register').classList.remove('hidden');
        document.getElementById('toggle-to-login').classList.add('hidden');
        document.getElementById('auth-subtitle').innerText = 'Welcome back! Please login to your account.';
    });

    // Login Form Submit
    document.getElementById('login-form').addEventListener('submit', (e) => {
        e.preventDefault();
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;
        handleLogin(email, password);
    });

    // Register Form Submit
    document.getElementById('register-form').addEventListener('submit', (e) => {
        e.preventDefault();
        const username = document.getElementById('register-username').value;
        const email = document.getElementById('register-email').value;
        const password = document.getElementById('register-password').value;
        const phone = document.getElementById('register-phone').value;
        handleRegister(username, email, password, phone);
    });

    // Sidebar navigation clicks
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const view = item.id.replace('nav-btn-', '');
            showView(view);
        });
    });

    // Log Out button click
    document.getElementById('btn-logout').addEventListener('click', handleLogout);

    // Profile Form submit
    document.getElementById('profile-form').addEventListener('submit', updateProfile);

    // Inventory Search
    document.getElementById('inventory-search').addEventListener('input', (e) => {
        loadInventoryData(e.target.value);
    });

    // Store Search
    document.getElementById('store-search').addEventListener('input', (e) => {
        loadStorefrontData(e.target.value);
    });

    // Order status filter change
    document.getElementById('filter-order-status').addEventListener('change', loadOrdersData);

    // Modals setup
    document.getElementById('btn-add-product').addEventListener('click', openAddProductModal);
    document.getElementById('product-form').addEventListener('submit', saveProduct);
    document.getElementById('stock-form').addEventListener('submit', handleStockAdjust);
    document.getElementById('btn-view-cart').addEventListener('click', showCartModal);
    document.getElementById('checkout-form').addEventListener('submit', handleCheckout);
    
    // Close modals hooks
    document.querySelectorAll('.close-modal-btn, .btn-secondary').forEach(btn => {
        btn.addEventListener('click', () => {
            const modal = btn.closest('.modal');
            if (modal) {
                closeModal(modal.id.replace('modal-', ''));
            }
        });
    });
    
    // Clicking outside modal content closes it
    window.addEventListener('click', (e) => {
        if (e.target.classList.contains('modal')) {
            closeModal(e.target.id.replace('modal-', ''));
        }
    });

    // Mobile Sidebar toggle
    document.getElementById('sidebar-toggle').addEventListener('click', () => {
        const sidebar = document.querySelector('.sidebar');
        if (sidebar.style.display === 'flex') {
            sidebar.style.display = 'none';
        } else {
            sidebar.style.display = 'flex';
        }
    });
}

function openModal(id) {
    document.getElementById(`modal-${id}`).classList.add('active');
}

function closeModal(id) {
    document.getElementById(`modal-${id}`).classList.remove('active');
}

// Toast alerts utility
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    let icon = 'fa-check-circle';
    if (type === 'error') icon = 'fa-times-circle';
    if (type === 'info') icon = 'fa-info-circle';
    
    toast.innerHTML = `
        <i class="fa-solid ${icon}"></i>
        <span>${message}</span>
    `;
    container.appendChild(toast);
    
    setTimeout(() => {
        toast.style.animation = 'slideIn 0.3s ease reverse forwards';
        setTimeout(() => {
            container.removeChild(toast);
        }, 300);
    }, 4000);
}
