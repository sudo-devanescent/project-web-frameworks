/**
 * @file habitaciones.js
 * @description Vista Habitaciones: grid con filtros, modal detalle, editar,
 * bloquear/liberar y cambio de estado contra la API.
 * @author HostOps Team
 * @version 1.0.0
 */

import { DB, filtros, listados, HOY, buscarPor } from '../core/state.js';
import { ESTH, TIPOS, TABLA } from '../core/constants.js';
import { soles, esc } from '../core/utils.js';
import { api, mensajeError } from '../core/api.js';
import { refrescarCatalogo } from '../core/catalogos.js';
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
import { renderizarVista, tokenRender } from '../ui/router.js';

/**
 * Renderiza la vista Habitaciones
 * @returns {string} HTML
 */
export function hab() {
  return (
    encabezado(
      'Habitaciones',
      `<select class="form-select w-auto" aria-label="Filtrar por estado" onchange="filtros.hab.estado=this.value;pintarHab()"><option value="">Todos los estados</option>${opcionesLista(ESTH, filtros.hab.estado)}</select>
  <select class="form-select w-auto" aria-label="Filtrar por tipo" onchange="filtros.hab.tipo=this.value;pintarHab()"><option value="">Todos los tipos</option>${opcionesLista(TIPOS, filtros.hab.tipo)}</select>`,
    ) + '<div class="row g-3" id="l"></div>'
  );
}

/**
 * Descarga habitaciones desde la API y pinta el grid
 */
listados.hab = async () => {
  const token = tokenRender;
  await refrescarCatalogo('habitacion');
  if (token !== tokenRender) return;
  pintarHab();
};

/**
 * Pinta el grid de habitaciones con los filtros aplicados (datos en caché)
 */
export function pintarHab() {
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
          <button class="btn btn-sm btn-outline-primary" onclick="hDet(${h.idHabitacion})">Detalles</button>
          <button class="btn btn-sm btn-outline-secondary" onclick="bloquear(${h.idHabitacion})"><i class="bi bi-wrench"></i> Bloquear</button>
          ${esAdmin() ? `<button class="btn btn-sm btn-outline-secondary" onclick="editarHab(${h.idHabitacion})" title="Editar"><i class="bi bi-pencil"></i></button>` : ''}
        </div>
      </div>
    </article>
  </div>`,
    )
    .join('') || '<p class="text-body-secondary">No hay habitaciones con esos filtros.</p>';

  document.querySelector('#l').innerHTML = html;
}

/**
 * Modal detalle de habitación: info, estado, bloqueos y reservas activas
 * @param {number} id - ID habitación
 */
export async function hDet(id) {
  try {
    const h = buscarPor('habitacion', 'idHabitacion', id);
    if (!h) return;
    const [bl, ds] = await Promise.all([
      api.get('/api/habitaciones/' + id + '/bloqueos'),
      api.get('/api/detalles-reserva?habitacionId=' + id),
    ]);
    DB[TABLA.bl] = [...DB[TABLA.bl].filter((x) => x.idHabitacion != id), ...bl];
    const activos = ds.filter((d) => ['confirmada', 'en_estadia'].includes(d.estado));
    const ocu = h.estado === 'ocupada';

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
              `<tr><td>${b.fechaInicio}</td><td>${b.fechaFin}</td><td>${esc(b.motivo)}</td><td><button class="btn btn-sm btn-outline-danger" onclick="delBloqueo(${b.idBloqueo})">Liberar</button></td></tr>`,
          )
          .join(''),
      )}
      <button class="btn btn-sm btn-primary mt-2" onclick="bloquear(${id})"><i class="bi bi-plus-lg"></i> Nuevo bloqueo</button>
    </div>
    <div class="col-12">
      <h3 class="h6">Reservas activas</h3>
      ${tabla(
        ['Reserva', 'Entrada', 'Salida', 'Estado'],
        activos
          .map(
            (d) =>
              `<tr><td>#${d.idReserva}</td><td>${d.fechaEntradaPrevista}</td><td>${d.fechaSalidaPrevista}</td><td>${badgeEstado(d.estado)}</td></tr>`,
          )
          .join(''),
      )}
    </div>
  </div>`,
    );
  } catch (e) {
    toast(mensajeError(e), 'danger');
  }
}

/**
 * Cambia el estado de la habitación (PATCH explícito) y refresca el grid
 * @param {number} id
 * @param {string} estado
 */
export async function setEstado(id, estado) {
  try {
    const h = await api.patch('/api/habitaciones/' + id + '/estado', { estado });
    const i = DB.habitacion.findIndex((x) => x.idHabitacion == id);
    if (i >= 0) DB.habitacion[i] = h;
    toast('Estado actualizado a ' + estado);
    pintarHab();
  } catch (e) {
    toast(mensajeError(e), 'danger');
    await hDet(id); // re-sincroniza el modal con el estado real del servidor
  }
}

/**
 * Modal editar habitación (solo admin)
 * @param {number} id
 */
export function editarHab(id) {
  if (!esAdmin()) return denegar();
  const h = buscarPor('habitacion', 'idHabitacion', id);
  if (!h) return;

  modal(
    'Editar habitación ' + h.numero,
    `<form class="row g-3 needs-validation" novalidate>
    ${campoForm('Tipo', `<select name="t" class="form-select" required>${opcionesLista(TIPOS, h.tipo)}</select>`, 'col-md-4')}
    ${campoForm('Capacidad', `<input type="number" name="c" class="form-control" min="1" max="12" step="1" value="${h.capacidad}" required>`, 'col-md-4')}
    ${campoForm('Tarifa por noche (S/)', `<input type="number" name="r" class="form-control" min="0" step="0.01" value="${h.tarifa}" required>`, 'col-md-4')}
  </form>
  <p class="small text-body-secondary mt-3 mb-0">La nueva tarifa solo se aplica a reservas futuras: las existentes conservan su tarifa congelada. El servidor valida que la capacidad no baje por debajo de los ocupantes de reservas activas.</p>`,
    async (fd) => {
      const actualizada = await api.put('/api/habitaciones/' + id, {
        numero: h.numero,
        tipo: fd.get('t'),
        capacidad: +fd.get('c'),
        tarifa: +fd.get('r'),
      });
      const i = DB.habitacion.findIndex((x) => x.idHabitacion == id);
      if (i >= 0) DB.habitacion[i] = actualizada;
      toast('Habitación ' + actualizada.numero + ' actualizada');
    },
  );
}

/**
 * Modal bloquear habitación por mantenimiento
 * @param {number} id
 */
export async function bloquear(id) {
  const h = buscarPor('habitacion', 'idHabitacion', id);
  if (!h) return;
  try {
    // Datos frescos para pre-validar conflictos de fechas
    const [ds, bl] = await Promise.all([
      api.get('/api/detalles-reserva'),
      api.get('/api/habitaciones/' + id + '/bloqueos'),
    ]);
    DB[TABLA.det] = ds;
    DB[TABLA.bl] = [...DB[TABLA.bl].filter((x) => x.idHabitacion != id), ...bl];

    modal(
      'Bloquear habitación ' + h.numero,
      `<form class="row g-3 needs-validation" novalidate>
    ${campoForm('Desde (inclusive)', `<input type="date" name="a" class="form-control" min="${HOY}" required>`)}
    ${campoForm('Hasta (inclusive)', `<input type="date" name="b" class="form-control" min="${HOY}" required>`)}
    ${campoForm('Motivo', '<input name="m" class="form-control" maxlength="200" required>', 'col-12')}
  </form>`,
      async (fd) => {
        const a = fd.get('a');
        const b = fd.get('b');
        if (b < a) return 'La fecha final no puede ser anterior a la inicial.';

        // Conflicto con reservas activas
        if (
          DB[TABLA.det].some(
            (d) =>
              d.idHabitacion == id &&
              ['confirmada', 'en_estadia'].includes(d.estado) &&
              d.fechaEntradaPrevista <= b &&
              d.fechaSalidaPrevista > a,
          )
        )
          return 'El bloqueo se cruza con reservas activas de la habitación ' + h.numero + '.';

        // Conflicto con otros bloqueos
        if (
          DB[TABLA.bl].some((x) => x.idHabitacion == id && a <= x.fechaFin && b >= x.fechaInicio)
        )
          return 'Ya existe un bloqueo que se solapa con esas fechas.';

        const creado = await api.post('/api/habitaciones/' + id + '/bloqueos', {
          fechaInicio: a,
          fechaFin: b,
          motivo: fd.get('m'),
        });
        DB[TABLA.bl] = [...DB[TABLA.bl].filter((x) => x.idHabitacion != id), creado];

        // El backend no cambia el estado: si el bloqueo cubre hoy, se pide explícitamente
        if (a <= HOY && HOY <= b && h.estado !== 'ocupada' && h.estado !== 'mantenimiento') {
          const nueva = await api.patch('/api/habitaciones/' + id + '/estado', {
            estado: 'mantenimiento',
          });
          const i = DB.habitacion.findIndex((x) => x.idHabitacion == id);
          if (i >= 0) DB.habitacion[i] = nueva;
        }
        toast('Bloqueo registrado');
      },
      'Bloquear',
    );
  } catch (e) {
    toast(mensajeError(e), 'danger');
  }
}

/**
 * Libera un bloqueo y vuelve a la disponibilidad si corresponde
 * @param {number} idBloqueo
 */
export async function delBloqueo(idBloqueo) {
  const b = buscarPor(TABLA.bl, 'idBloqueo', idBloqueo);
  if (!b) return;
  try {
    await api.del('/api/bloqueos/' + idBloqueo);
    DB[TABLA.bl] = DB[TABLA.bl].filter((x) => x.idBloqueo != idBloqueo);

    let h = await api.get('/api/habitaciones/' + b.idHabitacion);
    if (
      h.estado === 'mantenimiento' &&
      !DB[TABLA.bl].some(
        (x) => x.idHabitacion == b.idHabitacion && x.fechaInicio <= HOY && HOY <= x.fechaFin,
      )
    ) {
      h = await api.patch('/api/habitaciones/' + b.idHabitacion + '/estado', {
        estado: 'disponible',
      });
    }
    const i = DB.habitacion.findIndex((x) => x.idHabitacion == b.idHabitacion);
    if (i >= 0) DB.habitacion[i] = h;

    toast('Bloqueo liberado');
    await renderizarVista();
    await hDet(b.idHabitacion);
  } catch (e) {
    toast(mensajeError(e), 'danger');
  }
}
