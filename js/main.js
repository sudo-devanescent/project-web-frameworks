/* ============================================================
   MÓDULO PRINCIPAL (punto de entrada)
   Coordina la inicialización de todos los módulos de la
   aplicación. Se ejecuta una vez que el DOM está completamente
   cargado para garantizar que los contenedores existan.
   ============================================================= */

// Importa los módulos de la aplicación
import { initViews } from './views-loader.js';
import { initRouter } from './router.js';
import { initAuth } from './auth.js';
import { initFilters } from './filters.js';
import { initForms } from './forms.js';
import { initRender } from './render.js';

// Espera a que el documento esté listo antes de inicializar todo
document.addEventListener('DOMContentLoaded', async () => {
    // Primero se cargan las vistas parciales (son asíncronas),
    // ya que los demás módulos dependen de que existan los elementos HTML
    await initViews();

    // Luego se inicializan el resto de módulos sobre el DOM ya poblado
    initRouter();   // Navegación entre vistas
    initAuth();     // Login, cierre de sesión y control de roles
    initFilters();  // Buscadores de habitaciones y huéspedes
    initForms();    // Formulario de reserva
    initRender();   // Poblado de las tablas de habitaciones y pagos
});
