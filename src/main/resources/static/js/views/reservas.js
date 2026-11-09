/**
 * @file reservas.js
 * @description Vista Reservas y recepción: listado, check-in/out, cancelación,
 * creación multi-habitación y edición de reservas contra la API.
 * @author HostOps Team
 * @version 1.0.0
 */

import { DB, filtros, listados, HOY, buscarPor } from '../core/state.js';
import { TABLA, METODOS } from '../core/constants.js';
import { reglas } from '../core/rules.js';
import { esc, nombreHuesped, soles } from '../core/utils.js';
import { api, mensajeError } from '../core/api.js';
import { refrescarCatalogo, refrescarDetalles, cargarBloqueos } from '../core/catalogos.js';
import {
  encabezado,
  tabla,
  badgeEstado,
  campoForm,
  opcionesDe,
  opcionesLista,
  modal,
  toast,
  confirmar,
} from '../ui/components.js';
import { renderizarVista, tokenRender } from '../ui/router.js';

/**
 * Renderiza la vista Reservas y recepción
 * @returns {string} HTML
 */
export function res() {
  return (
    encabezado(
      'Reservas y recepción',
      `<input class="form-control w-auto" type="search" placeholder="Buscar huésped, hab. o N.º" aria-label="Buscar" value="${esc(filtros.res.q)}" oninput="filtros.res.q=this.value;pintarRes()">
  <select class="form-select w-auto" aria-label="Filtrar por estado" onchange="filtros.res.estado=this.value;pintarRes()"><option value="">Todos los estados</option>${opcionesLista(['confirmada', 'en_estadia', 'finalizada', 'cancelada'], filtros.res.estado)}</select>
 <button class="btn btn-primary" onclick="nuevaReserva()"><i class="bi bi-plus-lg"></i> Nueva reserva</button>`,
    ) + '<div class="card shadow-sm" id="l"></div>'
  );
}

/**
 * Descarga detalles de reserva y habitaciones, y pinta el listado
 */
listados.res = async () => {
  const token = tokenRender;
  await Promise.all([refrescarCatalogo('habitacion'), refrescarDetalles()]);
  if (token !== tokenRender) return;
  pintarRes();
};

/**
 * Pinta el listado de reservas (una fila por detalle) con filtros y búsqueda
 */
export function pintarRes() {
  const q = filtros.res.q.toLowerCase();
  const rows = DB[TABLA.det]
    .filter(
      (d) =>
        (!filtros.res.estado || d.estado == filtros.res.estado) &&
        (!q ||
          (d.titular + ' ' + d.numeroHabitacion + ' ' + d.idReserva).toLowerCase().includes(q)),
    )
    .sort((a, b) => b.fechaEntradaPrevista.localeCompare(a.fechaEntradaPrevista));

  document.querySelector('#l').innerHTML = tabla(
    [
      'Reserva',
      'Titular',
      'Hab.',
      'Entrada',
      'Salida',
      'Noches',
      'Canal',
      'Estado',
      'Saldo',
      'Acciones',
    ],
    rows
      .map(
        (d) => `<tr><td>#${d.idReserva}</td><td>${esc(d.titular)}</td><td>${esc(d.numeroHabitacion)}</td><td>${d.fechaEntradaPrevista}</td><td>${d.fechaSalidaPrevista}</td><td>${reglas.noches(d.fechaEntradaPrevista, d.fechaSalidaPrevista)}</td><td class="text-capitalize">${esc(d.canal)}</td><td>${badgeEstado(d.estado)}</td><td>${soles(d.saldoPendiente)}</td>
 <td class="text-nowrap">${d.estado == 'confirmada' ? `<button class="btn btn-sm btn-primary" onclick="checkin(${d.idDetalle})"><i class="bi bi-box-arrow-in-right"></i> Check-in</button> ` : d.estado == 'en_estadia' ? `<button class="btn btn-sm btn-primary" onclick="checkout(${d.idDetalle})"><i class="bi bi-box-arrow-right"></i> Check-out</button> ` : ''}${['confirmada', 'en_estadia'].includes(d.estado) ? `<button class="btn btn-sm btn-outline-secondary" onclick="editarRes(${d.idReserva}, ${d.idDetalle})" title="Modificar"><i class="bi bi-pencil"></i></button> ` : ''}${d.estado == 'confirmada' ? `<button class="btn btn-sm btn-outline-danger" onclick="cancelar(${d.idDetalle})" title="Cancelar"><i class="bi bi-x-lg"></i></button>` : ''}</td></tr>`,
      )
      .join(''),
  );
}

/**
 * Check-in: valida fecha de llegada y disponibilidad, luego llama a la API
 * @param {number} idDetalle - ID del detalle de reserva
 */
export async function checkin(idDetalle) {
  const d = DB[TABLA.det].find((x) => x.idDetalle == idDetalle);
  if (!d) return;
  const h = buscarPor('habitacion', 'idHabitacion', d.idHabitacion);
  if (!h) return toast('No se encontró la habitación de la reserva.', 'danger');
  const e = reglas.errorCheckin(d, h);
  if (e) return toast(e, 'warning');
  try {
    await api.post('/api/detalles-reserva/' + idDetalle + '/checkin');
    toast('Check-in registrado en la habitación ' + d.numeroHabitacion);
    await renderizarVista();
  } catch (err) {
    toast(mensajeError(err), 'danger');
  }
}

/**
 * Check-out: advierte si hay saldo pendiente, luego llama a la API
 * @param {number} idDetalle - ID del detalle de reserva
 */
export async function checkout(idDetalle) {
  const d = DB[TABLA.det].find((x) => x.idDetalle == idDetalle);
  if (!d) return;
  try {
    const s = await api.get('/api/reservas/' + d.idReserva + '/saldo');
    const ejecutar = async () => {
      await api.post('/api/detalles-reserva/' + idDetalle + '/checkout');
      toast('Check-out registrado. Habitación ' + d.numeroHabitacion + ' pasa a limpieza.');
      await renderizarVista();
    };
    if (s.saldoPendiente > 0) {
      confirmar(
        'Check-out con saldo pendiente',
        `La reserva #${d.idReserva} tiene un saldo pendiente de ${soles(s.saldoPendiente)}. ¿Registrar el check-out de todos modos?`,
        ejecutar,
        'Sí, registrar check-out',
      );
      return;
    }
    await ejecutar();
  } catch (e) {
    toast(mensajeError(e), 'danger');
  }
}

/**
 * Cancela una reserva: modal con motivo y penalidad
 * @param {number} idDetalle - ID del detalle de reserva
 */
export function cancelar(idDetalle) {
  const d = DB[TABLA.det].find((x) => x.idDetalle == idDetalle);
  if (!d) return;
  modal(
    'Cancelar reserva #' + d.idReserva,
    `<form class="row g-3 needs-validation" novalidate>${campoForm('Motivo', '<input name="m" class="form-control" required>', 'col-12')}${campoForm('Penalidad (S/)', '<input type="number" name="p" class="form-control" min="0" step="0.01" value="0" required>')}</form>`,
    async (fd) => {
      await api.post('/api/detalles-reserva/' + idDetalle + '/cancelaciones', {
        motivo: fd.get('m'),
        penalidad: +fd.get('p'),
      });
      toast('Cancelación registrada');
    },
    'Confirmar cancelación',
  );
}

/* =====================================================================
   Líneas de habitación (una por detalle_reserva):
   cada línea se valida de forma independiente
   ===================================================================== */

/**
 * Genera el HTML de una línea de habitación del formulario
 * @param {Object} [v] - Valores actuales { a, b, o: [ids] }
 * @param {Object} [c] - Configuración { ex, fijo, unica, minA, minB }
 * @returns {string} HTML de la línea
 */
export function lineaHTML(v = {}, c = {}) {
  return `<div class="card card-body mb-2 linea" data-ex="${c.ex ?? ''}" data-mina="${c.minA ?? ''}" data-minb="${c.minB ?? ''}"><div class="row g-2">
 ${campoForm('Habitación', `<select class="form-select l-h" required ${c.fijo ? 'disabled' : ''}></select>`, 'col-md-4')}
 ${campoForm('Entrada', `<input type="date" class="form-control l-a" value="${v.a || ''}" ${c.minA ? `min="${c.minA}"` : ''} required ${c.fijo ? 'disabled' : ''}>`, 'col-md-4 col-6')}
 ${campoForm('Salida', `<input type="date" class="form-control l-b" value="${v.b || ''}" min="${c.minB || c.minA || ''}" required>`, 'col-md-4 col-6')}
 ${campoForm('Ocupantes (máximo la capacidad de la habitación)', `<select multiple size="3" class="form-select l-o" required>${opcionesDe(DB.huesped, 'idHuesped', nombreHuesped).replace(/value="(\d+)"/g, (m, i) => `value="${i}" ${(v.o || []).includes(+i) ? 'selected' : ''}`)}</select>`, c.unica ? 'col-12' : 'col-md-10')}
 ${c.unica ? '' : '<div class="col-md-2 d-flex align-items-end"><button type="button" class="btn btn-outline-danger w-100" onclick="quitarLinea(this)">Quitar</button></div>'}
 <div class="col-12 small text-danger lerr" role="alert"></div></div></div>`;
}

/**
 * Lee todas las líneas del formulario presentes en el modal
 * @returns {Object[]} [{ n, h, a, b, o: [ids], op: { ex, minA, minB } }]
 */
export function leerLineas() {
  return [...document.querySelectorAll('#mb .linea')].map((n) => ({
    n,
    h: n.querySelector('.l-h').value,
    a: n.querySelector('.l-a').value,
    b: n.querySelector('.l-b').value,
    o: [...n.querySelector('.l-o').selectedOptions].map((x) => +x.value),
    op: { ex: n.dataset.ex || null, minA: n.dataset.mina || null, minB: n.dataset.minb || null },
  }));
}

/**
 * Refresca cada línea: deshabilita habitaciones no disponibles
 * y muestra el error propio de esa línea
 */
export function refrescarLineas() {
  const R = leerLineas();
  R.forEach((l) => {
    const s = l.n.querySelector('.l-h');
    const ok = l.a && l.b && l.b > l.a;
    s.innerHTML =
      '<option value="">Selecciona…</option>' +
      DB.habitacion
        .map((h) => {
          const mal =
            ok &&
            (reglas.conflicto(h.idHabitacion, l.a, l.b, l.op.ex) ||
              R.some(
                (o) =>
                  o !== l &&
                  o.h == h.idHabitacion &&
                  o.a &&
                  o.b &&
                  reglas.solapa(o.a, o.b, l.a, l.b),
              ));
          return `<option value="${h.idHabitacion}" ${mal ? 'disabled' : ''}>${h.numero} · ${h.tipo} · cap. ${h.capacidad} · ${soles(h.tarifa)}${mal ? ' — no disponible' : ''}</option>`;
        })
        .join('');
    s.value = l.h;
    const e = reglas.validarLinea(
      l,
      R.filter((o) => o !== l),
      l.op,
    );
    s.setCustomValidity(e);
    l.n.querySelector('.lerr').textContent = e;
  });
}

/**
 * Agrega una línea vacía al formulario de reserva
 */
export function agregarLinea() {
  document.querySelector('#lineas').insertAdjacentHTML('beforeend', lineaHTML({}, { minA: HOY }));
  refrescarLineas();
}

/**
 * Quita una línea del formulario (mínimo una obligatoria)
 * @param {HTMLButtonElement} b - Botón "Quitar"
 */
export function quitarLinea(b) {
  if (document.querySelectorAll('#mb .linea').length < 2)
    return toast('La reserva necesita al menos una habitación.', 'warning');
  b.closest('.linea').remove();
  refrescarLineas();
}

/**
 * Descarga los catálogos necesarios para los formularios de reserva
 * (habitaciones, huéspedes, canales) y datos de conflictos
 */
async function prepararFormularioReserva() {
  await Promise.all([
    refrescarCatalogo('habitacion'),
    refrescarCatalogo('huesped'),
    refrescarCatalogo('canal'),
  ]);
  await Promise.all([refrescarDetalles(), cargarBloqueos()]);
}

/**
 * Modal de nueva reserva con una o varias habitaciones
 */
export async function nuevaReserva() {
  try {
    await prepararFormularioReserva();
  } catch (e) {
    return toast(mensajeError(e), 'danger');
  }
  modal(
    'Nueva reserva',
    `<form class="row g-3 needs-validation" novalidate>
 ${campoForm('Huésped titular', `<select name="t" class="form-select" required><option value="">Selecciona…</option>${opcionesDe(DB.huesped, 'idHuesped', nombreHuesped)}</select>`)}
 ${campoForm('Canal', `<select name="c" class="form-select" required>${opcionesDe(DB.canal, 'idCanal', (c) => c.nombre)}</select>`)}
 <div class="col-12"><div class="d-flex justify-content-between align-items-center mb-2"><h3 class="h6 mb-0">Habitaciones de la reserva</h3><button type="button" class="btn btn-sm btn-outline-primary" onclick="agregarLinea()"><i class="bi bi-plus-lg"></i> Agregar habitación</button></div><div id="lineas">${lineaHTML({}, { minA: HOY })}</div></div>
 ${campoForm('Anticipo (S/, opcional)', '<input type="number" name="m" class="form-control" min="0" step="0.01" value="0">')}
 ${campoForm('Método del anticipo', `<select name="mp" class="form-select">${opcionesLista(METODOS)}</select>`)}</form>`,
    async (fd) => {
      const R = leerLineas();
      const t = +fd.get('t');
      for (const l of R) {
        const e = reglas.validarLinea(
          l,
          R.filter((o) => o !== l),
          l.op,
        );
        if (e) return e;
      }
      if (!R.some((l) => l.o.includes(t)))
        return 'El huésped titular debe alojarse en al menos una de las habitaciones.';
      const tot = R.reduce(
        (s, l) =>
          s + reglas.noches(l.a, l.b) * buscarPor('habitacion', 'idHabitacion', l.h).tarifa,
        0,
      );
      const m = +fd.get('m');
      if (m > tot) return `El anticipo no puede superar el total de la reserva (${soles(tot)}).`;

      await api.post('/api/reservas', {
        idHuespedTitular: t,
        idCanal: +fd.get('c'),
        detalles: R.map((l) => ({
          idHabitacion: +l.h,
          fechaEntradaPrevista: l.a,
          fechaSalidaPrevista: l.b,
          huespedes: l.o,
        })),
        pagoAnticipo: m > 0 ? { metodo: fd.get('mp'), monto: m } : null,
      });
      toast(`Reserva creada con ${R.length} habitación(es) · total ${soles(tot)}`);
    },
    'Crear reserva',
  );
  document.querySelector('#mb').oninput = document.querySelector('#mb').onchange =
    refrescarLineas;
  refrescarLineas();
}

/**
 * Modal de modificación de una habitación reservada.
 * Confirmada: cambia habitación, fechas y ocupantes.
 * En estadía: solo extiende/acorta la salida y cambia ocupantes.
 * Finalizadas y canceladas no se editan.
 * @param {number} idReserva - ID de la reserva
 * @param {number} idDetalle - ID del detalle de reserva
 */
export async function editarRes(idReserva, idDetalle) {
  const d = DB[TABLA.det].find((x) => x.idDetalle == idDetalle);
  if (!d) return;
  const act = d.estado == 'en_estadia';
  if (!['confirmada', 'en_estadia'].includes(d.estado))
    return toast('Solo se pueden modificar reservas confirmadas o en estadía.', 'warning');
  try {
    await prepararFormularioReserva();
    const det = await api.get('/api/detalles-reserva/' + idDetalle);
    const o = det.huespedes.map((x) => x.idHuesped);

    modal(
      'Modificar reserva #' + idReserva,
      `<form class="row g-3 needs-validation" novalidate>
 ${campoForm('Huésped titular', `<select name="t" class="form-select" required>${opcionesDe(DB.huesped, 'idHuesped', nombreHuesped, d.idHuespedTitular)}</select>`)}
 ${campoForm('Canal', `<select name="c" class="form-select" required>${opcionesDe(DB.canal, 'idCanal', (c) => c.nombre, d.idCanal)}</select>`)}
 <div class="col-12"><h3 class="h6">Habitación y estadía</h3><div id="lineas">${lineaHTML({ a: d.fechaEntradaPrevista, b: d.fechaSalidaPrevista, o }, { ex: idDetalle, fijo: act, unica: true, minA: act ? '' : HOY, minB: act ? HOY : '' })}</div>
 <p class="small text-body-secondary mb-0">${act ? 'Estadía en curso: puedes extender o acortar la salida (no antes de hoy) y cambiar ocupantes.' : 'Si cambias de habitación, la tarifa por noche se actualiza a la tarifa vigente de la nueva habitación.'} La tarifa congelada se conserva si no cambias de habitación.</p></div></form>`,
      async (fd) => {
        const l = leerLineas()[0];
        const e = reglas.validarLinea(l, [], l.op);
        if (e) return e;
        if (act) {
          l.h = d.idHabitacion;
          l.a = d.fechaEntradaPrevista;
        }
        await api.put('/api/reservas/' + idReserva + '/detalles/' + idDetalle, {
          idHuespedTitular: +fd.get('t'),
          idCanal: +fd.get('c'),
          detalle: {
            idHabitacion: +l.h,
            fechaEntradaPrevista: l.a,
            fechaSalidaPrevista: l.b,
            huespedes: l.o,
          },
        });
        toast('Reserva #' + idReserva + ' modificada');
      },
      'Guardar cambios',
    );
    document.querySelector('#mb').oninput = document.querySelector('#mb').onchange =
      refrescarLineas;
    refrescarLineas();
  } catch (e) {
    toast(mensajeError(e), 'danger');
  }
}
