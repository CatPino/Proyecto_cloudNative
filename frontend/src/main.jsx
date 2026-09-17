import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { PublicClientApplication } from '@azure/msal-browser';
import { MsalProvider } from '@azure/msal-react';
import 'bootstrap/dist/css/bootstrap.min.css';
import './index.css'
import App from './App.jsx'
import { msalConfig } from './authConfig.js';

const msalInstance = new PublicClientApplication(msalConfig);

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <MsalProvider instance={msalInstance}>
      <App />
    </MsalProvider>
  </StrictMode>,
)