/**
 * @file finanzas.js
 * @description Vista Finanzas: estado de cuenta por reserva, movimientos
 * recientes y registro de pagos (anticipo, saldo, reembolso).
 * @author HostOps Team
 * @version 1.0.0
 */

import { DB, HOY, buscarPor, siguienteId } from '../core/state.js';
import { METODOS } from '../core/constants.js';
import { reglas } from '../core/rules.js';
import { esc, nombreHuesped, soles, ahora } from '../core/utils.js';
import {
  encabezado,
  kpi,
  tarjeta,
  tabla,
  badgeEstado,
  campoForm,
  opcionesLista,
  modal,
  toast,
} from '../ui/components.js';

/**
 * Renderiza la vista Finanzas y pagos
 * @returns {string} HTML
 */
export function fin() {
  const mes = HOY.slice(0, 7);
  const ss = DB.reserva.map((r) => reglas.saldo(r.id_reserva));
  const ing = DB.pago
    .filter((p) => p.fecha_pago.startsWith(mes))
    .reduce((s, p) => s + (p.tipo == 'reembolso' ? -p.monto : p.monto), 0);

  return (
    encabezado('Finanzas y pagos') +
    `<div class="row g-3 mb-3">${kpi('cash-stack', 'Cobrado neto del mes', soles(ing), 'Anticipos + saldos − reembolsos')}${kpi('hourglass-split', 'Saldos por cobrar', soles(ss.reduce((s, x) => s + x.sp, 0)), 'Suma de saldo_pendiente')}${kpi('arrow-counterclockwise', 'Reembolsos por devolver', soles(ss.reduce((s, x) => s + x.rp, 0)), 'Suma de reembolso_pendiente', 1)}</div>
 <div class="row g-3"><div class="col-12">${tarjeta(
   'Estado de cuenta por reserva',
   tabla(
     [
       'Reserva',
       'Titular',
       'Alojamiento',
       'Penalidades',
       'Total a pagar',
       'Cobrado neto',
       'Saldo pendiente',
       'Reembolso pendiente',
       '',
     ],
     DB.reserva
       .map((r) => {
         const s = reglas.saldo(r.id_reserva);
         return `<tr><td>#${r.id_reserva}</td><td>${esc(nombreHuesped(buscarPor('huesped', 'id_huesped', r.id_huesped_titular)))}</td><td>${soles(s.al)}</td><td>${soles(s.pe)}</td><td>${soles(s.tp)}</td><td>${soles(s.co)}</td><td class="${s.sp > 0 ? 'fw-bold' : ''}">${soles(s.sp)}</td><td>${soles(s.rp)}</td><td><button class="btn btn-sm btn-outline-primary" onclick="pagar(${r.id_reserva})">Registrar pago</button></td></tr>`;
       })
       .join(''),
   ),
 )}</div>
 <div class="col-12">${tarjeta(
   'Movimientos recientes',
   tabla(
     ['Fecha', 'Reserva', 'Tipo', 'Método', 'Monto'],
     [...DB.pago]
       .sort((a, b) => b.fecha_pago.localeCompare(a.fecha_pago))
       .slice(0, 12)
       .map(
         (p) =>
           `<tr><td>${p.fecha_pago.slice(0, 10)}</td><td>#${p.id_reserva}</td><td>${badgeEstado(p.tipo)}</td><td class="text-capitalize">${p.metodo.replace('_', ' ')}</td><td>${p.tipo == 'reembolso' ? '−' : ''}${soles(p.monto)}</td></tr>`,
       )
       .join(''),
   ),
 )}</div></div>`
  );
}

/**
 * Modal de registro de pago para una reserva
 * @param {number} id - ID de la reserva
 */
export function pagar(id) {
  const s = reglas.saldo(id);
  modal(
    'Registrar pago · reserva #' + id,
    `<p class="small text-body-secondary">Total a pagar ${soles(s.tp)} · cobrado ${soles(s.co)} · saldo ${soles(s.sp)} · reembolso pendiente ${soles(s.rp)}</p><form class="row g-3 needs-validation" novalidate>
 ${campoForm('Tipo', `<select name="t" class="form-select" required>${opcionesLista(['anticipo', 'saldo', 'reembolso'])}</select>`, 'col-md-4')}
 ${campoForm('Método', `<select name="mt" class="form-select" required>${opcionesLista(METODOS)}</select>`, 'col-md-4')}
 ${campoForm('Monto (S/)', `<input type="number" name="m" class="form-control" min="0.01" step="0.01" value="${s.sp > 0 ? s.sp.toFixed(2) : ''}" required>`, 'col-md-4')}</form>`,
    (fd) => {
      const t = fd.get('t');
      const m = +fd.get('m');
      if (t == 'reembolso' && m > s.rp + 0.005)
        return `El reembolso no puede superar lo que el hotel debe devolver (${soles(s.rp)}).`;
      if (t != 'reembolso' && m > s.sp + 0.005)
        return `El monto no puede superar el saldo pendiente (${soles(s.sp)}).`;
      DB.pago.push({
        id_pago: siguienteId('pago', 'id_pago'),
        id_reserva: id,
        tipo: t,
        metodo: fd.get('mt'),
        fecha_pago: ahora(),
        monto: m,
      });
      toast('Pago registrado');
    },
    'Registrar',
  );
}
