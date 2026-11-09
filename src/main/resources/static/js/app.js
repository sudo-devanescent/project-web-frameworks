/**
 * @file app.js
 * @description Punto de entrada de la aplicación HostOps.
 * Expone los manejadores usados por HTML inline en window, inicializa
 * el enrutador, restaura la sesión contra la API y arranca la renderización.
 * @author HostOps Team
 * @version 1.0.0
 */

import { filtros, listados, SES, cerrarSesion } from './core/index.js';
import { login, verCred, salir, restaurarSesion, entrar, mostrarLogin } from './auth/index.js';
import {
  hDet,
  setEstado,
  editarHab,
  bloquear,
  delBloqueo,
  checkin,
  checkout,
  cancelar,
  editarRes,
  nuevaReserva,
  agregarLinea,
  quitarLinea,
  hueDet,
  huespedModal,
  pagar,
  tgU,
  addCanal,
  nuevoUsuario,
  pintarHab,
  pintarRes,
  pintarHue,
} from './views/index.js';
import { initRouter, toast } from './ui/index.js';

/* El HTML estático (login, navbar) y el HTML generado por las vistas usan
   manejadores inline (onclick/onchange/onsubmit/oninput) que resuelven los
   identificadores en el scope global: se exponen de forma centralizada. */
Object.assign(window, {
  // Estado compartido consultado por los manejadores inline
  filtros,
  listados,
  // Sesión
  login,
  verCred,
  salir,
  // Habitaciones
  hDet,
  setEstado,
  editarHab,
  bloquear,
  delBloqueo,
  // Reservas
  checkin,
  checkout,
  cancelar,
  editarRes,
  nuevaReserva,
  agregarLinea,
  quitarLinea,
  // Huéspedes
  hueDet,
  huespedModal,
  // Finanzas
  pagar,
  // Configuración
  tgU,
  addCanal,
  nuevoUsuario,
  // Pintado síncrono de listados filtrables (sin volver a consultar la API)
  pintarHab,
  pintarRes,
  pintarHue,
});

// Sesión expirada o revocada por un administrador (evento desde core/api.js)
addEventListener('sesion-expirada', () => {
  if (!SES) return;
  cerrarSesion();
  mostrarLogin();
  toast('Tu sesión ha expirado. Vuelve a iniciar sesión.', 'warning');
});

// Listeners de hashchange y toggle de tema
initRouter();

// Arranque: login visible de inmediato; si hay sesión en el servidor, se restaura
mostrarLogin();
restaurarSesion().then((haySesion) => {
  if (haySesion && SES) entrar();
});
