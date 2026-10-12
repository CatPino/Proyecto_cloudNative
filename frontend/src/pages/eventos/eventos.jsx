import { useState, useEffect } from "react";
import { compraApi } from "../../api/api";

const EVENTOS = [
  {
    routingKey: "compra.creada",
    etiqueta: "Compra creada",
    clase: "btn-primary",
    destino: "inventario.queue y auditoria.queue",
  },
  {
    routingKey: "compra.cancelada",
    etiqueta: "Compra cancelada",
    clase: "btn-warning",
    destino: "auditoria.queue",
  },
  {
    routingKey: "inventario.stock.actualizado",
    etiqueta: "Stock actualizado",
    clase: "btn-success",
    destino: "auditoria.queue",
  },
  // Nuevo evento agregado para probar la DLQ
  {
    routingKey: "compra.creada",
    etiqueta: "Simular Fallo",
    clase: "btn-danger",
    destino: "tienda.dlq (tras 3 reintentos)",
    isFail: true,
  },
];

function Eventos() {
  const [historial, setHistorial] = useState([]);
  const [error, setError] = useState("");
  
  // Nuevos estados para DLQ y monitoreo
  const [metrics, setMetrics] = useState({ dlqReady: 0, alert: false });
  const [backendStatus, setBackendStatus] = useState("Verificando...");

  // Función para consultar las métricas de RabbitMQ
  const fetchMetrics = async () => {
    try {
      const response = await compraApi.get("/eventos/metrics");
      setMetrics(response.data);
      setBackendStatus("Conectado a Backend y RabbitMQ");
    } catch (error) {
      console.error("Error al consultar métricas:", error);
      setBackendStatus("Desconectado del Backend");
    }
  };

  // Polling cada 5 segundos
  useEffect(() => {
    fetchMetrics();
    const intervalId = setInterval(fetchMetrics, 5000);
    return () => clearInterval(intervalId);
  }, []);

  const enviar = async (evento) => {
    setError("");
    // Si es el evento de fallo, enviamos la palabra clave "FAIL"
    const mensajePayload = evento.isFail 
      ? "FAIL: Prueba de fallo forzado" 
      : `${evento.etiqueta} (enviado desde el frontend)`;

    try {
      await compraApi.post("/eventos", {
        routingKey: evento.routingKey,
        mensaje: mensajePayload,
      });
      
      setHistorial((prev) => [
        {
          hora: new Date().toLocaleTimeString("es-CL"),
          routingKey: evento.routingKey,
          destino: evento.destino,
        },
        ...prev,
      ]);

      // Refrescar las métricas con un ligero retraso para dar tiempo a los reintentos
      setTimeout(fetchMetrics, evento.isFail ? 3500 : 1000);

    } catch (err) {
      setError("No se pudo enviar el evento. Revisa que hayas iniciado sesión y que Compra esté corriendo.");
    }
  };

  return (
    <div className="container">
      <h2>Eventos RabbitMQ</h2>
      
      <p style={{ color: backendStatus.includes("Conectado") ? "green" : "red", fontWeight: "bold" }}>
        {backendStatus}
      </p>

      {/* Panel de Alerta DLQ */}
      {metrics.alert && (
        <section style={{ backgroundColor: "#fee2e2", borderLeft: "6px solid #dc2626", padding: "16px", margin: "20px 0", borderRadius: "8px", color: "#991b1b" }}>
          <h3 style={{ marginTop: 0 }}>⚠️ ALERTA DE SISTEMA</h3>
          <p>Existen <strong>{metrics.dlqReady}</strong> mensajes en la Dead Letter Queue (Cuarentena).</p>
          <p>Revisa la consola de RabbitMQ (localhost:15672) para analizar las compras fallidas.</p>
        </section>
      )}

      <p>Cada botón publica un mensaje en el exchange <b>tienda.exchange</b> con su routing key.</p>

      {error && <p className="error-text">{error}</p>}

      <div style={{ display: "flex", gap: "1rem", margin: "1.5rem 0" }}>
        {EVENTOS.map((evento, index) => (
          <button
            key={index}
            className={`btn ${evento.clase}`}
            onClick={() => enviar(evento)}
            style={evento.isFail ? { backgroundColor: "#dc3545", color: "white" } : {}}
          >
            {evento.etiqueta}
          </button>
        ))}
      </div>

      <h3>Eventos enviados</h3>
      {historial.length === 0 ? (
        <p>Aún no se ha enviado ningún evento.</p>
      ) : (
        <table className="table table-hover">
          <thead>
            <tr>
              <th>Hora</th>
              <th>Routing key</th>
              <th>Llega a</th>
            </tr>
          </thead>
          <tbody>
            {historial.map((item, index) => (
              <tr key={index}>
                <td>{item.hora}</td>
                <td>{item.routingKey}</td>
                <td>{item.destino}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default Eventos;