package proyecto.usuario.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import proyecto.usuario.service.AnaliticaStreamingService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios/analitica")
@RequiredArgsConstructor
public class AnaliticaController {

    private final AnaliticaStreamingService analiticaStreamingService;

    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Object>> obtenerResumen() {
        return ResponseEntity.ok(analiticaStreamingService.obtenerResumen());
    }

    @GetMapping("/alertas")
    public ResponseEntity<List<Map<String, Object>>> obtenerAlertas() {
        return ResponseEntity.ok(analiticaStreamingService.obtenerAlertas());
    }

    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, String>> reiniciar() {
        analiticaStreamingService.reiniciarMetricas();
        return ResponseEntity.ok(Map.of("estado", "metricas_reiniciadas"));
    }
}
