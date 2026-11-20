# Guía de Operación y Supervisión — Apache Kafka + RabbitMQ (Evaluación Parcial N° 6)

## 1. Levantar toda la infraestructura (RabbitMQ + 3 Zookeeper + 3 Brokers Kafka + Kafka UI)

```powershell
docker compose up -d
```

Verificar que todos los contenedores estén activos y saludables:

```powershell
docker compose ps
```

Accesos web:
- **Kafka UI:** http://localhost:8090
- **RabbitMQ Management:** http://localhost:15672 (`guest` / `guest`)
- **Frontend React:** http://localhost:5173 (pestaña **Eventos** para RabbitMQ y **Streaming (Kafka)** para Kafka)

---

## 2. Arquitectura y Casos de Uso

### Complementariedad RabbitMQ vs. Kafka
- **RabbitMQ (intacto):** Mensajería transaccional y manejo de fallos (`tienda.exchange` -> `inventario.queue` / `auditoria.queue` -> reintentos con backoff -> `tienda.dlq`).
- **Apache Kafka:** Event streaming y analítica en tiempo real con log distribuido, retención configurable y múltiples **consumer groups** independientes.

### Tópicos creados automáticamente (3 particiones, 3 réplicas, `min.insync.replicas=2`)

| Tópico | Particiones | Réplicas | Retención (`retention.ms`) | Key | Caso de uso |
| :--- | :---: | :---: | :---: | :---: | :--- |
| `compras.eventos.stream` | 3 | 3 | 7 días (`604800000`) | `producto` | Flujo de compras en tiempo real para actualización reactiva de stock y analítica de ventas. |
| `inventario.alertas.stream` | 3 | 3 | 24 horas (`86400000`) | `producto` | Flujo de cambios de stock y alertas operacionales (`ALERTA_STOCK_BAJO`, `ALERTA_SIN_STOCK`). |

### Consumer Groups independientes
1. `inventario-stock-group` (en `inventario:8082`): consume `compras.eventos.stream`, descuenta stock automáticamente en BD y publica en `inventario.alertas.stream`.
2. `analitica-ventas-group` (en `usuario:8083`): consume **el mismo tópico** `compras.eventos.stream` en paralelo para calcular métricas de ventas e ingresos en tiempo real.
3. `monitoreo-alertas-group` (en `usuario:8083`): consume `inventario.alertas.stream` y mantiene el panel de alertas de stock.

---

## 3. Comandos CLI de Supervisión (Offsets, Lag, Particiones y Réplicas)

### Describir los tópicos (verificar 3 particiones, 3 réplicas, Líder e ISR)
```powershell
docker exec kafka-1 kafka-topics --bootstrap-server kafka-1:29092 --describe --topic compras.eventos.stream
docker exec kafka-1 kafka-topics --bootstrap-server kafka-1:29092 --describe --topic inventario.alertas.stream
```

### Verificar configuraciones de retención y `min.insync.replicas`
```powershell
docker exec kafka-1 kafka-configs --bootstrap-server kafka-1:29092 --entity-type topics --entity-name compras.eventos.stream --describe
docker exec kafka-1 kafka-configs --bootstrap-server kafka-1:29092 --entity-type topics --entity-name inventario.alertas.stream --describe
```

### Listar Consumer Groups activos
```powershell
docker exec kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:29092 --list
```

### Supervisar Offsets, Consumer Lag y distribución por partición
```powershell
docker exec kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:29092 --describe --group inventario-stock-group
docker exec kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:29092 --describe --group analitica-ventas-group
docker exec kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:29092 --describe --group monitoreo-alertas-group
```

### Demostrar tolerancia a fallos (opcional en presentación)
Si detienes un broker (`docker stop kafka-2`), los tópicos siguen operando gracias a `replication-factor=3` y `min.insync.replicas=2`, y Kafka reasigna automáticamente los líderes de partición a `kafka-1` y `kafka-3`. Luego vuelve a levantarlo con `docker start kafka-2`.
