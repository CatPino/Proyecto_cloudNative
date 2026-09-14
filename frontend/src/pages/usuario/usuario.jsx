import { useEffect, useState } from "react";
import { usuarioApi } from "../../api/api";
import "./usuario.css";

function Usuario() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState({ nombre: "", email: "", password: "" });
  const [editandoId, setEditandoId] = useState(null);
  const [error, setError] = useState("");

  const cargar = async () => {
    try {
      const res = await usuarioApi.get("");
      setItems(res.data);
    } catch (err) {
      setError("No se pudo conectar con el servicio de usuarios.");
    }
  };

  useEffect(() => {
    cargar();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editandoId) {
        await usuarioApi.put(`/${editandoId}`, form);
      } else {
        await usuarioApi.post("", form);
      }
      setForm({ nombre: "", email: "", password: "" });
      setEditandoId(null);
      cargar();
    } catch (err) {
      setError("No se pudo guardar el usuario.");
    }
  };

  const handleEditar = (item) => {
    setForm({ nombre: item.nombre, email: item.email, password: item.password });
    setEditandoId(item.id);
  };

  const handleEliminar = async (id) => {
    try {
      await usuarioApi.delete(`/${id}`);
      cargar();
    } catch (err) {
      setError("No se pudo eliminar el usuario.");
    }
  };

  return (
    <div className="container">
      <h2>Usuarios</h2>
      {error && <p className="error-text">{error}</p>}

      <form className="form-crud" onSubmit={handleSubmit}>
        <input
          className="form-control mb-2"
          placeholder="Nombre"
          value={form.nombre}
          onChange={(e) => setForm({ ...form, nombre: e.target.value })}
          required
        />
        <input
          className="form-control mb-2"
          type="email"
          placeholder="Email"
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
          required
        />
        <input
          className="form-control mb-2"
          type="password"
          placeholder="Password"
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
          required
        />
        <button className="btn btn-primary" type="submit">
          {editandoId ? "Actualizar" : "Crear"}
        </button>
      </form>

      <table className="table table-hover tabla-crud">
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Email</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.id}>
              <td>{item.nombre}</td>
              <td>{item.email}</td>
              <td>
                <button className="btn btn-sm btn-outline-primary me-2" onClick={() => handleEditar(item)}>
                  Editar
                </button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => handleEliminar(item.id)}>
                  Eliminar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default Usuario;