# DECISIONES

Este documento registra las decisiones y supuestos tomados para implementar el servicio de reservas,
lo que se está fuera de alcance, y lo que cambiaría antes de llevar esto a
producción.

### Pedidos y reintentos

- **`reserve()` es idempotente ante reintentos exactos**: si `orderId` ya tiene una reserva
  activa con el mismo `sku` y la misma `quantity`, se devuelve esa misma reserva sin volver a
  descontar stock.
- **Un `orderId` con reserva activa que recibe una solicitud distinta** (otro `sku` u otra
  `quantity`) se trata como un conflicto y lanza `IllegalStateException`.
- **Un `orderId` solo puede tener una reserva activa a la vez**. Una vez que esa reserva se 
  confirma o expira, el `orderId` queda libre para un nuevo intento de reserva, por ejemplo, 
  si el cliente vuelve a intentar comprar tras dejar vencer el primer intento.
- **La verificación de idempotencia se hace antes de aplicar el límite por pedido y de
  descontar stock.** Así, una solicitud con datos distintos para un `orderId` ya reservado
  siempre falla con `IllegalStateException` por conflicto de identidad.
- **La instalación de la reserva nueva es atómica por `orderId`** : si dos reintentos idénticos
  llegan al mismo tiempo, solo uno gana y pasa a descontar stock; el que pierde deshace su
  descuento y se resuelve igual que si hubiera llegado después del otro. Esto evita duplicar 
  la reserva sin necesitar un lock explícito por pedido.

### Categorías y reglas

- Las reglas de tiempo de expiración y límite por pedido de cada `ProductCategory` se modelan
  como un mapa de categorías y reglas, no como condicionales, para que ajustar una categoría 
  existente sea un cambio de configuración.
- **Reglas**: una reserva se crea con el tiempo de expiración vigente en el instante de 
  `reserve()`. Si luego cambian los parámetros de esa categoría, las reservas ya creadas 
  conservan su expiración original.
- `registerProduct` es idempotente: volver a llamarlo para el mismo `sku` actualiza su
  categoría sin afectar el stock ya cargado.

### Expiración y disponibilidad

- La expiración se evalúa de forma **perezosa** comparando `expiresAt` contra el `Clock` inyectado. 
  Esto es suficiente para que `available()` nunca cuente una reserva vencida como activa, sin
  depender de que alguien más "toque" ese SKU primero.
- El instante límite se trata como **inclusive**, osea que una reserva con `expiresAt == ahora` se
  considera expirada y ya no se puede confirmar.
- Pago aprobado después de que la reserva expiró: `confirm()` lanza `IllegalStateException`,
  igual que para cualquier pedido sin reserva activa. La regla de negocio de qué hacer con ese pago 
  se asume que vive en Pagos/Órdenes, no en este servicio. No existe la manera de comunicar una 
  "confirmación tardía" de otra forma.
- La evaluación perezosa libera todas las reservas vencidas del inventario, no solo las del SKU
  consultado.
- **Cada operación (`reserve`, `confirm`, `available`) lee el `Clock` una sola vez** al empezar,
  y usa ese mismo instante tanto para liberar reservas vencidas como para su propia decisión. 
- **Las reservas confirmadas y expiradas se conservan en memoria**, no se eliminan del mapa,
  porque un `confirm()` fallido necesita poder distinguir "este pedido expiró" de "este pedido
  nunca existió" de "este pedido ya fue confirmado antes", son tres mensajes de error distintos
  sobre el mismo `IllegalStateException`.

### Alertas de bajo stock

- Se evalúa sobre `available()`, no sobre el stock físico, porque la alerta busca reflejar 
  lo que realmente se puede vender ahora.
- **"Reabastecer" se define como**: el disponible sube estrictamente por encima del umbral
  (> 5). Esto puede ocurrir tanto por `addStock` como por la liberación de una reserva
  expirada, ambos casos rearman la alerta de la misma forma. Un `addStock` que deja el
  disponible en ≤ 5 no rearma la alerta.
- **La deduplicación vive en `LowStockAlerter`**: mantiene un conjunto de SKUs "en alerta"; 
  si el disponible resultante es mayor al umbral, quita el SKU del conjunto (rearma) sin avisar; 
  si es menor o igual, solo avisa cuando el SKU no estaba ya en el conjunto. `InMemoryStockRepository` 
  lo notifica después de `addStock`, de una `reserveUnits` exitosa y de `releaseUnits`, nunca 
  después de `confirmUnits`, confirmar mueve unidades de reservado a vendido y `available()` no cambia,
  así que nunca puede cruzar el umbral.

### Validación

- `sku` u `orderId` nulos o en blanco lanzan `IllegalArgumentException`.
- No se valida nada sobre montos de pago: se asume que `confirm()` solo significa "el pago fue
  aprobado", y que la validación del pago en sí es responsabilidad de otro servicio.

### Concurrencia

- La consistencia entre reservas concurrentes sobre el mismo `sku` se garantiza con sincronización
  a nivel de proceso (bloqueo por SKU), suficiente para que la suma de reservas activas + confirmadas 
  nunca supere el stock dentro de una misma instancia.

## Fuera de alcance

- **Agregar categorías nuevas más allá de `STANDARD`, `PRE_ORDER` y `FLASH_SALE`**: el enum es
  parte del contrato congelado; esto requiere coordinación con el equipo dueño de `api`, no es
  algo que este servicio pueda resolver con configuración interna.
- **Cancelación manual de una reserva** antes de que expire. El contrato solo expone liberación
  pasiva por tiempo.
- **Límite de compra por cliente** para `FLASH_SALE`. El contrato no modela "cliente", solo 
  `orderId`.
- **Consulta de estado de una reserva sin efectos secundarios**, hoy la única forma de saber
  si un pedido sigue activo es a través de `available()` o intentando `confirm()`.
- **Umbral de bajo stock configurable por categoría o por producto.** Se usa el valor fijo (5)
  que define el README para todas las categorías.
- **Persistencia en base de datos y coordinación real entre múltiples instancias.** La
  implementación actual es en memoria y de una sola instancia.
- **Múltiples canales de notificación** (correo, SMS, Slack, etc.) implementados de forma
  concreta. Se deja la extensibilidad arquitectónica pero no las integraciones.

## Qué cambiaría antes de llevar esto a producción

- **Persistencia real**: mover stock y reservas de estructuras en memoria a una base de datos.
- **Concurrencia multi-instancia**: reemplazar el bloqueo en memoria por un mecanismo que
  funcione entre instancias (bloqueo optimista con columna de versión, `SELECT ... FOR UPDATE`,
  o una cola/serialización por SKU), ya que el bloqueo por proceso no protege nada una vez que
  hay más de una instancia corriendo.
- **Barrido activo de expiraciones**: además de la evaluación perezosa, un job periódico que
  libere reservas vencidas y reevalúe el umbral de bajo stock, para que la alerta no dependa de
  que alguien llame a la API de ese SKU después de que la reserva expiró.
- **Retención de reservas confirmadas/expiradas**: hoy se conservan todas sin límite ni
  índice por estado o fecha, y cada barrido de expiración recorre el mapa completo. Aceptable
  por el momento, pero antes de producción necesitaría un índice por `expiresAt`, o con base 
  de datos, una consulta indexada, y una política de archivado o purga de reservas viejas.
- **Idempotencia robusta ante reintentos**: una clave de idempotencia persistida (no solo en
  memoria) para que los reintentos de la app sigan siendo seguros incluso si el reintento llega
  a otra instancia o después de un reinicio.
- **Evolución del contrato para categorías nuevas**.
- **Observabilidad**: logging estructurado y métricas por cada reserva creada, confirmada y
  expirada, y por cada alerta emitida, con `orderId`/`sku` y timestamps.
- **Notificaciones multicanal**: un `StockAlertListener` que reparta a varios canales
  como correo, Slack, dashboard interno.
- **Pruebas de concurrencia/carga**.
