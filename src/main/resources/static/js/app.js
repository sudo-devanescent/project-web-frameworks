/**
 * @file app.js
 * @description Punto de entrada de la aplicación HostOps.
 * Expone los manejadores usados por HTML inline en window, inicializa
 * el enrutador y arranca la renderización (login o vista).
 * @author HostOps Team
 * @version 1.0.0
 */

import { filtros, listados } from './core/index.js';
import { login, verCred, salir } from './auth/index.js';
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
} from './views/index.js';
import { initRouter, renderizarVista } from './ui/index.js';

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
});

// Listeners de hashchange, toggle de tema y preferencia del sistema
initRouter();

// Arranque: sin sesión muestra el login; con sesión, la vista del hash actual
renderizarVista();
