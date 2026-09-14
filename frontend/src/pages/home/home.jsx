import { useEffect, useState } from "react";
import { inventarioApi } from "../../api/api";
import { useCart } from "../../context/cartContext";
import "./home.css";

function formatearMiles(valor) {
  if (valor === "" || valor === null || valor === undefined) return "";
  return Number(valor).toLocaleString("es-CL");
}

function Home() {
  const [productos, setProductos] = useState([]);
  const [error, setError] = useState("");
  const { agregarAlCarrito } = useCart();

  useEffect(() => {
    const cargar = async () => {
      try {
        const res = await inventarioApi.get("");
        setProductos(res.data);
      } catch (err) {
        setError("No se pudo conectar con el servicio de inventario.");
      }
    };
    cargar();
  }, []);

  return (
    <div className="container home-container">
      <h1>Conoce nuestros productos</h1>

      {error && <p className="error-text">{error}</p>}

      {productos.length === 0 ? (
        <p>Todavía no hay productos creados. Ve a "Inventario" para agregar el primero.</p>
      ) : (
        <div className="productos-grid">
          {productos.map((producto) => (
            <div key={producto.id} className="card producto-card p-3">
              <h3>{producto.producto}</h3>
              <p>Stock disponible: {producto.stock}</p>
              <p className="precio-text">${formatearMiles(producto.precio)}</p>
              <button
                className="btn btn-primary"
                onClick={() => agregarAlCarrito({ nombre: producto.producto })}
              >
                Agregar al carrito
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default Home;