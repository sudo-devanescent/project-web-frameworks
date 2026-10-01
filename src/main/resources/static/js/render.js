/* ============================================================
   MÓDULO: RENDERIZADO DE TABLAS
   Genera dinámicamente las filas de las tablas de habitaciones
   y de pagos a partir de los datos definidos en /js/data.
   De esta forma, el HTML no repite filas estáticas y los datos
   están centralizados para su futura conexión con un backend.
   ============================================================= */

// Importa los datos hardcodeados para poder pintarlos
import { habitaciones } from './data/habitaciones.js';
import { huespedes } from './data/huespedes.js';
import { pagos } from './data/pagos.js';

/**
 * Inicializa la renderización de todas las tablas que lo requieren.
 */
export function initRender() {
    renderTablaHabitaciones();
    renderTablaHuespedes();
    renderTablaPagos();
}

/**
 * Rellena la tabla de habitaciones a partir de los datos.
 * Cada fila muestra número, tipo, capacidad, precio y un
 * badge de estado con el color correspondiente.
 */
function renderTablaHabitaciones() {
    const cuerpo = document.getElementById('cuerpo-tabla-habitaciones');

    // Convierte cada habitación del catálogo en una fila HTML
    const filasHTML = habitaciones.map(function (hab) {
        // Determina la clase del badge según el estado de la habitación
        let claseBadge = 'badge-disponible';
        if (hab.estado === 'Ocupada')       claseBadge = 'badge-ocupada';
        else if (hab.estado === 'Mantenimiento') claseBadge = 'badge-mantenimiento';

        return `
            <tr>
                <td>${hab.numero}</td>
                <td>${hab.tipo}</td>
                <td>${hab.capacidad}</td>
                <td>${hab.precio}</td>
                <td><span class="badge ${claseBadge}">${hab.estado}</span></td>
            </tr>
        `;
    });

    // Inserta todas las filas generadas en el cuerpo de la tabla
    cuerpo.innerHTML = filasHTML.join('');
}

/**
 * Rellena la tabla de huéspedes a partir de los datos.
 * Cada fila muestra el número, nombre, documento, fecha de entrada
 * y el estado del huésped.
 */
function renderTablaHuespedes() {
    const cuerpo = document.getElementById('cuerpo-tabla-huespedes');

    // Convierte cada huésped de la lista en una fila HTML
    const filasHTML = huespedes.map(function (huesped) {
        // Determina la clase del badge según el estado del huésped
        const claseBadge = (huesped.estado === 'Activo')
            ? 'badge-disponible'
            : 'badge-pago-pendiente';

        return `
            <tr>
                <td>${huesped.numero}</td>
                <td>${huesped.nombre}</td>
                <td>${huesped.documento}</td>
                <td>${huesped.fechaEntrada}</td>
                <td><span class="badge ${claseBadge}">${huesped.estado}</span></td>
                <td>-</td>
            </tr>
        `;
    });

    // Inserta todas las filas generadas en el cuerpo de la tabla
    cuerpo.innerHTML = filasHTML.join('');
}

/**
 * Rellena la tabla de pagos por estado a partir de los datos.
 * Cada fila muestra la reserva, el huésped, el monto y un badge
 * de estado de pago (pagado o pendiente).
 */
function renderTablaPagos() {
    const cuerpo = document.getElementById('cuerpo-tabla-pagos');

    // Convierte cada pago de la lista en una fila HTML
    const filasHTML = pagos.map(function (pago) {
        // Determina la clase del badge según el estado de pago
        const claseBadge = (pago.estado === 'Pagado')
            ? 'badge-pago-pagado'
            : 'badge-pago-pendiente';

        return `
            <tr>
                <td>${pago.reserva}</td>
                <td>${pago.huesped}</td>
                <td>${pago.monto}</td>
                <td><span class="badge ${claseBadge}">${pago.estado}</span></td>
                <td>${pago.fecha}</td>
            </tr>
        `;
    });

    // Inserta todas las filas generadas en el cuerpo de la tabla
    cuerpo.innerHTML = filasHTML.join('');
}
