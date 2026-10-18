/**
 * @file configuracion.js
 * @description Vista Configuración (solo administrador): usuarios del sistema,
 * roles y canales de origen.
 * @author HostOps Team
 * @version 1.0.0
 */

import { DB, buscarPor, siguienteId } from '../core/state.js';
import { esc } from '../core/utils.js';
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
import { renderizarVista } from '../ui/router.js';

/**
 * Renderiza la vista Configuración y empleados
 * @returns {string} HTML
 */
export function cfg() {
  return (
    encabezado('Configuración y empleados') +
    `<div class="row g-3"><div class="col-12">${tarjeta(
      'Usuarios del sistema',
      tabla(
        ['Usuario', 'Nombre completo', 'Correo', 'Rol', 'Activo'],
        DB.usuario
          .map(
            (u) =>
              `<tr><td>${esc(u.nombre_usuario)}</td><td>${esc(u.nombre_completo)}</td><td>${esc(u.correo)}</td><td class="text-capitalize">${buscarPor('rol', 'id_rol', u.id_rol).nombre}</td><td><div class="form-check form-switch"><input class="form-check-input" type="checkbox" role="switch" aria-label="Activo" ${u.activo ? 'checked' : ''} onchange="tgU(${u.id_usuario},this.checked)"></div></td></tr>`,
          )
          .join(''),
      ),
      '<button class="btn btn-sm btn-primary" onclick="nuevoUsuario()"><i class="bi bi-person-plus"></i> Nuevo usuario</button>',
    )}</div>
 <div class="col-md-6">${tarjeta(
   'Roles',
   `<ul class="list-group list-group-flush">${DB.rol.map((r) => `<li class="list-group-item d-flex justify-content-between text-capitalize bg-transparent">${r.nombre}<span class="badge text-bg-primary">${DB.usuario.filter((u) => u.id_rol == r.id_rol).length} usuario(s)</span></li>`).join('')}</ul>`,
 )}</div>
 <div class="col-md-6">${tarjeta(
   'Canales de origen',
   `<ul class="list-group list-group-flush">${DB.canal.map((c) => `<li class="list-group-item d-flex justify-content-between text-capitalize bg-transparent">${esc(c.nombre)}<span class="badge text-bg-info">${DB.reserva.filter((r) => r.id_canal == c.id_canal).length} reserva(s)</span></li>`).join('')}</ul><form class="d-flex gap-2 p-3" onsubmit="addCanal(event)"><input name="n" class="form-control" placeholder="Nuevo canal" aria-label="Nuevo canal" maxlength="50" required><button class="btn btn-primary">Agregar</button></form>`,
 )}</div></div>`
  );
}

/**
 * Activa o desactiva un usuario (no puede quedar ningún admin activo)
 * @param {number} id - ID del usuario
 * @param {boolean} v - Nuevo estado "activo"
 */
export function tgU(id, v) {
  if (!puede('cfg')) return denegar();
  const u = buscarPor('usuario', 'id_usuario', id);
  if (!v && u.id_rol == 1 && DB.usuario.filter((x) => x.id_rol == 1 && x.activo).length == 1) {
    toast('Debe quedar al menos un administrador activo.', 'danger');
    return renderizarVista(); // revierte el switch visualmente
  }
  u.activo = v;
  toast(`Usuario ${u.nombre_usuario} ${v ? 'activado' : 'desactivado'}`);
}

/**
 * Agrega un canal de origen nuevo desde el formulario del pie de tarjeta
 * @param {Event} e - Evento submit
 */
export function addCanal(e) {
  e.preventDefault();
  if (!puede('cfg')) return denegar();
  const n = e.target.n.value.trim().toLowerCase();
  if (DB.canal.some((c) => c.nombre == n)) return toast('Ese canal ya existe.', 'danger');
  DB.canal.push({ id_canal: siguienteId('canal', 'id_canal'), nombre: n });
  toast('Canal agregado');
  renderizarVista();
}

/**
 * Modal de creación de usuario del sistema
 */
export function nuevoUsuario() {
  if (!puede('cfg')) return denegar();
  modal(
    'Nuevo usuario',
    `<form class="row g-3 needs-validation" novalidate>${campoForm('Nombre de usuario', '<input name="u" class="form-control" maxlength="50" required>')}${campoForm('Nombre completo', '<input name="n" class="form-control" maxlength="120" required>')}
 ${campoForm('Correo', '<input type="email" name="c" class="form-control" required>')}${campoForm('Rol', `<select name="r" class="form-select" required>${opcionesDe(DB.rol, 'id_rol', (r) => r.nombre)}</select>`)}
 ${campoForm('Contraseña (mínimo 8 caracteres; en la BD se guardaría solo su hash)', '<input type="password" name="pw" class="form-control" minlength="8" required>', 'col-12')}</form>`,
    (fd) => {
      const u = fd.get('u').trim();
      const c = fd.get('c').trim().toLowerCase();
      if (DB.usuario.some((x) => x.nombre_usuario == u)) return 'Ese nombre de usuario ya existe.';
      if (DB.usuario.some((x) => x.correo == c)) return 'Ese correo ya está registrado.';
      DB.usuario.push({
        id_usuario: siguienteId('usuario', 'id_usuario'),
        id_rol: +fd.get('r'),
        nombre_usuario: u,
        nombre_completo: fd.get('n'),
        correo: c,
        activo: true,
      });
      toast('Usuario creado');
    },
    'Crear usuario',
  );
}
