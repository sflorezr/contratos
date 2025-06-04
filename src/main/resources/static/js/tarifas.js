// Variables globales
let tarifaModal;
let currentUuid = null;

// Inicialización cuando el DOM está listo
document.addEventListener('DOMContentLoaded', function() {
    tarifaModal = new bootstrap.Modal(document.getElementById('modalTarifa'));
    cargarEstadisticas();
    cargarTarifas();
    cargarPlanesTarifas();
    cargarServicios();
});

// Funciones de carga de datos
async function cargarEstadisticas() {
    try {
        const response = await fetch('/admin/tarifas/resumen/general');
        const stats = await response.json();
        
        document.getElementById('totalTarifas').textContent = stats.totalTarifas || 0;
        document.getElementById('tarifasActivas').textContent = stats.tarifasActivas || 0;
        document.getElementById('promedioUrbano').textContent = formatearPrecio(stats.promedioUrbano);
        document.getElementById('promedioRural').textContent = formatearPrecio(stats.promedioRural);
    } catch (error) {
        console.error('Error al cargar estadísticas:', error);
        mostrarAlerta('Error al cargar estadísticas', 'danger');
    }
}

async function cargarTarifas() {
    mostrarLoading(true);
    try {
        const params = new URLSearchParams();
        
        const busqueda = document.getElementById('filtroBusqueda').value;
        const estado = document.getElementById('filtroEstado').value;
        const planUuid = document.getElementById('filtroPlan').value;
        const servicioUuid = document.getElementById('filtroServicio').value;
        
        if (busqueda) params.append('filtro', busqueda);
        if (estado) params.append('activo', estado);
        if (planUuid) params.append('planTarifaUuid', planUuid);
        if (servicioUuid) params.append('servicioUuid', servicioUuid);
        
        const response = await fetch(`/admin/tarifas/detalles?${params.toString()}`);
        const tarifas = await response.json();
        
        const container = document.getElementById('tarifasContainer');
        container.innerHTML = '';
        
        if (tarifas.length === 0) {
            mostrarEmptyState(true);
            return;
        }
        
        mostrarEmptyState(false);
        tarifas.forEach(tarifa => {
            container.appendChild(crearTarifaCard(tarifa));
        });
    } catch (error) {
        console.error('Error al cargar tarifas:', error);
        mostrarAlerta('Error al cargar tarifas', 'danger');
    } finally {
        mostrarLoading(false);
    }
}

async function cargarPlanesTarifas() {
    try {
        const response = await fetch('/admin/planes-tarifas/activos');
        const planes = await response.json();
        
        const selectModal = document.getElementById('planTarifaUuid');
        const selectFiltro = document.getElementById('filtroPlan');
        
        selectModal.innerHTML = '<option value="">Seleccione un plan...</option>';
        selectFiltro.innerHTML = '<option value="">Todos los planes</option>';
        
        planes.forEach(plan => {
            selectModal.innerHTML += `<option value="${plan.uuid}">${plan.nombre}</option>`;
            selectFiltro.innerHTML += `<option value="${plan.uuid}">${plan.nombre}</option>`;
        });
    } catch (error) {
        console.error('Error al cargar planes:', error);
    }
}

async function cargarServicios() {
    try {
        const response = await fetch('/admin/servicios/activos');
        const servicios = await response.json();
        
        const selectModal = document.getElementById('servicioUuid');
        const selectFiltro = document.getElementById('filtroServicio');
        
        selectModal.innerHTML = '<option value="">Seleccione un servicio...</option>';
        selectFiltro.innerHTML = '<option value="">Todos los servicios</option>';
        
        servicios.forEach(servicio => {
            selectModal.innerHTML += `<option value="${servicio.uuid}">${servicio.nombre}</option>`;
            selectFiltro.innerHTML += `<option value="${servicio.uuid}">${servicio.nombre}</option>`;
        });
    } catch (error) {
        console.error('Error al cargar servicios:', error);
    }
}

// Funciones de UI
function crearTarifaCard(tarifa) {
    const card = document.createElement('div');
    card.className = 'tarifa-card';
    card.innerHTML = `
        <div class="tarifa-header">
            <div>
                <h6 class="mb-1 text-primary">${tarifa.planTarifaNombre || 'Plan no disponible'}</h6>
                <h5 class="mb-2">${tarifa.servicioNombre || 'Servicio no disponible'}</h5>
                <span class="estado-badge ${tarifa.activo ? 'estado-activo' : 'estado-inactivo'}">
                    ${tarifa.activo ? 'Activa' : 'Inactiva'}
                </span>
            </div>
            <div class="action-buttons">
                <button class="btn btn-sm btn-outline-primary" onclick="editarTarifa('${tarifa.uuid}')" title="Editar">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm ${tarifa.activo ? 'btn-outline-danger' : 'btn-outline-success'}"
                        onclick="cambiarEstado('${tarifa.uuid}', ${!tarifa.activo})" 
                        title="${tarifa.activo ? 'Desactivar' : 'Activar'}">
                    <i class="fas fa-${tarifa.activo ? 'times' : 'check'}"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger" onclick="eliminarTarifa('${tarifa.uuid}')" title="Eliminar">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        </div>
        <div class="row g-3 mt-3">
            <div class="col-md-6">
                <div class="precio-container">
                    <div class="precio-item">
                        <small class="text-muted d-block">Precio Urbano</small>
                        <span class="precio-badge">${formatearPrecio(tarifa.precioUrbano)}</span>
                    </div>
                </div>
            </div>
            <div class="col-md-6">
                <div class="precio-container">
                    <div class="precio-item">
                        <small class="text-muted d-block">Precio Rural</small>
                        <span class="precio-badge">${formatearPrecio(tarifa.precioRural)}</span>
                    </div>
                </div>
            </div>
        </div>
        <div class="row g-2 mt-2">
            <div class="col-12">
                <small class="text-muted">
                    <i class="fas fa-calendar me-1"></i>
                    Creada: ${formatearFecha(tarifa.fechaCreacion)}
                </small>
            </div>
        </div>
    `;
    return card;
}

function mostrarLoading(show) {
    document.getElementById('loadingIndicator').classList.toggle('d-none', !show);
}

function mostrarEmptyState(show) {
    document.getElementById('emptyState').classList.toggle('d-none', !show);
}

function mostrarAlerta(mensaje, tipo) {
    const alertsContainer = document.getElementById('alertsContainer');
    const alert = document.createElement('div');
    alert.className = `alert alert-${tipo} alert-dismissible fade show`;
    alert.innerHTML = `
        ${mensaje}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    alertsContainer.appendChild(alert);
    setTimeout(() => alert.remove(), 5000);
}

// Funciones de gestión de tarifas
function openNewTarifaModal() {
    currentUuid = null;
    document.getElementById('modalTarifaTitle').textContent = 'Nueva Tarifa';
    document.getElementById('tarifaForm').reset();
    document.getElementById('activo').checked = true;
    tarifaModal.show();
}

async function editarTarifa(uuid) {
    try {
        const response = await fetch(`/admin/tarifas/${uuid}`);
        if (!response.ok) throw new Error('Tarifa no encontrada');
        
        const tarifa = await response.json();
        
        currentUuid = uuid;
        document.getElementById('modalTarifaTitle').textContent = 'Editar Tarifa';
        document.getElementById('planTarifaUuid').value = tarifa.planTarifaUuid || '';
        document.getElementById('servicioUuid').value = tarifa.servicioUuid || '';
        document.getElementById('precioUrbano').value = tarifa.precioUrbano || '';
        document.getElementById('precioRural').value = tarifa.precioRural || '';
        document.getElementById('activo').checked = tarifa.activo;
        
        tarifaModal.show();
    } catch (error) {
        console.error('Error al cargar tarifa:', error);
        mostrarAlerta('Error al cargar la tarifa', 'danger');
    }
}

async function guardarTarifa() {
    const form = document.getElementById('tarifaForm');
    if (!form.checkValidity()) {
        form.classList.add('was-validated');
        return;
    }
    
    const tarifaData = {
        planTarifaUuid: document.getElementById('planTarifaUuid').value,
        servicioUuid: document.getElementById('servicioUuid').value,
        precioUrbano: parseFloat(document.getElementById('precioUrbano').value),
        precioRural: parseFloat(document.getElementById('precioRural').value),
        activo: document.getElementById('activo').checked
    };
    
    try {
        const url = currentUuid ? 
            `/api/tarifas/${currentUuid}` : 
            '/api/tarifas';
        
        const response = await fetch(url, {
            method: currentUuid ? 'PUT' : 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(tarifaData)
        });
        
        if (!response.ok) {
            const errorData = await response.text();
            throw new Error(errorData || 'Error en la operación');
        }
        
        tarifaModal.hide();
        mostrarAlerta(
            `Tarifa ${currentUuid ? 'actualizada' : 'creada'} correctamente`, 
            'success'
        );
        cargarEstadisticas();
        cargarTarifas();
    } catch (error) {
        console.error('Error al guardar tarifa:', error);
        mostrarAlerta(error.message || 'Error al guardar la tarifa', 'danger');
    }
}

async function cambiarEstado(uuid, nuevoEstado) {
    try {
        const response = await fetch(`/admin/tarifas/${uuid}/estado`, {
            method: 'PATCH'
        });
        
        if (!response.ok) throw new Error('Error en la operación');
        
        mostrarAlerta('Estado actualizado correctamente', 'success');
        cargarEstadisticas();
        cargarTarifas();
    } catch (error) {
        console.error('Error al cambiar estado:', error);
        mostrarAlerta('Error al cambiar el estado', 'danger');
    }
}

async function eliminarTarifa(uuid) {
    if (!confirm('¿Está seguro de que desea eliminar esta tarifa? Esta acción no se puede deshacer.')) {
        return;
    }
    
    try {
        const response = await fetch(`/admin/tarifas/${uuid}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) throw new Error('Error al eliminar');
        
        mostrarAlerta('Tarifa eliminada correctamente', 'success');
        cargarEstadisticas();
        cargarTarifas();
    } catch (error) {
        console.error('Error al eliminar tarifa:', error);
        mostrarAlerta('Error al eliminar la tarifa', 'danger');
    }
}

// Funciones de filtrado
function aplicarFiltros() {
    cargarTarifas();
}

// Funciones utilitarias
function formatearPrecio(precio) {
    if (!precio) return '$0.00';
    return new Intl.NumberFormat('es-CO', {
        style: 'currency',
        currency: 'COP',
        minimumFractionDigits: 2
    }).format(precio);
}

function formatearFecha(fecha) {
    if (!fecha) return 'N/A';
    return new Date(fecha).toLocaleDateString('es-CO', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Event listeners para filtros en tiempo real
document.getElementById('filtroBusqueda').addEventListener('input', function() {
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => {
        aplicarFiltros();
    }, 500);
});

document.getElementById('filtroEstado').addEventListener('change', aplicarFiltros);
document.getElementById('filtroPlan').addEventListener('change', aplicarFiltros);
document.getElementById('filtroServicio').addEventListener('change', aplicarFiltros);

// Validaciones adicionales del formulario
document.getElementById('planTarifaUuid').addEventListener('change', validarCombinacionPlanServicio);
document.getElementById('servicioUuid').addEventListener('change', validarCombinacionPlanServicio);

async function validarCombinacionPlanServicio() {
    const planUuid = document.getElementById('planTarifaUuid').value;
    const servicioUuid = document.getElementById('servicioUuid').value;
    
    if (!planUuid || !servicioUuid) return;
    
    // Solo validar si no estamos editando la misma tarifa
    if (currentUuid) return;
    
    try {
        const response = await fetch(`/admin/tarifas/plan/${planUuid}/servicio/${servicioUuid}`);
        if (response.ok) {
            mostrarAlerta('Ya existe una tarifa para esta combinación de plan y servicio', 'warning');
            document.getElementById('servicioUuid').setCustomValidity('Combinación duplicada');
        } else {
            document.getElementById('servicioUuid').setCustomValidity('');
        }
    } catch (error) {
        // Si hay error 404, significa que no existe, lo cual está bien
        document.getElementById('servicioUuid').setCustomValidity('');
    }
}

// Funciones adicionales para operaciones masivas (futuras expansiones)
function exportarTarifas() {
    // Implementar exportación de tarifas
    mostrarAlerta('Función de exportación en desarrollo', 'info');
}

function importarTarifas() {
    // Implementar importación masiva
    mostrarAlerta('Función de importación en desarrollo', 'info');
}

// Cleanup al cerrar modal
document.getElementById('modalTarifa').addEventListener('hidden.bs.modal', function() {
    document.getElementById('tarifaForm').classList.remove('was-validated');
    document.getElementById('servicioUuid').setCustomValidity('');
});