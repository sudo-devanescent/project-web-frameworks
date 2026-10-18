/**
 * @file dashboard.js
 * @description Vista Dashboard: KPIs, llegadas/salidas pendientes, saldos por cobrar.
 * @author HostOps Team
 * @version 1.0.0
 */

import { DB, HOY, buscarPor } from '../core/state.js';
import { TABLA } from '../core/constants.js';
import { reglas } from '../core/rules.js';
import { nombreHuesped, soles } from '../core/utils.js';
import { encabezado, kpi, tarjeta, tabla, badgeEstado } from '../ui/components.js';

/**
 * Renderiza la vista Dashboard
 * @returns {string} HTML
 */
export function dash() {
  const H = DB.habitacion;
  const oc = H.filter((h) => h.estado === 'ocupada').length;
  const mes = HOY.slice(0, 7);

  // Ingresos netos del mes
  const ing = DB.pago
    .filter((p) => p.fecha_pago.startsWith(mes))
    .reduce((s, p) => s + (p.tipo === 'reembolso' ? -p.monto : p.monto), 0);

  // Reservas activas (confirmadas o en estadía)
  const act = new Set(
    DB[TABLA.det]
      .filter((d) => ['confirmada', 'en_estadia'].includes(d.estado))
      .map((d) => d.id_reserva),
  ).size;

  // Habitaciones en mantenimiento
  const mant = H.filter((h) => h.estado === 'mantenimiento').length;

  // Reservas con saldo pendiente
  const pend = DB.reserva.filter(
    (r) =>
      reglas.saldo(r.id_reserva).sp > 0 &&
      DB[TABLA.det].some((d) => d.id_reserva === r.id_reserva && d.estado !== 'cancelada'),
  );

  // Helper para filas de llegadas/salidas
  const fila = (d) => {
    const r = buscarPor('reserva', 'id_reserva', d.id_reserva);
    return `<tr><td>#${r.id_reserva}</td><td>${nombreHuesped(
      buscarPor('huesped', 'id_huesped', r.id_huesped_titular),
    )}</td><td>${buscarPor('habitacion', 'id_habitacion', d.id_habitacion).numero}</td><td>${badgeEstado(
      d.estado,
    )}</td></tr>`;
  };

  const lleg = DB[TABLA.det].filter(
    (d) => d.estado === 'confirmada' && d.fecha_entrada_prevista <= HOY,
  );
  const sal = DB[TABLA.det].filter(
    (d) => d.estado === 'en_estadia' && d.fecha_salida_prevista <= HOY,
  );

  return (
    encabezado(
      'Panel de hoy',
      `<span class="text-body-secondary">${new Date().toLocaleDateString('es-PE', {
        dateStyle: 'full',
      })}</span>`,
    ) +
    `
  <div class="row g-3 mb-3">
    ${kpi('door-open', 'Ocupación actual', Math.round((oc / H.length) * 100) + '%', `${oc} de ${H.length} habitaciones`)}
    ${kpi('cash-stack', 'Ingresos netos del mes', soles(ing), 'Anticipos + saldos − reembolsos')}
    ${kpi('calendar2-check', 'Reservas activas', act, 'Confirmadas o en estadía')}
    ${kpi('exclamation-triangle', 'Alertas', mant + pend.length, `${mant} en mantenimiento · ${pend.length} con saldo`, true)}
  </div>
  <div class="row g-3">
    <div class="col-lg-6">${tarjeta(
      'Llegadas pendientes de check-in',
      tabla(['Reserva', 'Titular', 'Hab.', 'Estado'], lleg.map(fila).join('')),
      '<a href="#res" class="small">Ir a reservas</a>',
    )}</div>
    <div class="col-lg-6">${tarjeta(
      'Salidas pendientes de check-out',
      tabla(['Reserva', 'Titular', 'Hab.', 'Estado'], sal.map(fila).join('')),
      '<a href="#res" class="small">Ir a reservas</a>',
    )}</div>
    <div class="col-12">${tarjeta(
      'Saldos pendientes de cobro',
      tabla(
        ['Reserva', 'Titular', 'Total a pagar', 'Cobrado', 'Saldo'],
        pend
          .map((r) => {
            const s = reglas.saldo(r.id_reserva);
            return `<tr><td>#${r.id_reserva}</td><td>${nombreHuesped(
              buscarPor('huesped', 'id_huesped', r.id_huesped_titular),
            )}</td><td>${soles(s.tp)}</td><td>${soles(s.co)}</td><td class="fw-semibold">${soles(s.sp)}</td></tr>`;
          })
          .join(''),
      ),
      '<a href="#fin" class="small">Ir a finanzas</a>',
    )}</div>
  </div>`
  );
}