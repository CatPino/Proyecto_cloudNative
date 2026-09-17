import { useMsal, useIsAuthenticated } from "@azure/msal-react";
import { loginRequest } from "../../authConfig";

function AuthButtons() {
  const { instance } = useMsal();
  const isAuthenticated = useIsAuthenticated();

  const login = () => {
    instance.loginRedirect(loginRequest);
  };

  const logout = () => {
    instance.logoutRedirect();
  };

  return isAuthenticated ? (
    <button className="btn btn-outline-primary" onClick={logout}>
      Cerrar sesión
    </button>
  ) : (
    <button className="btn btn-primary" onClick={login}>
      Iniciar sesión
    </button>
  );
}

export default AuthButtons;