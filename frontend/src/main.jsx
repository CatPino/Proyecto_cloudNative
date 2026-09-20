import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { MsalProvider } from '@azure/msal-react';
import 'bootstrap/dist/css/bootstrap.min.css';
import './index.css'
import App from './App.jsx'
import { msalInstance } from './msalInstance';

await msalInstance.initialize();

// Procesa la respuesta del login redirect antes de renderizar
await msalInstance.handleRedirectPromise().catch((error) => {
  console.error(error);
});

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <MsalProvider instance={msalInstance}>
      <App />
    </MsalProvider>
  </StrictMode>,
)