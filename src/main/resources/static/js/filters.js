/* ============================================================
   MÓDULO: BUSCADORES / FILTROS DE TABLAS
   Implementa el filtrado en tiempo real de las tablas de
   habitaciones y de huéspedes. Mientras el usuario escribe en
   el buscador, se ocultan las filas que no coinciden.
   ============================================================= */

/**
 * Inicializa los filtros de las tablas. Expone las funciones de
 * filtrado en el ámbito global porque se invocan desde el atributo
 * onkeyup de los buscadores en el HTML.
 */
export function initFilters() {
    window.filtrarHabitaciones = filtrarHabitaciones;
    window.filtrarHuespedes = filtrarHuespedes;
}

/**
 * Filtra las filas de la tabla de habitaciones según el texto buscado.
 * Se invoca desde el atributo onkeyup del buscador correspondiente.
 */
function filtrarHabitaciones() {
    // Obtiene el texto buscado en minúsculas para comparar sin distinguir mayúsculas
    const texto = document.getElementById('buscador-habitaciones').value.toLowerCase();

    // Selecciona todas las filas del cuerpo de la tabla de habitaciones
    const filas = document.querySelectorAll('#cuerpo-tabla-habitaciones tr');
    let hayResultados = false;

    // Recorre cada fila: la muestra u oculta según si coincide con la búsqueda
    filas.forEach(function (fila) {
        const contenido = fila.textContent.toLowerCase();
        const coincide = contenido.includes(texto);
        fila.style.display = coincide ? '' : 'none';
        if (coincide) hayResultados = true;
    });

    // Muestra u oculta el mensaje de "sin resultados" según corresponda
    document.getElementById('mensaje-sin-resultados').classList.toggle('d-none', hayResultados);
}

/**
 * Filtra las filas de la tabla de huéspedes según el texto buscado.
 * Se invoca desde el atributo onkeyup del buscador correspondiente.
 */
function filtrarHuespedes() {
    // Obtiene el texto buscado en minúsculas para comparar sin distinguir mayúsculas
    const texto = document.getElementById('buscador-huespedes').value.toLowerCase();

    // Selecciona todas las filas del cuerpo de la tabla de huéspedes
    const filas = document.querySelectorAll('#cuerpo-tabla-huespedes tr');
    let hayResultados = false;

    // Recorre cada fila: la muestra u oculta según si coincide con la búsqueda
    filas.forEach(function (fila) {
        const contenido = fila.textContent.toLowerCase();
        const coincide = contenido.includes(texto);
        fila.style.display = coincide ? '' : 'none';
        if (coincide) hayResultados = true;
    });

    // Muestra u oculta el mensaje de "sin resultados" según corresponda
    document.getElementById('mensaje-sin-resultados-huespedes').classList.toggle('d-none', hayResultados);
}
