package proyecto.usuario.service;
 
import proyecto.usuario.entities.usuario;
import proyecto.usuario.repositories.usuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
 
import java.util.List;
 
@Service
@RequiredArgsConstructor
public class usuarioService {
 
    private final usuarioRepository usuarioRepository;
 
    public List<usuario> listarTodos() {
        return usuarioRepository.findAll();
    }
 
    public usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
    }
 
    public usuario crear(usuario usuario) {
        return usuarioRepository.save(usuario);
    }
 
    public usuario actualizar(Long id, usuario datosActualizados) {
        usuario usuarioExistente = buscarPorId(id);
        usuarioExistente.setNombre(datosActualizados.getNombre());
        usuarioExistente.setEmail(datosActualizados.getEmail());
        usuarioExistente.setPassword(datosActualizados.getPassword());
        return usuarioRepository.save(usuarioExistente);
    }
 
    public void eliminar(Long id) {
        usuarioRepository.delete(buscarPorId(id));
    }
}
 