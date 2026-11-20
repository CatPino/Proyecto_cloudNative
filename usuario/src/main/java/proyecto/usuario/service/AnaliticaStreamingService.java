package proyecto.usuario.service;

import org.springframework.stereotype.Service;
import proyecto.usuario.config.KafkaConfig;
import proyecto.usuario.messaging.CompraStreamEvent;
import proyecto.usuario.messaging.InventarioAlertaEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

@Service
public class AnaliticaStreamingService {

    private static final int MAX_HISTORIAL = 30;

    private final AtomicLong totalComprasProcesadas = new AtomicLong(0);
    private final AtomicLong totalCancelaciones = new AtomicLong(0);
    private final AtomicLong totalUnidadesVendidas = new AtomicLong(0);
    private final DoubleAdder ingresosAcumulados = new DoubleAdder();

    private final AtomicLong totalAlertasRecibidas = new AtomicLong(0);
    private final AtomicLong alertasCriticas = new AtomicLong(0);
    private final AtomicLong alertasAdvertencia = new AtomicLong(0);

    private final Map<Integer, AtomicLong> comprasPorParticion = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicLong> alertasPorParticion = new ConcurrentHashMap<>();
    private final Map<String, ProductoMetricas> ventasPorProducto = new ConcurrentHashMap<>();

    private final Deque<Map<String, Object>> ultimasCompras = new ConcurrentLinkedDeque<>();
    private final Deque<Map<String, Object>> ultimasAlertas = new ConcurrentLinkedDeque<>();

    public AnaliticaStreamingService() {
        inicializarParticiones();
    }

    private void inicializarParticiones() {
        for (int p = 0; p < 3; p++) {
            comprasPorParticion.put(p, new AtomicLong(0));
            alertasPorParticion.put(p, new AtomicLong(0));
        }
    }

    public void registrarCompra(CompraStreamEvent evento, int partition, long offset) {
        comprasPorParticion.computeIfAbsent(partition, k -> new AtomicLong(0)).incrementAndGet();

        int cantidad = evento.cantidad() != null ? evento.cantidad() : 0;
        double monto = evento.montoTotal() != null ? evento.montoTotal() : 0.0;
        String producto = evento.producto() != null ? evento.producto() : "Desconocido";

        if ("COMPRA_CANCELADA".equalsIgnoreCase(evento.eventType())) {
            totalCancelaciones.incrementAndGet();
        } else {
            totalComprasProcesadas.incrementAndGet();
            totalUnidadesVendidas.addAndGet(cantidad);
            ingresosAcumulados.add(monto);
            ventasPorProducto.computeIfAbsent(producto, k -> new ProductoMetricas())
                    .registrar(cantidad, monto);
        }

        Map<String, Object> registro = new LinkedHashMap<>();
        registro.put("eventId", evento.eventId());
        registro.put("eventType", evento.eventType());
        registro.put("timestamp", evento.timestamp());
        registro.put("compraId", evento.compraId());
        registro.put("producto", producto);
        registro.put("cantidad", cantidad);
        registro.put("precioUnitario", evento.precioUnitario());
        registro.put("montoTotal", monto);
        registro.put("origen", evento.origen());
        registro.put("partition", partition);
        registro.put("offset", offset);

        ultimasCompras.addFirst(registro);
        while (ultimasCompras.size() > MAX_HISTORIAL) {
            ultimasCompras.pollLast();
        }
    }

    public void registrarAlerta(InventarioAlertaEvent evento, int partition, long offset) {
        alertasPorParticion.computeIfAbsent(partition, k -> new AtomicLong(0)).incrementAndGet();
        totalAlertasRecibidas.incrementAndGet();

        if ("CRITICAL".equalsIgnoreCase(evento.severidad())) {
            alertasCriticas.incrementAndGet();
        } else if ("WARNING".equalsIgnoreCase(evento.severidad())) {
            alertasAdvertencia.incrementAndGet();
        }

        Map<String, Object> registro = new LinkedHashMap<>();
        registro.put("eventId", evento.eventId());
        registro.put("eventType", evento.eventType());
        registro.put("timestamp", evento.timestamp());
        registro.put("inventarioId", evento.inventarioId());
        registro.put("producto", evento.producto());
        registro.put("stockAnterior", evento.stockAnterior());
        registro.put("stockActual", evento.stockActual());
        registro.put("umbralMinimo", evento.umbralMinimo());
        registro.put("severidad", evento.severidad());
        registro.put("compraIdOrigen", evento.compraIdOrigen());
        registro.put("mensaje", evento.mensaje());
        registro.put("partition", partition);
        registro.put("offset", offset);

        ultimasAlertas.addFirst(registro);
        while (ultimasAlertas.size() > MAX_HISTORIAL) {
            ultimasAlertas.pollLast();
        }
    }

    public Map<String, Object> obtenerResumen() {
        Map<String, Long> distCompras = new LinkedHashMap<>();
        comprasPorParticion.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> distCompras.put("P" + e.getKey(), e.getValue().get()));

        Map<String, Long> distAlertas = new LinkedHashMap<>();
        alertasPorParticion.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> distAlertas.put("P" + e.getKey(), e.getValue().get()));

        List<Map<String, Object>> rankingProductos = ventasPorProducto.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("producto", entry.getKey());
                    m.put("compras", entry.getValue().compras.get());
                    m.put("unidades", entry.getValue().unidades.get());
                    m.put("ingresos", entry.getValue().ingresos.sum());
                    return m;
                })
                .sorted(Comparator.comparingDouble((Map<String, Object> m) -> (Double) m.get("ingresos")).reversed())
                .toList();

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("totalComprasProcesadas", totalComprasProcesadas.get());
        resumen.put("totalCancelaciones", totalCancelaciones.get());
        resumen.put("totalUnidadesVendidas", totalUnidadesVendidas.get());
        resumen.put("ingresosAcumulados", ingresosAcumulados.sum());
        resumen.put("totalAlertasRecibidas", totalAlertasRecibidas.get());
        resumen.put("alertasCriticas", alertasCriticas.get());
        resumen.put("alertasAdvertencia", alertasAdvertencia.get());
        resumen.put("distribucionComprasParticiones", distCompras);
        resumen.put("distribucionAlertasParticiones", distAlertas);
        resumen.put("ventasPorProducto", rankingProductos);
        resumen.put("ultimasCompras", new ArrayList<>(ultimasCompras));
        resumen.put("ultimasAlertas", new ArrayList<>(ultimasAlertas));
        resumen.put("arquitectura", Map.of(
                "topicoCompras", KafkaConfig.TOPIC_COMPRAS_STREAM,
                "topicoAlertas", KafkaConfig.TOPIC_INVENTARIO_ALERTAS,
                "consumerGroups", List.of(
                        "inventario-stock-group",
                        KafkaConfig.GROUP_ANALITICA_VENTAS,
                        KafkaConfig.GROUP_MONITOREO_ALERTAS
                )
        ));
        return resumen;
    }

    public List<Map<String, Object>> obtenerAlertas() {
        return new ArrayList<>(ultimasAlertas);
    }

    public void reiniciarMetricas() {
        totalComprasProcesadas.set(0);
        totalCancelaciones.set(0);
        totalUnidadesVendidas.set(0);
        ingresosAcumulados.reset();
        totalAlertasRecibidas.set(0);
        alertasCriticas.set(0);
        alertasAdvertencia.set(0);
        ventasPorProducto.clear();
        ultimasCompras.clear();
        ultimasAlertas.clear();
        comprasPorParticion.clear();
        alertasPorParticion.clear();
        inicializarParticiones();
    }

    private static final class ProductoMetricas {
        private final AtomicLong compras = new AtomicLong(0);
        private final AtomicLong unidades = new AtomicLong(0);
        private final DoubleAdder ingresos = new DoubleAdder();

        private void registrar(int cantidad, double monto) {
            compras.incrementAndGet();
            unidades.addAndGet(cantidad);
            ingresos.add(monto);
        }
    }
}
