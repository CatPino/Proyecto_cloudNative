import { useState } from "react";
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
];

function Eventos() {
  const [historial, setHistorial] = useState([]);
  const [error, setError] = useState("");

  const enviar = async (evento) => {
    setError("");
    try {
      await compraApi.post("/eventos", {
        routingKey: evento.routingKey,
        mensaje: `${evento.etiqueta} (enviado desde el frontend)`,
      });
      setHistorial((prev) => [
        {
          hora: new Date().toLocaleTimeString("es-CL"),
          routingKey: evento.routingKey,
          destino: evento.destino,
        },
        ...prev,
      ]);
    } catch (err) {
      setError("No se pudo enviar el evento. Revisa que hayas iniciado sesión y que Compra esté corriendo.");
    }
  };

  return (
    <div className="container">
      <h2>Eventos RabbitMQ</h2>
      <p>Cada botón publica un mensaje en el exchange <b>tienda.exchange</b> con su routing key.</p>

      {error && <p className="error-text">{error}</p>}

      <div style={{ display: "flex", gap: "1rem", margin: "1.5rem 0" }}>
        {EVENTOS.map((evento) => (
          <button
            key={evento.routingKey}
            className={`btn ${evento.clase}`}
            onClick={() => enviar(evento)}
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