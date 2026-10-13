Reglas de negocio — Sistema Campo Deportivo + Bodega
8 oct 2026 · @Nestor
Reglas generales
Estas reglas aplican a todas las tablas. Cada regla tiene un código para citarla en el código fuente, los tests y los mensajes de error.
• RN-G01 Separación de negocios. Cancha y bodega no comparten dinero. Ninguna operación de un módulo modifica tablas del otro. Solo un reporte general de lectura puede mostrar ambos totales lado a lado.
• RN-G02 Dinero. Todo monto está en soles con 2 decimales. En Java se usa BigDecimal, nunca double.
• RN-G03 Zona horaria. Todas las fechas se interpretan en America/Lima y se guardan como TIMESTAMPTZ.
• RN-G04 Nada se borra. Reservas, pagos, ventas, compras, cajas y movimientos no se eliminan ni se editan: se cancelan o anulan con motivo obligatorio. Los catálogos (canchas, productos, clientes, proveedores, usuarios) se desactivan.
• RN-G05 Caja abierta. Toda operación que mueve dinero exige una caja abierta del módulo correspondiente y se asocia a ella automáticamente.
• RN-G06 Auditoría. El campo registrado_por o creado_por siempre es el usuario autenticado, tomado del JWT. Nunca se acepta desde el frontend.
• RN-G07 Cálculos en el backend. Precios, totales, saldos y stock los calcula el backend. Lo que envíe el frontend en esos campos se ignora.
• RN-G08 Transacciones. Una operación que toca varias tablas (venta + stock + kardex + pagos) se guarda completa o no se guarda (@Transactional).
core.usuario
Hay dos roles: el ADMIN (el dueño) controla precios, anulaciones y reportes; el ENCARGADO solo opera el día a día.
Acción
ADMIN
ENCARGADO
Crear y reprogramar reservas, cobrar, vender
Sí
Sí
Abrir y cerrar caja
Sí
Sí
Cancelar reservas
Sí
Sí, con motivo
Bloqueos de mantenimiento
Sí
No
Cambiar tarifas y precios de productos
Sí
No
Anular ventas o compras, devoluciones de dinero
Sí
No
Ajustes de inventario y mermas
Sí
No
Reportes de ganancia y diferencias de caja
Sí
No
Gestionar usuarios
Sí
No
• RN-USU-01 El username es único, de 4 a 50 caracteres, en minúsculas y sin espacios.
• RN-USU-02 La contraseña tiene mínimo 8 caracteres y se guarda solo como hash BCrypt. La API nunca devuelve el hash.
• RN-USU-03 Un usuario no se elimina, se desactiva. Un usuario inactivo no puede iniciar sesión y sus tokens dejan de ser válidos.
• RN-USU-04 Siempre debe existir al menos un ADMIN activo. Un usuario no puede desactivarse ni quitarse el rol ADMIN a sí mismo.
Módulo Cancha: cancha, tarifa y cliente
Las tarifas definen tanto el precio como el horario de atención: fuera de una franja no se puede reservar.
cancha.cancha
• RN-CAN-01 El nombre es obligatorio y único.
• RN-CAN-02 Una cancha inactiva no acepta reservas nuevas.
• RN-CAN-03 No se puede desactivar una cancha con reservas futuras PENDIENTE o CONFIRMADA. Primero se cancelan o reprograman.
cancha.tarifa
• RN-TAR-01 Las franjas activas de una misma cancha y día no se cruzan (lo garantiza la base de datos).
• RN-TAR-02 El horario de atención es la unión de las franjas activas. Con las tarifas actuales: 08:00 a 23:00.
• RN-TAR-03 El precio por hora es mayor que cero. Solo el ADMIN crea, cambia o desactiva tarifas.
• RN-TAR-04 Una tarifa con dia_semana específico tiene prioridad sobre la general (dia_semana NULL) en ese día.
• RN-TAR-05 Cambiar una tarifa solo afecta reservas nuevas. Las existentes conservan su monto_total.
• RN-TAR-06 Para cambiar un precio no se edita la fila: se desactiva la tarifa y se crea una nueva. Así queda historial.
cancha.cliente
• RN-CLI-01 Nombre y teléfono son obligatorios. El teléfono es un celular peruano: 9 dígitos que empiezan con 9.
• RN-CLI-02 El teléfono es único. Al registrar una reserva, primero se busca al cliente por teléfono y se reutiliza si existe.
• RN-CLI-03 Un cliente con reservas no se elimina.
• RN-CLI-04 Si un cliente tiene 2 o más reservas en estado NO_ASISTIO, el sistema lo advierte al reservar y exige adelanto (no se permite confirmación sin pago).
Módulo Cancha: reserva
Una hora solo puede estar ocupada por una reserva o un mantenimiento a la vez, y el precio siempre lo calcula el sistema.
Creación
• RN-RES-01 La reserva empieza en hora en punto y dura bloques completos de 1 hora (mínimo 1 hora).
• RN-RES-02 Debe caer completa dentro del horario de atención (RN-TAR-02) y en el mismo día: no cruza la medianoche.
• RN-RES-03 No se crean reservas en el pasado. Se admite la hora en curso para quien llega sin reservar.
• RN-RES-04 Se puede reservar con un máximo de 30 días de anticipación.
• RN-RES-05 La cancha debe estar activa (RN-CAN-02).
• RN-RES-06 No puede cruzarse con otra reserva o mantenimiento no cancelado (lo garantiza la base de datos). Las reservas contiguas sí se permiten: 20:00–22:00 y 22:00–23:00.
• RN-RES-07 Se registra el origen (presencial, WhatsApp, llamada, web) para saber por dónde llegan los clientes.
Precio
• RN-RES-08 monto_total es la suma, hora por hora, del precio de la franja en que cae cada hora. Ejemplo: 16:00–18:00 cuesta S/ 30 + S/ 60 = S/ 90.
• RN-RES-09 El monto queda fijo al crear la reserva. Solo se recalcula al reprogramar (RN-RES-17).
• RN-RES-10 Solo el ADMIN puede cambiar el monto manualmente (descuento a un cliente frecuente), con observación obligatoria.
Estados
• RN-RES-11 Toda reserva nace PENDIENTE, salvo que se pague el adelanto mínimo o el total en el mismo registro: entonces nace CONFIRMADA.
• RN-RES-12 COMPLETADA, CANCELADA y NO_ASISTIO son estados finales: no cambian ni reciben pagos.
Adelanto
• RN-RES-13 El adelanto mínimo es el 50% del monto_total (valor configurable). Al alcanzarlo, la reserva pasa a CONFIRMADA.
• RN-RES-14 fecha_limite_adelanto = hora de creación + 2 horas, sin pasar de la hora de inicio. Un proceso automático cada 10 minutos cancela las PENDIENTE vencidas con el motivo "Adelanto no pagado".
Cancelación, reprogramación e inasistencia
• RN-RES-15 Solo se cancelan reservas PENDIENTE o CONFIRMADA, con motivo obligatorio. La hora queda libre al instante.
• RN-RES-16 Si se cancela con 24 horas o más de anticipación, el adelanto se devuelve (RN-PAG-07). Con menos de 24 horas, el adelanto queda para el dueño. Los plazos se confirman con el dueño.
• RN-RES-17 Reprogramar cambia la hora de una reserva PENDIENTE o CONFIRMADA: se valida el cruce, se recalcula el monto y los pagos se conservan. Si lo pagado supera el nuevo monto, se devuelve la diferencia.
• RN-RES-18 Una reserva CONFIRMADA pasa a NO_ASISTIO solo si pasaron 15 minutos desde la hora de inicio sin que el cliente llegue. El adelanto no se devuelve.
• RN-RES-19 Una reserva pasa a COMPLETADA solo cuando el saldo es S/ 0 y la hora ya empezó. Nunca se completa con deuda.
Mantenimiento
• RN-RES-20 Solo el ADMIN registra mantenimientos: sin cliente, con monto S/ 0 y motivo obligatorio en observacion.
• RN-RES-21 Un mantenimiento nace CONFIRMADO, no recibe pagos y respeta las mismas reglas de horario y cruce.
• RN-RES-22 Si el horario elegido choca con reservas, el sistema las lista con el teléfono de cada cliente. El ADMIN las reprograma o cancela primero; el mantenimiento nunca las pisa.
• RN-RES-23 Un mantenimiento se puede cancelar para liberar las horas.
Módulo Cancha: caja, pago_reserva y movimiento_caja
Cada sol que entra o sale por la cancha queda ligado a una caja; al cerrar, el sistema compara el efectivo esperado con el contado.
cancha.caja
• RN-CAJ-01 Solo puede haber una caja abierta a la vez (lo garantiza la base de datos).
• RN-CAJ-02 Al abrir se registra el monto_apertura (el sencillo con que se empieza), que puede ser S/ 0.
• RN-CAJ-03 Al cerrar, el usuario cuenta el efectivo y registra monto_cierre_contado. El sistema calcula el efectivo esperado: apertura + cobros en efectivo + ingresos en efectivo − egresos en efectivo − devoluciones en efectivo.
• RN-CAJ-04 Si la diferencia entre contado y esperado no es S/ 0, la observación es obligatoria.
• RN-CAJ-05 Una caja cerrada no se reabre ni se modifica. Cualquier corrección se registra en la caja abierta actual.
• RN-CAJ-06 Se recomienda una caja por día. El sistema avisa si hay una caja abierta de un día anterior.
cancha.pago_reserva
• RN-PAG-01 Requiere caja de cancha abierta y se asocia a ella (RN-G05).
• RN-PAG-02 Solo se registran pagos a reservas tipo RESERVA en estado PENDIENTE o CONFIRMADA. Nunca a mantenimientos ni a reservas en estado final.
• RN-PAG-03 El monto es mayor que cero y la suma de lo pagado nunca supera el monto_total: no hay sobrepagos.
• RN-PAG-04 El tipo lo decide el sistema: es SALDO si deja la deuda en S/ 0, y ADELANTO si no.
• RN-PAG-05 En pagos por Yape, Plin o transferencia, el número de operación es obligatorio. No se acepta un número ya registrado con el mismo método: evita aceptar dos veces la misma captura.
• RN-PAG-06 Un pago no se edita ni se elimina. Un error se corrige con una devolución.
• RN-PAG-07 Una DEVOLUCION la registra solo el ADMIN, en reservas canceladas o reprogramadas con pago en exceso. No supera lo pagado y sale de la caja abierta actual.
• RN-PAG-08 Tras cada pago se reevalúa el estado de la reserva: CONFIRMADA al alcanzar el adelanto mínimo (RN-RES-13) y COMPLETADA según RN-RES-19.
cancha.movimiento_caja
• RN-MCA-01 Sirve para ingresos o gastos de la cancha que no son reservas: pago de luz, compra de una pelota, reparación de un arco.
• RN-MCA-02 Requiere caja abierta, concepto obligatorio y monto mayor que cero.
• RN-MCA-03 Un egreso en efectivo no puede superar el efectivo esperado en caja en ese momento.
• RN-MCA-04 Nunca se usa para mercadería de la bodega: eso va en el módulo Bodega (RN-G01).
Módulo Bodega: categoria, producto y proveedor
El stock de un producto nunca se escribe a mano: solo cambia por compras, ventas, anulaciones, ajustes o mermas, y cada cambio deja rastro en el kardex.
bodega.categoria
• RN-CAT-01 El nombre es obligatorio y único.
• RN-CAT-02 No se elimina una categoría con productos.
bodega.producto
• RN-PRO-01 Nombre y categoría son obligatorios. El código de barras es opcional, pero único si existe.
• RN-PRO-02 El stock nunca es negativo y no se edita directamente. Solo lo cambian los movimientos del kardex (RN-KAR-01).
• RN-PRO-03 El stock inicial de un producto nuevo se registra como AJUSTE_POSITIVO con el motivo "Inventario inicial".
• RN-PRO-04 Si el precio de venta es menor que el de compra, el sistema advierte pero no bloquea (puede ser una promoción).
• RN-PRO-05 Solo el ADMIN cambia el precio de venta.
• RN-PRO-06 El precio de compra se actualiza con el costo unitario de la última compra registrada. La alternativa más precisa, costo promedio ponderado, se decide con el dueño.
• RN-PRO-07 unidades_por_caja es al menos 1. Para cerveza y gaseosa, se registra cuántas unidades trae cada caja o paquete.
• RN-PRO-08 Un producto retornable apunta con envase_id a un producto de tipo ENVASE. Un ENVASE no puede tener envase a su vez, y su precio de venta es el valor de la garantía.
• RN-PRO-09 Un producto no se elimina, se desactiva. Los inactivos no aparecen al vender ni al comprar.
• RN-PRO-10 Cuando el stock llega al stock_minimo, el producto aparece en la alerta de reposición.
bodega.proveedor
• RN-PRV-01 El nombre es obligatorio. El RUC es opcional, pero si existe tiene 11 dígitos y es único.
• RN-PRV-02 Un proveedor con compras no se elimina, se desactiva.
Módulo Bodega: compras y ventas
Una compra suma stock y una venta lo resta, siempre dentro de la misma transacción que escribe el kardex; ninguna de las dos se edita después de registrada.
bodega.compra y bodega.detalle_compra
• RN-COM-01 Una compra tiene al menos un detalle, y cada producto aparece una sola vez en ella.
• RN-COM-02 El backend calcula cantidad_unidades = cajas × unidades_por_caja + unidades sueltas. Ejemplo: 2 cajas de 12 + 3 sueltas = 27 unidades.
• RN-COM-03 El total es la suma de los subtotales, calculada por el backend.
• RN-COM-04 Al registrar la compra: el stock sube, se escribe un ENTRADA_COMPRA en el kardex por cada producto y se actualiza su precio de compra (RN-PRO-06).
• RN-COM-05 Si la compra se paga con efectivo de la caja de la bodega, se registra un EGRESO en bodega.movimiento_caja con el concepto "Compra #id".
• RN-COM-06 Solo el ADMIN anula una compra, con motivo. La anulación resta el stock (ANULACION_COMPRA). Si ya se vendió parte de esa mercadería y el stock no alcanza, no se anula: se corrige con un ajuste.
bodega.venta
• RN-VEN-01 Requiere caja de bodega abierta (RN-G05) y al menos un detalle.
• RN-VEN-02 El total es la suma de los subtotales, calculada por el backend.
• RN-VEN-03 La suma de los pagos es exactamente igual al total. El sistema no maneja ventas al fiado; si el dueño fía, hay que definirlo antes como funcionalidad aparte.
• RN-VEN-04 Solo el ADMIN anula una venta, con motivo, y solo mientras su caja siga abierta. La anulación devuelve el stock (ANULACION_VENTA) y sus pagos dejan de contar en la caja.
• RN-VEN-05 Una venta de una caja ya cerrada no se anula: el reembolso se registra como EGRESO en la caja actual y el producto devuelto, como AJUSTE_POSITIVO.
bodega.detalle_venta
• RN-DVE-01 La cantidad es un entero mayor que cero, y cada producto aparece una sola vez por venta.
• RN-DVE-02 El producto está activo y tiene stock suficiente. Si una línea no alcanza, no se guarda ninguna (RN-G08).
• RN-DVE-03 precio_unitario se toma del precio de venta actual y costo_unitario del precio de compra actual. El ENCARGADO no puede cambiar el precio.
• RN-DVE-04 Cada línea descuenta stock y escribe un SALIDA_VENTA en el kardex.
• RN-DVE-05 Al vender un producto retornable, el sistema pregunta si el cliente entregó un envase vacío. Si no lo entregó, agrega una línea con el envase para cobrar la garantía (RN-KAR-06).
bodega.pago_venta
• RN-PVE-01 Una venta puede pagarse con varios métodos (parte efectivo, parte Yape). Cada monto es mayor que cero.
• RN-PVE-02 En efectivo se registra lo que corresponde a la venta. El vuelto lo calcula el frontend y no se guarda.
• RN-PVE-03 En Yape, Plin o transferencia, el número de operación es obligatorio y no puede repetirse con el mismo método.