/**
 * @file huespedes.js
 * @description Vista Huéspedes: listado con búsqueda, ficha con historial
 * y formulario único de creación/edición con validación de documento.
 * @author HostOps Team
 * @version 1.0.0
 */

import { DB, filtros, listados, HOY, buscarPor, siguienteId } from '../core/state.js';
import { TABLA } from '../core/constants.js';
import { esc, nombreHuesped } from '../core/utils.js';
import {
  encabezado,
  tabla,
  badgeEstado,
  campoForm,
  opcionesLista,
  modal,
  toast,
} from '../ui/components.js';

/**
 * Renderiza la vista Huéspedes
 * @returns {string} HTML
 */
export function hue() {
  return (
    encabezado(
      'Huéspedes',
      `<input class="form-control w-auto" type="search" placeholder="Buscar nombre o documento" aria-label="Buscar" value="${esc(filtros.hue)}" oninput="filtros.hue=this.value;listados.hue()"><button class="btn btn-primary" onclick="huespedModal()"><i class="bi bi-person-plus"></i> Nuevo huésped</button>`,
    ) + '<div class="card shadow-sm" id="l"></div>'
  );
}

/**
 * Todas las estadías en las que participa un huésped
 * (como titular de la reserva o como ocupante asignado)
 * @param {number} idHuesped
 * @returns {Object[]} Detalles de reserva
 */
export const estadias = (idHuesped) =>
  DB[TABLA.det].filter(
    (d) =>
      buscarPor('reserva', 'id_reserva', d.id_reserva).id_huesped_titular == idHuesped ||
      DB[TABLA.dh].some((x) => x.id_detalle == d.id_detalle && x.id_huesped == idHuesped),
  );

/**
 * Listado de huéspedes con búsqueda por nombre o documento
 */
listados.hue = () => {
  const q = filtros.hue.toLowerCase();
  document.querySelector('#l').innerHTML = tabla(
    ['Documento', 'Nombre', 'Teléfono', 'Nacionalidad', 'Estadías', ''],
    DB.huesped
      .filter((h) => (nombreHuesped(h) + h.numero_documento).toLowerCase().includes(q))
      .map(
        (h) =>
          `<tr><td>${h.tipo_documento} ${esc(h.numero_documento)}</td><td>${esc(nombreHuesped(h))}</td><td>${esc(h.telefono)}</td><td>${esc(h.nacionalidad)}</td><td>${estadias(h.id_huesped).filter((d) => d.estado != 'cancelada').length}</td><td><button class="btn btn-sm btn-outline-primary" onclick="hueDet(${h.id_huesped})">Ver ficha</button></td></tr>`,
      )
      .join(''),
  );
};

/**
 * Modal con la ficha completa del huésped y su historial de estadías
 * @param {number} id - ID del huésped
 */
export function hueDet(id) {
  const h = buscarPor('huesped', 'id_huesped', id);
  modal(
    nombreHuesped(h),
    `<div class="row g-3"><div class="col-md-6"><dl class="mb-0"><dt>Documento</dt><dd>${h.tipo_documento} ${esc(h.numero_documento)}</dd><dt>Nacimiento</dt><dd>${h.fecha_nacimiento}</dd><dt>Nacionalidad</dt><dd>${esc(h.nacionalidad)}</dd></dl></div>
 <div class="col-md-6"><dl class="mb-0"><dt>Teléfono</dt><dd>${esc(h.telefono)}</dd><dt>Correo</dt><dd>${esc(h.correo) || '—'}</dd><dt>Preferencias</dt><dd>${esc(h.preferencias) || '—'}</dd></dl></div>
 <div class="col-12"><button class="btn btn-sm btn-outline-primary" onclick="huespedModal(${id})"><i class="bi bi-pencil"></i> Editar datos</button></div>
 <div class="col-12"><h3 class="h6">Historial de estadías</h3>${tabla(
   ['Reserva', 'Hab.', 'Entrada', 'Salida', 'Estado'],
   estadias(id)
     .map(
       (d) =>
         `<tr><td>#${d.id_reserva}</td><td>${buscarPor('habitacion', 'id_habitacion', d.id_habitacion).numero}</td><td>${d.fecha_entrada_prevista}</td><td>${d.fecha_salida_prevista}</td><td>${badgeEstado(d.estado)}</td></tr>`,
     )
     .join(''),
 )}</div></div>`,
  );
}

/**
 * Formulario único para crear o editar un huésped
 * @param {number} [id] - ID del huésped a editar; omitido = crear nuevo
 */
export function huespedModal(id) {
  const h = id ? buscarPor('huesped', 'id_huesped', id) : {};
  const v = (k) => esc(h[k] ?? '');
  modal(
    id ? 'Editar huésped' : 'Nuevo huésped',
    `<form class="row g-3 needs-validation" novalidate>
 ${campoForm('Tipo de documento', `<select name="td" class="form-select" required>${opcionesLista(['DNI', 'pasaporte', 'CE'], h.tipo_documento)}</select>`)}${campoForm('Número de documento', `<input name="nd" class="form-control" maxlength="30" value="${v('numero_documento')}" required>`)}
 ${campoForm('Nombres', `<input name="n" class="form-control" maxlength="100" value="${v('nombres')}" required>`)}${campoForm('Apellidos', `<input name="ap" class="form-control" maxlength="100" value="${v('apellidos')}" required>`)}
 ${campoForm('Teléfono', `<input name="tel" class="form-control" maxlength="30" value="${v('telefono')}" required>`)}${campoForm('Correo (opcional)', `<input type="email" name="co" class="form-control" value="${v('correo')}">`)}
 ${campoForm('Nacionalidad', `<input name="na" class="form-control" value="${v('nacionalidad') || 'Perú'}" required>`)}${campoForm('Fecha de nacimiento', `<input type="date" name="fn" class="form-control" max="${HOY}" value="${v('fecha_nacimiento')}" required>`)}
 ${campoForm('Preferencias (opcional)', `<textarea name="p" class="form-control" rows="2">${v('preferencias')}</textarea>`, 'col-12')}</form>`,
    (fd) => {
      const td = fd.get('td');
      const nd = fd.get('nd').trim();
      if (td == 'DNI' && !/^\d{8}$/.test(nd)) return 'El DNI debe tener 8 dígitos.';
      if (
        DB.huesped.some(
          (x) => x.id_huesped != id && x.tipo_documento == td && x.numero_documento == nd,
        )
      )
        return 'Ya existe otro huésped con ese documento.';
      const dat = {
        tipo_documento: td,
        numero_documento: nd,
        nombres: fd.get('n'),
        apellidos: fd.get('ap'),
        telefono: fd.get('tel'),
        correo: fd.get('co'),
        nacionalidad: fd.get('na'),
        fecha_nacimiento: fd.get('fn'),
        preferencias: fd.get('p'),
      };
      if (id) Object.assign(h, dat);
      else DB.huesped.push({ id_huesped: siguienteId('huesped', 'id_huesped'), ...dat });
      toast(id ? 'Datos del huésped actualizados' : 'Huésped registrado');
    },
    id ? 'Guardar cambios' : 'Registrar',
  );
}
