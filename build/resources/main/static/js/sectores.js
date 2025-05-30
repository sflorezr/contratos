// sectores.js - Gestión de Sectores

// Variables globales
let sectores = [];
let modalSector = null;
let currentEditUuid = null;

// Inicialización cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', function() {
    modalSector = new bootstrap.Modal(document.getElementById('modalSector'));
    inicializar();
    configurarEventListeners();
});

// Configurar event listeners
function configurarEventListeners() {
    // Formatear código mientras se escribe
    const codigoInput = document.getElementById('codigo');
    if (codigoInput) {
        codigoInput.addEventListener('input', function(e) {
            // Convertir a mayúsculas y permitir solo caracteres válidos
            e.target.value = e.target.value.toUpperCase().replace(/[^A-Z0-9-]/g, '');
        });
    }
}

// Inicialización principal
async function inicializar() {
    await cargarSectores();
    await cargarEstadisticas();
}

// Cargar sectores desde el servidor
async function cargarSectores() {
    showLoading(true);
    try {
        const response = await fetch('/admin/sectores/api/listar');
        if (response.ok) {
            sectores = await response.json();
            renderSectores();
        } else {
            showAlert('Error al cargar sectores', 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error de conexión', 'danger');
    } finally {
        showLoading(false);
    }
}

// Cargar estadísticas
async function cargarEstadisticas() {
    try {
        const response = await fetch('/admin/sectores/api/estadisticas');
        if (response.ok) {
            const stats = await response.json();
            actualizarEstadisticas(stats);
        }
    } catch (error) {
        console.error('Error cargando estadísticas:', error);
    }
}

// Actualizar estadísticas en la vista
function actualizarEstadisticas(stats) {
    document.getElementById('totalSectores').textContent = stats.total || 0;
    document.getElementById('sectoresActivos').textContent = stats.activos || 0;
    document.getElementById('sectoresConPredios').textContent = stats.conPredios || 0;
    document.getElementById('sectoresConContratos').textContent = stats.conContratos || 0;
    
    // Calcular área total y población total desde los sectores cargados
    const areaTotal = sectores.reduce((sum, s) => sum + (s.area || 0), 0);
    const poblacionTotal = sectores.reduce((sum, s) => sum + (s.poblacion || 0), 0);
    
    document.getElementById('areaTotal').textContent = areaTotal.toFixed(2);
    document.getElementById('poblacionTotal').textContent = poblacionTotal.toLocaleString('es-ES');
}

// Renderizar la lista de sectores
function renderSectores() {
    const container = document.getElementById('sectoresContainer');
    const emptyState = document.getElementById('emptyState');
    
    if (sectores.length === 0) {
        container.innerHTML = '';
        emptyState.classList.remove('d-none');
        return;
    }
    
    emptyState.classList.add('d-none');
    container.innerHTML = sectores.map(sector => `
        <div class="sector-card">
            <div class="sector-header">
                <div>
                    <h5 class="mb-1">${sector.nombre}</h5>
                    <span class="sector-code">${sector.codigo}</span>
                </div>
                <div>
                    <span class="badge ${sector.activo ? 'bg-success' : 'bg-secondary'}">
                        ${sector.activo ? 'Activo' : 'Inactivo'}
                    </span>
                </div>
            </div>
            <div class="sector-body">
                ${sector.descripcion ? `<p class="text-muted mb-3">${sector.descripcion}</p>` : ''}
                
                <div class="row">
                    <div class="col-md-6">
                        <div class="sector-info-row">
                            <i class="fas fa-chart-area"></i>
                            <span>Área: ${sector.area ? sector.area + ' km²' : 'No especificada'}</span>
                        </div>
                        <div class="sector-info-row">
                            <i class="fas fa-users"></i>
                            <span>Población: ${sector.poblacion ? sector.poblacion.toLocaleString('es-ES') : 'No especificada'}</span>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="sector-info-row">
                            <i class="fas fa-map-marker-alt"></i>
                            <span>Predios: ${sector.totalPredios || 0}</span>
                        </div>
                        <div class="sector-info-row">
                            <i class="fas fa-file-contract"></i>
                            <span>Contratos activos: ${sector.contratosActivos || 0}</span>
                        </div>
                    </div>
                </div>
                
                ${mostrarLimites(sector) ? `
                <div class="mt-3">
                    <small class="text-muted d-block mb-2">
                        <i class="fas fa-compass me-1"></i>Límites:
                    </small>
                    <div>
                        ${sector.limiteNorte ? `<span class="limite-badge"><strong>N:</strong> ${sector.limiteNorte}</span>` : ''}
                        ${sector.limiteSur ? `<span class="limite-badge"><strong>S:</strong> ${sector.limiteSur}</span>` : ''}
                        ${sector.limiteEste ? `<span class="limite-badge"><strong>E:</strong> ${sector.limiteEste}</span>` : ''}
                        ${sector.limiteOeste ? `<span class="limite-badge"><strong>O:</strong> ${sector.limiteOeste}</span>` : ''}
                    </div>
                </div>
                ` : ''}
            </div>
            <div class="mt-3">
                <div class="action-buttons">
                    <button class="btn btn-sm btn-outline-primary" onclick="verSector('${sector.uuid}')">
                        <i class="fas fa-eye me-1"></i>Ver Detalles
                    </button>
                    <button class="btn btn-sm btn-outline-info" onclick="editarSector('${sector.uuid}')">
                        <i class="fas fa-edit me-1"></i>Editar
                    </button>
                    <button class="btn btn-sm btn-outline-warning" onclick="cambiarEstadoSector('${sector.uuid}')">
                        <i class="fas fa-toggle-${sector.activo ? 'on' : 'off'} me-1"></i>Estado
                    </button>
                    ${!sector.totalPredios && !sector.contratosActivos ? `
                    <button class="btn btn-sm btn-outline-danger" onclick="eliminarSector('${sector.uuid}')">
                        <i class="fas fa-trash me-1"></i>Eliminar
                    </button>
                    ` : ''}
                </div>
            </div>
        </div>
    `).join('');
}

// Verificar si el sector tiene límites definidos
function mostrarLimites(sector) {
    return sector.limiteNorte || sector.limiteSur || sector.limiteEste || sector.limiteOeste;
}

// Abrir modal para nuevo sector
function openNewSectorModal() {
    currentEditUuid = null;
    document.getElementById('isEdit').value = 'false';
    document.getElementById('modalSectorTitle').textContent = 'Nuevo Sector';
    document.getElementById('sectorForm').reset();
    document.getElementById('activo').checked = true;
    
    // Limpiar clases de validación
    document.getElementById('sectorForm').classList.remove('was-validated');
    
    modalSector.show();
}

// Editar sector existente
async function editarSector(uuid) {
    currentEditUuid = uuid;
    document.getElementById('isEdit').value = 'true';
    document.getElementById('sectorUuid').value = uuid;
    document.getElementById('modalSectorTitle').textContent = 'Editar Sector';
    
    const sector = sectores.find(s => s.uuid === uuid);
    if (!sector) {
        showAlert('Sector no encontrado', 'danger');
        return;
    }
    
    // Llenar el formulario
    document.getElementById('nombre').value = sector.nombre;
    document.getElementById('codigo').value = sector.codigo || '';
    document.getElementById('descripcion').value = sector.descripcion || '';
    document.getElementById('limiteNorte').value = sector.limiteNorte || '';
    document.getElementById('limiteSur').value = sector.limiteSur || '';
    document.getElementById('limiteEste').value = sector.limiteEste || '';
    document.getElementById('limiteOeste').value = sector.limiteOeste || '';
    document.getElementById('area').value = sector.area || '';
    document.getElementById('poblacion').value = sector.poblacion || '';
    document.getElementById('activo').checked = sector.activo;
    
    modalSector.show();
}

// Guardar sector (crear o editar)
async function saveSector() {
    const form = document.getElementById('sectorForm');
    const isEdit = document.getElementById('isEdit').value === 'true';
    
    // Validar formulario
    if (!form.checkValidity()) {
        form.classList.add('was-validated');
        return;
    }
    
    const formData = new FormData(form);
    const data = {
        nombre: formData.get('nombre'),
        codigo: formData.get('codigo') || null,
        descripcion: formData.get('descripcion'),
        limiteNorte: formData.get('limiteNorte'),
        limiteSur: formData.get('limiteSur'),
        limiteEste: formData.get('limiteEste'),
        limiteOeste: formData.get('limiteOeste'),
        area: formData.get('area') ? parseFloat(formData.get('area')) : null,
        poblacion: formData.get('poblacion') ? parseInt(formData.get('poblacion')) : null,
        activo: formData.get('activo') === 'on'
    };
    
    const saveBtn = document.getElementById('saveSectorBtn');
    const originalText = saveBtn.innerHTML;
    saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Guardando...';
    saveBtn.disabled = true;
    
    try {
        const url = isEdit 
            ? `/admin/sectores/${currentEditUuid}`
            : '/admin/sectores';
            
        const method = isEdit ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });
        
        if (response.ok) {
            modalSector.hide();
            showAlert(isEdit ? 'Sector actualizado exitosamente' : 'Sector creado exitosamente', 'success');
            await cargarSectores();
            await cargarEstadisticas();
        } else {
            const error = await response.json();
            showAlert(error.message || 'Error al guardar sector', 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error de conexión', 'danger');
    } finally {
        saveBtn.innerHTML = originalText;
        saveBtn.disabled = false;
    }
}

// Ver detalles del sector
async function verSector(uuid) {
    try {
        const response = await fetch(`/admin/sectores/${uuid}/estadisticas`);
        if (response.ok) {
            const stats = await response.json();
            // Aquí podrías mostrar un modal con estadísticas detalladas
            showAlert(`Sector: ${stats.sector.nombre} - ${stats.totalPredios} predios, ${stats.contratosActivos} contratos activos`, 'info');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al cargar detalles', 'danger');
    }
}

// Cambiar estado del sector
async function cambiarEstadoSector(uuid) {
    const sector = sectores.find(s => s.uuid === uuid);
    if (!sector) return;
    
    const nuevoEstado = !sector.activo;
    const mensaje = nuevoEstado ? 'activar' : 'desactivar';
    
    if (confirm(`¿Está seguro de ${mensaje} el sector ${sector.nombre}?`)) {
        try {
            const response = await fetch(`/admin/sectores/${uuid}/estado`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ activo: nuevoEstado })
            });
            
            if (response.ok) {
                showAlert(`Sector ${nuevoEstado ? 'activado' : 'desactivado'} exitosamente`, 'success');
                await cargarSectores();
                await cargarEstadisticas();
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

// Eliminar sector
async function eliminarSector(uuid) {
    const sector = sectores.find(s => s.uuid === uuid);
    if (!sector) return;
    
    if (confirm(`¿Está seguro de eliminar el sector ${sector.nombre}?\n\nEsta acción no se puede deshacer.`)) {
        try {
            const response = await fetch(`/admin/sectores/${uuid}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                showAlert('Sector eliminado exitosamente', 'success');
                await cargarSectores();
                await cargarEstadisticas();
            } else {
                const error = await response.json();
                showAlert(error.message || 'No se puede eliminar el sector', 'danger');
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
    const estado = document.getElementById('filtroEstado').value;
    
    let sectoresFiltrados = sectores;
    
    if (busqueda) {
        sectoresFiltrados = sectoresFiltrados.filter(s => 
            s.nombre.toLowerCase().includes(busqueda) ||
            s.codigo.toLowerCase().includes(busqueda) ||
            (s.descripcion && s.descripcion.toLowerCase().includes(busqueda))
        );
    }
    
    if (estado !== '') {
        const esActivo = estado === 'true';
        sectoresFiltrados = sectoresFiltrados.filter(s => s.activo === esActivo);
    }
    
    // Temporalmente actualizar la lista
    const sectoresOriginales = sectores;
    sectores = sectoresFiltrados;
    renderSectores();
    sectores = sectoresOriginales;
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
    const container = document.getElementById('sectoresContainer');
    
    if (show) {
        loading.classList.remove('d-none');
        container.innerHTML = '';
    } else {
        loading.classList.add('d-none');
    }
}

// Exportar funciones para uso global
window.openNewSectorModal = openNewSectorModal;
window.editarSector = editarSector;
window.saveSector = saveSector;
window.verSector = verSector;
window.cambiarEstadoSector = cambiarEstadoSector;
window.eliminarSector = eliminarSector;
window.aplicarFiltros = aplicarFiltros;