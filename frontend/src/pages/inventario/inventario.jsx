import { useEffect, useState } from "react";
import { inventarioApi } from "../../api/api";
import "./inventario.css";

function formatearMiles(valor) {
  if (valor === "" || valor === null || valor === undefined) return "";
  return Number(valor).toLocaleString("es-CL");
}

function Inventario() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState({ producto: "", stock: "", precio: "" });
  const [editandoId, setEditandoId] = useState(null);
  const [error, setError] = useState("");

  const cargar = async () => {
    try {
      const res = await inventarioApi.get("");
      setItems(res.data);
    } catch (err) {
      setError("No se pudo conectar con el servicio de inventario.");
    }
  };

  useEffect(() => {
    cargar();
  }, []);

  const handlePrecioChange = (e) => {
    const soloNumeros = e.target.value.replace(/\D/g, "");
    setForm({ ...form, precio: soloNumeros });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const datos = {
      producto: form.producto,
      stock: Number(form.stock),
      precio: Number(form.precio),
    };
    try {
      if (editandoId) {
        await inventarioApi.put(`/${editandoId}`, datos);
      } else {
        await inventarioApi.post("", datos);
      }
      setForm({ producto: "", stock: "", precio: "" });
      setEditandoId(null);
      cargar();
    } catch (err) {
      setError("No se pudo guardar el producto.");
    }
  };

  const handleEditar = (item) => {
    setForm({ producto: item.producto, stock: item.stock, precio: item.precio });
    setEditandoId(item.id);
  };

  const handleEliminar = async (id) => {
    try {
      await inventarioApi.delete(`/${id}`);
      cargar();
    } catch (err) {
      setError("No se pudo eliminar el producto.");
    }
  };

  return (
    <div className="container">
      <h2>Inventario</h2>
      {error && <p className="error-text">{error}</p>}

      <form className="form-crud" onSubmit={handleSubmit}>
        <input
          className="form-control mb-2"
          placeholder="Producto"
          value={form.producto}
          onChange={(e) => setForm({ ...form, producto: e.target.value })}
          required
        />
        <input
          className="form-control mb-2"
          type="number"
          placeholder="Stock"
          value={form.stock}
          onChange={(e) => setForm({ ...form, stock: e.target.value })}
          required
        />
        <input
          className="form-control mb-2"
          placeholder="Precio"
          value={formatearMiles(form.precio)}
          onChange={handlePrecioChange}
          required
        />
        <button className="btn btn-primary" type="submit">
          {editandoId ? "Actualizar" : "Crear"}
        </button>
      </form>

      <table className="table table-hover tabla-crud">
        <thead>
          <tr>
            <th>Producto</th>
            <th>Stock</th>
            <th>Precio</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.id}>
              <td>{item.producto}</td>
              <td>{item.stock}</td>
              <td>${formatearMiles(item.precio)}</td>
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

export default Inventario;