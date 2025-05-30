// predios.js - Gestión de Predios

// Variables globales
let predios = [];
let sectores = [];
let modalPredio = null;
let currentEditUuid = null;

// Inicialización cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', function() {
    modalPredio = new bootstrap.Modal(document.getElementById('modalPredio'));
    inicializar();
    configurarEventListeners();
});

// Configurar event listeners
function configurarEventListeners() {
    // Validación de coordenadas en tiempo real
    const latitudInput = document.getElementById('latitud');
    if (latitudInput) {
        latitudInput.addEventListener('input', function(e) {
            const value = parseFloat(e.target.value);
            if (value < -90 || value > 90) {
                e.target.setCustomValidity('La latitud debe estar entre -90 y 90');
            } else {
                e.target.setCustomValidity('');
            }
        });
    }
    
    const longitudInput = document.getElementById('longitud');
    if (longitudInput) {
        longitudInput.addEventListener('input', function(e) {
            const value = parseFloat(e.target.value);
            if (value < -180 || value > 180) {
                e.target.setCustomValidity('La longitud debe estar entre -180 y 180');
            } else {
                e.target.setCustomValidity('');
            }
        });
    }
    
    // Formatear código catastral mientras se escribe
    const codigoCatastralInput = document.getElementById('codigoCatastral');
    if (codigoCatastralInput) {
        codigoCatastralInput.addEventListener('input', function(e) {
            // Convertir a mayúsculas automáticamente
            e.target.value = e.target.value.toUpperCase();
        });
    }
}

// Inicialización principal
async function inicializar() {
    await Promise.all([
        cargarPredios(),
        cargarSectores()
    ]);
    actualizarEstadisticas();
}

// Cargar predios desde el servidor
async function cargarPredios() {
    showLoading(true);
    try {
        const response = await fetch('/admin/predios/api/listar');
        if (response.ok) {
            predios = await response.json();
            renderPredios();
            actualizarEstadisticas();
        } else {
            showAlert('Error al cargar predios', 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error de conexión', 'danger');
    } finally {
        showLoading(false);
    }
}

// Cargar sectores para los selects
async function cargarSectores() {
    try {
        const response = await fetch('/admin/sectores/api/listar');
        if (response.ok) {
            sectores = await response.json();
            actualizarSelectSectores();
        }
    } catch (error) {
        console.error('Error cargando sectores:', error);
    }
}

// Actualizar los selects de sectores
function actualizarSelectSectores() {
    const select = document.getElementById('sectorUuid');
    const filtroSelect = document.getElementById('filtroSector');
    
    select.innerHTML = '<option value="">Seleccionar sector...</option>';
    filtroSelect.innerHTML = '<option value="">Todos los sectores</option>';
    
    sectores.forEach(sector => {
        select.innerHTML += `<option value="${sector.uuid}">${sector.nombre}</option>`;
        filtroSelect.innerHTML += `<option value="${sector.uuid}">${sector.nombre}</option>`;
    });
}

// Renderizar la lista de predios
function renderPredios() {
    const container = document.getElementById('prediosContainer');
    const emptyState = document.getElementById('emptyState');
    
    if (predios.length === 0) {
        container.innerHTML = '';
        emptyState.classList.remove('d-none');
        return;
    }
    
    emptyState.classList.add('d-none');
    container.innerHTML = predios.map(predio => `
        <div class="predio-card">
            <div class="predio-header">
                <div>
                    <h5 class="mb-1">${predio.direccion}</h5>
                    <span class="text-muted">Código: ${predio.codigoCatastral}</span>
                </div>
                <div>
                    <span class="tipo-badge tipo-${predio.tipo.toLowerCase()}">
                        ${predio.tipo}
                    </span>
                    <span class="badge ${predio.activo ? 'bg-success' : 'bg-secondary'} ms-2">
                        ${predio.activo ? 'Activo' : 'Inactivo'}
                    </span>
                </div>
            </div>
            <div class="predio-body">
                <div class="row">
                    <div class="col-md-6">
                        <div class="predio-info-row">
                            <i class="fas fa-map"></i>
                            <span>Sector: ${predio.sectorNombre || 'Sin definir'}</span>
                        </div>
                        <div class="predio-info-row">
                            <i class="fas fa-ruler-square"></i>
                            <span>Área: ${predio.area ? predio.area + ' m²' : 'No especificada'}</span>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="predio-info-row">
                            <i class="fas fa-file-contract"></i>
                            <span>Contrato: ${predio.contratoActual || 'Sin asignar'}</span>
                        </div>
                        <div class="predio-info-row">
                            <i class="fas fa-user"></i>
                            <span>Operario: ${predio.operarioAsignado || 'Sin asignar'}</span>
                        </div>
                    </div>
                </div>
                ${predio.observaciones ? `
                <div class="mt-3">
                    <small class="text-muted">
                        <i class="fas fa-info-circle me-1"></i>
                        ${predio.observaciones}
                    </small>
                </div>
                ` : ''}
            </div>
            <div class="mt-3">
                <div class="action-buttons">
                    <button class="btn btn-sm btn-outline-primary" onclick="verPredio('${predio.uuid}')">
                        <i class="fas fa-eye me-1"></i>Ver
                    </button>
                    <button class="btn btn-sm btn-outline-info" onclick="editarPredio('${predio.uuid}')">
                        <i class="fas fa-edit me-1"></i>Editar
                    </button>
                    <button class="btn btn-sm btn-outline-warning" onclick="cambiarEstadoPredio('${predio.uuid}')">
                        <i class="fas fa-toggle-${predio.activo ? 'on' : 'off'} me-1"></i>Estado
                    </button>
                    ${!predio.contratoActual ? `
                    <button class="btn btn-sm btn-outline-danger" onclick="eliminarPredio('${predio.uuid}')">
                        <i class="fas fa-trash me-1"></i>Eliminar
                    </button>
                    ` : ''}
                </div>
            </div>
        </div>
    `).join('');
}

// Actualizar estadísticas
function actualizarEstadisticas() {
    const total = predios.length;
    const urbanos = predios.filter(p => p.tipo === 'URBANO').length;
    const rurales = predios.filter(p => p.tipo === 'RURAL').length;
    const disponibles = predios.filter(p => !p.contratoActual && p.activo).length;
    
    document.getElementById('totalPredios').textContent = total;
    document.getElementById('prediosUrbanos').textContent = urbanos;
    document.getElementById('prediosRurales').textContent = rurales;
    document.getElementById('prediosDisponibles').textContent = disponibles;
}

// Abrir modal para nuevo predio
function openNewPredioModal() {
    currentEditUuid = null;
    document.getElementById('isEdit').value = 'false';
    document.getElementById('modalPredioTitle').textContent = 'Nuevo Predio';
    document.getElementById('predioForm').reset();
    document.getElementById('activo').checked = true;
    
    // Limpiar clases de validación
    document.getElementById('predioForm').classList.remove('was-validated');
    
    modalPredio.show();
}

// Editar predio existente
async function editarPredio(uuid) {
    currentEditUuid = uuid;
    document.getElementById('isEdit').value = 'true';
    document.getElementById('predioUuid').value = uuid;
    document.getElementById('modalPredioTitle').textContent = 'Editar Predio';
    
    const predio = predios.find(p => p.uuid === uuid);
    if (!predio) {
        showAlert('Predio no encontrado', 'danger');
        return;
    }
    
    // Llenar el formulario
    document.getElementById('codigoCatastral').value = predio.codigoCatastral;
    document.getElementById('direccion').value = predio.direccion;
    document.getElementById('tipo').value = predio.tipo;
    document.getElementById('sectorUuid').value = predio.sectorUuid || '';
    document.getElementById('area').value = predio.area || '';
    document.getElementById('latitud').value = predio.latitud || '';
    document.getElementById('longitud').value = predio.longitud || '';
    document.getElementById('observaciones').value = predio.observaciones || '';
    document.getElementById('activo').checked = predio.activo;
    
    modalPredio.show();
}

// Guardar predio (crear o editar)
async function savePredio() {
    const form = document.getElementById('predioForm');
    const isEdit = document.getElementById('isEdit').value === 'true';
    
    // Validar formulario
    if (!form.checkValidity()) {
        form.classList.add('was-validated');
        return;
    }
    
    const formData = new FormData(form);
    const data = {
        codigoCatastral: formData.get('codigoCatastral'),
        direccion: formData.get('direccion'),
        tipo: formData.get('tipo'),
        sectorUuid: formData.get('sectorUuid'),
        area: formData.get('area') ? parseFloat(formData.get('area')) : null,
        latitud: formData.get('latitud') ? parseFloat(formData.get('latitud')) : null,
        longitud: formData.get('longitud') ? parseFloat(formData.get('longitud')) : null,
        observaciones: formData.get('observaciones'),
        activo: formData.get('activo') === 'on'
    };
    
    const saveBtn = document.getElementById('savePredioBtn');
    const originalText = saveBtn.innerHTML;
    saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Guardando...';
    saveBtn.disabled = true;
    
    try {
        const url = isEdit 
            ? `/admin/predios/${currentEditUuid}`
            : '/admin/predios';
            
        const method = isEdit ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });
        
        if (response.ok) {
            modalPredio.hide();
            showAlert(isEdit ? 'Predio actualizado exitosamente' : 'Predio creado exitosamente', 'success');
            cargarPredios();
        } else {
            const error = await response.json();
            showAlert(error.message || 'Error al guardar predio', 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error de conexión', 'danger');
    } finally {
        saveBtn.innerHTML = originalText;
        saveBtn.disabled = false;
    }
}

// Ver detalles del predio
async function verPredio(uuid) {
    const predio = predios.find(p => p.uuid === uuid);
    if (!predio) return;
    
    // En una implementación real, esto podría abrir una vista detallada
    showAlert(`Ver detalles del predio: ${predio.direccion}`, 'info');
}

// Cambiar estado del predio
async function cambiarEstadoPredio(uuid) {
    const predio = predios.find(p => p.uuid === uuid);
    if (!predio) return;
    
    const nuevoEstado = !predio.activo;
    const mensaje = nuevoEstado ? 'activar' : 'desactivar';
    
    if (confirm(`¿Está seguro de ${mensaje} el predio ${predio.direccion}?`)) {
        try {
            const response = await fetch(`/admin/predios/${uuid}/estado`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ activo: nuevoEstado })
            });
            
            if (response.ok) {
                showAlert(`Predio ${nuevoEstado ? 'activado' : 'desactivado'} exitosamente`, 'success');
                cargarPredios();
            } else {
                showAlert('Error al cambiar estado', 'danger');
            }
        } catch (error) {
            console.error('Error:', error);
            showAlert('Error de conexión', 'danger');
        }
    }
}

// Eliminar predio
async function eliminarPredio(uuid) {
    const predio = predios.find(p => p.uuid === uuid);
    if (!predio) return;
    
    if (confirm(`¿Está seguro de eliminar el predio ${predio.direccion}?\n\nEsta acción no se puede deshacer.`)) {
        try {
            const response = await fetch(`/admin/predios/${uuid}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                showAlert('Predio eliminado exitosamente', 'success');
                cargarPredios();
            } else {
                const error = await response.json();
                showAlert(error.message || 'No se puede eliminar el predio', 'danger');
            }
        } catch (error) {
            console.error('Error:', error);
            showAlert('Error de conexión', 'danger');
        }
    }
}

// Aplicar filtros de búsqueda
function aplicarFiltros() {
    const busqueda = document.getElementById('filtroBusqueda').value.toLowerCase();
    const sector = document.getElementById('filtroSector').value;
    const tipo = document.getElementById('filtroTipo').value;
    const estado = document.getElementById('filtroEstado').value;
    
    let prediosFiltrados = predios;
    
    if (busqueda) {
        prediosFiltrados = prediosFiltrados.filter(p => 
            p.direccion.toLowerCase().includes(busqueda) ||
            p.codigoCatastral.toLowerCase().includes(busqueda)
        );
    }
    
    if (sector) {
        prediosFiltrados = prediosFiltrados.filter(p => p.sectorUuid === sector);
    }
    
    if (tipo) {
        prediosFiltrados = prediosFiltrados.filter(p => p.tipo === tipo);
    }
    
    if (estado !== '') {
        const esActivo = estado === 'true';
        prediosFiltrados = prediosFiltrados.filter(p => p.activo === esActivo);
    }
    
    // Temporalmente actualizar la lista
    const prediosOriginales = predios;
    predios = prediosFiltrados;
    renderPredios();
    predios = prediosOriginales;
}

// Mostrar alertas
function showAlert(message, type) {
    const container = document.getElementById('alertsContainer');
    const alertId = 'alert-' + Date.now();
    
    const icons = {
        success: 'check-circle',
        danger: 'times-circle',
        warning: 'exclamation-triangle',
        info: 'info-circle'
    };
    
    const alertHtml = `
        <div id="${alertId}" class="alert alert-${type} alert-dismissible fade show">
            <i class="fas fa-${icons[type] || 'info-circle'} me-2"></i>
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    
    container.insertAdjacentHTML('afterbegin', alertHtml);
    
    // Auto-remove después de 5 segundos
    setTimeout(() => {
        const alert = document.getElementById(alertId);
        if (alert) alert.remove();
    }, 5000);
}

// Mostrar/ocultar loading
function showLoading(show) {
    const loading = document.getElementById('loadingIndicator');
    const container = document.getElementById('prediosContainer');
    
    if (show) {
        loading.classList.remove('d-none');
        container.innerHTML = '';
    } else {
        loading.classList.add('d-none');
    }
}

// Exportar funciones para uso global
window.openNewPredioModal = openNewPredioModal;
window.editarPredio = editarPredio;
window.savePredio = savePredio;
window.verPredio = verPredio;
window.cambiarEstadoPredio = cambiarEstadoPredio;
window.eliminarPredio = eliminarPredio;
window.aplicarFiltros = aplicarFiltros;