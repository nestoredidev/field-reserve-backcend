# Seed de datos de prueba

`seed.sql` carga datos **falsos** para desarrollo. Requiere que `bd.sql` ya esté aplicado y se ejecuta **una sola vez**.

| Usuario | Contraseña | Rol |
|---------|-----------|-----|
| `admin` | `Admin12345` | ADMIN |
| `encargado` | `Encargado123` | ENCARGADO |

Incluye: 2 canchas con tarifas, 4 clientes, reservas (completada, no asistió, cancelada, confirmadas, pendientes y un mantenimiento), cajas de cancha (una cerrada de ayer y una abierta hoy) con pagos y movimientos, 6 productos con kardex (un envase retornable y uno con stock bajo), 2 proveedores, una compra, 3 ventas y una caja de bodega abierta. Las fechas son relativas a hoy en hora de Lima.

## Cargarlo

Con el contenedor del `compose.yaml` encendido:

```bash
docker exec -i field-reserve-backend-postgres-1 psql -U postgres -d field_reserve -v ON_ERROR_STOP=1 -1 < seed/seed.sql
```

Con un Postgres propio: `psql -U postgres -d field_reserve -v ON_ERROR_STOP=1 -1 -f seed/seed.sql`.

## Empezar de cero

```bash
docker compose down -v      # borra el volumen; bd.sql se vuelve a aplicar al levantar
```
Luego vuelve a cargar el seed.
