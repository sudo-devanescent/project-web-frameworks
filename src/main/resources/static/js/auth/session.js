/**
 * @file session.js
 * @description Gestión de sesión, autenticación y login/logout.
 * Maneja el estado SES (vía setters de state), login simplificado y transiciones de UI.
 * @author HostOps Team
 * @version 1.0.0
 */

import { qs, esc } from '../core/utils.js';
import { DB, SES, filtros, buscarPor, iniciarSesion, cerrarSesion } from '../core/state.js';
import { NAV } from '../core/constants.js';
import { puede } from './permissions.js';
import { toast, ocultarModal } from '../ui/components.js';
import { renderizarVista } from '../ui/router.js';

/**
 * Muestra toast de acceso denegado (usado por vistas y router)
 */
export const denegar = () => toast('Acceso denegado: tu rol no permite esta acción.', 'danger');

/**
 * Maneja el submit del formulario de login.
 * Autenticación simplificada: usuario y credencial de 4 o más caracteres;
 * busca un usuario activo con el rol seleccionado.
 * @param {Event} e - Evento submit
 */
export function login(e) {
  e.preventDefault();
  const f = e.target;
  const n = f.u.value.trim();
  const p = f.p.value.trim();
  const er = qs('#lerr');

  if (n.length < 4 || p.length < 4) {
    er.textContent = 'El usuario y la credencial deben tener al menos 4 caracteres.';
    er.classList.remove('d-none');
    return;
  }

  const rol = f.r.value;
  const u = DB.usuario.find((x) => x.activo && x.id_rol == buscarPor('rol', 'nombre', rol).id_rol);

  if (!u) {
    er.textContent = `No hay usuarios activos con el rol ${rol}.`;
    er.classList.remove('d-none');
    return;
  }

  // uid: usuario del rol elegido, para las FK de auditoría (creador/modificador)
  iniciarSesion({ nombre: n, rol, uid: u.id_usuario });

  f.reset();
  er.classList.add('d-none');
  location.hash = '#dash';
  entrar();
}

/**
 * Transición UI: oculta login, muestra shell, renderiza menú y usuario
 */
export function entrar() {
  qs('#login').hidden = true;
  qs('#shell').hidden = false;

  // Menú filtrado por permisos del rol
  qs('#menu').innerHTML = NAV.filter((n) => puede(n[0]))
    .map(
      (n) =>
        `<li class="nav-item"><a class="nav-link" href="#${n[0]}" data-v="${n[0]}"><i class="bi bi-${n[1]}"></i> ${n[2]}</a></li>`,
    )
    .join('');

  // Info de usuario en la navbar
  qs('#usr').innerHTML =
    `<i class="bi bi-person-circle"></i> ${esc(SES.nombre)} <span class="badge bg-white bg-opacity-25 border border-white border-opacity-25 text-white text-capitalize">${SES.rol}</span>`;

  renderizarVista();
}

/**
 * Cierra sesión: limpia estado, resetea filtros y vuelve al login
 */
export function salir() {
  ocultarModal();
  cerrarSesion();

  filtros.hab = { estado: '', tipo: '' };
  filtros.res = { q: '', estado: '' };
  filtros.hue = '';

  history.replaceState(null, '', '#');
  renderizarVista();
}

/**
 * Alterna visibilidad de la credencial en el login
 * @param {HTMLButtonElement} b - Botón con el icono de ojo
 */
export function verCred(b) {
  const i = qs('#lp');
  const v = i.type === 'password';
  i.type = v ? 'text' : 'password';
  b.firstElementChild.className = 'bi bi-eye' + (v ? '-slash' : '');
}

/**
 * Fuerza la vista de login (sin sesión)
 */
export function mostrarLogin() {
  qs('#shell').hidden = true;
  qs('#login').hidden = false;
  qs('#lu').focus();
}
