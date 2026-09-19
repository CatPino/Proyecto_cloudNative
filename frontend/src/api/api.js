import axios from "axios";
import { msalInstance } from "../msalInstance";

// Clientes axios por microservicio, configurados con variables de entorno
export const inventarioApi = axios.create({
  baseURL: import.meta.env.VITE_INVENTARIO_URL || "http://localhost:8082/api/inventario",
});

export const compraApi = axios.create({
  baseURL: import.meta.env.VITE_COMPRA_URL || "http://localhost:8081/api/compras",
});

export const usuarioApi = axios.create({
  baseURL: import.meta.env.VITE_USUARIO_URL || "http://localhost:8083/api/usuarios",
});

async function getAccessToken(scopes = [import.meta.env.VITE_API_SCOPE]) {
  try {
    const accounts = msalInstance.getAllAccounts();
    if (accounts && accounts.length > 0) {
      const activeScopes = scopes.filter(Boolean);
      const response = await msalInstance.acquireTokenSilent({
        scopes: activeScopes.length > 0 ? activeScopes : ["openid", "profile"],
        account: accounts[0],
      });
      return response.accessToken;
    }
  } catch (error) {
    console.warn("No se pudo obtener token silenciosamente:", error);
  }
  return null;
}

const attachAuthInterceptor = (apiInstance, scopes) => {
  apiInstance.interceptors.request.use(
    async (config) => {
      const token = await getAccessToken(scopes);
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => Promise.reject(error)
  );
};

attachAuthInterceptor(inventarioApi, [import.meta.env.VITE_API_SCOPE]);
attachAuthInterceptor(compraApi, [import.meta.env.VITE_API_SCOPE]);
attachAuthInterceptor(usuarioApi, [import.meta.env.VITE_API_SCOPE]);
