/**
 * @file router.js
 * @description Enrutador SPA por hash con control de acceso por rol.
 * Gestiona vistas, navegación, cambio de tema, arranque inicial y el render
 * asíncrono de los listados (con token para descartar renders obsoletos).
 * @author HostOps Team
 * @version 1.0.0
 */

import { SES, listados, qs } from '../core/index.js';
import { mensajeError } from '../core/api.js';
import { puede, mostrarLogin, denegar } from '../auth/index.js';
import { toast } from './components.js';
import { dash, hab, res, hue, fin, cfg } from '../views/index.js';

/** Mapa de hash -> función render de vista */
const vistas = {
  dash: () => dash(),
  hab: () => hab(),
  res: () => res(),
  hue: () => hue(),
  fin: () => fin(),
  cfg: () => cfg(),
};

/**
 * Token de generación del render actual. Se incrementa en cada render para
 * que un listado asíncrono descarte su escritura si ya hubo otra navegación.
 * @type {number}
 */
export let tokenRender = 0;

/**
 * Renderiza la vista actual según hash y permisos (async: espera los listados)
 * - Si no hay sesión -> login
 * - Si hash inválido -> dashboard
 * - Si sin permiso -> toast + redirige a dashboard
 * - Si falla la carga -> toast de error y mensaje en el contenedor
 */
export async function renderizarVista() {
  const token = ++tokenRender;
  if (!SES) return mostrarLogin();

  qs('#login').hidden = true;
  qs('#shell').hidden = false;

  let hash = location.hash.slice(1);
  if (!vistas[hash]) hash = 'dash';

  // Control de acceso por rol
  if (!puede(hash)) {
    denegar();
    hash = 'dash';
    history.replaceState(null, '', '#dash');
  }

  // Render del shell de la vista
  qs('#app').innerHTML = vistas[hash]();

  // Ejecutar listado asociado (descarga de datos + pintado)
  try {
    await listados[hash]?.();
  } catch (e) {
    if (token === tokenRender) {
      const contenedor = qs('#l');
      if (contenedor) {
        contenedor.innerHTML =
          '<p class="text-body-secondary p-3 mb-0">No se pudieron cargar los datos.</p>';
      }
      toast(mensajeError(e), 'danger');
    }
  }
  if (token !== tokenRender) return;

  // Marcar enlace activo en navbar
  document
    .querySelectorAll('[data-v]')
    .forEach((a) => a.classList.toggle('active', a.dataset.v === hash));

  // Cerrar menú móvil si está abierto
  bootstrap.Collapse.getInstance('#nv')?.hide();
}

/**
 * Inicializa el enrutador: listeners de hashchange y tema
 * El tema inicial es el claro (default del HTML); el toggle manual lo alterna por sesión
 */
export function initRouter() {
  // Navegación por hash
  addEventListener('hashchange', () => renderizarVista());

  // Toggle tema claro/oscuro
  document.querySelectorAll('.js-tema').forEach((btn) => {
    btn.onclick = () => {
      const html = document.documentElement;
      html.dataset.bsTheme = html.dataset.bsTheme === 'dark' ? 'light' : 'dark';
    };
  });
}
