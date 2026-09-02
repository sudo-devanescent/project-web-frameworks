# HotelGest - Sistema de Gestión Hotelera

Sistema de gestión hotelera desarrollado como proyecto para el curso **Marcos de Desarrollo Web**. Es un prototipo de front-end: no tiene backend ni base de datos, y todos los datos que muestra están simulados directamente en el código.

---

## Tecnologías utilizadas

| Tecnología | Descripción |
|---|---|
| **HTML5** | Estructura de las vistas (parciales en `views/`) |
| **CSS3** | Estilos propios, modularizados por responsabilidad en `css/` |
| **JavaScript (módulos ES)** | Lógica de la aplicación, separada en módulos en `js/` |
| **Bootstrap 5.3.3** | Framework de front end (grid, componentes, utilidades) vía CDN |
| **Bootstrap Icons 1.11.3** | Iconografía para navbar, tarjetas y botones vía CDN |
| **pnpm** | Gestor de paquetes del proyecto (versión fijada en `packageManager`) |
| **serve** | Servidor estático usado para desarrollo local (devDependency) |

---

## Estructura del proyecto

```
.
├── README.md
├── css/                        # Hojas de estilo propias, modularizadas
│   ├── base.css                # Estilos globales
│   ├── components.css          # Componentes (badges, selectores)
│   ├── layout.css              # Estructura general (navbar, footer, vistas)
│   ├── main.css                # Archivo maestro que importa el resto
│   ├── utilities.css           # Clases utilitarias reutilizables
│   ├── variables.css           # Paleta de colores (variables CSS)
│   └── views/
│       └── dashboard.css       # Estilos del dashboard
├── index.html                  # Esqueleto principal (punto de entrada)
├── js/                         # Lógica de la aplicación en módulos ES
│   ├── auth.js                 # Login, cierre de sesión y roles
│   ├── data/                   # Datos simulados (reemplazables por API)
│   │   ├── habitaciones.js     # Catálogo de habitaciones
│   │   ├── huespedes.js        # Lista de huéspedes (2 registrados)
│   │   └── pagos.js            # Resumen de pagos
│   ├── filters.js               # Buscadores de las tablas
│   ├── forms.js                  # Validación y envío del formulario
│   ├── main.js                    # Punto de entrada del JS
│   ├── render.js                   # Puebla las tablas desde los datos
│   ├── router.js                    # Navegación entre vistas
│   └── views-loader.js               # Carga los HTML parciales
├── node_modules/                       # Generado por `pnpm install`, no versionado
├── package.json                        # Manifiesto del proyecto y script de arranque (`dev`)
├── pnpm-lock.yaml                       # Lockfile de pnpm
└── views/                                # HTML parciales (una por vista)
    ├── contenido-interno.html            # Vistas autenticadas (navbar + módulos)
    └── login.html                        # Pantalla de inicio de sesión
```

---

## Requisitos previos

- **Node.js** (versión compatible con pnpm 11.x; se recomienda una LTS reciente).
- **pnpm**, habilitado vía `corepack enable` o instalado globalmente con `npm install -g pnpm`. La versión exacta (`11.25.0`) queda fijada en el campo `packageManager` de `package.json`.
- Conexión a internet (los estilos e iconos de Bootstrap se cargan desde un CDN).

---

## Ejecución (desarrollo)

> **Importante:** este proyecto usa **módulos ES** (`type="module"`) y carga las
> vistas con `fetch()`, por lo que **no funciona abriendo `index.html` con doble
> clic** (el navegador bloquea ese acceso por la política CORS sobre `file://`).
> Debes servirlo con un servidor local.

El proyecto usa [`serve`](https://github.com/vercel/serve) como servidor estático de desarrollo, instalado como `devDependency` y fijado en `pnpm-lock.yaml`. No requiere ningún paso de build ni reestructuración de carpetas: sirve el proyecto tal cual está.

1. Instala las dependencias:

```bash
pnpm install
```

2. Levanta el servidor de desarrollo:

```bash
pnpm dev
```

3. Abre en el navegador: <http://localhost:8080>

**Nota:** `serve` no incluye recarga automática (no hay watch/HMR). Después de cada cambio en el código hay que refrescar el navegador manualmente.

---

## Cómo usar el prototipo

1. **Iniciar sesión**: escribe cualquier usuario y contraseña (la contraseña debe tener al menos 4 caracteres) y elige un rol.
2. **Roles simulados**: el administrador tiene acceso completo, incluidas las secciones de Huéspedes y Pagos; recepcionista y huésped no tienen acceso a esas dos secciones.
3. **Navegación**: el menú superior permite moverse entre el dashboard, habitaciones, nueva reserva, huéspedes (con datos de ejemplo y filtro en tiempo real) y pagos.
4. **Filtros**: escribir en los buscadores filtra las tablas en tiempo real.
5. **Nueva reserva**: al completar y guardar el formulario se muestra un mensaje de éxito simulado.
6. **Cerrar sesión**: mediante el botón **Salir**, en la esquina superior derecha.

---

## Datos del proyecto

| Aspecto | Detalle |
|---|---|
| Título | HotelGest - Sistema de Gestión Hotelera |
| Tipo | Prototipo de front end (SPA simulada) |
| Estado | Modularizado y listo para integrar backend |
| Frontend | HTML5 + CSS3 + JavaScript (ES Modules) |
| Framework UI | Bootstrap 5.3.3 (CDN) |
| Gestor de paquetes | pnpm 11.25.0 |
| Servidor de desarrollo | serve (devDependency, `pnpm dev`) |
| Huéspedes | 2 registrados (Juan Pérez, María García) |

---

## Notas sobre la arquitectura

La carga de vistas ocurre en `js/views-loader.js`, que inserta los parciales de `views/` dentro de los contenedores vacíos de `index.html` mediante `fetch()` e `innerHTML`. Los datos que alimentan las tablas de habitaciones, huéspedes y pagos se generan dinámicamente desde `js/data/`; para conectar un backend real bastaría con reemplazar el contenido de esos archivos, o mover las llamadas a la API dentro de `render.js`.

La autorización por rol, por su parte, es puramente visual: se implementa con la clase `elemento-solo-admin`, que oculta u muestra elementos según el rol elegido en el login, sin ningún control real detrás. En una versión definitiva, ese control debería resolverlo el backend (por ejemplo, con Spring Security).

