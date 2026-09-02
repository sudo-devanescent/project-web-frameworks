/* ============================================================
   MÓDULO: CARGADOR DE VISTAS (HTML parciales)
   Carga dinámicamente los archivos HTML parciales ubicados en
   la carpeta /views dentro de los contenedores correspondientes
   del index.html. De esta forma, cada vista de la aplicación
   vive en su propio archivo, manteniendo el HTML ordenado.
   ============================================================= */

/**
 * Carga todas las vistas parciales en sus contenedores.
 * Se ejecuta al iniciar la aplicación. Usa "fetch" para obtener
 * cada archivo HTML y lo inserta con innerHTML.
 * @returns {Promise<void>} Suministra la finalización de la carga.
 */
export async function initViews() {
    // Definición de cada contenedor y el archivo que lo pobla
    const vistas = [
        { contenedor: 'vista-login',         archivo: 'views/login.html' },
        { contenedor: 'contenedor-interno',  archivo: 'views/contenido-interno.html' }
    ];

    // Recorre cada vista y carga su contenido de forma secuencial
    for (const { contenedor, archivo } of vistas) {
        const respuesta = await fetch(archivo);
        const html = await respuesta.text();
        document.getElementById(contenedor).innerHTML = html;
    }
}
