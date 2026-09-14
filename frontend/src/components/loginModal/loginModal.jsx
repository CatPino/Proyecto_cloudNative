import "./loginModal.css";

function LoginModal({ cerrar }) {
  return (
    <div className="modal-overlay">
      <div className="modal-content card p-4">
        <h3>Iniciar sesión</h3>
        <p>Login con Microsoft (Entra ID) — próximamente.</p>
        <button className="btn btn-outline-primary" onClick={cerrar}>
          Cerrar
        </button>
      </div>
    </div>
  );
}

export default LoginModal;