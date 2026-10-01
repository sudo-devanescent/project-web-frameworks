/* ============================================================
   MÓDULO: NAVEGACIÓN ENTRE VISTAS (Router)
   El prototipo funciona como una aplicación de una sola página
   (SPA): la "navegación" entre pantallas se simula mostrando
   y ocultando secciones HTML en lugar de recargar páginas.
   Este módulo se encarga de mostrar/ocultar cada vista y de
   resaltar el enlace activo en el menú de navegación.
   ============================================================= */

/**
 * Inicializa la lógica de navegación. Como la función mostrarVista
 * se usa desde los atributos onclick del HTML y desde otros módulos,
 * se expone en el objeto global (window) para que sea accesible.
 */
export function initRouter() {
    // Exponer mostrarVista en el ámbito global para su uso en los
    // manejadores onclick definidos en el HTML de las vistas.
    window.mostrarVista = mostrarVista;
}

/**
 * Muestra la vista solicitada y oculta las demás.
 * @param {string} nombreVista - Nombre de la vista a mostrar
 *                               (ej.: 'dashboard', 'listado', 'formulario').
 */
function mostrarVista(nombreVista) {
    // Lista de todas las vistas internas disponibles en el sistema
    const vistasInternas = ['dashboard', 'listado', 'formulario', 'listado-huespedes', 'pagos'];

    // Recorre cada vista: la oculta y quita el resaltado del enlace
    vistasInternas.forEach(function (vista) {
        document.getElementById('vista-' + vista).classList.remove('activa');
        document.getElementById('link-' + vista)?.classList.remove('active');
    });

    // Muestra la vista solicitada y resalta el enlace del menú
    document.getElementById('vista-' + nombreVista).classList.add('activa');
    document.getElementById('link-' + nombreVista)?.classList.add('active');
}
