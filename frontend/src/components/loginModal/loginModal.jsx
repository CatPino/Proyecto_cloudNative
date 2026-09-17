import { useMsal, useIsAuthenticated } from "@azure/msal-react";
import AuthButtons from "../authButtons/authButtons";
import "./loginModal.css";

function LoginModal({ cerrar }) {
  const { accounts } = useMsal();
  const isAuthenticated = useIsAuthenticated();

  return (
    <div className="modal-overlay">
      <div className="modal-content card p-4">
        <h3>Iniciar sesión</h3>

        {isAuthenticated ? (
          <p>Sesión iniciada como {accounts[0]?.name || accounts[0]?.username}</p>
        ) : (
          <p>Inicia sesión con tu cuenta de Microsoft (Entra ID).</p>
        )}

        <AuthButtons />

        <button className="btn btn-outline-primary mt-2" onClick={cerrar}>
          Cerrar
        </button>
      </div>
    </div>
  );
}

export default LoginModal;