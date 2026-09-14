import { useState } from "react";
import { Link } from "react-router-dom";
import { useCart } from "../../context/cartContext";
import LoginModal from "../loginModal/loginModal";
import "./navbar.css";

function Navbar() {
  const { carrito, eliminarDelCarrito } = useCart();
  const [carritoAbierto, setCarritoAbierto] = useState(false);
  const [loginAbierto, setLoginAbierto] = useState(false);

  return (
    <nav className="navbar">
      <div className="navbar-links">
        <Link to="/">Home</Link>
        <Link to="/inventario">Inventario</Link>
        <Link to="/compra">Compras</Link>
        <Link to="/usuario">Usuarios</Link>
      </div>

      <div className="navbar-right">
        <button onClick={() => setLoginAbierto(true)}>👤</button>

        <div className="carrito-wrapper">
          <button onClick={() => setCarritoAbierto(!carritoAbierto)}>
            🛒 ({carrito.length})
          </button>

          {carritoAbierto && (
            <div className="carrito-dropdown">
              <h3>Carrito de compra</h3>

              {carrito.length === 0 ? (
                <p>No hay productos seleccionados</p>
              ) : (
                <ul>
                  {carrito.map((producto, index) => (
                    <li key={index}>
                      {producto.nombre}
                      <button onClick={() => eliminarDelCarrito(index)}>Quitar</button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </div>
      </div>

      {loginAbierto && <LoginModal cerrar={() => setLoginAbierto(false)} />}
    </nav>
  );
}

export default Navbar;