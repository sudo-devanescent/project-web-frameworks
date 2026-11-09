/**
 * @file api.js
 * @description Cliente HTTP de la API HostOps: sesión por cookie, CSRF,
 * JSON y errores ProblemDetail (RFC 7807) normalizados en ApiError.
 * @author HostOps Team
 * @version 1.0.0
 */

/**
 * Error normalizado de una petición a la API
 */
export class ApiError extends Error {
  /**
   * @param {number} status - Código HTTP (0 = fallo de red)
   * @param {string} title - Título del ProblemDetail
   * @param {string} detail - Detalle visible para el usuario
   * @param {Object|null} [errores] - Mapa campo → mensaje (validación 400)
   */
  constructor(status, title, detail, errores = null) {
    super(detail || title || 'Error de comunicación con el servidor.');
    this.name = 'ApiError';
    this.status = status;
    this.title = title;
    this.detail = detail;
    this.errores = errores;
  }
}

/**
 * Lee el valor de una cookie por nombre
 * @param {string} nombre - Nombre de la cookie
 * @returns {string|null}
 */
const leerCookie = (nombre) => {
  const prefijo = nombre + '=';
  const par = document.cookie.split('; ').find((c) => c.startsWith(prefijo));
  return par ? decodeURIComponent(par.slice(prefijo.length)) : null;
};

/**
 * Asegura la cookie CSRF (XSRF-TOKEN) llamando a GET /api/auth/csrf
 * @returns {Promise<void>}
 */
async function asegurarCsrf() {
  if (leerCookie('XSRF-TOKEN')) return;
  await fetch('/api/auth/csrf', {
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  });
}

/**
 * Ejecuta una petición a la API y devuelve el JSON (o null en 204)
 * @param {string} metodo - GET, POST, PUT, PATCH o DELETE
 * @param {string} ruta - Ruta relativa (ej. /api/habitaciones)
 * @param {any} [cuerpo] - Cuerpo JSON para métodos con payload
 * @returns {Promise<any>}
 * @throws {ApiError} Si la respuesta no es 2xx o falla la red
 */
async function pedir(metodo, ruta, cuerpo) {
  const esLectura = metodo === 'GET';
  if (!esLectura) await asegurarCsrf();

  const cabeceras = { Accept: 'application/json' };
  if (cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json';
  if (!esLectura) {
    const token = leerCookie('XSRF-TOKEN');
    if (token) cabeceras['X-XSRF-TOKEN'] = token;
  }

  let respuesta;
  try {
    respuesta = await fetch(ruta, {
      method: metodo,
      credentials: 'same-origin',
      headers: cabeceras,
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Sin conexión', 'No se pudo contactar con el servidor.');
  }

  if (respuesta.status === 204) return null;

  let datos = null;
  const texto = await respuesta.text();
  if (texto) {
    try {
      datos = JSON.parse(texto);
    } catch {
      /* respuesta sin cuerpo JSON */
    }
  }

  if (!respuesta.ok) {
    if (respuesta.status === 401 && !ruta.startsWith('/api/auth/login')) {
      window.dispatchEvent(new Event('sesion-expirada'));
    }
    throw new ApiError(
      respuesta.status,
      datos?.title || 'Error',
      datos?.detail || datos?.title || `Error ${respuesta.status}`,
      datos?.errores || null,
    );
  }
  return datos;
}

/**
 * Operaciones HTTP de la API HostOps
 */
export const api = {
  /** @param {string} ruta @returns {Promise<any>} */
  get: (ruta) => pedir('GET', ruta),
  /** @param {string} ruta @param {any} cuerpo @returns {Promise<any>} */
  post: (ruta, cuerpo) => pedir('POST', ruta, cuerpo),
  /** @param {string} ruta @param {any} cuerpo @returns {Promise<any>} */
  put: (ruta, cuerpo) => pedir('PUT', ruta, cuerpo),
  /** @param {string} ruta @param {any} cuerpo @returns {Promise<any>} */
  patch: (ruta, cuerpo) => pedir('PATCH', ruta, cuerpo),
  /** @param {string} ruta @returns {Promise<any>} */
  del: (ruta) => pedir('DELETE', ruta),
};

/**
 * Mensaje de usuario para un error (ApiError u otro)
 * @param {any} e - Error capturado
 * @returns {string}
 */
export const mensajeError = (e) => {
  if (e instanceof ApiError) {
    if (e.errores) {
      const primero = Object.values(e.errores)[0];
      if (primero) return primero;
    }
    return e.detail || e.title;
  }
  return 'Se produjo un error inesperado.';
};
