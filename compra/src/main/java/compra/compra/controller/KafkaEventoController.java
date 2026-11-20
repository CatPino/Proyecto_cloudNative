package compra.compra.controller;

import compra.compra.entities.compra;
import compra.compra.messaging.CompraStreamEvent;
import compra.compra.messaging.KafkaCompraProducer;
import compra.compra.repositories.compraRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compras/kafka")
@RequiredArgsConstructor
@Slf4j
public class KafkaEventoController {

    private final KafkaCompraProducer kafkaCompraProducer;
    private final compraRepository compraRepository;

    public record PublicarCompraRequest(
            @NotBlank(message = "producto es obligatorio") String producto,
            @NotNull(message = "cantidad es obligatoria") @Min(1) Integer cantidad,
            @NotNull(message = "precioUnitario es obligatorio") @Min(1) Double precioUnitario,
            Boolean persistirCompra
    ) {}

    public record RafagaRequest(
            @Min(1) @Max(30) Integer cantidadEventos,
            List<String> productos
    ) {}

    @PostMapping("/publicar")
    public ResponseEntity<Map<String, Object>> publicarCompra(@Valid @RequestBody PublicarCompraRequest request) {
        Long compraId = null;
        boolean guardar = request.persistirCompra() == null || request.persistirCompra();

        if (guardar) {
            compra nuevaCompra = compra.builder()
                    .producto(request.producto().trim())
                    .cantidad(request.cantidad())
                    .precio(request.precioUnitario())
                    .fecha(LocalDate.now())
                    .build();
            compra guardada = compraRepository.save(nuevaCompra);
            compraId = guardada.getId();
        }

        CompraStreamEvent evento = CompraStreamEvent.of(
                "COMPRA_CREADA",
                compraId,
                request.producto().trim(),
                request.cantidad(),
                request.precioUnitario(),
                "STREAM_API"
        );

        Map<String, Object> resultado = kafkaCompraProducer.publicarSincrono(evento);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/rafaga")
    public ResponseEntity<Map<String, Object>> publicarRafaga(@RequestBody(required = false) RafagaRequest request) {
        int total = (request != null && request.cantidadEventos() != null)
                ? Math.min(Math.max(request.cantidadEventos(), 1), 30)
                : 9;

        List<String> catalogo = (request != null && request.productos() != null && !request.productos().isEmpty())
                ? request.productos()
                : List.of("Laptop Pro", "Mouse Gamer", "Teclado Mecanico", "Monitor 4K", "Audifonos Studio", "SSD 1TB");

        List<Map<String, Object>> resultados = new ArrayList<>();
        Map<Integer, Integer> distribucionParticiones = new LinkedHashMap<>();

        for (int i = 0; i < total; i++) {
            String producto = catalogo.get(i % catalogo.size());
            int cantidad = (i % 3) + 1;
            double precio = 15000.0 + ((i % 6) * 12500.0);

            CompraStreamEvent evento = CompraStreamEvent.of(
                    "COMPRA_CREADA",
                    null,
                    producto,
                    cantidad,
                    precio,
                    "RAFAGA_STREAMING"
            );

            Map<String, Object> res = kafkaCompraProducer.publicarSincrono(evento);
            resultados.add(res);
            Integer partition = (Integer) res.get("partition");
            distribucionParticiones.merge(partition, 1, Integer::sum);
        }

        log.info("[KAFKA-COMPRA] Ráfaga de {} eventos publicada. Distribución por partición: {}", total, distribucionParticiones);

        return ResponseEntity.ok(Map.of(
                "estado", "rafaga_publicada",
                "totalEventos", total,
                "distribucionParticiones", distribucionParticiones,
                "eventos", resultados
        ));
    }
}
