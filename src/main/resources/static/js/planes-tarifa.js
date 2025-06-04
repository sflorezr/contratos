// planes-tarifa.js
// Variables globales
let planesTarifa = [];
let usuarioActual = null;
let modalPlanTarifa = null;
let currentEditUuid = null;

// Inicialización
document.addEventListener('DOMContentLoaded', function() {
    modalPlanTarifa = new bootstrap.Modal(document.getElementById('modalPlanTarifa'));
    inicializar();
    
    // Event listeners
    document.getElementById('btnNewPlan').addEventListener('click', openNewPlanModal);
    document.getElementById('btnNewPlan2').addEventListener('click', openNewPlanModal);
    document.getElementById('btnAplicarFiltros').addEventListener('click', aplicarFiltros);
    document.getElementById('savePlanTarifaBtn').addEventListener('click', savePlanTarifa);
    document.getElementById('btnAgregarTarifas').addEventListener('click', abrirModalTarifas);
    
    // Filtro en tiempo real
    document.getElementById('filtroNombre').addEventListener('input', aplicarFiltros);
});

async function inicializar() {
    cargarUsuarioActual();
    await Promise.all([
        cargarPlanesTarifa(),
        cargarEstadisticas()
    ]);
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

async function cargarPlanesTarifa() {
    showLoading(true);
    try {
        const response = await fetch('/admin/planes-tarifa/api/listar');
        if (response.ok) {
            planesTarifa = await response.json();
            renderPlanesTarifa();
        } else {
            showAlert('Error al cargar planes de tarifa', 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error de conexión', 'danger');
    } finally {
        showLoading(false);
    }
}

async function cargarEstadisticas() {
    try {
        const response = await fetch('/admin/planes-tarifa/resumen/general');
        if (response.ok) {
            const stats = await response.json();
            actualizarEstadisticas(stats);
        }
    } catch (error) {
        console.error('Error cargando estadísticas:', error);
    }
}

function renderPlanesTarifa() {
    const container = document.getElementById('planesTarifaContainer');
    const emptyState = document.getElementById('emptyState');
    
    if (planesTarifa.length === 0) {
        container.innerHTML = '';
        emptyState.classList.remove('d-none');
        return;
    }
    
    emptyState.classList.add('d-none');
    container.innerHTML = planesTarifa.map(plan => `
        <div class="contract-card">
            <div class="contract-header">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <h5 class="mb-1">
                            <i class="fas fa-tags text-primary me-2"></i>
                            ${plan.nombre}
                        </h5>
                        <p class="text-muted mb-0">${plan.descripcion || 'Sin descripción'}</p>
                    </div>
                    <span class="estado-badge estado-${plan.activo ? 'activo' : 'cancelado'}">
                        ${plan.activo ? 'Activo' : 'Inactivo'}
                    </span>
                </div>
            </div>
            <div class="contract-body">
                <div class="row">
                    <div class="col-md-6">
                        <div class="mb-3">
                            <small class="text-muted d-block">Fecha de Creación</small>
                            <span><i class="fas fa-calendar me-1"></i>${formatDate(plan.fechaCreacion)}</span>
                        </div>
                        <div class="mb-3">
                            <small class="text-muted d-block">Estado</small>
                            <span class="badge ${plan.activo ? 'bg-success' : 'bg-secondary'}">
                                <i class="fas fa-${plan.activo ? 'check' : 'pause'}-circle me-1"></i>
                                ${plan.activo ? 'Activo' : 'Inactivo'}
                            </span>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="mb-3">
                            <small class="text-muted d-block">Tarifas Asociadas</small>
                            <span><i class="fas fa-money-bill-wave me-1"></i>${plan.totalTarifas || 0} tarifas</span>
                        </div>
                        <div class="mb-3">
                            <small class="text-muted d-block">Contratos Activos</small>
                            <span><i class="fas fa-file-contract me-1"></i>${plan.contratosActivos || 0} contratos</span>
                        </div>
                    </div>
                </div>
                
                <div class="mt-3">
                    <small class="text-muted d-block mb-2">Resumen</small>
                    <div>
                        <span class="user-badge">
                            <i class="fas fa-money-bill-wave"></i>
                            ${plan.totalTarifas || 0} tarifas
                        </span>
                        <span class="user-badge">
                            <i class="fas fa-check-circle"></i>
                            ${plan.tarifasActivas || 0} activas
                        </span>
                        <span class="user-badge">
                            <i class="fas fa-file-contract"></i>
                            ${plan.totalContratos || 0} contratos
                        </span>
                        ${plan.activo ? `
                        <span class="user-badge">
                            <i class="fas fa-shield-alt"></i>
                            Disponible
                        </span>` : `
                        <span class="user-badge">
                            <i class="fas fa-ban"></i>
                            No disponible
                        </span>`}
                    </div>
                </div>
            </div>
            <div class="contract-footer">
                <div class="action-buttons">
                    <a href="/admin/planes-tarifa/${plan.uuid}/tarifas" class="btn btn-sm btn-outline-primary">
                        <i class="fas fa-money-bill-wave me-1"></i>Tarifas
                    </a>
                    <button class="btn btn-sm btn-outline-info" onclick="editarPlanTarifa('${plan.uuid}')">
                        <i class="fas fa-edit me-1"></i>Editar
                    </button>
                    <button class="btn btn-sm btn-outline-${plan.activo ? 'warning' : 'success'}" onclick="cambiarEstadoPlan('${plan.uuid}')">
                        <i class="fas fa-${plan.activo ? 'pause' : 'play'}-circle me-1"></i>
                        ${plan.activo ? 'Desactivar' : 'Activar'}
                    </button>
                    ${plan.puedeSerEliminado ? `
                    <button class="btn btn-sm btn-outline-danger" onclick="eliminarPlanTarifa('${plan.uuid}')">
                        <i class="fas fa-trash me-1"></i>Eliminar
                    </button>` : ''}
                </div>
            </div>
        </div>
    `).join('');
}

function actualizarEstadisticas(stats = null) {
    if (stats) {
        document.getElementById('totalPlanes').textContent = stats.totalPlanes || 0;
        document.getElementById('planesActivos').textContent = stats.planesActivos || 0;
        document.getElementById('planesInactivos').textContent = stats.planesInactivos || 0;
        document.getElementById('totalTarifas').textContent = stats.totalTarifas || 0;
    } else {
        // Calcular desde los datos locales
        const total = planesTarifa.length;
        const activos = planesTarifa.filter(p => p.activo).length;
        const inactivos = total - activos;
        const totalTarifas = planesTarifa.reduce((sum, p) => sum + (p.totalTarifas || 0), 0);
        
        document.getElementById('totalPlanes').textContent = total;
        document.getElementById('planesActivos').textContent = activos;
        document.getElementById('planesInactivos').textContent = inactivos;
        document.getElementById('totalTarifas').textContent = totalTarifas;
    }
}

function openNewPlanModal() {
    currentEditUuid = null;
    document.getElementById('isEdit').value = 'false';
    document.getElementById('modalPlanTarifaTitle').textContent = 'Nuevo Plan de Tarifa';
    document.getElementById('planTarifaForm').reset();
    document.getElementById('tarifasSection').classList.add('d-none');
    document.getElementById('contratosSection').classList.add('d-none');
    
    // Establecer valores por defecto
    document.getElementById('activo').value = 'true';
    
    modalPlanTarifa.show();
}

async function editarPlanTarifa(uuid) {
    currentEditUuid = uuid;
    document.getElementById('isEdit').value = 'true';
    document.getElementById('planTarifaUuid').value = uuid;
    document.getElementById('modalPlanTarifaTitle').textContent = 'Editar Plan de Tarifa';
    
    try {
        const response = await fetch(`/admin/planes-tarifa/${uuid}`);
        if (!response.ok) {
            showAlert('Error al cargar el plan de tarifa', 'danger');
            return;
        }
        
        const plan = await response.json();
        
        // Llenar el formulario
        document.getElementById('nombre').value = plan.nombre;
        document.getElementById('descripcion').value = plan.descripcion || '';
        document.getElementById('activo').value = plan.activo ? 'true' : 'false';
        
        // Mostrar secciones adicionales en modo edición
        document.getElementById('tarifasSection').classList.remove('d-none');
        document.getElementById('contratosSection').classList.remove('d-none');
        
        // Cargar detalles del plan
        await cargarDetallesPlan(uuid);
        
        modalPlanTarifa.show();
        
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al cargar el plan de tarifa', 'danger');
    }
}

async function cargarDetallesPlan(planUuid) {
    try {
        const response = await fetch(`/admin/planes-tarifa/detalles/${planUuid}`);
        if (response.ok) {
            const detalles = await response.json();
            renderTarifasAsociadas(detalles.tarifas || []);
            renderContratosAsociados(detalles.contratos || []);
        }
    } catch (error) {
        console.error('Error cargando detalles del plan:', error);
    }
}

function renderTarifasAsociadas(tarifas) {
    const container = document.getElementById('tarifasAsociadas');
    const total = tarifas.length;
    const activas = tarifas.filter(t => t.activa).length;
    const inactivas = total - activas;
    
    document.getElementById('totalTarifasCount').textContent = total;
    document.getElementById('tarifasActivasCount').textContent = activas;
    document.getElementById('tarifasInactivasCount').textContent = inactivas;
    
    if (tarifas.length === 0) {
        container.innerHTML = '<p class="text-muted text-center">No hay tarifas asociadas a este plan</p>';
        return;
    }
    
    container.innerHTML = tarifas.map(tarifa => `
        <div class="predio-item">
            <div class="d-flex justify-content-between align-items-center">
                <div>
                    <strong>${tarifa.nombre}</strong>
                    <small class="text-muted d-block">${tarifa.descripcion || 'Sin descripción'}</small>
                </div>
                <div class="text-end">
                    <span class="badge ${tarifa.activa ? 'bg-success' : 'bg-secondary'}">
                        ${tarifa.activa ? 'Activa' : 'Inactiva'}
                    </span>
                    <small class="text-muted d-block">$${formatMoney(tarifa.valor)}</small>
                </div>
            </div>
        </div>
    `).join('');
}

function renderContratosAsociados(contratos) {
    const container = document.getElementById('contratosAsociados');
    
    if (contratos.length === 0) {
        container.innerHTML = '<p class="text-muted text-center">No hay contratos usando este plan de tarifa</p>';
        return;
    }
    
    container.innerHTML = contratos.map(contrato => `
        <div class="predio-item">
            <div class="d-flex justify-content-between align-items-center">
                <div>
                    <strong>${contrato.numeroContrato || contrato.codigo}</strong>
                    <small class="text-muted d-block">${contrato.objetivo}</small>
                </div>
                <div class="text-end">
                    <span class="badge estado-${contrato.estado.toLowerCase()}">
                        ${getEstadoText(contrato.estado)}
                    </span>
                    <small class="text-muted d-block">${formatDate(contrato.fechaInicio)} - ${formatDate(contrato.fechaFin)}</small>
                </div>
            </div>
        </div>
    `).join('');
}

async function savePlanTarifa() {
    const form = document.getElementById('planTarifaForm');
    const isEdit = document.getElementById('isEdit').value === 'true';
    
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    const formData = new FormData(form);
    const data = Object.fromEntries(formData);
    
    // Convertir el campo activo a boolean
    data.activo = data.activo === 'true';
    
    const saveBtn = document.getElementById('savePlanTarifaBtn');
    const originalText = saveBtn.innerHTML;
    saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Guardando...';
    saveBtn.disabled = true;
    
    try {
        const url = isEdit 
            ? `/admin/planes-tarifa/${currentEditUuid}`
            : '/admin/planes-tarifa';
            
        const method = isEdit ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });
        
        if (response.ok) {
            modalPlanTarifa.hide();
            showAlert(isEdit ? 'Plan de tarifa actualizado exitosamente' : 'Plan de tarifa creado exitosamente', 'success');
            cargarPlanesTarifa();
            cargarEstadisticas();
        } else {
            const error = await response.json();
            showAlert(error.message || 'Error al guardar el plan de tarifa', 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error de conexión', 'danger');
    } finally {
        saveBtn.innerHTML = originalText;
        saveBtn.disabled = false;
    }
}

async function cambiarEstadoPlan(uuid) {
    const plan = planesTarifa.find(p => p.uuid === uuid);
    if (!plan) return;
    
    const accion = plan.activo ? 'desactivar' : 'activar';
    const nuevoEstado = !plan.activo;
    
    const result = await Swal.fire({
        title: `¿${accion.charAt(0).toUpperCase() + accion.slice(1)} plan?`,
        text: `¿Está seguro de ${accion} el plan "${plan.nombre}"?`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: nuevoEstado ? '#28a745' : '#ffc107',
        cancelButtonColor: '#6c757d',
        confirmButtonText: `Sí, ${accion}`,
        cancelButtonText: 'Cancelar'
    });
    
    if (result.isConfirmed) {
        try {
            const response = await fetch(`/admin/planes-tarifa/${uuid}/estado`, {
                method: 'PATCH'
            });
            
            if (response.ok) {
                showAlert(`Plan ${nuevoEstado ? 'activado' : 'desactivado'} exitosamente`, 'success');
                cargarPlanesTarifa();
                cargarEstadisticas();
            } else {
                showAlert('Error al cambiar estado del plan', 'danger');
            }
        } catch (error) {
            console.error('Error:', error);
            showAlert('Error de conexión', 'danger');
        }
    }
}

async function eliminarPlanTarifa(uuid) {
    const plan = planesTarifa.find(p => p.uuid === uuid);
    if (!plan) return;
    
    const result = await Swal.fire({
        title: '¿Eliminar plan de tarifa?',
        text: `¿Está seguro de eliminar el plan "${plan.nombre}"? Esta acción no se puede deshacer.`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    });
    
    if (result.isConfirmed) {
        try {
            const response = await fetch(`/admin/planes-tarifa/${uuid}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                showAlert('Plan de tarifa eliminado exitosamente', 'success');
                cargarPlanesTarifa();
                cargarEstadisticas();
            } else {
                const error = await response.json();
                showAlert(error.message || 'Error al eliminar el plan de tarifa', 'danger');
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
    const tarifas = document.getElementById('filtroTarifas').value;
    
    let planesFiltrados = planesTarifa;
    
    if (nombre) {
        planesFiltrados = planesFiltrados.filter(p => 
            p.nombre.toLowerCase().includes(nombre) ||
            (p.descripcion && p.descripcion.toLowerCase().includes(nombre))
        );
    }
    
    if (estado !== '') {
        const esActivo = estado === 'true';
        planesFiltrados = planesFiltrados.filter(p => p.activo === esActivo);
    }
    
    if (tarifas === 'con-tarifas') {
        planesFiltrados = planesFiltrados.filter(p => (p.totalTarifas || 0) > 0);
    } else if (tarifas === 'sin-tarifas') {
        planesFiltrados = planesFiltrados.filter(p => (p.totalTarifas || 0) === 0);
    }
    
    // Temporalmente actualizar la lista
    const planesOriginales = planesTarifa;
    planesTarifa = planesFiltrados;
    renderPlanesTarifa();
    planesTarifa = planesOriginales;
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

function formatMoney(amount) {
    if (!amount) return '0';
    return new Intl.NumberFormat('es-CO', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    }).format(amount);
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
    const container = document.getElementById('planesTarifaContainer');
    
    if (show) {
        loading.classList.remove('d-none');
        container.innerHTML = '';
    } else {
        loading.classList.add('d-none');
    }
}

// Función placeholder para abrir modal de tarifas
function abrirModalTarifas() {
    showAlert('Funcionalidad de gestión de tarifas en desarrollo', 'info');
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