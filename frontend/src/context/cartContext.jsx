import { createContext, useContext, useState } from "react";

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const [carrito, setCarrito] = useState([]);

  const agregarAlCarrito = (producto) => {
    setCarrito([...carrito, producto]);
  };

  const eliminarDelCarrito = (index) => {
    setCarrito(carrito.filter((_, i) => i !== index));
  };

  return (
    <CartContext.Provider value={{ carrito, agregarAlCarrito, eliminarDelCarrito }}>
      {children}
    </CartContext.Provider>
  );
}

// Hook para usar el carrito desde cualquier componente:
// const { carrito, agregarAlCarrito, eliminarDelCarrito } = useCart();
export function useCart() {
  return useContext(CartContext);
}