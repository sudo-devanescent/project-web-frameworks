/**
 * @file configuracion.js
 * @description Vista Configuración (solo administrador): usuarios del sistema,
 * roles y canales de origen contra la API.
 * @author HostOps Team
 * @version 1.0.0
 */

import { DB, listados } from '../core/state.js';
import { esc } from '../core/utils.js';
import { api, mensajeError } from '../core/api.js';
import { refrescarCatalogo } from '../core/catalogos.js';
import { puede } from '../auth/permissions.js';
import { denegar } from '../auth/session.js';
import {
  encabezado,
  tarjeta,
  tabla,
  campoForm,
  opcionesDe,
  modal,
  toast,
} from '../ui/components.js';
import { renderizarVista, tokenRender } from '../ui/router.js';

/**
 * Renderiza el shell de la vista Configuración (el contenido lo llena el listado)
 * @returns {string} HTML
 */
export function cfg() {
  return encabezado('Configuración y empleados') + '<div id="l"></div>';
}

/**
 * Descarga usuarios, roles y canales, y pinta la vista
 */
listados.cfg = async () => {
  const token = tokenRender;
  await Promise.all([
    refrescarCatalogo('usuario'),
    refrescarCatalogo('rol'),
    refrescarCatalogo('canal'),
  ]);
  if (token !== tokenRender) return;

  document.querySelector('#l').innerHTML = `<div class="row g-3"><div class="col-12">${tarjeta(
    'Usuarios del sistema',
    tabla(
      ['Usuario', 'Nombre completo', 'Correo', 'Rol', 'Activo'],
      DB.usuario
        .map(
          (u) =>
            `<tr><td>${esc(u.nombreUsuario)}</td><td>${esc(u.nombreCompleto)}</td><td>${esc(u.correo)}</td><td class="text-capitalize">${esc(u.nombreRol)}</td><td><div class="form-check form-switch"><input class="form-check-input" type="checkbox" role="switch" aria-label="Activo" ${u.activo ? 'checked' : ''} onchange="tgU(${u.idUsuario},this.checked)"></div></td></tr>`,
        )
        .join(''),
    ),
    '<button class="btn btn-sm btn-primary" onclick="nuevoUsuario()"><i class="bi bi-person-plus"></i> Nuevo usuario</button>',
  )}</div>
 <div class="col-md-6">${tarjeta(
   'Roles',
   `<ul class="list-group list-group-flush">${DB.rol
     .map(
       (r) =>
         `<li class="list-group-item d-flex justify-content-between text-capitalize bg-transparent">${esc(r.nombre)}<span class="badge text-bg-primary">${r.usuarios} usuario(s)</span></li>`,
     )
     .join('')}</ul>`,
 )}</div>
 <div class="col-md-6">${tarjeta(
   'Canales de origen',
   `<ul class="list-group list-group-flush">${DB.canal
     .map(
       (c) =>
         `<li class="list-group-item d-flex justify-content-between text-capitalize bg-transparent">${esc(c.nombre)}<span class="badge text-bg-info">${c.reservas} reserva(s)</span></li>`,
     )
     .join('')}</ul><form class="d-flex gap-2 p-3" onsubmit="addCanal(event)"><input name="n" class="form-control" placeholder="Nuevo canal" aria-label="Nuevo canal" maxlength="50" required><button class="btn btn-primary">Agregar</button></form>`,
 )}</div></div>`;
};

/**
 * Activa o desactiva un usuario (el servidor exige al menos un admin activo)
 * @param {number} id - ID del usuario
 * @param {boolean} v - Nuevo estado "activo"
 */
export async function tgU(id, v) {
  if (!puede('cfg')) return denegar();
  try {
    const u = await api.patch('/api/usuarios/' + id + '/activo', { activo: v });
    const i = DB.usuario.findIndex((x) => x.idUsuario == id);
    if (i >= 0) DB.usuario[i] = u;
    toast(`Usuario ${u.nombreUsuario} ${v ? 'activado' : 'desactivado'}`);
  } catch (e) {
    toast(mensajeError(e), 'danger');
  }
  await renderizarVista(); // re-renderiza y revierte el switch si el servidor rechazó
}

/**
 * Agrega un canal de origen nuevo desde el formulario del pie de tarjeta
 * @param {Event} e - Evento submit
 */
export async function addCanal(e) {
  e.preventDefault();
  if (!puede('cfg')) return denegar();
  const n = e.target.n.value.trim().toLowerCase();
  if (DB.canal.some((c) => c.nombre == n)) return toast('Ese canal ya existe.', 'danger');
  try {
    await api.post('/api/canales', { nombre: n });
    toast('Canal agregado');
    await renderizarVista();
  } catch (err) {
    toast(mensajeError(err), 'danger');
  }
}

/**
 * Modal de creación de usuario del sistema
 */
export function nuevoUsuario() {
  if (!puede('cfg')) return denegar();
  modal(
    'Nuevo usuario',
    `<form class="row g-3 needs-validation" novalidate>${campoForm('Nombre de usuario', '<input name="u" class="form-control" maxlength="50" required>')}${campoForm('Nombre completo', '<input name="n" class="form-control" maxlength="120" required>')}
 ${campoForm('Correo', '<input type="email" name="c" class="form-control" required>')}${campoForm('Rol', `<select name="r" class="form-select" required>${opcionesDe(DB.rol, 'idRol', (r) => r.nombre)}</select>`)}
 ${campoForm('Contraseña (mínimo 8 caracteres)', '<input type="password" name="pw" class="form-control" minlength="8" required>', 'col-12')}</form>`,
    async (fd) => {
      const u = fd.get('u').trim();
      const c = fd.get('c').trim().toLowerCase();
      if (DB.usuario.some((x) => x.nombreUsuario == u)) return 'Ese nombre de usuario ya existe.';
      if (DB.usuario.some((x) => x.correo == c)) return 'Ese correo ya está registrado.';
      await api.post('/api/usuarios', {
        idRol: +fd.get('r'),
        nombreUsuario: u,
        nombreCompleto: fd.get('n'),
        correo: c,
        contrasena: fd.get('pw'),
        activo: true,
      });
      toast('Usuario creado');
    },
    'Crear usuario',
  );
}
