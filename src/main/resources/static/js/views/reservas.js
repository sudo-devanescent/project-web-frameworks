/**
 * @file reservas.js
 * @description Vista Reservas y recepción: listado, check-in/out, cancelación,
 * creación multi-habitación y edición de reservas.
 * @author HostOps Team
 * @version 1.0.0
 */

import { DB, filtros, listados, HOY, buscarPor, siguienteId } from '../core/state.js';
import { TABLA, METODOS } from '../core/constants.js';
import { reglas } from '../core/rules.js';
import { esc, nombreHuesped, soles, ahora } from '../core/utils.js';
import { getUid } from '../auth/permissions.js';
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
import { renderizarVista } from '../ui/router.js';

/**
 * Renderiza la vista Reservas y recepción
 * @returns {string} HTML
 */
export function res() {
  return (
    encabezado(
      'Reservas y recepción',
      `<input class="form-control w-auto" type="search" placeholder="Buscar huésped, hab. o N.º" aria-label="Buscar" value="${esc(filtros.res.q)}" oninput="filtros.res.q=this.value;listados.res()">
  <select class="form-select w-auto" aria-label="Filtrar por estado" onchange="filtros.res.estado=this.value;listados.res()"><option value="">Todos los estados</option>${opcionesLista(['confirmada', 'en_estadia', 'finalizada', 'cancelada'], filtros.res.estado)}</select>
 <button class="btn btn-primary" onclick="nuevaReserva()"><i class="bi bi-plus-lg"></i> Nueva reserva</button>`,
    ) + '<div class="card shadow-sm" id="l"></div>'
  );
}

/**
 * Listado de reservas (una fila por detalle) con filtros y búsqueda
 */
listados.res = () => {
  const q = filtros.res.q.toLowerCase();
  const rows = DB[TABLA.det]
    .map((d) => {
      const r = buscarPor('reserva', 'id_reserva', d.id_reserva);
      const h = buscarPor('habitacion', 'id_habitacion', d.id_habitacion);
      const t = nombreHuesped(buscarPor('huesped', 'id_huesped', r.id_huesped_titular));
      return { d, r, h, t };
    })
    .filter(
      (x) =>
        (!filtros.res.estado || x.d.estado == filtros.res.estado) &&
        (!q || (x.t + ' ' + x.h.numero + ' ' + x.r.id_reserva).toLowerCase().includes(q)),
    )
    .sort((a, b) => b.d.fecha_entrada_prevista.localeCompare(a.d.fecha_entrada_prevista));

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
        ({ d, r, h, t }) => `<tr><td>#${r.id_reserva}</td><td>${esc(t)}</td><td>${h.numero}</td><td>${d.fecha_entrada_prevista}</td><td>${d.fecha_salida_prevista}</td><td>${reglas.noches(d.fecha_entrada_prevista, d.fecha_salida_prevista)}</td><td class="text-capitalize">${esc(buscarPor('canal', 'id_canal', r.id_canal).nombre)}</td><td>${badgeEstado(d.estado)}</td><td>${soles(reglas.saldo(r.id_reserva).sp)}</td>
 <td class="text-nowrap">${d.estado == 'confirmada' ? `<button class="btn btn-sm btn-primary" onclick="checkin(${d.id_detalle})"><i class="bi bi-box-arrow-in-right"></i> Check-in</button> ` : d.estado == 'en_estadia' ? `<button class="btn btn-sm btn-primary" onclick="checkout(${d.id_detalle})"><i class="bi bi-box-arrow-right"></i> Check-out</button> ` : ''}${['confirmada', 'en_estadia'].includes(d.estado) ? `<button class="btn btn-sm btn-outline-secondary" onclick="editarRes(${d.id_detalle})" title="Modificar"><i class="bi bi-pencil"></i></button> ` : ''}${d.estado == 'confirmada' ? `<button class="btn btn-sm btn-outline-danger" onclick="cancelar(${d.id_detalle})" title="Cancelar"><i class="bi bi-x-lg"></i></button>` : ''}</td></tr>`,
      )
      .join(''),
  );
};

/**
 * Check-in: valida fecha de llegada y disponibilidad, actualiza estados
 * @param {number} idDetalle - ID del detalle de reserva
 */
export function checkin(idDetalle) {
  const d = buscarPor(TABLA.det, 'id_detalle', idDetalle);
  const h = buscarPor('habitacion', 'id_habitacion', d.id_habitacion);
  const e = reglas.errorCheckin(d, h);
  if (e) return toast(e, 'warning');
  d.estado = 'en_estadia';
  d.checkin_real = ahora();
  h.estado = 'ocupada';
  toast('Check-in registrado en la habitación ' + h.numero);
  renderizarVista();
}

/**
 * Check-out: advierte si hay saldo pendiente, actualiza estados
 * @param {number} idDetalle - ID del detalle de reserva
 */
export function checkout(idDetalle) {
  const d = buscarPor(TABLA.det, 'id_detalle', idDetalle);
  const h = buscarPor('habitacion', 'id_habitacion', d.id_habitacion);
  const s = reglas.saldo(d.id_reserva);
  const ejecutar = () => {
    d.estado = 'finalizada';
    d.checkout_real = ahora();
    h.estado = 'limpieza';
    toast('Check-out registrado. Habitación ' + h.numero + ' pasa a limpieza.');
    renderizarVista();
  };
  if (s.sp > 0) {
    confirmar(
      'Check-out con saldo pendiente',
      `La reserva #${d.id_reserva} tiene un saldo pendiente de ${soles(s.sp)}. ¿Registrar el check-out de todos modos?`,
      ejecutar,
      'Sí, registrar check-out',
    );
    return;
  }
  ejecutar();
}

/**
 * Cancela una reserva: modal con motivo y penalidad
 * @param {number} idDetalle - ID del detalle de reserva
 */
export function cancelar(idDetalle) {
  const d = buscarPor(TABLA.det, 'id_detalle', idDetalle);
  modal(
    'Cancelar reserva #' + d.id_reserva,
    `<form class="row g-3 needs-validation" novalidate>${campoForm('Motivo', '<input name="m" class="form-control" required>', 'col-12')}${campoForm('Penalidad (S/)', '<input type="number" name="p" class="form-control" min="0" step="0.01" value="0" required>')}</form>`,
    (fd) => {
      DB.cancelacion.push({
        id_cancelacion: siguienteId('cancelacion', 'id_cancelacion'),
        id_detalle: idDetalle,
        fecha_cancelacion: ahora(),
        motivo: fd.get('m'),
        penalidad: +fd.get('p'),
      });
      d.estado = 'cancelada';
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
 ${campoForm('Ocupantes (máximo la capacidad de la habitación)', `<select multiple size="3" class="form-select l-o" required>${opcionesDe(DB.huesped, 'id_huesped', nombreHuesped).replace(/value="(\d+)"/g, (m, i) => `value="${i}" ${(v.o || []).includes(+i) ? 'selected' : ''}`)}</select>`, c.unica ? 'col-12' : 'col-md-10')}
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
            (reglas.conflicto(h.id_habitacion, l.a, l.b, l.op.ex) ||
              R.some(
                (o) =>
                  o !== l &&
                  o.h == h.id_habitacion &&
                  o.a &&
                  o.b &&
                  reglas.solapa(o.a, o.b, l.a, l.b),
              ));
          return `<option value="${h.id_habitacion}" ${mal ? 'disabled' : ''}>${h.numero} · ${h.tipo} · cap. ${h.capacidad} · ${soles(h.tarifa)}${mal ? ' — no disponible' : ''}</option>`;
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
 * Modal de nueva reserva con una o varias habitaciones
 */
export function nuevaReserva() {
  modal(
    'Nueva reserva',
    `<form class="row g-3 needs-validation" novalidate>
 ${campoForm('Huésped titular', `<select name="t" class="form-select" required><option value="">Selecciona…</option>${opcionesDe(DB.huesped, 'id_huesped', nombreHuesped)}</select>`)}
 ${campoForm('Canal', `<select name="c" class="form-select" required>${opcionesDe(DB.canal, 'id_canal', (c) => c.nombre)}</select>`)}
 <div class="col-12"><div class="d-flex justify-content-between align-items-center mb-2"><h3 class="h6 mb-0">Habitaciones de la reserva</h3><button type="button" class="btn btn-sm btn-outline-primary" onclick="agregarLinea()"><i class="bi bi-plus-lg"></i> Agregar habitación</button></div><div id="lineas">${lineaHTML({}, { minA: HOY })}</div></div>
 ${campoForm('Anticipo (S/, opcional)', '<input type="number" name="m" class="form-control" min="0" step="0.01" value="0">')}
 ${campoForm('Método del anticipo', `<select name="mp" class="form-select">${opcionesLista(METODOS)}</select>`)}</form>`,
    (fd) => {
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
          s + reglas.noches(l.a, l.b) * buscarPor('habitacion', 'id_habitacion', l.h).tarifa,
        0,
      );
      const m = +fd.get('m');
      if (m > tot) return `El anticipo no puede superar el total de la reserva (${soles(tot)}).`;

      const r = {
        id_reserva: siguienteId('reserva', 'id_reserva'),
        id_huesped_titular: t,
        id_canal: +fd.get('c'),
        id_usuario_creador: getUid(),
        id_usuario_modificador: null,
        fecha_creacion: ahora(),
        fecha_modificacion: null,
      };
      DB.reserva.push(r);
      R.forEach((l) => {
        const h = buscarPor('habitacion', 'id_habitacion', l.h);
        const d = {
          id_detalle: siguienteId(TABLA.det, 'id_detalle'),
          id_reserva: r.id_reserva,
          id_habitacion: h.id_habitacion,
          fecha_entrada_prevista: l.a,
          fecha_salida_prevista: l.b,
          checkin_real: null,
          checkout_real: null,
          tarifa_noche: h.tarifa,
          estado: 'confirmada',
        };
        DB[TABLA.det].push(d);
        l.o.forEach((i) => DB[TABLA.dh].push({ id_detalle: d.id_detalle, id_huesped: i }));
      });
      if (m > 0)
        DB.pago.push({
          id_pago: siguienteId('pago', 'id_pago'),
          id_reserva: r.id_reserva,
          tipo: 'anticipo',
          metodo: fd.get('mp'),
          fecha_pago: ahora(),
          monto: m,
        });
      toast(`Reserva #${r.id_reserva} creada con ${R.length} habitación(es) · total ${soles(tot)}`);
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
 * @param {number} idDetalle - ID del detalle de reserva
 */
export function editarRes(idDetalle) {
  const d = buscarPor(TABLA.det, 'id_detalle', idDetalle);
  const r = buscarPor('reserva', 'id_reserva', d.id_reserva);
  const act = d.estado == 'en_estadia';
  if (!['confirmada', 'en_estadia'].includes(d.estado))
    return toast('Solo se pueden modificar reservas confirmadas o en estadía.', 'warning');
  const o = DB[TABLA.dh].filter((x) => x.id_detalle == idDetalle).map((x) => x.id_huesped);

  modal(
    'Modificar reserva #' + r.id_reserva,
    `<form class="row g-3 needs-validation" novalidate>
 ${campoForm('Huésped titular', `<select name="t" class="form-select" required>${opcionesDe(DB.huesped, 'id_huesped', nombreHuesped, r.id_huesped_titular)}</select>`)}
 ${campoForm('Canal', `<select name="c" class="form-select" required>${opcionesDe(DB.canal, 'id_canal', (c) => c.nombre, r.id_canal)}</select>`)}
 <div class="col-12"><h3 class="h6">Habitación y estadía</h3><div id="lineas">${lineaHTML({ a: d.fecha_entrada_prevista, b: d.fecha_salida_prevista, o }, { ex: idDetalle, fijo: act, unica: true, minA: act ? '' : HOY, minB: act ? HOY : '' })}</div>
 <p class="small text-body-secondary mb-0">${act ? 'Estadía en curso: puedes extender o acortar la salida (no antes de hoy) y cambiar ocupantes.' : 'Si cambias de habitación, la tarifa por noche se actualiza a la tarifa vigente de la nueva habitación.'} La tarifa congelada se conserva si no cambias de habitación.</p></div></form>`,
    (fd) => {
      const l = leerLineas()[0];
      const e = reglas.validarLinea(l, [], l.op);
      if (e) return e;
      if (act) {
        l.h = d.id_habitacion;
        l.a = d.fecha_entrada_prevista;
      }
      if (!act && +l.h != d.id_habitacion)
        d.tarifa_noche = buscarPor('habitacion', 'id_habitacion', l.h).tarifa;
      d.id_habitacion = +l.h;
      d.fecha_entrada_prevista = l.a;
      d.fecha_salida_prevista = l.b;
      DB[TABLA.dh] = DB[TABLA.dh].filter((x) => x.id_detalle != idDetalle);
      l.o.forEach((i) => DB[TABLA.dh].push({ id_detalle: idDetalle, id_huesped: i }));
      r.id_huesped_titular = +fd.get('t');
      r.id_canal = +fd.get('c');
      r.id_usuario_modificador = getUid();
      r.fecha_modificacion = ahora();
      toast('Reserva #' + r.id_reserva + ' modificada');
    },
    'Guardar cambios',
  );
  document.querySelector('#mb').oninput = document.querySelector('#mb').onchange =
    refrescarLineas;
  refrescarLineas();
}
