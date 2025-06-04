// Variables globales
let servicioModal;
let currentUuid = null;

// Inicialización cuando el DOM está listo
document.addEventListener('DOMContentLoaded', function() {
    servicioModal = new bootstrap.Modal(document.getElementById('modalServicio'));
    cargarEstadisticas();
    cargarServicios();
});

// Funciones de carga de datos
async function cargarEstadisticas() {
    try {
        const response = await fetch('/admin/servicios/estadisticas');
        const stats = await response.json();
        
        document.getElementById('totalServicios').textContent = stats.totalServicios;
        document.getElementById('serviciosActivos').textContent = stats.serviciosActivos;
        document.getElementById('serviciosConTarifas').textContent = stats.serviciosConTarifas;
        document.getElementById('serviciosConActividades').textContent = stats.serviciosConActividades;
    } catch (error) {
        mostrarAlerta('Error al cargar estadísticas', 'danger');
    }
}
function aplicarFiltros() {
    const busqueda = document.getElementById('filtroBusqueda').value;
    const estado = document.getElementById('filtroEstado').value;
    const tipo = document.getElementById('filtroTipo').value;
    
    // Implementar lógica de filtrado aquí
    cargarServicios(); // Por ahora recarga todos
}
async function cargarServicios() {
    mostrarLoading(true);
    try {
        const response = await fetch('/admin/servicios/resumen');
        const servicios = await response.json();
        
        const container = document.getElementById('serviciosContainer');
        container.innerHTML = '';
        
        if (servicios.length === 0) {
            mostrarEmptyState(true);
            return;
        }
        
        mostrarEmptyState(false);
        servicios.forEach(servicio => {
            container.appendChild(crearServicioCard(servicio));
        });
    } catch (error) {
        mostrarAlerta('Error al cargar servicios', 'danger');
    } finally {
        mostrarLoading(false);
    }
}

// Funciones de UI
function crearServicioCard(servicio) {
    const card = document.createElement('div');
    card.className = 'servicio-card';
    card.innerHTML = `
        <div class="servicio-header">
            <div>
                <h5 class="mb-1">${servicio.nombre}</h5>
                <span class="estado-badge ${servicio.activo ? 'estado-activo' : 'estado-inactivo'}">
                    ${servicio.activo ? 'Activo' : 'Inactivo'}
                </span>
            </div>
            <div class="action-buttons">
                <button class="btn btn-sm btn-outline-primary" onclick="editarServicio('${servicio.uuid}')">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm ${servicio.activo ? 'btn-outline-danger' : 'btn-outline-success'}"
                        onclick="cambiarEstado('${servicio.uuid}', ${!servicio.activo})">
                    <i class="fas fa-${servicio.activo ? 'times' : 'check'}"></i>
                </button>
            </div>
        </div>
        <div class="row g-2 mt-3">
            <div class="col-6">
                <small class="text-muted">
                    <i class="fas fa-tag me-1"></i>
                    ${servicio.cantidadTarifas} tarifas
                </small>
            </div>
            <div class="col-6">
                <small class="text-muted">
                    <i class="fas fa-tasks me-1"></i>
                    ${servicio.cantidadActividades} actividades
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

// Funciones de gestión de servicios
function openNewServicioModal() {
    currentUuid = null;
    document.getElementById('modalServicioTitle').textContent = 'Nuevo Servicio';
    document.getElementById('servicioForm').reset();
    servicioModal.show();
}

async function editarServicio(uuid) {
    try {
        const response = await fetch(`/admin/servicios/${uuid}`);
        const servicio = await response.json();
        
        currentUuid = uuid;
        document.getElementById('modalServicioTitle').textContent = 'Editar Servicio';
        document.getElementById('nombre').value = servicio.nombre;
        document.getElementById('descripcion').value = servicio.descripcion || '';
        document.getElementById('activo').checked = servicio.activo;
        
        servicioModal.show();
    } catch (error) {
        mostrarAlerta('Error al cargar el servicio', 'danger');
    }
}

async function guardarServicio() {
    const form = document.getElementById('servicioForm');
    if (!form.checkValidity()) {
        form.classList.add('was-validated');
        return;
    }
    
    const servicioData = {
        nombre: document.getElementById('nombre').value,
        descripcion: document.getElementById('descripcion').value,
        activo: document.getElementById('activo').checked
    };
    
    try {
        const url = currentUuid ? 
            `/admin/servicios/${currentUuid}` : 
            '/admin/servicios';
        
        const response = await fetch(url, {
            method: currentUuid ? 'PUT' : 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(servicioData)
        });
        
        if (!response.ok) throw new Error('Error en la operación');
        
        servicioModal.hide();
        mostrarAlerta(
            `Servicio ${currentUuid ? 'actualizado' : 'creado'} correctamente`, 
            'success'
        );
        cargarEstadisticas();
        cargarServicios();
    } catch (error) {
        mostrarAlerta('Error al guardar el servicio', 'danger');
    }
}

async function cambiarEstado(uuid, nuevoEstado) {
    try {
        const response = await fetch(`/admin/servicios/${uuid}/estado?activo=${nuevoEstado}`, {
            method: 'PUT'
        });
        
        if (!response.ok) throw new Error('Error en la operación');
        
        mostrarAlerta('Estado actualizado correctamente', 'success');
        cargarEstadisticas();
        cargarServicios();
    } catch (error) {
        mostrarAlerta('Error al cambiar el estado', 'danger');
    }
}

// Funciones de filtrado
