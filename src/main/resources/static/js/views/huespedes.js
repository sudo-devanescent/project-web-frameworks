/**
 * @file huespedes.js
 * @description Vista Huéspedes: listado con búsqueda, ficha con historial
 * y formulario único de creación/edición contra la API.
 * @author HostOps Team
 * @version 1.0.0
 */

import { DB, filtros, listados, HOY, buscarPor } from '../core/state.js';
import { esc, nombreHuesped } from '../core/utils.js';
import { api, mensajeError } from '../core/api.js';
import {
  encabezado,
  tabla,
  badgeEstado,
  campoForm,
  opcionesLista,
  modal,
  toast,
} from '../ui/components.js';
import { tokenRender } from '../ui/router.js';

/**
 * Renderiza la vista Huéspedes
 * @returns {string} HTML
 */
export function hue() {
  return (
    encabezado(
      'Huéspedes',
      `<input class="form-control w-auto" type="search" placeholder="Buscar nombre o documento" aria-label="Buscar" value="${esc(filtros.hue)}" oninput="filtros.hue=this.value;pintarHue()"><button class="btn btn-primary" onclick="huespedModal()"><i class="bi bi-person-plus"></i> Nuevo huésped</button>`,
    ) + '<div class="card shadow-sm" id="l"></div>'
  );
}

/**
 * Descarga huéspedes desde la API y pinta el listado
 */
listados.hue = async () => {
  const token = tokenRender;
  DB.huesped = await api.get('/api/huespedes');
  if (token !== tokenRender) return;
  pintarHue();
};

/**
 * Pinta el listado de huéspedes con búsqueda por nombre o documento
 */
export function pintarHue() {
  const q = filtros.hue.toLowerCase();
  document.querySelector('#l').innerHTML = tabla(
    ['Documento', 'Nombre', 'Teléfono', 'Nacionalidad', 'Estadías', ''],
    DB.huesped
      .filter((h) => (nombreHuesped(h) + h.numeroDocumento).toLowerCase().includes(q))
      .map(
        (h) =>
          `<tr><td>${esc(h.tipoDocumento)} ${esc(h.numeroDocumento)}</td><td>${esc(nombreHuesped(h))}</td><td>${esc(h.telefono)}</td><td>${esc(h.nacionalidad)}</td><td>${h.estadias}</td><td><button class="btn btn-sm btn-outline-primary" onclick="hueDet(${h.idHuesped})">Ver ficha</button></td></tr>`,
      )
      .join(''),
  );
}

/**
 * Modal con la ficha completa del huésped y su historial de estadías
 * @param {number} id - ID del huésped
 */
export async function hueDet(id) {
  try {
    const h = buscarPor('huesped', 'idHuesped', id);
    if (!h) return;
    const est = await api.get('/api/huespedes/' + id + '/estadias');

    modal(
      nombreHuesped(h),
      `<div class="row g-3"><div class="col-md-6"><dl class="mb-0"><dt>Documento</dt><dd>${esc(h.tipoDocumento)} ${esc(h.numeroDocumento)}</dd><dt>Nacimiento</dt><dd>${h.fechaNacimiento}</dd><dt>Nacionalidad</dt><dd>${esc(h.nacionalidad)}</dd></dl></div>
 <div class="col-md-6"><dl class="mb-0"><dt>Teléfono</dt><dd>${esc(h.telefono)}</dd><dt>Correo</dt><dd>${esc(h.correo) || '—'}</dd><dt>Preferencias</dt><dd>${esc(h.preferencias) || '—'}</dd></dl></div>
 <div class="col-12"><button class="btn btn-sm btn-outline-primary" onclick="huespedModal(${id})"><i class="bi bi-pencil"></i> Editar datos</button></div>
 <div class="col-12"><h3 class="h6">Historial de estadías</h3>${tabla(
   ['Reserva', 'Hab.', 'Entrada', 'Salida', 'Estado'],
   est
     .map(
       (e) =>
         `<tr><td>#${e.idReserva}</td><td>${esc(e.numeroHabitacion)}</td><td>${e.fechaEntradaPrevista}</td><td>${e.fechaSalidaPrevista}</td><td>${badgeEstado(e.estado)}</td></tr>`,
     )
     .join(''),
 )}</div></div>`,
    );
  } catch (e) {
    toast(mensajeError(e), 'danger');
  }
}

/**
 * Formulario único para crear o editar un huésped
 * @param {number} [id] - ID del huésped a editar; omitido = crear nuevo
 */
export function huespedModal(id) {
  const h = id ? buscarPor('huesped', 'idHuesped', id) : {};
  if (id && !h) return;
  const v = (k) => esc(h[k] ?? '');
  modal(
    id ? 'Editar huésped' : 'Nuevo huésped',
    `<form class="row g-3 needs-validation" novalidate>
 ${campoForm('Tipo de documento', `<select name="td" class="form-select" required>${opcionesLista(['DNI', 'pasaporte', 'CE'], h.tipoDocumento)}</select>`)}${campoForm('Número de documento', `<input name="nd" class="form-control" maxlength="30" value="${v('numeroDocumento')}" required>`)}
 ${campoForm('Nombres', `<input name="n" class="form-control" maxlength="100" value="${v('nombres')}" required>`)}${campoForm('Apellidos', `<input name="ap" class="form-control" maxlength="100" value="${v('apellidos')}" required>`)}
 ${campoForm('Teléfono', `<input name="tel" class="form-control" maxlength="30" value="${v('telefono')}" required>`)}${campoForm('Correo (opcional)', `<input type="email" name="co" class="form-control" value="${v('correo')}">`)}
 ${campoForm('Nacionalidad', `<input name="na" class="form-control" value="${v('nacionalidad') || 'Perú'}" required>`)}${campoForm('Fecha de nacimiento', `<input type="date" name="fn" class="form-control" max="${HOY}" value="${v('fechaNacimiento')}" required>`)}
 ${campoForm('Preferencias (opcional)', `<textarea name="p" class="form-control" rows="2">${v('preferencias')}</textarea>`, 'col-12')}</form>`,
    async (fd) => {
      const td = fd.get('td');
      const nd = fd.get('nd').trim();
      if (td == 'DNI' && !/^\d{8}$/.test(nd)) return 'El DNI debe tener 8 dígitos.';
      if (
        DB.huesped.some(
          (x) => x.idHuesped != id && x.tipoDocumento == td && x.numeroDocumento == nd,
        )
      )
        return 'Ya existe otro huésped con ese documento.';
      const dat = {
        tipoDocumento: td,
        numeroDocumento: nd,
        nombres: fd.get('n'),
        apellidos: fd.get('ap'),
        telefono: fd.get('tel'),
        correo: fd.get('co'),
        nacionalidad: fd.get('na'),
        fechaNacimiento: fd.get('fn'),
        preferencias: fd.get('p'),
      };
      if (id) await api.put('/api/huespedes/' + id, dat);
      else await api.post('/api/huespedes', dat);
      toast(id ? 'Datos del huésped actualizados' : 'Huésped registrado');
    },
    id ? 'Guardar cambios' : 'Registrar',
  );
}
