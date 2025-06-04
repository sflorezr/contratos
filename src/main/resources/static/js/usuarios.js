
// Estado de la aplicación
let usuarios = [];
let modal = null;
let currentEditUuid = null;
let cameraStream = null;
let cameraModal = null;

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', function() {
    modal = new bootstrap.Modal(document.getElementById('modalUsuario'));
    setupEventListeners();
    loadUsuarios();
    cameraModal = new bootstrap.Modal(document.getElementById('cameraModal'));
    setupPhotoEventListeners();    
});

// Configurar event listeners
function setupEventListeners() {
    document.getElementById('saveUserBtn').addEventListener('click', saveUser);
    document.getElementById('filterPerfil').addEventListener('change', applyFilters);
    document.getElementById('filterSearch').addEventListener('input', debounce(applyFilters, 500));
    
    // Validación en tiempo real
    document.getElementById('username').addEventListener('blur', validateUsername);
    document.getElementById('email').addEventListener('blur', validateEmail);
    document.getElementById('confirmPassword').addEventListener('input', validatePasswords);
    document.getElementById('password').addEventListener('input', validatePasswords);
}

// Abrir modal para nuevo usuario
function openNewUserModal() {
    currentEditUuid = null;
    document.getElementById('isEdit').value = 'false';
    document.getElementById('modalUsuarioTitle').innerHTML = '<i class="fas fa-user-plus me-2"></i>Nuevo Usuario';
    document.getElementById('userForm').reset();
    
    // Mostrar campos de contraseña como requeridos
    document.getElementById('password').required = true;
    document.getElementById('confirmPassword').required = true;
    document.getElementById('passwordRequired').style.display = 'inline';
    document.getElementById('confirmPasswordRequired').style.display = 'inline';
    document.getElementById('passwordHint').classList.add('d-none');
    document.getElementById('passwordSectionTitle').textContent = 'Contraseña';
    
    // Limpiar validaciones
    document.getElementById('userForm').classList.remove('was-validated');
    clearFieldErrors();
    
    modal.show();
}

// Abrir modal para editar usuario
async function editUser(uuid) {
    currentEditUuid = uuid;
    document.getElementById('isEdit').value = 'true';
    document.getElementById('userUuid').value = uuid;
    document.getElementById('modalUsuarioTitle').innerHTML = '<i class="fas fa-user-edit me-2"></i>Editar Usuario';
    
    // Buscar usuario en la lista local
    const usuario = usuarios.find(u => u.uuid === uuid);
    if (!usuario) {
        showAlert('Usuario no encontrado', 'danger');
        return;
    }
    
    // Llenar el formulario
    document.getElementById('username').value = usuario.username;
    document.getElementById('perfil').value = usuario.perfil;
    document.getElementById('nombre').value = usuario.nombre;
    document.getElementById('apellido').value = usuario.apellido;
    document.getElementById('email').value = usuario.email;
    document.getElementById('telefono').value = usuario.telefono || '';
    
    // Configurar campos de contraseña para edición
    document.getElementById('password').value = '';
    document.getElementById('confirmPassword').value = '';
    document.getElementById('password').required = false;
    document.getElementById('confirmPassword').required = false;
    document.getElementById('passwordRequired').style.display = 'none';
    document.getElementById('confirmPasswordRequired').style.display = 'none';
    document.getElementById('passwordHint').classList.remove('d-none');
    document.getElementById('passwordSectionTitle').textContent = 'Cambiar Contraseña (Opcional)';
    
    // Limpiar validaciones
    document.getElementById('userForm').classList.remove('was-validated');
    clearFieldErrors();
    
    modal.show();
}

// Cargar usuarios desde el servidor
async function loadUsuarios() {
    showLoading(true);
    try {
        const response = await fetch('/admin/usuarios/api/buscar');
        if (response.ok) {
            const data = await response.json();
            usuarios = data.content || [];
            renderUsuarios();
        } else {
            showAlert('Error al cargar usuarios', 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error de conexión', 'danger');
    } finally {
        showLoading(false);
    }
}

// Renderizar tabla de usuarios
function renderUsuarios() {
    const tbody = document.getElementById('usuariosTableBody');
    
    if (usuarios.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center py-4">
                    <i class="fas fa-users fa-3x text-muted mb-3 d-block"></i>
                    <span class="text-muted">No hay usuarios</span>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = usuarios.map(usuario => `
        <tr>
            <td>
                <div class="d-flex align-items-center">
                    ${usuario.foto ? 
                        `<img src="${usuario.foto}" class="rounded-circle me-2" style="width: 30px; height: 30px; object-fit: cover;" alt="Foto">` :
                        `<div class="avatar-circle me-2" style="width: 30px; height: 30px; font-size: 12px;">
                            ${getInitials(usuario.nombre, usuario.apellido)}
                        </div>`
                    }
                    <strong>${usuario.username}</strong>
                </div>
            </td>
            <td>${usuario.nombre} ${usuario.apellido}</td>
            <td>${usuario.email}</td>
            <td><span class="badge profile-badge ${getPerfilColor(usuario.perfil)}">${getPerfilText(usuario.perfil)}</span></td>
            <td><span class="badge ${usuario.activo ? 'bg-success' : 'bg-secondary'}">${usuario.activo ? 'Activo' : 'Inactivo'}</span></td>
            <td>
                <button class="btn btn-sm btn-outline-primary me-1" onclick="editUser('${usuario.uuid}')" title="Editar">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm btn-outline-warning me-1" onclick="toggleUserStatus('${usuario.uuid}')" title="Cambiar Estado">
                    <i class="fas fa-toggle-${usuario.activo ? 'on' : 'off'}"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger" onclick="deleteUser('${usuario.uuid}')" title="Eliminar">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

// Guardar usuario (crear o editar)
async function saveUser() {
    const form = document.getElementById('userForm');
    const isEdit = document.getElementById('isEdit').value === 'true';
    
    // Validar formulario
    if (!validateForm()) {
        return;
    }
    
    const formData = new FormData(form);
    
    const saveBtn = document.getElementById('saveUserBtn');
    const originalText = saveBtn.innerHTML;
    saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Guardando...';
    saveBtn.disabled = true;
    
    try {
        const url = isEdit 
            ? `/admin/usuarios/${currentEditUuid}/editar` 
            : '/admin/usuarios/nuevo';
            
        const response = await fetch(url, {
            method: 'POST',
            body: formData
        });
        
        if (response.redirected) {
            // Si hay redirección, recargar la página
            window.location.href = response.url;
        } else if (response.ok) {
            modal.hide();
            showAlert(isEdit ? 'Usuario actualizado exitosamente' : 'Usuario creado exitosamente', 'success');
            loadUsuarios();
        } else {
            const text = await response.text();
            showAlert('Error al guardar usuario', 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error de conexión', 'danger');
    } finally {
        saveBtn.innerHTML = originalText;
        saveBtn.disabled = false;
    }
}

// Validar formulario
function validateForm() {
    const form = document.getElementById('userForm');
    const isEdit = document.getElementById('isEdit').value === 'true';
    
    // Validar campos requeridos
    let isValid = true;
    
    // Username
    const username = document.getElementById('username');
    if (!username.value.trim()) {
        setFieldError(username, 'El username es requerido');
        isValid = false;
    }
    
    // Email
    const email = document.getElementById('email');
    if (!email.value.trim() || !email.value.includes('@')) {
        setFieldError(email, 'El email es requerido y debe ser válido');
        isValid = false;
    }
    
    // Nombre
    const nombre = document.getElementById('nombre');
    if (!nombre.value.trim()) {
        setFieldError(nombre, 'El nombre es requerido');
        isValid = false;
    }
    
    // Apellido
    const apellido = document.getElementById('apellido');
    if (!apellido.value.trim()) {
        setFieldError(apellido, 'El apellido es requerido');
        isValid = false;
    }
    
    // Perfil
    const perfil = document.getElementById('perfil');
    if (!perfil.value) {
        setFieldError(perfil, 'El perfil es requerido');
        isValid = false;
    }
    
    // Contraseñas
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    
    if (!isEdit && !password) {
        setFieldError(document.getElementById('password'), 'La contraseña es requerida');
        isValid = false;
    }
    
    if (password && password !== confirmPassword) {
        setFieldError(document.getElementById('confirmPassword'), 'Las contraseñas no coinciden');
        isValid = false;
    }
    
    form.classList.add('was-validated');
    return isValid;
}

// Validar username único
async function validateUsername() {
    const username = document.getElementById('username').value.trim();
    if (!username) return;
    
    const uuid = currentEditUuid || null;
    
    try {
        const response = await fetch(`/admin/usuarios/api/validar-username?username=${encodeURIComponent(username)}&uuid=${uuid || ''}`);
        const data = await response.json();
        
        if (!data.disponible) {
            setFieldError(document.getElementById('username'), 'Este username ya está en uso');
        } else {
            clearFieldError(document.getElementById('username'));
        }
    } catch (error) {
        console.error('Error validando username:', error);
    }
}

// Validar email único
async function validateEmail() {
    const email = document.getElementById('email').value.trim();
    if (!email) return;
    
    const uuid = currentEditUuid || null;
    
    try {
        const response = await fetch(`/admin/usuarios/api/validar-email?email=${encodeURIComponent(email)}&uuid=${uuid || ''}`);
        const data = await response.json();
        
        if (!data.disponible) {
            setFieldError(document.getElementById('email'), 'Este email ya está en uso');
        } else {
            clearFieldError(document.getElementById('email'));
        }
    } catch (error) {
        console.error('Error validando email:', error);
    }
}

// Validar que las contraseñas coincidan
function validatePasswords() {
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    
    if (password && confirmPassword && password !== confirmPassword) {
        setFieldError(document.getElementById('confirmPassword'), 'Las contraseñas no coinciden');
    } else if (confirmPassword) {
        clearFieldError(document.getElementById('confirmPassword'));
    }
}

// Cambiar estado de usuario
async function toggleUserStatus(uuid) {
    if (!confirm('¿Desea cambiar el estado del usuario?')) return;
    
    try {
        const response = await fetch(`/admin/usuarios/${uuid}/cambiar-estado`, {
            method: 'POST'
        });
        
        if (response.ok) {
            showAlert('Estado cambiado exitosamente', 'success');
            loadUsuarios();
        } else {
            showAlert('Error al cambiar estado', 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error de conexión', 'danger');
    }
}

// Eliminar usuario
async function deleteUser(uuid) {
    const usuario = usuarios.find(u => u.uuid === uuid);
    if (!usuario) return;
    
    if (!confirm(`¿Está seguro de eliminar al usuario ${usuario.username}?\n\nEsta acción no se puede deshacer.`)) {
        return;
    }
    
    try {
        const response = await fetch(`/admin/usuarios/${uuid}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            showAlert('Usuario eliminado exitosamente', 'success');
            loadUsuarios();
        } else {
            const data = await response.json();
            showAlert(data.message || 'Error al eliminar usuario', 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error de conexión', 'danger');
    }
}

// Aplicar filtros
function applyFilters() {
    const perfilFilter = document.getElementById('filterPerfil').value;
    const searchFilter = document.getElementById('filterSearch').value.toLowerCase();
    
    const filtered = usuarios.filter(usuario => {
        const matchesPerfil = !perfilFilter || usuario.perfil === perfilFilter;
        const matchesSearch = !searchFilter || 
            usuario.nombre.toLowerCase().includes(searchFilter) ||
            usuario.apellido.toLowerCase().includes(searchFilter) ||
            usuario.username.toLowerCase().includes(searchFilter) ||
            usuario.email.toLowerCase().includes(searchFilter);
        
        return matchesPerfil && matchesSearch;
    });
    
    // Temporalmente actualizar la lista para el filtro
    const originalUsuarios = usuarios;
    usuarios = filtered;
    renderUsuarios();
    usuarios = originalUsuarios;
}

// Utilidades para manejo de errores en campos
function setFieldError(field, message) {
    field.classList.add('is-invalid');
    const feedback = field.nextElementSibling;
    if (feedback && feedback.classList.contains('invalid-feedback')) {
        feedback.textContent = message;
    }
}

function clearFieldError(field) {
    field.classList.remove('is-invalid');
}

function clearFieldErrors() {
    document.querySelectorAll('.is-invalid').forEach(field => {
        field.classList.remove('is-invalid');
    });
}

// Utilidades
function getInitials(nombre, apellido) {
    return ((nombre || '').charAt(0) + (apellido || '').charAt(0)).toUpperCase();
}

function getPerfilColor(perfil) {
    const colors = {
        'ADMINISTRADOR': 'bg-danger',
        'SUPERVISOR': 'bg-warning',
        'COORDINADOR': 'bg-info',
        'OPERARIO': 'bg-primary'
    };
    return colors[perfil] || 'bg-secondary';
}

function getPerfilText(perfil) {
    const texts = {
        'ADMINISTRADOR': 'Admin',
        'SUPERVISOR': 'Supervisor',
        'COORDINADOR': 'Coordinador',
        'OPERARIO': 'Operario'
    };
    return texts[perfil] || perfil;
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
    
    // Auto-remove después de 5 segundos
    setTimeout(() => {
        const alert = document.getElementById(alertId);
        if (alert) alert.remove();
    }, 5000);
}

function showLoading(show) {
    const loading = document.getElementById('loadingIndicator');
    const table = document.getElementById('usuariosTableBody');
    
    if (show) {
        loading.classList.remove('d-none');
        table.innerHTML = '';
    } else {
        loading.classList.add('d-none');
    }
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}
function setupPhotoEventListeners() {
    // Event listener para input de archivo
    document.getElementById('photoFileInput').addEventListener('change', handleFileSelect);
}

// Seleccionar archivo de foto
function selectPhotoFile() {
    document.getElementById('photoFileInput').click();
}

// Manejar selección de archivo
function handleFileSelect(event) {
    const file = event.target.files[0];
    if (file && file.type.startsWith('image/')) {
        processImageFile(file);
    }
}

// Procesar archivo de imagen
function processImageFile(file) {
    const reader = new FileReader();
    reader.onload = function(e) {
        const img = new Image();
        img.onload = function() {
            // Redimensionar y comprimir imagen
            const canvas = document.getElementById('photoCanvas');
            const ctx = canvas.getContext('2d');
            
            // Configurar tamaño del canvas (máximo 300x300)
            const maxSize = 300;
            let { width, height } = img;
            
            if (width > height) {
                if (width > maxSize) {
                    height = height * (maxSize / width);
                    width = maxSize;
                }
            } else {
                if (height > maxSize) {
                    width = width * (maxSize / height);
                    height = maxSize;
                }
            }
            
            canvas.width = width;
            canvas.height = height;
            
            // Dibujar imagen redimensionada
            ctx.drawImage(img, 0, 0, width, height);
            
            // Convertir a blob y mostrar preview
            canvas.toBlob(function(blob) {
                displayPhotoPreview(canvas.toDataURL('image/jpeg', 0.8));
                // Crear nuevo archivo para el formulario
                const newFile = new File([blob], 'foto.jpg', { type: 'image/jpeg' });
                updateFileInput(newFile);
            }, 'image/jpeg', 0.8);
        };
        img.src = e.target.result;
    };
    reader.readAsDataURL(file);
}

// Abrir cámara
async function openCamera() {
    try {
        const stream = await navigator.mediaDevices.getUserMedia({ 
            video: { 
                width: { ideal: 640 },
                height: { ideal: 480 },
                facingMode: 'user' // Cámara frontal preferida
            } 
        });
        
        cameraStream = stream;
        const video = document.getElementById('cameraVideo');
        video.srcObject = stream;
        
        cameraModal.show();
    } catch (error) {
        console.error('Error accediendo a la cámara:', error);
        showAlert('No se pudo acceder a la cámara. Verifique los permisos.', 'warning');
    }
}

// Capturar foto de la cámara
function capturePhoto() {
    const video = document.getElementById('cameraVideo');
    const canvas = document.getElementById('photoCanvas');
    const ctx = canvas.getContext('2d');
    
    // Configurar canvas con las dimensiones del video
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    
    // Capturar frame del video
    ctx.drawImage(video, 0, 0);
    
    // Redimensionar si es necesario
    const maxSize = 300;
    if (canvas.width > maxSize || canvas.height > maxSize) {
        const tempCanvas = document.createElement('canvas');
        const tempCtx = tempCanvas.getContext('2d');
        
        let { width, height } = canvas;
        if (width > height) {
            if (width > maxSize) {
                height = height * (maxSize / width);
                width = maxSize;
            }
        } else {
            if (height > maxSize) {
                width = width * (maxSize / height);
                height = maxSize;
            }
        }
        
        tempCanvas.width = width;
        tempCanvas.height = height;
        tempCtx.drawImage(canvas, 0, 0, width, height);
        
        // Reemplazar canvas principal
        canvas.width = width;
        canvas.height = height;
        ctx.drawImage(tempCanvas, 0, 0);
    }
    
    // Convertir a blob y mostrar preview
    canvas.toBlob(function(blob) {
        displayPhotoPreview(canvas.toDataURL('image/jpeg', 0.8));
        // Crear archivo para el formulario
        const file = new File([blob], 'foto_camara.jpg', { type: 'image/jpeg' });
        updateFileInput(file);
        closeCamera();
    }, 'image/jpeg', 0.8);
}

// Cerrar cámara
function closeCamera() {
    if (cameraStream) {
        cameraStream.getTracks().forEach(track => track.stop());
        cameraStream = null;
    }
    cameraModal.hide();
}

// Mostrar preview de la foto
function displayPhotoPreview(dataUrl) {
    const placeholder = document.getElementById('photoPlaceholder');
    const preview = document.getElementById('photoPreview');
    const removeBtn = document.getElementById('removePhotoBtn');
    
    placeholder.classList.add('d-none');
    preview.classList.remove('d-none');
    preview.src = dataUrl;
    removeBtn.classList.remove('d-none');
}

// Actualizar input file con nueva imagen
function updateFileInput(file) {
    const fileInput = document.getElementById('photoFileInput');
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    fileInput.files = dataTransfer.files;
}

// Quitar foto
function removePhoto() {
    const placeholder = document.getElementById('photoPlaceholder');
    const preview = document.getElementById('photoPreview');
    const removeBtn = document.getElementById('removePhotoBtn');
    const fileInput = document.getElementById('photoFileInput');
    
    placeholder.classList.remove('d-none');
    preview.classList.add('d-none');
    removeBtn.classList.add('d-none');
    preview.src = '';
    fileInput.value = '';
}

// Limpiar foto al abrir modal para nuevo usuario
function openNewUserModal() {
    currentEditUuid = null;
    document.getElementById('isEdit').value = 'false';
    document.getElementById('modalUsuarioTitle').innerHTML = '<i class="fas fa-user-plus me-2"></i>Nuevo Usuario';
    document.getElementById('userForm').reset();
    
    // Mostrar campos de contraseña como requeridos
    document.getElementById('password').required = true;
    document.getElementById('confirmPassword').required = true;
    document.getElementById('passwordRequired').style.display = 'inline';
    document.getElementById('confirmPasswordRequired').style.display = 'inline';
    document.getElementById('passwordHint').classList.add('d-none');
    document.getElementById('passwordSectionTitle').textContent = 'Contraseña';
    
    // Limpiar validaciones
    document.getElementById('userForm').classList.remove('was-validated');
    clearFieldErrors();
    removePhoto(); // Agregar esta línea
    modal.show();
}

// Cargar foto existente al editar usuario
async function editUser(uuid) {
    currentEditUuid = uuid;
    document.getElementById('isEdit').value = 'true';
    document.getElementById('userUuid').value = uuid;
    document.getElementById('modalUsuarioTitle').innerHTML = '<i class="fas fa-user-edit me-2"></i>Editar Usuario';
    
    // Buscar usuario en la lista local
    const usuario = usuarios.find(u => u.uuid === uuid);
    if (!usuario) {
        showAlert('Usuario no encontrado', 'danger');
        return;
    }
    
    // Llenar el formulario
    document.getElementById('username').value = usuario.username;
    document.getElementById('perfil').value = usuario.perfil;
    document.getElementById('nombre').value = usuario.nombre;
    document.getElementById('apellido').value = usuario.apellido;
    document.getElementById('email').value = usuario.email;
    document.getElementById('telefono').value = usuario.telefono || '';
    
    // Configurar campos de contraseña para edición
    document.getElementById('password').value = '';
    document.getElementById('confirmPassword').value = '';
    document.getElementById('password').required = false;
    document.getElementById('confirmPassword').required = false;
    document.getElementById('passwordRequired').style.display = 'none';
    document.getElementById('confirmPasswordRequired').style.display = 'none';
    document.getElementById('passwordHint').classList.remove('d-none');
    document.getElementById('passwordSectionTitle').textContent = 'Cambiar Contraseña (Opcional)';
    if (usuario.foto) {
        displayPhotoPreview(usuario.foto);
    } else {
        removePhoto();
    }    
    // Limpiar validaciones
    document.getElementById('userForm').classList.remove('was-validated');    
    clearFieldErrors();    
    modal.show();
}