package proyecto.usuario.controller;

import proyecto.usuario.entities.usuario;
import proyecto.usuario.service.usuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import java.util.List;
 
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class controllerUsuario {
 
    private final usuarioService usuarioService;
 
    @GetMapping
    public ResponseEntity<List<usuario>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }
 
    @GetMapping("/{id}")
    public ResponseEntity<usuario> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }
 
    @PostMapping
    public ResponseEntity<usuario> crear(@RequestBody usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(usuario));
    }
 
    @PutMapping("/{id}")
    public ResponseEntity<usuario> actualizar(@PathVariable Long id, @RequestBody usuario usuario) {
        return ResponseEntity.ok(usuarioService.actualizar(id, usuario));
    }
 
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
 