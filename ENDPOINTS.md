# API Campo Deportivo + Bodega — Endpoints

Base URL: `http://localhost:8080/api/v2`
Reglas de negocio: ver `reglas.md` (los códigos `RN-XXX-NN` aparecen en los mensajes de error).
Esquema de base de datos: `bd.sql` (el `compose.yaml` lo carga automáticamente en una base nueva).

## Índice

1. [Convenciones](#1-convenciones)
2. [Auth](#2-auth)
3. [Usuarios](#3-usuarios)
4. [Canchas y tarifas](#4-canchas-y-tarifas)
5. [Clientes](#5-clientes)
6. [Reservas, mantenimientos y pagos](#6-reservas-mantenimientos-y-pagos)
7. [Caja de cancha](#7-caja-de-cancha)
8. [Bodega: categorías](#8-bodega-categorías)
9. [Bodega: proveedores](#9-bodega-proveedores)
10. [Bodega: productos e inventario (kardex)](#10-bodega-productos-e-inventario-kardex)
11. [Bodega: compras](#11-bodega-compras)
12. [Bodega: ventas](#12-bodega-ventas)
13. [Caja de bodega](#13-caja-de-bodega)
14. [Reportes](#14-reportes)
15. [Proceso automático](#15-proceso-automático)
16. [Configuración y arranque](#16-configuración-y-arranque)

---

## 1. Convenciones

### Autenticación y roles
Todos los endpoints exigen `Authorization: Bearer <token>`, salvo `POST /auth/login`.
El usuario auditado (`registrado_por`, `creado_por`, ...) siempre sale del token (RN-G06).

| Rol | Puede |
|-----|-------|
| `ADMIN` | Todo. |
| `ENCARGADO` | Operar el día a día: reservas, cobros, ventas, compras, abrir/cerrar caja, clientes, proveedores (crear/editar). |

En cada endpoint, **[ADMIN]** indica que un ENCARGADO recibe `403`.

### Respuesta exitosa (`PageResponse`)
Un recurso o lista sin paginar:
```json
{ "code": 200, "message": "Cliente encontrado", "data": [ { "...": "..." } ] }
```
Un recurso único viene como lista de un elemento. Las listas paginadas añaden:
```json
{
  "code": 200, "message": "Reservas obtenidas", "data": [ ],
  "page": 0, "size": 10, "total_elements": 25, "total_pages": 3,
  "has_next": true, "has_previous": false
}
```
Sin resultados: `data: []` y `message: "No results found"`.
Las operaciones de borrado o sin contenido responden `204` sin cuerpo.

### Paginación (todos los listados paginados)
| Parámetro | Defecto | Regla |
|-----------|---------|-------|
| `page` | 0 | >= 0 |
| `size` | 10 | 1 – 100 |
| `sortBy` | (propio de cada listado) | uno de los campos permitidos del listado |
| `direction` | `asc` | `asc` o `desc` |

### Fechas, horas y dinero
- Zona horaria **America/Lima** (RN-G03). Las fechas de entrada son hora local sin zona (`2026-10-09T20:00:00`); las de salida llevan offset (`2026-10-09T20:00:00-05:00`).
- Filtros `desde` / `hasta`: fecha `YYYY-MM-DD`, ambas inclusivas.
- Montos en soles con 2 decimales (RN-G02).

### Errores (`ApiError`)
```json
{ "code": 422, "message": "RN-PAG-03: el monto supera el saldo pendiente (S/ 50.00)", "errors": [] }
```
| Código | Cuándo |
|--------|--------|
| 400 | Validación del cuerpo/parámetros (`errors` lista `campo: mensaje`), JSON mal formado, `sortBy` inválido |
| 401 | Sin token, token inválido o usuario inactivo |
| 403 | Rol insuficiente |
| 404 | Recurso inexistente |
| 409 | Conflicto: duplicado, cruce de horarios (`errors` lista los choques), caja ya abierta |
| 422 | Regla de negocio incumplida |

---

## 2. Auth

### POST `/auth/login`
Público.
```json
{ "username": "admin", "password": "Admin12345" }
```
Respuesta `200`:
```json
{ "code": 200, "message": "Login exitoso", "data": [ {
  "token": "eyJ...", "tokenType": "Bearer", "expiresInSeconds": 28800,
  "usuario": { "id": 1, "nombre": "Dueño", "username": "admin", "rol": "ADMIN", "activo": true, "createdAt": "..." }
} ] }
```
Errores: `400` campos vacíos · `401` credenciales incorrectas o usuario inactivo (RN-USU-03).

### GET `/auth/me`
Devuelve el usuario autenticado.

---

## 3. Usuarios  **[ADMIN]**

Reglas: RN-USU-01..04. La API nunca devuelve el hash de la contraseña.

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/usuarios` | Crear usuario (`201`) |
| GET | `/usuarios` | Listar (paginado) · `sortBy`: `id, nombre, username, rol, activo, createdAt` (defecto `username`) |
| GET | `/usuarios/{id}` | Obtener |
| PUT | `/usuarios/{id}` | Cambiar `nombre` y `rol` |
| PUT | `/usuarios/{id}/password` | Cambiar contraseña (`204`) |
| PATCH | `/usuarios/{id}/activar` | Activar |
| PATCH | `/usuarios/{id}/desactivar` | Desactivar (nunca se elimina) |

**POST `/usuarios`**
```json
{ "nombre": "María", "username": "maria", "password": "maria12345", "rol": "ENCARGADO" }
```
| Campo | Validación |
|-------|-----------|
| nombre | obligatorio, máx. 100 |
| username | obligatorio, `^[a-z0-9._-]{4,50}$` (minúsculas, sin espacios), único (409) |
| password | obligatorio, 8 – 72 caracteres |
| rol | `ADMIN` o `ENCARGADO` |

**PUT `/usuarios/{id}`** → `{ "nombre": "...", "rol": "ADMIN|ENCARGADO" }`
**PUT `/usuarios/{id}/password`** → `{ "password": "nueva12345" }`

`422` (RN-USU-04): un usuario no puede desactivarse ni quitarse el rol ADMIN a sí mismo, y siempre debe quedar un ADMIN activo.

---

## 4. Canchas y tarifas

Las tarifas definen precio **y** horario de atención (RN-TAR-02). Solo el ADMIN escribe.

### Canchas
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| POST | `/canchas` | ADMIN | Crear (`201`) |
| GET | `/canchas?activa=true` | todos | Listar (paginado) · `sortBy`: `id, nombre, activa, createdAt` (defecto `nombre`) |
| GET | `/canchas/{id}` | todos | Obtener |
| PUT | `/canchas/{id}` | ADMIN | Actualizar |
| PATCH | `/canchas/{id}/activar` | ADMIN | Activar |
| PATCH | `/canchas/{id}/desactivar` | ADMIN | Desactivar |

**POST / PUT `/canchas`**
```json
{ "nombre": "Cancha 2", "descripcion": "Cemento" }
```
`nombre` obligatorio, máx. 80, único sin distinguir mayúsculas (RN-CAN-01, 409). `descripcion` máx. 255.
Desactivar falla con `422` si hay reservas/mantenimientos futuros PENDIENTE o CONFIRMADA (RN-CAN-03).

### Tarifas
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| POST | `/canchas/{canchaId}/tarifas` | ADMIN | Crear franja (`201`) |
| GET | `/canchas/{canchaId}/tarifas?soloActivas=true` | todos | Listar |
| GET | `/tarifas/{id}` | todos | Obtener |
| PATCH | `/tarifas/{id}/desactivar` | ADMIN | Desactivar |
| POST | `/tarifas/{id}/cambiar-precio` | ADMIN | Desactiva la tarifa y crea una nueva con otro precio (RN-TAR-06) |

**POST `/canchas/{canchaId}/tarifas`**
```json
{ "diaSemana": null, "horaInicio": "08:00", "horaFin": "17:00", "precioHora": 30.00 }
```
| Campo | Validación |
|-------|-----------|
| diaSemana | opcional, 1 (lunes) – 7 (domingo); `null` = todos los días. La tarifa de un día específico gana sobre la general (RN-TAR-04) |
| horaInicio / horaFin | obligatorias, `HH:mm`, `horaFin` > `horaInicio` |
| precioHora | obligatorio, > 0 (RN-TAR-03) |

`409` si la franja se cruza con otra activa de la misma cancha y día (RN-TAR-01).
**POST `/tarifas/{id}/cambiar-precio`** → `{ "precioHora": 35.00 }`. Las reservas existentes conservan su monto (RN-TAR-05).

---

## 5. Clientes

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/clientes` | Crear (`201`) |
| GET | `/clientes?q=` | Listar (paginado), `q` busca por nombre o teléfono · `sortBy`: `id, nombre, telefono, createdAt` (defecto `nombre`) |
| GET | `/clientes/{id}` | Obtener |
| GET | `/clientes/telefono/{telefono}` | Buscar por teléfono |
| PUT | `/clientes/{id}` | Actualizar |

```json
{ "nombre": "Luis Pérez", "telefono": "912345678", "notas": "Equipo Los Halcones" }
```
| Campo | Validación |
|-------|-----------|
| nombre | obligatorio, máx. 100 |
| telefono | obligatorio, celular peruano `^9\d{8}$`, único (409, RN-CLI-02) |
| notas | opcional, máx. 255 |

No existe borrado de clientes (RN-CLI-03).

---

## 6. Reservas, mantenimientos y pagos

Reservas y bloqueos de mantenimiento comparten la misma tabla, por eso un horario nunca se cruza. Estados: `PENDIENTE → CONFIRMADA → COMPLETADA`, y `CANCELADA` / `NO_ASISTIO` (finales, RN-RES-12).

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| POST | `/reservas` | todos | Crear reserva (`201`), opcionalmente con el primer pago |
| GET | `/reservas` | todos | Listar con filtros (paginado) |
| GET | `/reservas/{id}` | todos | Detalle con `pagado` y `saldo` |
| GET | `/reservas/disponibilidad?canchaId=&fecha=` | todos | Horas del día con precio y si están libres |
| PUT | `/reservas/{id}/reprogramar` | todos | Cambia la hora (RN-RES-17) |
| POST | `/reservas/{id}/cancelar` | todos* | Cancela con motivo |
| POST | `/reservas/{id}/no-asistio` | todos | Marca NO_ASISTIO (RN-RES-18) |
| POST | `/reservas/{id}/completar` | todos | Marca COMPLETADA (RN-RES-19) |
| PATCH | `/reservas/{id}/monto` | ADMIN | Cambia el monto (descuento) |
| POST | `/reservas/{id}/pagos` | todos** | Registra un pago |
| GET | `/reservas/{id}/pagos` | todos | Lista los pagos |
| POST | `/mantenimientos` | ADMIN | Bloquea un horario por mantenimiento (`201`) |

\* Cancelar un MANTENIMIENTO es solo ADMIN (RN-RES-23). \** Las devoluciones son solo ADMIN (RN-PAG-07).

### POST `/reservas`
```json
{
  "canchaId": 1,
  "clienteId": null,
  "clienteNombre": "Juan", "clienteTelefono": "987654321",
  "inicio": "2026-10-09T20:00:00", "fin": "2026-10-09T22:00:00",
  "origen": "WHATSAPP",
  "observacion": "Cumpleaños",
  "pagoInicial": { "metodo": "YAPE", "monto": 80.00, "nroOperacion": "123456" }
}
```
| Campo | Validación |
|-------|-----------|
| canchaId | obligatorio; la cancha debe estar activa (RN-CAN-02) |
| clienteId **o** clienteNombre + clienteTelefono | uno de los dos; con teléfono se reutiliza el cliente existente (RN-CLI-02) |
| inicio / fin | obligatorios, hora local de Lima |
| origen | `PRESENCIAL` (defecto), `WHATSAPP`, `LLAMADA`, `WEB` |
| observacion | máx. 255 |
| pagoInicial | opcional, mismo formato que un pago; si alcanza el adelanto mínimo la reserva nace `CONFIRMADA` (RN-RES-11) |

Reglas del horario (`422`): hora en punto y bloques de 1 h completas (RN-RES-01) · dentro del horario de las tarifas y en el mismo día (RN-RES-02) · no en el pasado (RN-RES-03) · máximo 30 días de anticipación (RN-RES-04). Si se cruza con otra ocupación → `409` con la lista de choques (RN-RES-06); las reservas contiguas sí se permiten.

El **monto** lo calcula el backend sumando hora por hora el precio de cada franja (RN-RES-08). Respuesta:
```json
{ "code": 201, "message": "Reserva creada", "data": [ {
  "id": 5, "canchaId": 1, "clienteId": 2, "clienteNombre": "Juan", "clienteTelefono": "987654321",
  "tipo": "RESERVA", "estado": "CONFIRMADA",
  "inicio": "2026-10-09T20:00:00-05:00", "fin": "2026-10-09T22:00:00-05:00",
  "montoTotal": 160.00, "pagado": 80.00, "saldo": 80.00, "adelantoMinimo": 80.00,
  "fechaLimiteAdelanto": "2026-10-09T00:08:10-05:00", "origen": "WHATSAPP",
  "observacion": "Cumpleaños", "motivoCancelacion": null, "creadoPor": 1, "createdAt": "...",
  "advertencias": null, "devolucionSugerida": null
} ] }
```
- `adelantoMinimo` = 50 % del monto (configurable, RN-RES-13). `fechaLimiteAdelanto` = creación + 2 h sin pasar del inicio (RN-RES-14).
- `advertencias`: p. ej. RN-CLI-04 cuando el cliente tiene 2 o más inasistencias.

### GET `/reservas`
Filtros opcionales: `canchaId`, `clienteId`, `estado` (`PENDIENTE|CONFIRMADA|COMPLETADA|CANCELADA|NO_ASISTIO`), `tipo` (`RESERVA|MANTENIMIENTO`), `desde`, `hasta` (sobre la fecha de inicio).
`sortBy`: `id, inicio, fin, estado, montoTotal, createdAt` (defecto `inicio`).

### GET `/reservas/disponibilidad?canchaId=1&fecha=2026-10-12`
```json
{ "data": [ { "horaInicio": "08:00:00", "horaFin": "09:00:00", "precioHora": 30.00, "disponible": true } ] }
```
Solo aparecen las horas dentro del horario de atención. `disponible=false` si está ocupada, es una hora pasada o la cancha está inactiva.

### PUT `/reservas/{id}/reprogramar`
```json
{ "inicio": "2026-10-10T10:00:00", "fin": "2026-10-10T11:00:00" }
```
Aplica las mismas reglas de horario y de cruce. Recalcula el monto; los pagos se conservan. Si lo pagado supera el nuevo monto, el sistema registra automáticamente una `DEVOLUCION` en efectivo por la diferencia desde la caja abierta (RN-RES-17).

### POST `/reservas/{id}/cancelar`
```json
{ "motivo": "El cliente avisó" }
```
Solo PENDIENTE o CONFIRMADA; `motivo` obligatorio (máx. 255). La respuesta incluye `devolucionSugerida`: lo pagado si faltan 24 h o más para el inicio, `0` si faltan menos (RN-RES-16). La devolución del dinero la registra el ADMIN con un pago de tipo devolución.

### POST `/reservas/{id}/no-asistio` · POST `/reservas/{id}/completar`
Sin cuerpo.
- `no-asistio`: solo CONFIRMADA y pasados 15 min desde el inicio; el adelanto no se devuelve.
- `completar`: solo CONFIRMADA, saldo S/ 0 y la hora ya empezó. Nunca con deuda. (También ocurre automáticamente al pagar el saldo si la hora ya empezó, RN-PAG-08.)

### PATCH `/reservas/{id}/monto`  **[ADMIN]**
```json
{ "monto": 120.00, "observacion": "Cliente frecuente" }
```
`observacion` obligatoria (RN-RES-10). El nuevo monto no puede ser menor a lo ya pagado.

### POST `/reservas/{id}/pagos`
```json
{ "metodo": "YAPE", "monto": 40.00, "nroOperacion": "A1B2C3", "voucherUrl": "https://...", "devolucion": false }
```
| Campo | Validación |
|-------|-----------|
| metodo | `EFECTIVO`, `YAPE`, `PLIN`, `TRANSFERENCIA` |
| monto | > 0; nunca supera el saldo (RN-PAG-03) |
| nroOperacion | obligatorio si no es efectivo, máx. 30; no se acepta repetido con el mismo método (409, RN-PAG-05) |
| voucherUrl | opcional, máx. 500 |
| devolucion | `true` = devolución (solo ADMIN) |

- Requiere **caja de cancha abierta** (RN-PAG-01); si no, `422`.
- El `tipo` lo decide el sistema: `SALDO` si deja la deuda en 0, si no `ADELANTO` (RN-PAG-04).
- Solo reservas tipo RESERVA en estado PENDIENTE o CONFIRMADA (RN-PAG-02).
- Tras cada pago se reevalúa el estado: CONFIRMADA al alcanzar el adelanto mínimo; COMPLETADA según RN-RES-19 (RN-PAG-08).
- **Devolución** `[ADMIN]`: solo en reservas CANCELADAS o con pago en exceso, no puede superar lo pagado, y en efectivo no puede superar el efectivo de la caja (RN-PAG-07).
- Los pagos no se editan ni se borran (RN-PAG-06).

### POST `/mantenimientos`  **[ADMIN]**
```json
{ "canchaId": 1, "inicio": "2026-10-12T10:00:00", "fin": "2026-10-12T12:00:00", "motivo": "Pintar líneas" }
```
Nace `CONFIRMADA`, sin cliente y con monto 0; el motivo se guarda en `observacion` (RN-RES-20/21). Si choca con reservas, responde `409` y `errors` lista cada reserva con nombre y teléfono del cliente (RN-RES-22); nunca las pisa. Para liberarlo: `POST /reservas/{id}/cancelar` (ADMIN).

---

## 7. Caja de cancha

Una sola caja abierta a la vez (RN-CAJ-01). `efectivoEsperado` y `diferencia` solo se muestran al ADMIN; para el ENCARGADO vienen en `null`.

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/cancha/cajas/abrir` | Abrir caja (`201`) · `409` si ya hay una abierta |
| GET | `/cancha/cajas/actual` | Caja abierta con totales · `404` si no hay |
| POST | `/cancha/cajas/cerrar` | Cerrar la caja abierta |
| GET | `/cancha/cajas` | Listar (paginado) · `sortBy`: `id, fecha, estado, abiertaAt` (defecto `abiertaAt`) |
| GET | `/cancha/cajas/{id}` | Detalle de una caja |
| POST | `/cancha/cajas/movimientos` | Ingreso/egreso manual en la caja abierta (`201`) |
| GET | `/cancha/cajas/{id}/movimientos` | Movimientos de una caja (paginado) · `sortBy`: `id, fecha, tipo, monto` (defecto `fecha`) |

**Abrir** → `{ "montoApertura": 100.00 }` (>= 0, puede ser 0).
**Cerrar** → `{ "montoCierreContado": 250.00, "observacion": "Faltan S/ 5" }`. Si el contado difiere del esperado, `observacion` es obligatoria (RN-CAJ-04). Una caja cerrada no se reabre (RN-CAJ-05).

Respuesta de caja:
```json
{ "id": 1, "fecha": "2026-10-08", "estado": "ABIERTA", "montoApertura": 100.00, "montoCierreContado": null,
  "observacion": null, "abiertaPor": 2, "cerradaPor": null, "abiertaAt": "...", "cerradaAt": null,
  "cobrosEfectivo": 70.00, "cobrosDigitales": 10.00, "otrosIngresosEfectivo": 0.00, "egresosEfectivo": 20.00,
  "efectivoEsperado": 150.00, "diferencia": null, "advertencia": null }
```
`efectivoEsperado` = apertura + cobros en efectivo + ingresos en efectivo − egresos en efectivo − devoluciones en efectivo (RN-CAJ-03). `advertencia` avisa si la caja abierta es de un día anterior (RN-CAJ-06).

**Movimiento** → `{ "tipo": "EGRESO", "metodo": "EFECTIVO", "concepto": "Pago de luz", "monto": 20.00 }`
`tipo`: `INGRESO|EGRESO` · `concepto` obligatorio (máx. 150) · `monto` > 0. Un egreso en efectivo no puede superar el efectivo disponible (RN-MCA-03). Nunca se usa para mercadería de la bodega (RN-MCA-04).

---

## 8. Bodega: categorías

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| POST | `/bodega/categorias` | ADMIN | Crear (`201`) |
| GET | `/bodega/categorias` | todos | Listar · `sortBy`: `id, nombre` (defecto `nombre`) |
| GET | `/bodega/categorias/{id}` | todos | Obtener |
| PUT | `/bodega/categorias/{id}` | ADMIN | Renombrar |
| DELETE | `/bodega/categorias/{id}` | ADMIN | Eliminar (`204`) · `422` si tiene productos (RN-CAT-02) |

Cuerpo: `{ "nombre": "Cerveza" }` — obligatorio, máx. 60, único sin distinguir mayúsculas (409).

---

## 9. Bodega: proveedores

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| POST | `/bodega/proveedores` | todos | Crear (`201`) |
| GET | `/bodega/proveedores?q=&activo=` | todos | Listar, `q` busca por nombre o RUC · `sortBy`: `id, nombre, ruc, activo, createdAt` (defecto `nombre`) |
| GET | `/bodega/proveedores/{id}` | todos | Obtener |
| PUT | `/bodega/proveedores/{id}` | todos | Actualizar |
| PATCH | `/bodega/proveedores/{id}/activar` | ADMIN | Activar |
| PATCH | `/bodega/proveedores/{id}/desactivar` | ADMIN | Desactivar (RN-PRV-02, no se elimina) |

```json
{ "nombre": "Backus", "ruc": "20100000001", "telefono": "014000000" }
```
`nombre` obligatorio (máx. 120) · `ruc` opcional, 11 dígitos y único (409) · `telefono` opcional (máx. 15).

---

## 10. Bodega: productos e inventario (kardex)

El stock **nunca se escribe a mano**: solo cambia por compras, ventas, anulaciones y ajustes, y cada cambio deja un movimiento en el kardex (RN-PRO-02).

### Productos
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| POST | `/bodega/productos` | ADMIN | Crear (`201`) |
| GET | `/bodega/productos` | todos | Listar con filtros (paginado) |
| GET | `/bodega/productos/stock-bajo` | todos | Reposición: activos con stock <= stock mínimo (RN-PRO-10) |
| GET | `/bodega/productos/codigo-barras/{codigo}` | todos | Buscar por código de barras (escaneo) |
| GET | `/bodega/productos/{id}` | todos | Obtener |
| PUT | `/bodega/productos/{id}` | ADMIN | Actualizar (no cambia el stock) |
| PATCH | `/bodega/productos/{id}/activar` | ADMIN | Activar |
| PATCH | `/bodega/productos/{id}/desactivar` | ADMIN | Desactivar (RN-PRO-09) |

Filtros de `GET /bodega/productos`: `q` (nombre o código), `categoriaId`, `tipo` (`PRODUCTO|ENVASE`), `activo`. `sortBy`: `id, nombre, codigoBarras, precioVenta, stock, createdAt` (defecto `nombre`).

**POST / PUT `/bodega/productos`**
```json
{
  "codigoBarras": "7750001", "nombre": "Cerveza Cusqueña 620ml", "categoriaId": 1, "tipo": "PRODUCTO",
  "precioCompra": 5.00, "precioVenta": 7.00, "stockMinimo": 6, "unidadesPorCaja": 12,
  "envaseId": 1, "stockInicial": 24
}
```
| Campo | Validación |
|-------|-----------|
| nombre | obligatorio, máx. 120 |
| categoriaId | obligatorio, debe existir |
| codigoBarras | opcional, máx. 30, único si existe (409) |
| tipo | `PRODUCTO` (defecto) o `ENVASE` |
| precioCompra / precioVenta | >= 0. Si venta < compra se responde con una `advertencias` (RN-PRO-04), no se bloquea |
| stockMinimo | >= 0 (defecto 0) |
| unidadesPorCaja | >= 1 (defecto 1, RN-PRO-07) |
| envaseId | opcional; debe apuntar a un producto de tipo `ENVASE`; un ENVASE no puede tener envase (RN-PRO-08) |
| stockInicial | solo al crear; si es > 0 se registra como `AJUSTE_POSITIVO` "Inventario inicial" (RN-PRO-03) |

Para un ENVASE, `precioVenta` es el valor de la garantía. Respuesta:
```json
{ "id": 2, "codigoBarras": "7750001", "nombre": "...", "categoriaId": 1, "tipo": "PRODUCTO",
  "precioCompra": 5.00, "precioVenta": 7.00, "stock": 24, "stockMinimo": 6, "unidadesPorCaja": 12,
  "envaseId": 1, "activo": true, "stockBajo": false, "advertencias": null, "createdAt": "..." }
```

### Kardex
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| POST | `/bodega/inventario/ajustes` | ADMIN | Ajuste manual (`201`) |
| GET | `/bodega/inventario/movimientos?productoId=&tipo=` | todos | Historial (paginado) · `sortBy`: `id, fecha, tipo, cantidad` (defecto `fecha`) |

**POST `/bodega/inventario/ajustes`**
```json
{ "productoId": 3, "tipo": "MERMA", "cantidad": 2, "motivo": "Botella rota" }
```
`tipo` manual: `AJUSTE_POSITIVO`, `AJUSTE_NEGATIVO`, `MERMA`, `DEVOLUCION_ENVASE` (suma), `SALIDA_ENVASE` (resta). Los demás tipos (`ENTRADA_COMPRA`, `SALIDA_VENTA`, `ANULACION_*`) los generan las compras y ventas. `cantidad` entero >= 1; `motivo` obligatorio (máx. 255). El stock no puede quedar negativo (`422`).

Movimiento: `{ id, productoId, tipo, cantidad, stockAnterior, stockResultante, compraId, ventaId, motivo, registradoPor, fecha }`.

---

## 11. Bodega: compras

Una compra suma stock (kardex `ENTRADA_COMPRA`) y actualiza el precio de compra del producto con el costo de esa compra (RN-PRO-06). Todo en una sola transacción (RN-G08). No se edita: solo se anula.

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| POST | `/bodega/compras` | todos | Registrar (`201`) |
| GET | `/bodega/compras` | todos | Listar (paginado) · filtros `proveedorId`, `estado` (`REGISTRADA|ANULADA`), `desde`, `hasta` · `sortBy`: `id, fecha, total, estado` (defecto `fecha`) |
| GET | `/bodega/compras/{id}` | todos | Detalle con líneas |
| POST | `/bodega/compras/{id}/anular` | ADMIN | Anular con motivo |

**POST `/bodega/compras`**
```json
{
  "proveedorId": 1, "nroComprobante": "F001-123", "pagadoDesdeCaja": true,
  "detalles": [ { "productoId": 3, "cantidadCajas": 2, "cantidadSueltas": 3, "costoUnitario": 2.20 } ]
}
```
| Campo | Validación |
|-------|-----------|
| proveedorId | opcional; si se envía debe existir y estar activo |
| nroComprobante | opcional, máx. 30 |
| pagadoDesdeCaja | `true` registra un EGRESO en efectivo "Compra #id" en la caja de bodega abierta (RN-COM-05) |
| detalles | al menos uno, sin productos repetidos (RN-COM-01) |
| cantidadCajas / cantidadSueltas | >= 0; el backend calcula `unidades = cajas × unidades_por_caja + sueltas` (RN-COM-02), que debe ser > 0 |
| costoUnitario | >= 0, por unidad |

El **total** lo calcula el backend (RN-COM-03). Los productos deben estar activos.
**Anular** → `{ "motivo": "..." }`: resta el stock (`ANULACION_COMPRA`); si ya se vendió parte de esa mercadería y el stock no alcanza, responde `422` (RN-COM-06). El motivo queda en el kardex. El egreso de caja no se revierte automáticamente: se corrige con un ingreso manual.

---

## 12. Bodega: ventas

Venta + stock + kardex + pagos se guardan completos o no se guardan (RN-G08). Requiere **caja de bodega abierta**. No se editan: solo se anulan.

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| POST | `/bodega/ventas` | todos | Registrar (`201`) |
| GET | `/bodega/ventas` | todos | Listar (paginado, sin líneas) · filtros `cajaId`, `estado` (`REGISTRADA|ANULADA`), `desde`, `hasta` · `sortBy`: `id, fecha, total, estado` (defecto `fecha`) |
| GET | `/bodega/ventas/{id}` | todos | Detalle con líneas y pagos |
| POST | `/bodega/ventas/{id}/anular` | ADMIN | Anular con motivo |

**POST `/bodega/ventas`**
```json
{
  "detalles": [
    { "productoId": 2, "cantidad": 2, "envaseEntregado": false },
    { "productoId": 3, "cantidad": 1 }
  ],
  "pagos": [
    { "metodo": "EFECTIVO", "monto": 5.00 },
    { "metodo": "YAPE", "monto": 11.50, "nroOperacion": "Y123" }
  ]
}
```
| Campo | Validación |
|-------|-----------|
| detalles | al menos uno, sin productos repetidos (RN-DVE-01); `cantidad` entero 1 – 100000 |
| precio / costo | **no se envían**: salen del producto (RN-DVE-03) |
| envaseEntregado | obligatorio si el producto es retornable (tiene `envaseId`, RN-DVE-05). `false` agrega una línea con el envase para cobrar la garantía; `true` devuelve el envase vacío al stock (`DEVOLUCION_ENVASE`) |
| pagos | al menos uno; la suma debe ser **exactamente** igual al total (RN-VEN-03, sin fiado); cada monto > 0 |
| nroOperacion | obligatorio en Yape, Plin y transferencia; no se repite con el mismo método (409, RN-PVE-03) |

Todos los productos deben estar activos y con stock suficiente; si una línea no alcanza no se guarda ninguna (RN-DVE-02). `costoUnitario` en el detalle solo lo ve el ADMIN. El vuelto no se guarda (RN-PVE-02).

**Anular** `[ADMIN]` → `{ "motivo": "Error de cobro" }`: devuelve el stock (`ANULACION_VENTA`) y revierte los envases recibidos. Solo mientras la caja de la venta siga abierta; si ya está cerrada responde `422` (RN-VEN-04/05): el reembolso se registra como EGRESO en la caja actual y el producto devuelto como `AJUSTE_POSITIVO`. Los pagos de una venta anulada dejan de contar en la caja.

---

## 13. Caja de bodega

Idéntica a la caja de cancha pero independiente (RN-G01: no comparten dinero). Base: `/bodega/cajas`.

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/bodega/cajas/abrir` | Abrir caja (`201`) |
| GET | `/bodega/cajas/actual` | Caja abierta con totales |
| POST | `/bodega/cajas/cerrar` | Cerrar la caja abierta |
| GET | `/bodega/cajas` | Listar (paginado) |
| GET | `/bodega/cajas/{id}` | Detalle |
| POST | `/bodega/cajas/movimientos` | Ingreso/egreso manual |
| GET | `/bodega/cajas/{id}/movimientos` | Movimientos de una caja |

Cuerpos, validaciones y respuesta: ver [sección 7](#7-caja-de-cancha). En esta caja, `cobrosEfectivo` y `cobrosDigitales` corresponden a las **ventas** no anuladas. El `MovimientoBodegaDto` tiene la misma forma que el de cancha.

---

## 14. Reportes  **[ADMIN]**

Son de solo lectura y los únicos que muestran cancha y bodega lado a lado (RN-G01). `desde` / `hasta` son opcionales (defecto: hoy).

### GET `/reportes/ganancia-productos?desde=2026-10-01&hasta=2026-10-31`
Ventas no anuladas, ordenadas por ganancia.
```json
{ "data": [ { "productoId": 2, "nombre": "Cerveza", "unidadesVendidas": 3, "totalVendido": 12.00, "costoTotal": 15.00, "ganancia": -3.00 } ] }
```

### GET `/reportes/resumen-general?desde=2026-10-08&hasta=2026-10-08`
```json
{ "data": [ {
  "desde": "2026-10-08", "hasta": "2026-10-08",
  "cancha": { "cobrosEfectivo": 70.00, "cobrosDigitales": 10.00, "otrosIngresos": 0.00, "egresos": 20.00, "reservasCompletadas": 0 },
  "bodega": { "ventasTotal": 12.00, "ventasRegistradas": 1, "gananciaBruta": -3.00, "comprasTotal": 0.00, "otrosIngresos": 0.00, "egresos": 0.00 }
} ] }
```
Los cobros de cancha se netean con las devoluciones.

---

## 15. Proceso automático

Cada 10 minutos (el primer ciclo, 1 minuto después de arrancar), el sistema cancela las reservas `PENDIENTE` cuya `fechaLimiteAdelanto` ya pasó, con el motivo **"Adelanto no pagado"** (RN-RES-14). No es un endpoint.

---

## 16. Configuración y arranque

`src/main/resources/application.yaml`:

| Propiedad | Variable de entorno | Defecto |
|-----------|--------------------|---------|
| `app.jwt.secret` | `JWT_SECRET` | secreto de desarrollo (**cámbialo en producción**, mínimo 32 caracteres) |
| `app.jwt.expiration-seconds` | — | 28800 (8 h) |
| `app.bootstrap.admin-password` | `ADMIN_PASSWORD` | `Admin12345` |
| `app.reserva.adelanto-porcentaje` | — | 0.50 |

- **Base de datos**: PostgreSQL con el esquema de `bd.sql` (`ddl-auto: none`). El `compose.yaml` lo ejecuta al crear el contenedor por primera vez. Si usas una base existente, aplica `bd.sql` antes de arrancar.
- **Usuario inicial**: `bd.sql` inserta `admin` con un hash de relleno. Al arrancar, la app lo reemplaza por el BCrypt de `app.bootstrap.admin-password`. Inicia sesión como `admin` y cambia esa contraseña con `PUT /usuarios/{id}/password`.
- **Arranque**: `./gradlew bootRun`.
