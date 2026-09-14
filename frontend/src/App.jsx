import { BrowserRouter, Routes, Route } from "react-router-dom";
import { CartProvider } from "./context/cartContext";
import Navbar from "./components/navbar/navbar";
import Home from "./pages/home/home";
import Inventario from "./pages/inventario/inventario";
import Compra from "./pages/compra/compra";
import Usuario from "./pages/Usuario/usuario";

function App() {
  return (
    <CartProvider>
      <BrowserRouter>
        <div style={{ padding: "0 2rem 2rem" }}>
          <Navbar />

          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/inventario" element={<Inventario />} />
            <Route path="/compra" element={<Compra />} />
            <Route path="/usuario" element={<Usuario />} />
          </Routes>
        </div>
      </BrowserRouter>
    </CartProvider>
  );
}

export default App;