// contratos.js - Actualizado para múltiples zonas
// Variables globales
let contratos = [];
let zonas = [];
let planesTarifa = [];
let supervisores = [];
let coordinadores = [];
let usuarioActual = null;
let modalContrato = null;
let modalZonas = null;
let currentEditUuid = null;
let zonasSeleccionadas = [];

// Inicialización
document.addEventListener('DOMContentLoaded', function() {
    modalContrato = new bootstrap.Modal(document.getElementById('modalContrato'));
    modalZonas = new bootstrap.Modal(document.getElementById('modalZonas'));
    inicializar();
    
    // Event listeners
    document.getElementById('btnNewContract').addEventListener('click', openNewContractModal);
    document.getElementById('btnNewContract2').addEventListener('click', openNewContractModal);
    document.getElementById('btnAplicarFiltros').addEventListener('click', aplicarFiltros);
    document.getElementById('saveContratoBtn').addEventListener('click', saveContrato);
    document.getElementById('btnAgregarZona').addEventListener('click', agregarZonaALista);
    document.getElementById('btnGuardarZonas').addEventListener('click', guardarZonasSeleccionadas);
    document.getElementById('btnGestionarZonas').addEventListener('click', abrirModalZonas);
});

async function inicializar() {
    cargarUsuarioActual();
    await Promise.all([
        cargarContratos(),
        cargarZonas(),
        cargarPlanesTarifa(),
        cargarSupervisores(),
        cargarCoordinadores()
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

async function cargarCoordinadores() {
    try {
        const response = await fetch('/admin/usuarios/api/coordinadores');
        if (response.ok) {
            coordinadores = await response.json();
            actualizarSelectCoordinadores();
        }
    } catch (error) {
        console.error('Error cargando coordinadores:', error);
    }
}

function actualizarSelectZonas() {
    const select = document.getElementById('zonaSelect');
    const filtroSelect = document.getElementById('filtroZona');
    
    select.innerHTML = '<option value="">Seleccionar zona...</option>';
    filtroSelect.innerHTML = '<option value="">Todas las zonas</option>';
    
    zonas.forEach(zona => {
        select.innerHTML += `<option value="${zona.uuid}">${zona.nombre}</option>`;
        filtroSelect.innerHTML += `<option value="${zona.uuid}">${zona.nombre}</option>`;
    });
}

function actualizarSelectPlanesTarifa() {
    const select = document.getElementById('planTarifaSelect');
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

function actualizarSelectCoordinadores() {
    const selectZona = document.getElementById('coordinadorZonaSelect');
    const selectOperativo = document.getElementById('coordinadorOperativoSelect');
    
    const opciones = '<option value="">Sin asignar</option>' + 
        coordinadores.map(coord => 
            `<option value="${coord.uuid}">${coord.nombre} ${coord.apellido}</option>`
        ).join('');
    
    selectZona.innerHTML = opciones;
    selectOperativo.innerHTML = opciones;
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
                            ${contrato.codigo}
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
                            <small class="text-muted d-block">Zonas</small>
                            <span><i class="fas fa-layer-group me-1"></i>${contrato.totalZonas || 0} zonas configuradas</span>
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
                            <i class="fas fa-user-tie"></i>
                            ${(contrato.totalCoordinadoresZona || 0) + (contrato.totalCoordinadoresOperativos || 0)} coordinadores
                        </span>                        
                    </div>
                </div>
                
                ${contrato.zonas && contrato.zonas.length > 0 ? `
                <div class="mt-3">
                    <small class="text-muted d-block mb-2">Zonas del Contrato</small>
                    <div class="zonas-preview">
                        ${contrato.zonas.slice(0, 3).map(zona => `
                            <span class="badge bg-info me-1 mb-1">
                                ${zona.zonaNombre}
                            </span>
                        `).join('')}
                        ${contrato.zonas.length > 3 ? `<span class="text-muted">+${contrato.zonas.length - 3} más</span>` : ''}
                    </div>
                </div>
                ` : ''}
            </div>
            <div class="contract-footer">
                <div class="action-buttons">
                    <a href="/admin/contratos/${contrato.uuid}/asignaciones" class="btn btn-sm btn-outline-primary">
                        <i class="fas fa-users me-1"></i>Asignaciones
                    </a>
                    <button class="btn btn-sm btn-outline-info" onclick="editarContrato('${contrato.uuid}')">
                        <i class="fas fa-edit me-1"></i>Editar
                    </button>
                    <button class="btn btn-sm btn-outline-secondary" onclick="gestionarZonasContrato('${contrato.uuid}')">
                        <i class="fas fa-layer-group me-1"></i>Zonas
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
    zonasSeleccionadas = [];
    document.getElementById('isEdit').value = 'false';
    document.getElementById('modalContratoTitle').textContent = 'Nuevo Contrato';
    document.getElementById('contratoForm').reset();
    renderZonasSeleccionadas();
    
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
        const response = await fetch(`/admin/contratos/api/${uuid}`);
        if (!response.ok) {
            throw new Error('Error al obtener datos del contrato');
        }
        
        const contrato = await response.json();
        console.log('Datos del contrato cargados:', contrato);
        
        // Llenar el formulario
        document.getElementById('codigo').value = contrato.codigo || '';
        document.getElementById('objetivo').value = contrato.objetivo || '';
        document.getElementById('fechaInicio').value = contrato.fechaInicio || '';
        document.getElementById('fechaFin').value = contrato.fechaFin || '';
        document.getElementById('supervisorUuid').value = contrato.supervisorUuid || '';
        document.getElementById('estado').value = contrato.estado || 'ACTIVO';
        
        // Cargar zonas del contrato
        zonasSeleccionadas = contrato.zonas || [];
        renderZonasSeleccionadas();
        
        modalContrato.show();
        
    } catch (error) {
        console.error('Error al cargar contrato:', error);
        showAlert('Error al cargar los datos del contrato', 'danger');
    }
}

async function gestionarZonasContrato(uuid) {
    currentEditUuid = uuid;
    
    try {
        // Cargar zonas actuales del contrato
        const response = await fetch(`/admin/contratos/${uuid}/zonas`);
        if (!response.ok) {
            throw new Error('Error al cargar zonas del contrato');
        }
        
        const data = await response.json();
        if (data.success) {
            zonasSeleccionadas = data.zonas || [];
            renderZonasSeleccionadas();
            abrirModalZonas();
        } else {
            throw new Error(data.message || 'Error al cargar zonas');
        }
        
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al cargar las zonas del contrato', 'danger');
    }
}

function abrirModalZonas() {
    modalZonas.show();
}

function agregarZonaALista() {
    const zonaUuid = document.getElementById('zonaSelect').value;
    const planTarifaUuid = document.getElementById('planTarifaSelect').value;
    const coordinadorZonaUuid = document.getElementById('coordinadorZonaSelect').value;
    const coordinadorOperativoUuid = document.getElementById('coordinadorOperativoSelect').value;
    
    if (!zonaUuid || !planTarifaUuid) {
        showAlert('Debe seleccionar zona y plan de tarifa', 'warning');
        return;
    }
    
    // Verificar que no esté duplicada
    if (zonasSeleccionadas.some(z => z.zonaUuid === zonaUuid)) {
        showAlert('La zona ya está agregada', 'warning');
        return;
    }
    
    const zona = zonas.find(z => z.uuid === zonaUuid);
    const planTarifa = planesTarifa.find(p => p.uuid === planTarifaUuid);
    const coordinadorZona = coordinadorZonaUuid ? coordinadores.find(c => c.uuid === coordinadorZonaUuid) : null;
    const coordinadorOperativo = coordinadorOperativoUuid ? coordinadores.find(c => c.uuid === coordinadorOperativoUuid) : null;
    
    const nuevaZona = {
        zonaUuid: zonaUuid,
        zonaNombre: zona.nombre,
        planTarifaUuid: planTarifaUuid,
        planTarifaNombre: planTarifa.nombre,
        coordinadorZonaUuid: coordinadorZonaUuid || null,
        coordinadorZonaNombre: coordinadorZona ? `${coordinadorZona.nombre} ${coordinadorZona.apellido}` : null,
        coordinadorOperativoUuid: coordinadorOperativoUuid || null,
        coordinadorOperativoNombre: coordinadorOperativo ? `${coordinadorOperativo.nombre} ${coordinadorOperativo.apellido}` : null,
        estado: 'ACTIVO'
    };
    
    zonasSeleccionadas.push(nuevaZona);
    renderZonasSeleccionadas();
    
    // Limpiar formulario
    document.getElementById('zonaSelect').value = '';
    document.getElementById('planTarifaSelect').value = '';
    document.getElementById('coordinadorZonaSelect').value = '';
    document.getElementById('coordinadorOperativoSelect').value = '';
}

function renderZonasSeleccionadas() {
    const container = document.getElementById('zonasSeleccionadas');
    const containerModal = document.getElementById('zonasSeleccionadasModal');
    
    if (zonasSeleccionadas.length === 0) {
        const emptyMessage = '<p class="text-muted">No se han seleccionado zonas</p>';
        container.innerHTML = emptyMessage;
        if (containerModal) containerModal.innerHTML = emptyMessage;
        return;
    }
    
    const zonasHtml = zonasSeleccionadas.map((zona, index) => `
        <div class="zona-item border rounded p-3 mb-2">
            <div class="d-flex justify-content-between align-items-start">
                <div class="flex-grow-1">
                    <h6 class="mb-1">
                        <i class="fas fa-map-marker-alt text-primary me-2"></i>
                        ${zona.zonaNombre}
                    </h6>
                    <p class="text-muted mb-2">
                        <i class="fas fa-dollar-sign me-1"></i>
                        ${zona.planTarifaNombre}
                    </p>
                    
                    ${zona.coordinadorZonaNombre || zona.coordinadorOperativoNombre ? `
                    <div class="coordinadores-info">
                        ${zona.coordinadorZonaNombre ? `
                        <span class="badge bg-info me-1">
                            <i class="fas fa-user-tie me-1"></i>Zona: ${zona.coordinadorZonaNombre}
                        </span>
                        ` : ''}
                        ${zona.coordinadorOperativoNombre ? `
                        <span class="badge bg-success">
                            <i class="fas fa-user-cog me-1"></i>Operativo: ${zona.coordinadorOperativoNombre}
                        </span>
                        ` : ''}
                    </div>
                    ` : `
                    <span class="badge bg-secondary">
                        <i class="fas fa-user-slash me-1"></i>Sin coordinadores
                    </span>
                    `}
                </div>
                <button type="button" class="btn btn-sm btn-outline-danger" 
                        onclick="removerZonaSeleccionada(${index})">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        </div>
    `).join('');
    
    container.innerHTML = zonasHtml;
    if (containerModal) containerModal.innerHTML = zonasHtml;
}

function removerZonaSeleccionada(index) {
    zonasSeleccionadas.splice(index, 1);
    renderZonasSeleccionadas();
}

async function guardarZonasSeleccionadas() {
    if (!currentEditUuid) {
        showAlert('Debe guardar el contrato primero', 'warning');
        return;
    }
    
    try {
        // Actualizar zonas del contrato
        for (const zona of zonasSeleccionadas) {
            if (!zona.uuid) {
                // Nueva zona
                await fetch(`/admin/contratos/${currentEditUuid}/zonas`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(zona)
                });
            } else {
                // Actualizar zona existente
                await fetch(`/admin/contratos/${currentEditUuid}/zonas/${zona.uuid}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(zona)
                });
            }
        }
        
        modalZonas.hide();
        showAlert('Zonas actualizadas exitosamente', 'success');
        cargarContratos();
        
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al actualizar las zonas', 'danger');
    }
}

async function saveContrato() {
    const form = document.getElementById('contratoForm');
    const isEdit = document.getElementById('isEdit').value === 'true';
    
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    if (zonasSeleccionadas.length === 0) {
        showAlert('Debe agregar al menos una zona al contrato', 'warning');
        return;
    }
    
    const formData = new FormData(form);
    const data = Object.fromEntries(formData);

    const contratoData = {
        codigo: data.codigo,
        objetivo: data.objetivo,
        fechaInicio: data.fechaInicio,
        fechaFin: data.fechaFin,
        supervisorUuid: data.supervisorUuid && data.supervisorUuid !== '' ? data.supervisorUuid : null,
        estado: data.estado || 'ACTIVO',
        zonas: zonasSeleccionadas
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
            ? `/admin/contratos/api/${currentEditUuid}`
            : '/admin/contratos/api/crear';
            
        const method = isEdit ? 'PUT' : 'POST';
        
        console.log('Enviando datos:', contratoData);
        
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },            
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
            const response = await fetch(`/admin/contratos/api/${uuid}/estado`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
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
        text: `¿Está seguro de eliminar el contrato ${contrato.codigo}?`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    });
    
    if (result.isConfirmed) {
        try {
            const response = await fetch(`/admin/contratos/api/${uuid}`, {
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
    
    if (codigo) {
        contratosFiltrados = contratosFiltrados.filter(c => 
            c.codigo.toLowerCase().includes(codigo) ||
            c.objetivo.toLowerCase().includes(codigo)
        );
    }
    
    if (estado) {
        contratosFiltrados = contratosFiltrados.filter(c => c.estado === estado);
    }
    
    if (zona) {
        contratosFiltrados = contratosFiltrados.filter(c => 
            c.zonas && c.zonas.some(z => z.zonaUuid === zona)
        );
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

// Funciones adicionales para predios
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
    if (!container) return;
    
    const total = predios.length;
    const asignados = predios.filter(p => p.operarioAsignado).length;
    const sinAsignar = total - asignados;
    
    const totalElement = document.getElementById('totalPredios');
    const asignadosElement = document.getElementById('prediosAsignadosCount');
    const sinAsignarElement = document.getElementById('prediosSinAsignarCount');
    
    if (totalElement) totalElement.textContent = total;
    if (asignadosElement) asignadosElement.textContent = asignados;
    if (sinAsignarElement) sinAsignarElement.textContent = sinAsignar;
    
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

// Funciones de validación
function validarZonasContrato() {
    if (zonasSeleccionadas.length === 0) {
        showAlert('El contrato debe tener al menos una zona configurada', 'warning');
        return false;
    }
    
    for (const zona of zonasSeleccionadas) {
        if (!zona.planTarifaUuid) {
            showAlert(`La zona ${zona.zonaNombre} debe tener un plan de tarifa asignado`, 'warning');
            return false;
        }
    }
    
    return true;
}

function validarFechasContrato(fechaInicio, fechaFin) {
    const inicio = new Date(fechaInicio);
    const fin = new Date(fechaFin);
    
    if (fin <= inicio) {
        showAlert('La fecha fin debe ser posterior a la fecha inicio', 'warning');
        return false;
    }
    
    const unAnoAtras = new Date();
    unAnoAtras.setFullYear(unAnoAtras.getFullYear() - 1);
    
    if (inicio < unAnoAtras) {
        const confirmar = confirm('La fecha de inicio es anterior a un año. ¿Está seguro?');
        if (!confirmar) return false;
    }
    
    return true;
}

// Funciones de utilidad
function formatearMoneda(cantidad) {
    return new Intl.NumberFormat('es-CO', {
        style: 'currency',
        currency: 'COP',
        minimumFractionDigits: 0
    }).format(cantidad);
}

function formatearNumero(numero) {
    return new Intl.NumberFormat('es-CO').format(numero);
}

function obtenerIniciales(nombreCompleto) {
    return nombreCompleto
        .split(' ')
        .map(palabra => palabra.charAt(0))
        .join('')
        .toUpperCase()
        .substring(0, 2);
}

function calcularDiasRestantes(fechaFin) {
    const hoy = new Date();
    const fin = new Date(fechaFin);
    const diferencia = fin.getTime() - hoy.getTime();
    const dias = Math.ceil(diferencia / (1000 * 3600 * 24));
    return dias;
}

// Funciones de búsqueda y filtrado
function buscarEnLista(termino, lista, campos) {
    if (!termino) return lista;
    
    const terminoLower = termino.toLowerCase();
    return lista.filter(item => {
        return campos.some(campo => {
            const valor = item[campo];
            return valor && valor.toString().toLowerCase().includes(terminoLower);
        });
    });
}

function ordenarContratos(contratos, criterio, ascendente = true) {
    return contratos.sort((a, b) => {
        let valorA, valorB;
        
        switch (criterio) {
            case 'codigo':
                valorA = a.codigo || '';
                valorB = b.codigo || '';
                break;
            case 'fechaInicio':
                valorA = new Date(a.fechaInicio);
                valorB = new Date(b.fechaInicio);
                break;
            case 'fechaFin':
                valorA = new Date(a.fechaFin);
                valorB = new Date(b.fechaFin);
                break;
            case 'estado':
                valorA = a.estado || '';
                valorB = b.estado || '';
                break;
            case 'progreso':
                valorA = a.porcentajeAvance || 0;
                valorB = b.porcentajeAvance || 0;
                break;
            default:
                return 0;
        }
        
        if (valorA < valorB) return ascendente ? -1 : 1;
        if (valorA > valorB) return ascendente ? 1 : -1;
        return 0;
    });
}

// Funciones de exportación
function exportarContratos(formato = 'csv') {
    const datosExport = contratos.map(contrato => ({
        'Código': contrato.codigo,
        'Objetivo': contrato.objetivo,
        'Fecha Inicio': contrato.fechaInicio,
        'Fecha Fin': contrato.fechaFin,
        'Estado': contrato.estado,
        'Supervisor': contrato.supervisorNombre || 'Sin asignar',
        'Total Zonas': contrato.totalZonas || 0,
        'Total Predios': contrato.totalPredios || 0,
        'Predios Asignados': contrato.prediosAsignados || 0,
        'Progreso %': contrato.porcentajeAvance || 0
    }));
    
    if (formato === 'csv') {
        exportarCSV(datosExport, 'contratos.csv');
    } else if (formato === 'json') {
        exportarJSON(datosExport, 'contratos.json');
    }
}

function exportarCSV(datos, nombreArchivo) {
    if (datos.length === 0) {
        showAlert('No hay datos para exportar', 'warning');
        return;
    }
    
    const headers = Object.keys(datos[0]);
    const csvContent = [
        headers.join(','),
        ...datos.map(row => headers.map(header => `"${row[header]}"`).join(','))
    ].join('\n');
    
    descargarArchivo(csvContent, nombreArchivo, 'text/csv');
}

function exportarJSON(datos, nombreArchivo) {
    const jsonContent = JSON.stringify(datos, null, 2);
    descargarArchivo(jsonContent, nombreArchivo, 'application/json');
}

function descargarArchivo(contenido, nombreArchivo, tipoMime) {
    const blob = new Blob([contenido], { type: tipoMime });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = nombreArchivo;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
}

// Manejo de errores
function manejarErrorRed(error, operacion = 'operación') {
    console.error(`Error en ${operacion}:`, error);
    
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
        showAlert('Error de conexión. Verifique su conexión a internet.', 'danger');
    } else if (error.status === 403) {
        showAlert('No tiene permisos para realizar esta acción.', 'warning');
    } else if (error.status === 404) {
        showAlert('Recurso no encontrado.', 'warning');
    } else if (error.status >= 500) {
        showAlert('Error del servidor. Intente nuevamente más tarde.', 'danger');
    } else {
        showAlert(`Error en ${operacion}. Intente nuevamente.`, 'danger');
    }
}

// Almacenamiento local
function guardarEnStorage(clave, valor) {
    try {
        localStorage.setItem(clave, JSON.stringify(valor));
    } catch (error) {
        console.warn('No se pudo guardar en localStorage:', error);
    }
}

function cargarDeStorage(clave, valorDefecto = null) {
    try {
        const valor = localStorage.getItem(clave);
        return valor ? JSON.parse(valor) : valorDefecto;
    } catch (error) {
        console.warn('No se pudo cargar de localStorage:', error);
        return valorDefecto;
    }
}

function cargarConfiguracionUsuario() {
    const config = cargarDeStorage('configUsuario', {
        ordenContratos: 'fechaInicio',
        ascendente: false,
        contratosPortPagina: 10
    });
    
    return config;
}

function guardarConfiguracionUsuario(config) {
    guardarEnStorage('configUsuario', config);
}

// Inicializar componentes Bootstrap
function inicializarComponentesBootstrap() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
    
    const popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    popoverTriggerList.map(function (popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });
}

// Funciones de gestión de zonas adicionales
async function removerZonaDelContrato(contratoUuid, zonaUuid) {
    if (!confirm('¿Está seguro de remover esta zona del contrato?')) return;
    
    try {
        const response = await fetch(`/admin/contratos/${contratoUuid}/zonas/${zonaUuid}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            showAlert('Zona removida exitosamente', 'success');
            cargarContratos();
        } else {
            const error = await response.json();
            showAlert(error.message || 'Error al remover zona', 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error de conexión', 'danger');
    }
}

async function obtenerZonasContrato(contratoUuid) {
    try {
        const response = await fetch(`/admin/contratos/${contratoUuid}/zonas`);
        if (response.ok) {
            const data = await response.json();
            return data.success ? data.zonas : [];
        }
        return [];
    } catch (error) {
        console.error('Error obteniendo zonas:', error);
        return [];
    }
}

// Limpiar formularios
function limpiarFormularioContrato() {
    document.getElementById('contratoForm').reset();
    zonasSeleccionadas = [];
    renderZonasSeleccionadas();
    currentEditUuid = null;
    
    document.getElementById('zonaSelect').value = '';
    document.getElementById('planTarifaSelect').value = '';
    document.getElementById('coordinadorZonaSelect').value = '';
    document.getElementById('coordinadorOperativoSelect').value = '';
}

// Event listeners adicionales
document.addEventListener('DOMContentLoaded', function() {
    inicializarComponentesBootstrap();
    
    const btnLimpiarFiltros = document.getElementById('btnLimpiarFiltros');
    if (btnLimpiarFiltros) {
        btnLimpiarFiltros.addEventListener('click', function() {
            document.getElementById('filtroCodigo').value = '';
            document.getElementById('filtroEstado').value = '';
            document.getElementById('filtroZona').value = '';
            renderContratos();
        });
    }
    
    const btnExportar = document.getElementById('btnExportar');
    if (btnExportar) {
        btnExportar.addEventListener('click', function() {
            exportarContratos('csv');
        });
    }
    
    const btnRefrescar = document.getElementById('btnRefrescar');
    if (btnRefrescar) {
        btnRefrescar.addEventListener('click', function() {
            cargarContratos();
        });
    }
});

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

// Función para debug
function debug() {
    console.log('=== DEBUG INFO ===');
    console.log('Contratos cargados:', contratos.length);
    console.log('Zonas disponibles:', zonas.length);
    console.log('Planes de tarifa:', planesTarifa.length);
    console.log('Supervisores:', supervisores.length);
    console.log('Coordinadores:', coordinadores.length);
    console.log('Zonas seleccionadas:', zonasSeleccionadas.length);
    console.log('Usuario actual:', usuarioActual);
    console.log('==================');
}

// Hacer disponible para debugging
window.debugContratos = debug;