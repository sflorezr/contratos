// contratos.js
// Variables globales
let contratos = [];
let zonas = [];
let planesTarifa = [];
let supervisores = [];
let usuarioActual = null;
let modalContrato = null;
let currentEditUuid = null;

// Inicialización
document.addEventListener('DOMContentLoaded', function() {
    modalContrato = new bootstrap.Modal(document.getElementById('modalContrato'));
    inicializar();
    
    // Event listeners
    document.getElementById('btnNewContract').addEventListener('click', openNewContractModal);
    document.getElementById('btnNewContract2').addEventListener('click', openNewContractModal);
    document.getElementById('btnAplicarFiltros').addEventListener('click', aplicarFiltros);
    document.getElementById('saveContratoBtn').addEventListener('click', saveContrato);
    document.getElementById('btnAgregarPredios').addEventListener('click', abrirModalPredios);
});

async function inicializar() {
    cargarUsuarioActual();
    await Promise.all([
        cargarContratos(),
        cargarZonas(),
        cargarPlanesTarifa(),
        cargarSupervisores()
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

async function cargarContratos() {
    showLoading(true);
    try {
        const response = await fetch('/admin/contratos/api/listar');
        if (response.ok) {
            contratos = await response.json();
            renderContratos();
            actualizarEstadisticas();
        } else {
            showAlert('Error al cargar contratos', 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error de conexión', 'danger');
    } finally {
        showLoading(false);
    }
}

async function cargarZonas() {
    try {
        const response = await fetch('/admin/zonas/api/select');
        if (response.ok) {
            zonas = await response.json();
            actualizarSelectZonas();
        }
    } catch (error) {
        console.error('Error cargando zonas:', error);
    }
}

async function cargarPlanesTarifa() {
    try {
        const response = await fetch('/admin/planes-tarifa/api/listar-simple');
        if (response.ok) {
            planesTarifa = await response.json();
            actualizarSelectPlanesTarifa();
        }
    } catch (error) {
        console.error('Error cargando planes de tarifa:', error);
    }
}

async function cargarSupervisores() {
    try {
        const response = await fetch('/admin/usuarios/api/supervisores');
        if (response.ok) {
            supervisores = await response.json();
            actualizarSelectSupervisores();
        }
    } catch (error) {
        console.error('Error cargando supervisores:', error);
    }
}

function actualizarSelectZonas() {
    const select = document.getElementById('zonaUuid');
    const filtroSelect = document.getElementById('filtroZona');
    
    select.innerHTML = '<option value="">Seleccionar zona...</option>';
    filtroSelect.innerHTML = '<option value="">Todas las zonas</option>';
    
    zonas.forEach(zona => {
        select.innerHTML += `<option value="${zona.uuid}">${zona.nombre}</option>`;
        filtroSelect.innerHTML += `<option value="${zona.uuid}">${zona.nombre}</option>`;
    });
}

function actualizarSelectPlanesTarifa() {
    const select = document.getElementById('planTarifaUuid');
    select.innerHTML = '<option value="">Seleccionar plan...</option>';
    
    planesTarifa.forEach(plan => {
        select.innerHTML += `<option value="${plan.uuid}">${plan.nombre}</option>`;
    });
}

function actualizarSelectSupervisores() {
    const select = document.getElementById('supervisorUuid');
    select.innerHTML = '<option value="">Sin asignar</option>';
    
    supervisores.forEach(supervisor => {
        select.innerHTML += `<option value="${supervisor.uuid}">${supervisor.nombre} ${supervisor.apellido}</option>`;
    });
}

function renderContratos() {
    const container = document.getElementById('contratosContainer');
    const emptyState = document.getElementById('emptyState');
    
    if (contratos.length === 0) {
        container.innerHTML = '';
        emptyState.classList.remove('d-none');
        return;
    }
    
    emptyState.classList.add('d-none');
    container.innerHTML = contratos.map(contrato => `
        <div class="contract-card">
            <div class="contract-header">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <h5 class="mb-1">
                            <i class="fas fa-file-contract text-primary me-2"></i>
                            ${contrato.numeroContrato || contrato.codigo}
                        </h5>
                        <p class="text-muted mb-0">${contrato.objetivo}</p>
                    </div>
                    <span class="estado-badge estado-${contrato.estado.toLowerCase()}">
                        ${getEstadoText(contrato.estado)}
                    </span>
                </div>
            </div>
            <div class="contract-body">
                <div class="row">
                    <div class="col-md-6">
                        <div class="mb-3">
                            <small class="text-muted d-block">Sector</small>
                            <span><i class="fas fa-map-marker-alt me-1"></i>${contrato.zonaNombre || 'Sin definir'}</span>
                        </div>
                        <div class="mb-3">
                            <small class="text-muted d-block">Período</small>
                            <span><i class="fas fa-calendar me-1"></i>${formatDate(contrato.fechaInicio)} - ${formatDate(contrato.fechaFin)}</span>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="mb-3">
                            <small class="text-muted d-block">Supervisor</small>
                            <span><i class="fas fa-user me-1"></i>${contrato.supervisorNombre || 'Sin asignar'}</span>
                        </div>
                        <div class="mb-3">
                            <small class="text-muted d-block">Progreso</small>
                            <div class="progress progress-custom">
                                <div class="progress-bar bg-primary" style="width: ${contrato.porcentajeAvance || 0}%"></div>
                            </div>
                            <small class="text-muted">${contrato.porcentajeAvance || 0}% completado</small>
                        </div>
                    </div>
                </div>
                
                <div class="mt-3">
                    <small class="text-muted d-block mb-2">Asignaciones</small>
                    <div>
                        <span class="user-badge">
                            <i class="fas fa-map-marker-alt"></i>
                            ${contrato.totalPredios || 0} predios
                        </span>
                        <span class="user-badge">
                            <i class="fas fa-users"></i>
                            ${contrato.totalOperarios || 0} operarios
                        </span>
                        <span class="user-badge">
                            <i class="fas fa-check-circle"></i>
                            ${contrato.prediosAsignados || 0} asignados
                        </span>
                        <span class="user-badge">
                            <i class="fas fa-layer-group"></i>
                            ${contrato.totalZonas || 0} zonas
                        </span>                        
                    </div>
                </div>
            </div>
            <div class="contract-footer">
                <div class="action-buttons">
                    <a href="/admin/contratos/${contrato.uuid}/asignaciones" class="btn btn-sm btn-outline-primary">
                        <i class="fas fa-users me-1"></i>Asignaciones
                    </a>
                    <button class="btn btn-sm btn-outline-info" onclick="editarContrato('${contrato.uuid}')">
                        <i class="fas fa-edit me-1"></i>Editar
                    </button>
                    <button class="btn btn-sm btn-outline-warning" onclick="cambiarEstadoContrato('${contrato.uuid}')">
                        <i class="fas fa-exchange-alt me-1"></i>Estado
                    </button>
                    ${contrato.puedeSerEliminado ? `
                    <button class="btn btn-sm btn-outline-danger" onclick="eliminarContrato('${contrato.uuid}')">
                        <i class="fas fa-trash me-1"></i>Eliminar
                    </button>` : ''}
                </div>
            </div>
        </div>
    `).join('');
}

function actualizarEstadisticas() {
    const total = contratos.length;
    const activos = contratos.filter(c => c.estado === 'ACTIVO').length;
    const enProceso = contratos.filter(c => c.estado === 'EN_PROCESO').length;
    const completados = contratos.filter(c => c.estado === 'COMPLETADO' || c.estado === 'FINALIZADO').length;
    
    document.getElementById('totalContratos').textContent = total;
    document.getElementById('contratosActivos').textContent = activos;
    document.getElementById('contratosEnProceso').textContent = enProceso;
    document.getElementById('contratosCompletados').textContent = completados;
}

function openNewContractModal() {
    currentEditUuid = null;
    document.getElementById('isEdit').value = 'false';
    document.getElementById('modalContratoTitle').textContent = 'Nuevo Contrato';
    document.getElementById('contratoForm').reset();
    document.getElementById('prediosSection').classList.add('d-none');
    
    // Establecer fecha inicio como hoy
    document.getElementById('fechaInicio').valueAsDate = new Date();
    
    modalContrato.show();
}

async function editarContrato(uuid) {
    currentEditUuid = uuid;
    document.getElementById('isEdit').value = 'true';
    document.getElementById('contratoUuid').value = uuid;
    document.getElementById('modalContratoTitle').textContent = 'Editar Contrato';
    
    try {
        // CORREGIDO: Obtener datos completos del contrato desde el servidor
        const response = await fetch(`/admin/contratos/api/${uuid}`);
        if (!response.ok) {
            throw new Error('Error al obtener datos del contrato');
        }
        
        const contrato = await response.json();
        console.log('Datos del contrato cargados:', contrato); // Para debug
        
        // CORREGIDO: Llenar el formulario con los campos correctos
        document.getElementById('codigo').value = contrato.codigo || contrato.numeroContrato || '';
        document.getElementById('objetivo').value = contrato.objetivo || '';
        document.getElementById('zonaUuid').value = contrato.zonaUuid || '';           // Usar zonaUuid
        document.getElementById('planTarifaUuid').value = contrato.planTarifaUuid || '';
        document.getElementById('fechaInicio').value = contrato.fechaInicio || '';
        document.getElementById('fechaFin').value = contrato.fechaFin || '';
        document.getElementById('supervisorUuid').value = contrato.supervisorUuid || '';  // Usar supervisorUuid
        document.getElementById('estado').value = contrato.estado || 'ACTIVO';
        
        // Mostrar sección de predios en modo edición
        document.getElementById('prediosSection').classList.remove('d-none');
        
        // Cargar predios del contrato
        await cargarPrediosContrato(uuid);
        
        modalContrato.show();
        
    } catch (error) {
        console.error('Error al cargar contrato:', error);
        showAlert('Error al cargar los datos del contrato', 'danger');
    }
}
async function cargarPrediosContrato(contratoUuid) {
    try {
        const response = await fetch(`/admin/contratos/${contratoUuid}/predios`);
        if (response.ok) {
            const predios = await response.json();
            renderPrediosAsignados(predios);
        }
    } catch (error) {
        console.error('Error cargando predios:', error);
    }
}

function renderPrediosAsignados(predios) {
    const container = document.getElementById('prediosAsignados');
    const total = predios.length;
    const asignados = predios.filter(p => p.operarioAsignado).length;
    const sinAsignar = total - asignados;
    
    document.getElementById('totalPredios').textContent = total;
    document.getElementById('prediosAsignadosCount').textContent = asignados;
    document.getElementById('prediosSinAsignarCount').textContent = sinAsignar;
    
    if (predios.length === 0) {
        container.innerHTML = '<p class="text-muted text-center">No hay predios asignados a este contrato</p>';
        return;
    }
    
    container.innerHTML = predios.map(predio => `
        <div class="predio-item ${predio.selected ? 'selected' : ''}">
            <div class="d-flex justify-content-between align-items-center">
                <div>
                    <strong>${predio.direccion}</strong>
                    <small class="text-muted d-block">${predio.codigoCatastral || 'Sin código'}</small>
                </div>
                <div class="text-end">
                    <span class="badge ${predio.operarioAsignado ? 'bg-success' : 'bg-secondary'}">
                        ${predio.operarioAsignado || 'Sin asignar'}
                    </span>
                </div>
            </div>
        </div>
    `).join('');
}

async function saveContrato() {
    const form = document.getElementById('contratoForm');
    const isEdit = document.getElementById('isEdit').value === 'true';
    
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    const formData = new FormData(form);
    const data = Object.fromEntries(formData);

    // CORREGIDO: Mapear correctamente los campos para que coincidan con el DTO
    const contratoData = {
        codigo: data.codigo,
        objetivo: data.objetivo,
        zonaUuid: data.zonaUuid,           // Cambiar zonaId -> zonaUuid
        planTarifaUuid: data.planTarifaUuid,
        fechaInicio: data.fechaInicio,
        fechaFin: data.fechaFin,
        supervisorUuid: data.supervisorUuid && data.supervisorUuid !== '' ? data.supervisorUuid : null,  // Cambiar supervisorId -> supervisorUuid
        estado: data.estado || 'ACTIVO'
    };
    
    // Validar fechas
    if (new Date(contratoData.fechaFin) <= new Date(contratoData.fechaInicio)) {
        showAlert('La fecha fin debe ser posterior a la fecha inicio', 'warning');
        return;
    }
    
    const saveBtn = document.getElementById('saveContratoBtn');
    const originalText = saveBtn.innerHTML;
    saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Guardando...';
    saveBtn.disabled = true;
    
    try {
        const url = isEdit 
            ? `/admin/contratos/api/${currentEditUuid}`  // Agregar /api/ para consistencia
            : '/admin/contratos/api/crear';              // Agregar /api/ para consistencia
            
        const method = isEdit ? 'PUT' : 'POST';
        
        console.log('Enviando datos:', contratoData); // Para debug
        
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },            
            body: JSON.stringify(contratoData)
        });
        
        if (response.ok) {
            modalContrato.hide();
            showAlert(isEdit ? 'Contrato actualizado exitosamente' : 'Contrato creado exitosamente', 'success');
            cargarContratos();
        } else {
            const error = await response.json();
            showAlert(error.message || 'Error al guardar contrato', 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error de conexión', 'danger');
    } finally {
        saveBtn.innerHTML = originalText;
        saveBtn.disabled = false;
    }
}

async function cambiarEstadoContrato(uuid) {
    const contrato = contratos.find(c => c.uuid === uuid);
    if (!contrato) return;
    
    const { value: nuevoEstado } = await Swal.fire({
        title: 'Cambiar Estado del Contrato',
        input: 'select',
        inputOptions: {
            'ACTIVO': 'Activo',
            'EN_PROCESO': 'En Proceso',
            'COMPLETADO': 'Completado',
            'CANCELADO': 'Cancelado'
        },
        inputValue: contrato.estado,
        showCancelButton: true,
        confirmButtonText: 'Cambiar',
        cancelButtonText: 'Cancelar'
    });
    
    if (nuevoEstado && nuevoEstado !== contrato.estado) {
        try {
            const response = await fetch(`/admin/contratos/${uuid}/estado`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ estado: nuevoEstado })
            });
            
            if (response.ok) {
                showAlert('Estado actualizado exitosamente', 'success');
                cargarContratos();
            } else {
                showAlert('Error al cambiar estado', 'danger');
            }
        } catch (error) {
            console.error('Error:', error);
            showAlert('Error de conexión', 'danger');
        }
    }
}

async function eliminarContrato(uuid) {
    const contrato = contratos.find(c => c.uuid === uuid);
    if (!contrato) return;
    
    const result = await Swal.fire({
        title: '¿Eliminar contrato?',
        text: `¿Está seguro de eliminar el contrato ${contrato.numeroContrato || contrato.codigo}?`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    });
    
    if (result.isConfirmed) {
        try {
            const response = await fetch(`/admin/contratos/${uuid}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                showAlert('Contrato eliminado exitosamente', 'success');
                cargarContratos();
            } else {
                const error = await response.json();
                showAlert(error.message || 'Error al eliminar contrato', 'danger');
            }
        } catch (error) {
            console.error('Error:', error);
            showAlert('Error de conexión', 'danger');
        }
    }
}

function aplicarFiltros() {
    const codigo = document.getElementById('filtroCodigo').value.toLowerCase();
    const estado = document.getElementById('filtroEstado').value;
    const zona = document.getElementById('filtroZona').value;
    
    let contratosFiltrados = contratos;
    if (zona) {
    contratosFiltrados = contratosFiltrados.filter(c => c.zonaUuid === zona);
    }
    if (codigo) {
        contratosFiltrados = contratosFiltrados.filter(c => 
            (c.numeroContrato || c.codigo).toLowerCase().includes(codigo) ||
            c.objetivo.toLowerCase().includes(codigo)
        );
    }
    
    if (estado) {
        contratosFiltrados = contratosFiltrados.filter(c => c.estado === estado);
    }
    
    if (sector) {
        contratosFiltrados = contratosFiltrados.filter(c => c.sectorUuid === sector);
    }
    
    // Temporalmente actualizar la lista
    const contratosOriginales = contratos;
    contratos = contratosFiltrados;
    renderContratos();
    contratos = contratosOriginales;
}

function getEstadoText(estado) {
    const estados = {
        'ACTIVO': 'Activo',
        'EN_PROCESO': 'En Proceso',
        'COMPLETADO': 'Completado',
        'FINALIZADO': 'Finalizado',
        'SUSPENDIDO': 'Suspendido',
        'CANCELADO': 'Cancelado'
    };
    return estados[estado] || estado;
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
    const container = document.getElementById('contratosContainer');
    
    if (show) {
        loading.classList.remove('d-none');
        container.innerHTML = '';
    } else {
        loading.classList.add('d-none');
    }
}

// Función placeholder para abrir modal de predios
function abrirModalPredios() {
    showAlert('Funcionalidad de agregar predios en desarrollo', 'info');
}

// Si no tienes SweetAlert2, usar confirm nativo
if (typeof Swal === 'undefined') {
    window.Swal = {
        fire: async (options) => {
            if (options.input === 'select') {
                const nuevoEstado = prompt(options.title + '\n\nOpciones: ACTIVO, EN_PROCESO, COMPLETADO, CANCELADO', options.inputValue);
                return { value: nuevoEstado };
            } else {
                const confirmado = confirm(options.text || options.title);
                return { isConfirmed: confirmado };
            }
        }
    };

}