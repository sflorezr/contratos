// zonas.js
// Variables globales
let zonas = [];
let usuarioActual = null;
let modalZona = null;
let currentEditUuid = null;

// Inicialización
document.addEventListener('DOMContentLoaded', function() {
    modalZona = new bootstrap.Modal(document.getElementById('modalZona'));
    inicializar();
    
    // Event listeners
    document.getElementById('btnNewZone').addEventListener('click', openNewZoneModal);
    document.getElementById('btnNewZone2').addEventListener('click', openNewZoneModal);
    document.getElementById('btnAplicarFiltros').addEventListener('click', aplicarFiltros);
    document.getElementById('saveZonaBtn').addEventListener('click', saveZona);
});

async function inicializar() {
    cargarUsuarioActual();
    await Promise.all([
        cargarZonas(),
    ]);
    actualizarEstadisticas();
}

function cargarUsuarioActual() {
    // En una implementación real, esto vendría del servidor
    usuarioActual = {
        uuid: 'user-uuid',
        nombre: 'Administrador',
        perfil: 'ADMINISTRADOR'
    };
    
    document.getElementById('currentUserName').textContent = usuarioActual.nombre;
    document.getElementById('currentUserRole').textContent = usuarioActual.perfil;
}

async function cargarZonas() {
    showLoading(true);
    try {
        const response = await fetch('/admin/zonas/api/listar');
        if (response.ok) {
            zonas = await response.json();
            renderZonas();
            actualizarEstadisticas();
        } else {
            showAlert('Error al cargar zonas', 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error de conexión', 'danger');
    } finally {
        showLoading(false);
    }
}


function renderZonas() {
    const container = document.getElementById('zonasContainer');
    const emptyState = document.getElementById('emptyState');
    
    if (zonas.length === 0) {
        container.innerHTML = '';
        emptyState.classList.remove('d-none');
        return;
    }
    
    emptyState.classList.add('d-none');
    container.innerHTML = zonas.map(zona => `
        <div class="contract-card">
            <div class="contract-header">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <h5 class="mb-1">
                            <i class="fas fa-map text-primary me-2"></i>
                            ${zona.nombre}
                        </h5>
                        <p class="text-muted mb-0">${zona.codigo}</p>
                    </div>
                    <span class="estado-badge ${zona.activo ? 'estado-activo' : 'estado-cancelado'}">
                        ${zona.activo ? 'Activo' : 'Inactivo'}
                    </span>
                </div>
            </div>
            <div class="contract-body">
                <div class="row">
                    <div class="col-md-6">
                        <div class="mb-3">
                            <small class="text-muted d-block">Sectores</small>
                            <span><i class="fas fa-layer-group me-1"></i>${zona.totalSectores || 0}</span>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="mb-3">
                            <small class="text-muted d-block">Predios</small>
                            <span><i class="fas fa-map-marker-alt me-1"></i>${zona.totalPredios || 0}</span>
                        </div>
                        <div class="mb-3">
                            <small class="text-muted d-block">Operarios</small>
                            <span><i class="fas fa-users me-1"></i>${zona.totalOperarios || 0}</span>
                        </div>
                    </div>
                </div>
                
                <div class="mt-3">
                    <p class="mb-0">${zona.descripcion || 'Sin descripción'}</p>
                </div>
            </div>
            <div class="contract-footer">
                <div class="action-buttons">
                    <a href="/admin/zonas/${zona.uuid}" class="btn btn-sm btn-outline-primary">
                        <i class="fas fa-eye me-1"></i>Ver Detalle
                    </a>
                    <button class="btn btn-sm btn-outline-info" onclick="editarZona('${zona.uuid}')">
                        <i class="fas fa-edit me-1"></i>Editar
                    </button>
                    <button class="btn btn-sm btn-outline-warning" onclick="cambiarEstadoZona('${zona.uuid}')">
                        <i class="fas fa-exchange-alt me-1"></i>Estado
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="eliminarZona('${zona.uuid}')">
                        <i class="fas fa-trash me-1"></i>Eliminar
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

function actualizarEstadisticas() {
    const total = zonas.length;
    const activas = zonas.filter(z => z.activo).length;
    const inactivas = total - activas;
    const sectores = zonas.reduce((acc, zona) => acc + (zona.totalSectores || 0), 0);
    
    document.getElementById('totalZonas').textContent = total;
    document.getElementById('zonasActivas').textContent = activas;
    document.getElementById('zonasInactivas').textContent = inactivas;
    document.getElementById('sectoresAsociados').textContent = sectores;
}

function openNewZoneModal() {
    currentEditUuid = null;
    document.getElementById('isEdit').value = 'false';
    document.getElementById('modalZonaTitle').textContent = 'Nueva Zona';
    document.getElementById('zonaForm').reset();
    modalZona.show();
}

async function editarZona(uuid) {
    currentEditUuid = uuid;
    document.getElementById('isEdit').value = 'true';
    document.getElementById('zonaUuid').value = uuid;
    document.getElementById('modalZonaTitle').textContent = 'Editar Zona';
    
    const zona = zonas.find(z => z.uuid === uuid);
    if (!zona) {
        showAlert('Zona no encontrada', 'danger');
        return;
    }
    
    // Llenar el formulario
    document.getElementById('nombre').value = zona.nombre;
    document.getElementById('codigo').value = zona.codigo;
    document.getElementById('activo').value = zona.activo.toString();
    document.getElementById('descripcion').value = zona.descripcion || '';
    
    modalZona.show();
}

async function saveZona() {
    const form = document.getElementById('zonaForm');
    const isEdit = document.getElementById('isEdit').value === 'true';
    
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    const formData = new FormData(form);
    const data = Object.fromEntries(formData);
    
    // Convertir activo a booleano
    data.activo = data.activo === 'true';
    
    const saveBtn = document.getElementById('saveZonaBtn');
    const originalText = saveBtn.innerHTML;
    saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Guardando...';
    saveBtn.disabled = true;
    
    try {
        const url = isEdit 
            ? `/admin/zonas/api/${currentEditUuid}`
            : '/admin/zonas/api/crear';
            
        const method = isEdit ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });
        
        if (response.ok) {
            const result = await response.json();
            modalZona.hide();
            showAlert(isEdit ? 'Zona actualizada exitosamente' : 'Zona creada exitosamente', 'success');
            cargarZonas();
        } else {
            const error = await response.json();
            showAlert(error.message || 'Error al guardar zona', 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error de conexión', 'danger');
    } finally {
        saveBtn.innerHTML = originalText;
        saveBtn.disabled = false;
    }
}

async function cambiarEstadoZona(uuid) {
    const zona = zonas.find(z => z.uuid === uuid);
    if (!zona) return;
    
    const result = await Swal.fire({
        title: 'Cambiar Estado de la Zona',
        text: `¿Está seguro de cambiar el estado de ${zona.nombre}?`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Sí, cambiar estado',
        cancelButtonText: 'Cancelar'
    });
    
    if (result.isConfirmed) {
        try {
            const response = await fetch(`/admin/zonas/api/${uuid}/estado`, {
                method: 'PATCH'
            });
            
            if (response.ok) {
                showAlert('Estado actualizado exitosamente', 'success');
                cargarZonas();
            } else {
                const error = await response.json();
                showAlert(error.message || 'Error al cambiar estado', 'danger');
            }
        } catch (error) {
            console.error('Error:', error);
            showAlert('Error de conexión', 'danger');
        }
    }
}

async function eliminarZona(uuid) {
    const zona = zonas.find(z => z.uuid === uuid);
    if (!zona) return;
    
    const result = await Swal.fire({
        title: '¿Eliminar zona?',
        text: `¿Está seguro de eliminar la zona ${zona.nombre}? Esta acción no se puede deshacer.`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    });
    
    if (result.isConfirmed) {
        try {
            const response = await fetch(`/admin/zonas/api/${uuid}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                showAlert('Zona eliminada exitosamente', 'success');
                cargarZonas();
            } else {
                const error = await response.json();
                showAlert(error.message || 'Error al eliminar zona', 'danger');
            }
        } catch (error) {
            console.error('Error:', error);
            showAlert('Error de conexión', 'danger');
        }
    }
}

function aplicarFiltros() {
    const nombre = document.getElementById('filtroNombre').value.toLowerCase();
    const estado = document.getElementById('filtroEstado').value;
    
    let zonasFiltradas = zonas;
    
    if (nombre) {
        zonasFiltradas = zonasFiltradas.filter(z => 
            z.nombre.toLowerCase().includes(nombre) ||
            (z.codigo && z.codigo.toLowerCase().includes(nombre))
        );
    }
    
    if (estado) {
        const activo = estado === 'true';
        zonasFiltradas = zonasFiltradas.filter(z => z.activo === activo);
    }
    
    // Temporalmente actualizar la lista
    const zonasOriginales = zonas;
    zonas = zonasFiltradas;
    renderZonas();
    zonas = zonasOriginales;
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('es-ES', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

function showAlert(message, type) {
    const container = document.getElementById('alertsContainer');
    const alertId = 'alert-' + Date.now();
    
    const alertHtml = `
        <div id="${alertId}" class="alert alert-${type} alert-dismissible fade show">
            <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'warning' ? 'exclamation-triangle' : 'times-circle'} me-2"></i>
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    
    container.insertAdjacentHTML('afterbegin', alertHtml);
    
    setTimeout(() => {
        const alert = document.getElementById(alertId);
        if (alert) alert.remove();
    }, 5000);
}

function showLoading(show) {
    const loading = document.getElementById('loadingIndicator');
    const container = document.getElementById('zonasContainer');
    
    if (show) {
        loading.classList.remove('d-none');
        container.innerHTML = '';
    } else {
        loading.classList.add('d-none');
    }
}

// Si no tienes SweetAlert2, usar confirm nativo
if (typeof Swal === 'undefined') {
    window.Swal = {
        fire: async (options) => {
            const confirmado = confirm(options.text || options.title);
            return { isConfirmed: confirmado };
        }
    };
}