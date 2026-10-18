/**
 * @file components.js
 * @description Componentes de UI reutilizables: badges, selects, formularios, tablas, tarjetas, KPIs, toasts.
 * Funciones puras que generan strings HTML.
 * @author HostOps Team
 * @version 1.0.0
 */

import { COLOR_BADGE } from '../core/constants.js';
import { esc, qs } from '../core/utils.js';
import { renderizarVista } from './router.js';

/**
 * Badge de estado con color semántico
 * @param {string} estado - Clave de estado
 * @returns {string} HTML del badge
 */
export const badgeEstado = (estado) =>
  `<span class="badge text-bg-${COLOR_BADGE[estado]}">${estado.replace('_', ' ')}</span>`;

/**
 * Genera <option> para un array de objetos
 * @param {Object[]} arr - Array de objetos
 * @param {string} valueKey - Propiedad para value
 * @param {Function} labelFn - Función que recibe objeto y devuelve label
 * @param {any} selected - Valor seleccionado
 * @returns {string} HTML de options
 */
export const opcionesDe = (arr, valueKey, labelFn, selected) =>
  arr
    .map(
      (x) =>
        `<option value="${x[valueKey]}" ${x[valueKey] == selected ? 'selected' : ''}>${esc(labelFn(x))}</option>`,
    )
    .join('');

/**
 * Genera <option> para array de valores simples (strings/numbers)
 * @param {any[]} arr
 * @param {any} selected
 * @returns {string}
 */
export const opcionesLista = (arr, selected) =>
  arr.map((x) => `<option ${x == selected ? 'selected' : ''}>${x}</option>`).join('');

/**
 * Campo de formulario con label, input y feedback de validación
 * @param {string} label - Texto del label
 * @param {string} input - HTML del input/select/textarea
 * @param {string} [col='col-md-6'] - Clase de columna Bootstrap
 * @returns {string}
 */
export const campoForm = (label, input, col = 'col-md-6') =>
  `<div class="${col}"><label class="form-label small fw-semibold">${label}</label>${input}<div class="invalid-feedback">Obligatorio o con formato no válido.</div></div>`;

/**
 * Genera tabla responsiva con data-label en celdas (para mobile cards)
 * @param {string[]} headers - Encabezados de columnas
 * @param {string} rowsHtml - HTML de filas <tr>...</tr> (sin tbody)
 * @returns {string} HTML completo de tabla envuelta en .table-responsive
 */
export const tabla = (headers, rowsHtml) =>
  `<div class="table-responsive"><table class="table table-hover align-middle mb-0 tabla-resp"><thead><tr>${headers
    .map((h) => `<th>${h}</th>`)
    .join('')}</tr></thead><tbody>${
    rowsHtml
      ? rowsHtml.replace(/<tr>([\s\S]*?)<\/tr>/g, (m, rowContent) => {
          let i = 0;
          return (
            '<tr>' +
            rowContent.replace(/<td(?=[\s>])/g, () => `<td data-label="${headers[i++] || ''}"`) +
            '</tr>'
          );
        })
      : `<tr><td colspan="${headers.length}" class="text-center text-body-secondary py-4">Sin resultados</td></tr>`
  }</tbody></table></div>`;

/**
 * Tarjeta genérica con header (título + acciones opcionales) y body
 * @param {string} titulo
 * @param {string} bodyHtml
 * @param {string} [headerExtra=''] - HTML extra en header (botones, enlaces)
 * @returns {string}
 */
export const tarjeta = (titulo, bodyHtml, headerExtra = '') =>
  `<section class="card h-100 shadow-sm"><div class="card-header bg-transparent d-flex justify-content-between align-items-center"><h2 class="h6 mb-0">${titulo}</h2>${headerExtra}</div><div class="card-body p-0">${bodyHtml}</div></section>`;

/**
 * Encabezado de vista con título a la izquierda y acciones a la derecha
 * @param {string} titulo
 * @param {string} [accionesHtml='']
 * @returns {string}
 */
export const encabezado = (titulo, accionesHtml = '') =>
  `<div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3"><h1 class="h4 mb-0">${titulo}</h1><div class="d-flex flex-wrap gap-2">${accionesHtml}</div></div>`;

/**
 * Tarjeta KPI para dashboard
 * @param {string} icono - Nombre icono Bootstrap (sin 'bi-')
 * @param {string} label - Etiqueta
 * @param {string} valor - Valor principal (grande)
 * @param {string} sub - Subtítulo/descripción
 * @param {boolean} [alerta=false] - Si true, usa color de alerta (amarillo)
 * @returns {string}
 */
export const kpi = (icono, label, valor, sub, alerta = false) =>
  `<div class="col-6 col-xl-3"><div class="card shadow-sm h-100"><div class="card-body d-flex gap-3 align-items-center"><div class="kpi-ico ${alerta ? 'alerta' : ''}"><i class="bi bi-${icono}"></i></div><div><div class="small text-body-secondary">${label}</div><div class="fs-3 fw-bold lh-1">${valor}</div><div class="small text-body-secondary mt-1">${sub}</div></div></div></div></div>`;

/**
 * Muestra toast notificación (Bootstrap Toast)
 * @param {string} mensaje
 * @param {string} [tipo='success'] - success, danger, warning, info
 */
export function toast(mensaje, tipo = 'success') {
  const contenedor = qs('#tc');
  const e = document.createElement('div');
  e.className = `toast align-items-center text-bg-${tipo} border-0`;
  e.innerHTML =
    '<div class="d-flex"><div class="toast-body"></div><button class="btn-close me-2 m-auto" data-bs-dismiss="toast" aria-label="Cerrar"></button></div>';
  e.querySelector('.toast-body').textContent = mensaje;
  contenedor.append(e);
  e.addEventListener('hidden.bs.toast', () => e.remove());
  new bootstrap.Toast(e, { delay: 4500 }).show();
}

/**
 * Referencia al modal Bootstrap (singleton)
 */
let modalBs = null;

/**
 * Abre modal dinámico con formulario y callback de validación
 * @param {string} titulo - Título del modal
 * @param {string} bodyHtml - HTML del body (debe contener <form> si hay callback ok)
 * @param {Function|null} ok - Callback(validación): recibe FormData, retorna string error o null/undefined si OK
 * @param {string} [labelBtn='Guardar'] - Texto botón principal
 */
export function modal(titulo, bodyHtml, ok, labelBtn = 'Guardar') {
  // Limpiar listeners previos
  qs('#mb').oninput = qs('#mb').onchange = null;

  qs('#mt').textContent = titulo;
  qs('#mb').innerHTML = bodyHtml + '<div id="err" class="alert alert-danger mt-3 d-none" role="alert"></div>';
  qs('#mf').innerHTML =
    (ok ? `<button class="btn btn-primary" id="mok">${labelBtn}</button>` : '') +
    '<button class="btn btn-outline-secondary" data-bs-dismiss="modal">Cerrar</button>';

  if (ok) {
    const form = qs('#mb form');
    form.onsubmit = (ev) => {
      ev.preventDefault();
      qs('#mok').click();
    };
    qs('#mok').onclick = () => {
      form.classList.add('was-validated');
      if (!form.checkValidity()) return;
      const error = ok(new FormData(form));
      if (error) {
        const alert = qs('#err');
        alert.textContent = error;
        alert.classList.remove('d-none');
        return;
      }
      modalBs.hide();
      renderizarVista();
    };
  }

  modalBs = modalBs || new bootstrap.Modal('#m');
  modalBs.show();
}

/**
 * Oculta el modal abierto si lo hay (usado al cerrar sesión)
 */
export function ocultarModal() {
  modalBs?.hide();
}

/**
 * Diálogo de confirmación temático (modal Bootstrap, hereda data-bs-theme).
 * Sustituye al confirm() nativo del navegador, que no sigue el tema de la app.
 * @param {string} titulo - Título del modal
 * @param {string} mensaje - Mensaje visible (se escapa con esc())
 * @param {Function} onOk - Callback a ejecutar al confirmar
 * @param {string} [labelBtn='Confirmar'] - Texto del botón principal
 */
export function confirmar(titulo, mensaje, onOk, labelBtn = 'Confirmar') {
  qs('#mb').oninput = qs('#mb').onchange = null;

  qs('#mt').textContent = titulo;
  qs('#mb').innerHTML = `<div class="alert alert-warning mb-0" role="alert">${esc(mensaje)}</div>`;
  qs('#mf').innerHTML =
    `<button class="btn btn-primary" id="mok">${labelBtn}</button>` +
    '<button class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>';

  qs('#mok').onclick = () => {
    modalBs.hide();
    onOk();
  };

  modalBs = modalBs || new bootstrap.Modal('#m');
  modalBs.show();
}