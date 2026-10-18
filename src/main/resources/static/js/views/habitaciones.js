/**
 * @file habitaciones.js
 * @description Vista Habitaciones: grid con filtros, modal detalle, editar, bloquear/liberar.
 * @author HostOps Team
 * @version 1.0.0
 */

import { DB, filtros, listados, HOY, buscarPor, siguienteId } from '../core/state.js';
import { ESTH, TIPOS, TABLA } from '../core/constants.js';
import { soles } from '../core/utils.js';
import { reglas } from '../core/rules.js';
import { esAdmin } from '../auth/permissions.js';
import { denegar } from '../auth/session.js';
import {
  encabezado,
  opcionesLista,
  tabla,
  badgeEstado,
  campoForm,
  modal,
  toast,
} from '../ui/components.js';

/**
 * Renderiza la vista Habitaciones
 * @returns {string} HTML
 */
export function hab() {
  return (
    encabezado(
      'Habitaciones',
      `<select class="form-select w-auto" aria-label="Filtrar por estado" onchange="filtros.hab.estado=this.value;listados.hab()"><option value="">Todos los estados</option>${opcionesLista(ESTH, filtros.hab.estado)}</select>
  <select class="form-select w-auto" aria-label="Filtrar por tipo" onchange="filtros.hab.tipo=this.value;listados.hab()"><option value="">Todos los tipos</option>${opcionesLista(TIPOS, filtros.hab.tipo)}</select>`,
    ) + '<div class="row g-3" id="l"></div>'
  );
}

/**
 * Listado de habitaciones con filtros aplicados
 */
listados.hab = () => {
  const f = filtros.hab;
  const html = DB.habitacion
    .filter((h) => (!f.estado || h.estado === f.estado) && (!f.tipo || h.tipo === f.tipo))
    .map(
      (h) => `
  <div class="col-12 col-sm-6 col-lg-4 col-xl-3">
    <article class="card room e-${h.estado} shadow-sm h-100">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-start mb-2">
          <div class="num">${h.numero}</div>
          ${badgeEstado(h.estado)}
        </div>
        <div class="text-capitalize fw-semibold">${h.tipo}</div>
        <div class="small text-body-secondary mb-3"><i class="bi bi-people"></i> ${h.capacidad} · ${soles(h.tarifa)} / noche</div>
        <div class="d-flex flex-wrap gap-2">
          <button class="btn btn-sm btn-outline-primary" onclick="hDet(${h.id_habitacion})">Detalles</button>
          <button class="btn btn-sm btn-outline-secondary" onclick="bloquear(${h.id_habitacion})"><i class="bi bi-wrench"></i> Bloquear</button>
          ${esAdmin() ? `<button class="btn btn-sm btn-outline-secondary" onclick="editarHab(${h.id_habitacion})" title="Editar"><i class="bi bi-pencil"></i></button>` : ''}
        </div>
      </div>
    </article>
  </div>`,
    )
    .join('') || '<p class="text-body-secondary">No hay habitaciones con esos filtros.</p>';

  document.querySelector('#l').innerHTML = html;
};

/**
 * Modal detalle de habitación: info, estado, bloqueos, reservas activas
 * @param {number} id - ID habitación
 */
export function hDet(id) {
  const h = buscarPor('habitacion', 'id_habitacion', id);
  const bl = DB[TABLA.bl].filter((b) => b.id_habitacion === id);
  const ds = DB[TABLA.det].filter(
    (d) => d.id_habitacion === id && ['confirmada', 'en_estadia'].includes(d.estado),
  );
  const ocu = ds.some((d) => d.estado === 'en_estadia');

  modal(
    'Habitación ' + h.numero,
    `<div class="row g-3">
    <div class="col-md-6">
      <dl class="mb-0">
        <dt>Tipo</dt><dd class="text-capitalize">${h.tipo}</dd>
        <dt>Capacidad</dt><dd>${h.capacidad} huésped(es)</dd>
        <dt>Tarifa vigente</dt><dd>${soles(h.tarifa)} por noche</dd>
      </dl>
      ${esAdmin() ? `<button class="btn btn-sm btn-outline-primary" onclick="editarHab(${id})"><i class="bi bi-pencil"></i> Editar tipo, capacidad y tarifa</button>` : ''}
    </div>
    <div class="col-md-6">
      <label class="form-label fw-semibold" for="se">Estado</label>
      <select id="se" class="form-select" ${ocu ? 'disabled' : ''} onchange="setEstado(${id},this.value)">
        ${(ocu ? ['ocupada'] : ['disponible', 'limpieza', 'mantenimiento'])
          .map((e) => `<option ${e === h.estado ? 'selected' : ''}>${e}</option>`)
          .join('')}
      </select>
      ${ocu ? '<div class="form-text">Ocupada por una estadía en curso: se libera con el check-out.</div>' : ''}
    </div>
    <div class="col-12">
      <h3 class="h6">Bloqueos por mantenimiento</h3>
      ${tabla(
        ['Desde', 'Hasta', 'Motivo', ''],
        bl
          .map(
            (b) =>
              `<tr><td>${b.fecha_inicio}</td><td>${b.fecha_fin}</td><td>${b.motivo}</td><td><button class="btn btn-sm btn-outline-danger" onclick="delBloqueo(${b.id_bloqueo})">Liberar</button></td></tr>`,
          )
          .join(''),
      )}
      <button class="btn btn-sm btn-primary mt-2" onclick="bloquear(${id})"><i class="bi bi-plus-lg"></i> Nuevo bloqueo</button>
    </div>
    <div class="col-12">
      <h3 class="h6">Reservas activas</h3>
      ${tabla(
        ['Reserva', 'Entrada', 'Salida', 'Estado'],
        ds
          .map(
            (d) =>
              `<tr><td>#${d.id_reserva}</td><td>${d.fecha_entrada_prevista}</td><td>${d.fecha_salida_prevista}</td><td>${badgeEstado(d.estado)}</td></tr>`,
          )
          .join(''),
      )}
    </div>
  </div>`,
  );
}

/**
 * Cambia estado de habitación y refresca vista
 * @param {number} id
 * @param {string} estado
 */
export function setEstado(id, estado) {
  buscarPor('habitacion', 'id_habitacion', id).estado = estado;
  toast('Estado actualizado a ' + estado);
  // Re-render para actualizar grid y mantener modal abierto
  listados.hab();
}

/**
 * Modal editar habitación (solo admin)
 * @param {number} id
 */
export function editarHab(id) {
  if (!esAdmin()) return denegar();
  const h = buscarPor('habitacion', 'id_habitacion', id);

  modal(
    'Editar habitación ' + h.numero,
    `<form class="row g-3 needs-validation" novalidate>
    ${campoForm('Tipo', `<select name="t" class="form-select" required>${opcionesLista(TIPOS, h.tipo)}</select>`, 'col-md-4')}
    ${campoForm('Capacidad', `<input type="number" name="c" class="form-control" min="1" max="12" step="1" value="${h.capacidad}" required>`, 'col-md-4')}
    ${campoForm('Tarifa por noche (S/)', `<input type="number" name="r" class="form-control" min="0" step="0.01" value="${h.tarifa}" required>`, 'col-md-4')}
  </form>
  <p class="small text-body-secondary mt-3 mb-0">La nueva tarifa solo se aplica a reservas futuras: las existentes conservan su tarifa congelada.</p>`,
    (fd) => {
      const c = +fd.get('c');
      const mx = reglas.maxOcupantes(id);
      if (c < mx)
        return `La capacidad no puede ser menor a ${mx}: hay una reserva activa con ${mx} huéspedes asignados.`;
      h.tipo = fd.get('t');
      h.capacidad = c;
      h.tarifa = +fd.get('r');
      toast('Habitación ' + h.numero + ' actualizada');
    },
  );
}

/**
 * Modal bloquear habitación por mantenimiento
 * @param {number} id
 */
export function bloquear(id) {
  const h = buscarPor('habitacion', 'id_habitacion', id);

  modal(
    'Bloquear habitación ' + h.numero,
    `<form class="row g-3 needs-validation" novalidate>
    ${campoForm('Desde (inclusive)', `<input type="date" name="a" class="form-control" min="${HOY}" required>`)}
    ${campoForm('Hasta (inclusive)', `<input type="date" name="b" class="form-control" min="${HOY}" required>`)}
    ${campoForm('Motivo', '<input name="m" class="form-control" maxlength="200" required>', 'col-12')}
  </form>`,
    (fd) => {
      const a = fd.get('a');
      const b = fd.get('b');
      if (b < a) return 'La fecha final no puede ser anterior a la inicial.';

      // Conflicto con reservas activas
      if (
        DB[TABLA.det].some(
          (d) =>
            d.id_habitacion === id &&
            ['confirmada', 'en_estadia'].includes(d.estado) &&
            d.fecha_entrada_prevista <= b &&
            d.fecha_salida_prevista > a,
        )
      )
        return 'El bloqueo se cruza con reservas activas de la habitación ' + h.numero + '.';

      // Conflicto con otros bloqueos
      if (
        DB[TABLA.bl].some((x) => x.id_habitacion === id && a <= x.fecha_fin && b >= x.fecha_inicio)
      )
        return 'Ya existe un bloqueo que se solapa con esas fechas.';

      DB[TABLA.bl].push({
        id_bloqueo: siguienteId(TABLA.bl, 'id_bloqueo'),
        id_habitacion: id,
        fecha_inicio: a,
        fecha_fin: b,
        motivo: fd.get('m'),
      });

      if (a <= HOY && HOY <= b) h.estado = 'mantenimiento';
      toast('Bloqueo registrado');
    },
    'Bloquear',
  );
}

/**
 * Libera un bloqueo y actualiza estado habitación si corresponde
 * @param {number} idBloqueo
 */
export function delBloqueo(idBloqueo) {
  const b = buscarPor(TABLA.bl, 'id_bloqueo', idBloqueo);
  const h = buscarPor('habitacion', 'id_habitacion', b.id_habitacion);

  DB[TABLA.bl] = DB[TABLA.bl].filter((x) => x !== b);

  if (
    h.estado === 'mantenimiento' &&
    !DB[TABLA.bl].some(
      (x) => x.id_habitacion === h.id_habitacion && x.fecha_inicio <= HOY && HOY <= x.fecha_fin,
    )
  )
    h.estado = 'disponible';

  toast('Bloqueo liberado');
  hDet(h.id_habitacion);
  listados.hab();
}

