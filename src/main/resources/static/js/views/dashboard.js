/**
 * @file dashboard.js
 * @description Vista Dashboard: KPIs, llegadas/salidas pendientes y saldos
 * por cobrar, servidos por GET /api/dashboard/resumen.
 * @author HostOps Team
 * @version 1.0.0
 */

import { listados } from '../core/state.js';
import { esc, soles } from '../core/utils.js';
import { api } from '../core/api.js';
import { encabezado, kpi, tarjeta, tabla, badgeEstado } from '../ui/components.js';
import { tokenRender } from '../ui/router.js';

/**
 * Renderiza el shell de la vista Dashboard (el contenido lo llena el listado)
 * @returns {string} HTML
 */
export function dash() {
  return (
    encabezado(
      'Panel de hoy',
      `<span class="text-body-secondary">${new Date().toLocaleDateString('es-PE', {
        dateStyle: 'full',
      })}</span>`,
    ) + '<div id="l"></div>'
  );
}

/**
 * Descarga el resumen del dashboard y pinta KPIs y paneles
 */
listados.dash = async () => {
  const token = tokenRender;
  const d = await api.get('/api/dashboard/resumen');
  if (token !== tokenRender) return;

  const oc = d.habitacionesOcupadas;
  const total = d.totalHabitaciones;
  const ocup = total ? Math.round((oc / total) * 100) + '%' : '—';
  const alertas = d.habitacionesMantenimiento + d.cuentasPendientes.length;

  const filaEstadia = (x) =>
    `<tr><td>#${x.idReserva}</td><td>${esc(x.titular)}</td><td>${esc(x.numeroHabitacion)}</td><td>${badgeEstado(x.estado)}</td></tr>`;

  document.querySelector('#l').innerHTML = `
  <div class="row g-3 mb-3">
    ${kpi('door-open', 'Ocupación actual', ocup, `${oc} de ${total} habitaciones`)}
    ${kpi('cash-stack', 'Ingresos netos del mes', soles(d.ingresosNetosMes), 'Anticipos + saldos − reembolsos')}
    ${kpi('calendar2-check', 'Reservas activas', d.reservasActivas, 'Confirmadas o en estadía')}
    ${kpi('exclamation-triangle', 'Alertas', alertas, `${d.habitacionesMantenimiento} en mantenimiento · ${d.cuentasPendientes.length} con saldo`, true)}
  </div>
  <div class="row g-3">
    <div class="col-lg-6">${tarjeta(
      'Llegadas pendientes de check-in',
      tabla(['Reserva', 'Titular', 'Hab.', 'Estado'], d.llegadasPendientes.map(filaEstadia).join('')),
      '<a href="#res" class="small">Ir a reservas</a>',
    )}</div>
    <div class="col-lg-6">${tarjeta(
      'Salidas pendientes de check-out',
      tabla(['Reserva', 'Titular', 'Hab.', 'Estado'], d.salidasPendientes.map(filaEstadia).join('')),
      '<a href="#res" class="small">Ir a reservas</a>',
    )}</div>
    <div class="col-12">${tarjeta(
      'Saldos pendientes de cobro',
      tabla(
        ['Reserva', 'Titular', 'Total a pagar', 'Cobrado', 'Saldo'],
        d.cuentasPendientes
          .map(
            (c) =>
              `<tr><td>#${c.idReserva}</td><td>${esc(c.titular)}</td><td>${soles(c.totalAPagar)}</td><td>${soles(c.cobradoNeto)}</td><td class="fw-semibold">${soles(c.saldoPendiente)}</td></tr>`,
          )
          .join(''),
      ),
      '<a href="#fin" class="small">Ir a finanzas</a>',
    )}</div>
  </div>`;
};
