/* ============================================================
   MÓDULO: AUTENTICACIÓN SIMULADA Y CONTROL DE ROLES
   Gestiona el inicio y cierre de sesión (simulado, sin backend)
   y la autorización según el rol seleccionado. En el sistema
   real, este control lo haría Spring Security en el backend;
   aquí solo se simula a nivel visual para el prototipo.
   ============================================================= */

/**
 * Inicializa todos los manejadores de eventos de autenticación:
 * el envío del formulario de login y los enlaces del menú que
 * dependen del rol (huéspedes y pagos).
 */
export function initAuth() {
    // --- Manejador del formulario de login ---
    document.getElementById('form-login').addEventListener('submit', function (evento) {
        // Evita que la página se recargue (comportamiento normal de un <form>)
        evento.preventDefault();

        // Si hay campos obligatorios sin completar, muestra la validación
        // visual de Bootstrap y detiene el envío
        if (!this.checkValidity()) {
            evento.stopPropagation();
            this.classList.add('was-validated');
            return;
        }

        // Guarda el rol elegido y actualiza la interfaz según el rol
        const rolSeleccionado = document.getElementById('rol-login').value;
        aplicarRol(rolSeleccionado);

        // Oculta la pantalla de login y muestra el contenedor interno
        // (dashboard por defecto)
        document.getElementById('vista-login').classList.remove('activa');
        document.getElementById('contenedor-interno').classList.add('activa');

        // Muestra el dashboard como pantalla de bienvenida
        mostrarVista('dashboard');
    });

    // --- Manejadores de los enlaces del menú (huéspedes y pagos) ---
    // Estos enlaces no usan la navegación por atributo onclick, así que
    // se les asigna su manejador aquí de forma programática.
    // El operador "?." evita errores si el elemento no existe.
    document.getElementById('link-huespedes')?.addEventListener('click', function (e) {
        e.preventDefault();
        mostrarVista('listado-huespedes');
    });

    document.getElementById('link-pagos')?.addEventListener('click', function (e) {
        e.preventDefault();
        mostrarVista('pagos');
    });

    // --- Manejador del botón "Salir" ---
    // El botón usa el atributo onclick directo en el HTML, por lo que
    // solo necesitamos exponer la función en el ámbito global.
    window.cerrarSesion = cerrarSesion;
}

/**
 * Cierra la sesión: vuelve a mostrar el login y reinicia su formulario.
 * Se expone en el ámbito global porque se invoca desde el atributo
 * onclick del botón "Salir" en el HTML.
 */
function cerrarSesion() {
    // Oculta el contenedor interno y muestra la pantalla de login
    document.getElementById('contenedor-interno').classList.remove('activa');
    document.getElementById('vista-login').classList.add('activa');

    // Limpia el formulario y quita el estado de validación
    document.getElementById('form-login').reset();
    document.getElementById('form-login').classList.remove('was-validated');
}

/**
 * Aplica la autorización visual según el rol seleccionado.
 * Los elementos con la clase "elemento-solo-admin" solo se muestran
 * si el rol activo es ADMINISTRADOR.
 * @param {string} rol - Clave del rol (ADMINISTRADOR, RECEPCIONISTA, HUESPED).
 */
function aplicarRol(rol) {
    // Mapa de claves de rol a etiquetas legibles para mostrar en la barra
    const nombresRol = {
        ADMINISTRADOR: 'Administrador',
        RECEPCIONISTA: 'Recepcionista',
        HUESPED: 'Huésped'
    };

    // Actualiza el texto del indicador de rol en la navbar
    document.getElementById('rol-actual-texto').textContent = nombresRol[rol];

    // Muestra u oculta los elementos exclusivos del administrador
    const elementosSoloAdmin = document.querySelectorAll('.elemento-solo-admin');
    elementosSoloAdmin.forEach(function (elemento) {
        elemento.style.display = (rol === 'ADMINISTRADOR') ? '' : 'none';
    });
}
