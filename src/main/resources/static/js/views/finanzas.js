/**
 * @file finanzas.js
 * @description Vista Finanzas: estado de cuenta por reserva, movimientos
 * recientes y registro de pagos contra la API.
 * @author HostOps Team
 * @version 1.0.0
 */

import { listados } from '../core/state.js';
import { soles, esc } from '../core/utils.js';
import { METODOS } from '../core/constants.js';
import { api, mensajeError } from '../core/api.js';
import { esAdmin } from '../auth/permissions.js';
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
import { tokenRender } from '../ui/router.js';

/**
 * Renderiza el shell de la vista Finanzas (el contenido lo llena el listado)
 * @returns {string} HTML
 */
export function fin() {
  return encabezado('Finanzas y pagos') + '<div id="l"></div>';
}

/**
 * Descarga el resumen financiero y pinta KPIs, cuentas y movimientos
 */
listados.fin = async () => {
  const token = tokenRender;
  const f = await api.get('/api/finanzas/resumen');
  if (token !== tokenRender) return;

  const movimientos = [...(f.movimientos || [])]
    .sort((a, b) => String(b.fechaPago).localeCompare(String(a.fechaPago)))
    .slice(0, 12);

  document.querySelector('#l').innerHTML = `
  <div class="row g-3 mb-3">${kpi('cash-stack', 'Cobrado neto del mes', soles(f.cobradoNetoMes), 'Anticipos + saldos − reembolsos')}${kpi('hourglass-split', 'Saldos por cobrar', soles(f.saldosPorCobrar), 'Suma de saldo_pendiente')}${kpi('arrow-counterclockwise', 'Reembolsos por devolver', soles(f.reembolsosPorDevolver), 'Suma de reembolso_pendiente', true)}</div>
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
     f.cuentas
       .map(
         (c) =>
           `<tr><td>#${c.idReserva}</td><td>${esc(c.titular)}</td><td>${soles(c.totalAlojamiento)}</td><td>${soles(c.totalPenalidades)}</td><td>${soles(c.totalAPagar)}</td><td>${soles(c.cobradoNeto)}</td><td class="${c.saldoPendiente > 0 ? 'fw-bold' : ''}">${soles(c.saldoPendiente)}</td><td>${soles(c.reembolsoPendiente)}</td><td><button class="btn btn-sm btn-outline-primary" onclick="pagar(${c.idReserva})">Registrar pago</button></td></tr>`,
       )
       .join(''),
   ),
 )}</div>
 <div class="col-12">${tarjeta(
   'Movimientos recientes',
   tabla(
     ['Fecha', 'Reserva', 'Tipo', 'Método', 'Monto'],
     movimientos
       .map(
         (p) =>
           `<tr><td>${String(p.fechaPago).slice(0, 10)}</td><td>#${p.idReserva}</td><td>${badgeEstado(p.tipo)}</td><td class="text-capitalize">${esc(p.metodo).replace('_', ' ')}</td><td>${p.tipo == 'reembolso' ? '−' : ''}${soles(p.monto)}</td></tr>`,
       )
       .join(''),
   ),
 )}</div></div>`;
};

/**
 * Modal de registro de pago para una reserva
 * @param {number} id - ID de la reserva
 */
export async function pagar(id) {
  try {
    const s = await api.get('/api/reservas/' + id + '/saldo');
    const tipos = esAdmin() ? ['anticipo', 'saldo', 'reembolso'] : ['anticipo', 'saldo'];
    modal(
      'Registrar pago · reserva #' + id,
      `<p class="small text-body-secondary">Total a pagar ${soles(s.totalAPagar)} · cobrado ${soles(s.cobradoNeto)} · saldo ${soles(s.saldoPendiente)} · reembolso pendiente ${soles(s.reembolsoPendiente)}</p><form class="row g-3 needs-validation" novalidate>
 ${campoForm('Tipo', `<select name="t" class="form-select" required>${opcionesLista(tipos)}</select>`, 'col-md-4')}
 ${campoForm('Método', `<select name="mt" class="form-select" required>${opcionesLista(METODOS)}</select>`, 'col-md-4')}
 ${campoForm('Monto (S/)', `<input type="number" name="m" class="form-control" min="0.01" step="0.01" value="${s.saldoPendiente > 0 ? Number(s.saldoPendiente).toFixed(2) : ''}" required>`, 'col-md-4')}</form>`,
      async (fd) => {
        const t = fd.get('t');
        const m = +fd.get('m');
        if (t == 'reembolso' && m > s.reembolsoPendiente + 0.005)
          return `El reembolso no puede superar lo que el hotel debe devolver (${soles(s.reembolsoPendiente)}).`;
        if (t != 'reembolso' && m > s.saldoPendiente + 0.005)
          return `El monto no puede superar el saldo pendiente (${soles(s.saldoPendiente)}).`;
        await api.post('/api/reservas/' + id + '/pagos', {
          tipo: t,
          metodo: fd.get('mt'),
          monto: m,
        });
        toast('Pago registrado');
      },
      'Registrar',
    );
  } catch (e) {
    toast(mensajeError(e), 'danger');
  }
}
