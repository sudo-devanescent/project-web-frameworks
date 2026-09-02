# HotelGest - Sistema de Gestión Hotelera (Prototipo)

Sistema de gestión hotelera desarrollado como proyecto final para la materia
**Marcos de Desarrollo Web**. Es un **prototipo de front end** que simula la
interfaz de un hotel: inicio de sesión con roles, dashboard con métricas,
listado de habitaciones, gestión de huéspedes, resumen de pagos y un
formulario de nueva reserva.

> **Prototipo de front end** · Sin backend · Sin base de datos ·
> Todos los datos son simulados/estáticos.

---

## Tecnologías utilizadas

| Tecnología | Descripción |
|---|---|
| **HTML5** | Estructura de las vistas (parciales en `views/`) |
| **CSS3** | Estilos propios, modularizados por responsabilidad en `css/` |
| **JavaScript (módulos ES)** | Lógica de la aplicación, separada en módulos en `js/` |
| **Bootstrap 5.3.3** | Framework de front end (grid, componentes, utilidades) vía CDN |
| **Bootstrap Icons 1.11.3** | Iconografía para navbar, tarjetas y botones vía CDN |

---

## Estructura del proyecto

```
prototype_bootstrap/
├── index.html                 # Esqueleto principal (punto de entrada)
├── css/                       # Hojas de estilo propias, modularizadas
│   ├── main.css               # Archivo maestro que importa el resto
│   ├── variables.css          # Paleta de colores (variables CSS)
│   ├── base.css               # Estilos globales
│   ├── utilities.css          # Clases utilitarias reutilizables
│   ├── components.css         # Componentes (badges, selectores)
│   ├── layout.css             # Estructura general (navbar, footer, vistas)
│   └── views/                 # Estilos específicos por vista
│       └── dashboard.css      # Estilos del dashboard
├── js/                        # Lógica de la aplicación en módulos ES
│   ├── main.js                # Punto de entrada del JS
│   ├── views-loader.js        # Carga los HTML parciales
│   ├── router.js              # Navegación entre vistas
│   ├── auth.js                # Login, cierre de sesión y roles
│   ├── filters.js             # Buscadores de las tablas
│   ├── forms.js               # Validación y envío del formulario
│   ├── render.js              # Puebla las tablas desde los datos
│   └── data/                  # Datos simulados (reemplazables por API)
│       ├── habitaciones.js    # Catálogo de habitaciones
│       ├── pagos.js           # Resumen de pagos
│       └── huespedes.js       # Lista de huéspedes
└── views/                     # HTML parciales (una por vista)
    ├── login.html             # Pantalla de inicio de sesión
    └── contenido-interno.html # Vistas autenticadas (navbar + módulos)
```

---

## Requisitos previos

- **Node.js** (v14 o superior) para levantar el servidor local.
- Conexión a internet (los estilos e iconos de Bootstrap se cargan desde un CDN).

---

## Ejecución (desarrollo)

> **Importante:** este proyecto usa **módulos ES** (`type="module"`) y carga las
> vistas con `fetch()`, por lo que **no funciona abriendo `index.html` con doble
> clic** (el navegador bloquea ese acceso por la política CORS sobre `file://`).
> Debes servirlo con un servidor local.

### Opción 1: `npx http-server` (recomendada, sin instalar nada)

Desde la raíz del proyecto:

```bash
npx http-server . -p 8080
```

Luego abre en el navegador: <http://localhost:8080>

### Opción 2: `npx serve`

```bash
npx serve -l 8080
```

Cualquiera de las dos descarga la herramienta de forma temporal y levanta un
servidor estático de una sola vez.

---

## Cómo usar el prototipo

1. **Iniciar sesión**: escribe cualquier usuario y contraseña (solo deben tener
   al menos 4 caracteres en la contraseña) y elige un **rol**.
2. **Roles simulados**:
   - **Administrador**: acceso completo (incluye Huéspedes y Pagos).
   - **Recepcionista** y **Huésped**: sin acceso a Huéspedes ni Pagos.
3. **Navegación**: usa el menú superior para moverse entre el dashboard,
   habitaciones, nueva reserva, huéspedes y pagos.
4. **Filtros**: escribe en los buscadores para filtrar las tablas en tiempo real.
5. **Nueva reserva**: completa el formulario; se muestra un mensaje de éxito
   simulado al guardar.
6. **Cerrar sesión**: botón **Salir** en la esquina superior derecha.

---

## Datos del proyecto

| Aspecto | Detalle |
|---|---|
| Título | HotelGest - Sistema de Gestión Hotelera |
| Tipo | Prototipo de front end (SPA simulada) |
| Estado | Modularizado y listo para integrar backend |
| Frontend | HTML5 + CSS3 + JavaScript (ES Modules) |
| Framework UI | Bootstrap 5.3.3 (CDN) |

---

## Notas sobre la arquitectura

- **Carga de vistas**: `js/views-loader.js` inserta los parciales de `views/`
  dentro de los contenedores vacíos del `index.html` mediante `fetch()` e
  `innerHTML`.
- **Datos**: las tablas de habitaciones y pagos se generan dinámicamente desde
  `js/data/`. Para conectar un backend real, solo habría que reemplazar el
  contenido de estos archivos (o hacer llamadas a la API en `render.js`).
- **Roles**: la autorización es solo visual (clase `elemento-solo-admin`).
  En la versión definitiva, este control lo haría el backend (p. ej. Spring
  Security).
