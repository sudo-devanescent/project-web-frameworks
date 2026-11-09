/**
 * @file session.js
 * @description Gestión de sesión, autenticación y login/logout contra la API.
 * Maneja el estado SES (vía setters de state), el arranque con restauración
 * de sesión y las transiciones de UI.
 * @author HostOps Team
 * @version 1.0.0
 */

import { qs, esc } from '../core/utils.js';
import { SES, filtros, iniciarSesion, cerrarSesion } from '../core/state.js';
import { api, ApiError, mensajeError } from '../core/api.js';
import { NAV } from '../core/constants.js';
import { puede } from './permissions.js';
import { toast, ocultarModal } from '../ui/components.js';
import { renderizarVista } from '../ui/router.js';

/**
 * Muestra toast de acceso denegado (usado por vistas y router)
 */
export const denegar = () => toast('Acceso denegado: tu rol no permite esta acción.', 'danger');

/**
 * Maneja el submit del formulario de login contra POST /api/auth/login.
 * @param {Event} e - Evento submit
 */
export async function login(e) {
  e.preventDefault();
  const f = e.target;
  const n = f.u.value.trim();
  const p = f.p.value;
  const er = qs('#lerr');
  const boton = f.querySelector('button[type="submit"]');

  if (!n || !p) {
    er.textContent = 'Introduce usuario y contraseña.';
    er.classList.remove('d-none');
    return;
  }

  boton.disabled = true;
  try {
    const sesion = await api.post('/api/auth/login', {
      nombreUsuario: n,
      contrasena: p,
    });
    iniciarSesion({
      nombre: sesion.nombreCompleto || sesion.nombreUsuario,
      rol: sesion.rol,
      uid: sesion.idUsuario,
    });
    f.reset();
    er.classList.add('d-none');
    location.hash = '#dash';
    entrar();
  } catch (err) {
    er.textContent =
      err instanceof ApiError && err.status === 401
        ? err.detail || 'Usuario o contraseña incorrectos.'
        : mensajeError(err);
    er.classList.remove('d-none');
  } finally {
    boton.disabled = false;
  }
}

/**
 * Restaura la sesión al arrancar la aplicación: obtiene el token CSRF y
 * consulta GET /api/auth/me; si hay sesión válida, establece SES.
 * @returns {Promise<boolean>} true si había sesión activa
 */
export async function restaurarSesion() {
  try {
    await api.get('/api/auth/csrf');
  } catch {
    /* sin conexión: se reintentará en cada petición */
  }
  try {
    const sesion = await api.get('/api/auth/me');
    iniciarSesion({
      nombre: sesion.nombreCompleto || sesion.nombreUsuario,
      rol: sesion.rol,
      uid: sesion.idUsuario,
    });
    return true;
  } catch {
    return false;
  }
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
    `<i class="bi bi-person-circle"></i> ${esc(SES.nombre)} <span class="badge bg-white bg-opacity-25 border border-white border-opacity-25 text-white text-capitalize">${esc(SES.rol)}</span>`;

  renderizarVista();
}

/**
 * Cierra sesión en el servidor y limpia el estado local
 */
export async function salir() {
  ocultarModal();
  try {
    await api.post('/api/auth/logout');
  } catch {
    /* la sesión local se limpia igualmente */
  }
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
