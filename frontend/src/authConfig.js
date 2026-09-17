export const msalConfig = {
  auth: {
    clientId: import.meta.env.VITE_AZURE_CLIENT_ID,
    authority: `https://login.microsoftonline.com/${import.meta.env.VITE_AZURE_TENANT_ID}`,
    redirectUri: window.location.origin,
    postLogoutRedirectUri: window.location.origin,
  },
  cache: {
    cacheLocation: "sessionStorage",
  },
};

export const loginRequest = {
  scopes: ["openid", "profile"],
};

export const protectedResources = {
  inventario: {
    endpoint: import.meta.env.VITE_INVENTARIO_URL,
    scopes: [import.meta.env.VITE_API_SCOPE],
  },
  compra: {
    endpoint: import.meta.env.VITE_COMPRA_URL,
    scopes: [import.meta.env.VITE_API_SCOPE],
  },
  usuario: {
    endpoint: import.meta.env.VITE_USUARIO_URL,
    scopes: [import.meta.env.VITE_API_SCOPE],
  },
};