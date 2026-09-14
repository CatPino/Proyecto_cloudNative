import { useEffect, useState } from "react";
import { compraApi } from "../../api/api";
import "./compra.css";

function formatearMiles(valor) {
  if (valor === "" || valor === null || valor === undefined) return "";
  return Number(valor).toLocaleString("es-CL");
}

function Compra() {
  const [items, setItems] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    const cargar = async () => {
      try {
        const res = await compraApi.get("");
        setItems(res.data);
      } catch (err) {
        setError("No se pudo conectar con el servicio de compras.");
      }
    };
    cargar();
  }, []);

  return (
    <div className="container">
      <h2>Compras</h2>
      {error && <p className="error-text">{error}</p>}

      <table className="table table-hover tabla-crud">
        <thead>
          <tr>
            <th>Producto</th>
            <th>Cantidad</th>
            <th>Precio</th>
            <th>Fecha</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.id}>
              <td>{item.producto}</td>
              <td>{item.cantidad}</td>
              <td>${formatearMiles(item.precio)}</td>
              <td>{item.fecha}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default Compra;