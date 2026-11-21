import { useCallback, useEffect, useState } from "react";
import { rabbitmqAdminApi } from "../../api/api";

function RabbitMqAdmin() {
  const [estado, setEstado] = useState(null);
  const [conexion, setConexion] = useState("Verificando...");
  const [mensaje, setMensaje] = useState("");
  const [error, setError] = useState("");

  const [exchangeForm, setExchangeForm] = useState({ nombre: "", tipo: "topic", durable: true });
  const [colaForm, setColaForm] = useState({ nombre: "", durable: true, crearDlqAsociada: true });
  const [bindingForm, setBindingForm] = useState({
    cola: "",
    exchange: "tienda.topic.exchange",
    routingKey: "compra.*",
  });

  const cargarEstado = useCallback(async () => {
    try {
      const res = await rabbitmqAdminApi.get("/estado");
      setEstado(res.data);
      setConexion("Conectado a rabbitmq-admin (puerto 8084 · Spring AmqpAdmin)");
    } catch (err) {
      console.error("Error consultando rabbitmq-admin:", err);
      setConexion("Sin conexión con rabbitmq-admin (8084)");
    }
  }, []);

  useEffect(() => {
    cargarEstado();
    const id = setInterval(cargarEstado, 5000);
    return () => clearInterval(id);
  }, [cargarEstado]);

  const inicializarTopologia = async () => {
    setError("");
    setMensaje("");
    try {
      await rabbitmqAdminApi.post("/topologia/inicializar");
      setMensaje("Topología base (Direct + Topic + Colas + DLQs) inicializada vía AmqpAdmin.");
      cargarEstado();
    } catch (err) {
      setError(err.response?.data?.error || "No se pudo inicializar la topología.");
    }
  };

  const crearExchange = async (e) => {
    e.preventDefault();
    setError("");
    setMensaje("");
    try {
      const res = await rabbitmqAdminApi.post("/exchanges", exchangeForm);
      setMensaje(`Exchange '${res.data.nombre}' (${res.data.tipo}) declarado vía AmqpAdmin.`);
      setExchangeForm({ nombre: "", tipo: "topic", durable: true });
      cargarEstado();
    } catch (err) {
      setError(err.response?.data?.error || "No se pudo declarar el exchange.");
    }
  };

  const crearCola = async (e) => {
    e.preventDefault();
    setError("");
    setMensaje("");
    try {
      const res = await rabbitmqAdminApi.post("/colas", colaForm);
      const extra = res.data.dlqAsociada ? ` con DLQ '${res.data.dlqAsociada}'` : "";
      setMensaje(`Cola '${res.data.nombre}' declarada${extra} vía AmqpAdmin.`);
      setColaForm({ nombre: "", durable: true, crearDlqAsociada: true });
      cargarEstado();
    } catch (err) {
      setError(err.response?.data?.error || "No se pudo declarar la cola.");
    }
  };

  const crearBinding = async (e) => {
    e.preventDefault();
    setError("");
    setMensaje("");
    try {
      const res = await rabbitmqAdminApi.post("/bindings", bindingForm);
      setMensaje(
        `Binding creado vía AmqpAdmin: '${res.data.cola}' <- '${res.data.exchange}' (rk='${res.data.routingKey}').`
      );
      setBindingForm((prev) => ({ ...prev, cola: "" }));
      cargarEstado();
    } catch (err) {
      setError(err.response?.data?.error || "No se pudo crear el binding.");
    }
  };

  const purgarCola = async (nombre) => {
    setError("");
    setMensaje("");
    try {
      await rabbitmqAdminApi.post(`/colas/${encodeURIComponent(nombre)}/purgar`);
      setMensaje(`Cola '${nombre}' purgada vía AmqpAdmin.`);
      cargarEstado();
    } catch (err) {
      setError(err.response?.data?.error || `No se pudo purgar '${nombre}'.`);
    }
  };

  const eliminarCola = async (nombre) => {
    setError("");
    setMensaje("");
    try {
      await rabbitmqAdminApi.delete(`/colas/${encodeURIComponent(nombre)}`);
      setMensaje(`Cola '${nombre}' eliminada vía AmqpAdmin.`);
      cargarEstado();
    } catch (err) {
      setError(err.response?.data?.error || `No se pudo eliminar '${nombre}'.`);
    }
  };

  return (
    <div className="container my-3">
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-2">
        <h2 className="mb-0">Administrador de RabbitMQ (AmqpAdmin)</h2>
        <div className="d-flex gap-2">
          <button className="btn btn-primary btn-sm" onClick={inicializarTopologia}>
            Inicializar Topología Base
          </button>
          <a
            href="http://localhost:15672"
            target="_blank"
            rel="noreferrer"
            className="btn btn-outline-dark btn-sm"
          >
            Abrir RabbitMQ UI (15672)
          </a>
        </div>
      </div>

      <p style={{ color: conexion.includes("Conectado") ? "green" : "#b91c1c", fontWeight: "bold" }}>
        {conexion}
      </p>

      {error && <div className="alert alert-danger">{error}</div>}
      {mensaje && <div className="alert alert-success">{mensaje}</div>}

      {/* Estado de colas vía AmqpAdmin */}
      <div className="card mb-4">
        <div className="card-header fw-bold">Colas Administradas (AmqpAdmin.getQueueInfo)</div>
        <div className="card-body p-0">
          <table className="table table-sm table-hover mb-0">
            <thead>
              <tr>
                <th>Cola</th>
                <th>Estado</th>
                <th>Mensajes</th>
                <th>Consumidores</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {!estado?.colas?.length ? (
                <tr>
                  <td colSpan="5" className="text-center text-muted py-3">
                    Inicia el microservicio <code>rabbitmq-admin</code> (puerto 8084) para administrar RabbitMQ.
                  </td>
                </tr>
              ) : (
                estado.colas.map((c) => (
                  <tr key={c.nombre}>
                    <td><code>{c.nombre}</code></td>
                    <td>
                      <span className={`badge ${c.existe ? "bg-success" : "bg-secondary"}`}>
                        {c.existe ? "Activa" : "No creada"}
                      </span>
                    </td>
                    <td>{c.messageCount}</td>
                    <td>{c.consumerCount}</td>
                    <td>
                      <button
                        className="btn btn-sm btn-outline-warning me-2"
                        onClick={() => purgarCola(c.nombre)}
                      >
                        Purgar
                      </button>
                      <button
                        className="btn btn-sm btn-outline-danger"
                        onClick={() => eliminarCola(c.nombre)}
                      >
                        Eliminar
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Formularios dinámicos AmqpAdmin */}
      <div className="row g-3">
        <div className="col-md-4">
          <div className="card h-100">
            <div className="card-header fw-bold">Declarar Exchange</div>
            <div className="card-body">
              <form onSubmit={crearExchange}>
                <input
                  className="form-control mb-2"
                  placeholder="Nombre (ej: pedidos.topic)"
                  value={exchangeForm.nombre}
                  onChange={(e) => setExchangeForm({ ...exchangeForm, nombre: e.target.value })}
                  required
                />
                <select
                  className="form-select mb-2"
                  value={exchangeForm.tipo}
                  onChange={(e) => setExchangeForm({ ...exchangeForm, tipo: e.target.value })}
                >
                  <option value="direct">direct</option>
                  <option value="topic">topic</option>
                  <option value="fanout">fanout</option>
                  <option value="headers">headers</option>
                </select>
                <button className="btn btn-primary btn-sm w-100" type="submit">
                  Crear Exchange
                </button>
              </form>
            </div>
          </div>
        </div>

        <div className="col-md-4">
          <div className="card h-100">
            <div className="card-header fw-bold">Declarar Cola (+ DLQ)</div>
            <div className="card-body">
              <form onSubmit={crearCola}>
                <input
                  className="form-control mb-2"
                  placeholder="Nombre (ej: envios.queue)"
                  value={colaForm.nombre}
                  onChange={(e) => setColaForm({ ...colaForm, nombre: e.target.value })}
                  required
                />
                <div className="form-check mb-2">
                  <input
                    id="chkDlq"
                    type="checkbox"
                    className="form-check-input"
                    checked={colaForm.crearDlqAsociada}
                    onChange={(e) => setColaForm({ ...colaForm, crearDlqAsociada: e.target.checked })}
                  />
                  <label className="form-check-label small" htmlFor="chkDlq">
                    Crear DLQ asociada automáticamente (<code>.dlq</code>)
                  </label>
                </div>
                <button className="btn btn-success btn-sm w-100" type="submit">
                  Crear Cola
                </button>
              </form>
            </div>
          </div>
        </div>

        <div className="col-md-4">
          <div className="card h-100">
            <div className="card-header fw-bold">Declarar Binding</div>
            <div className="card-body">
              <form onSubmit={crearBinding}>
                <input
                  className="form-control mb-2"
                  placeholder="Cola destino"
                  value={bindingForm.cola}
                  onChange={(e) => setBindingForm({ ...bindingForm, cola: e.target.value })}
                  required
                />
                <input
                  className="form-control mb-2"
                  placeholder="Exchange origen"
                  value={bindingForm.exchange}
                  onChange={(e) => setBindingForm({ ...bindingForm, exchange: e.target.value })}
                  required
                />
                <input
                  className="form-control mb-2"
                  placeholder="Routing key (ej: compra.*)"
                  value={bindingForm.routingKey}
                  onChange={(e) => setBindingForm({ ...bindingForm, routingKey: e.target.value })}
                />
                <button className="btn btn-dark btn-sm w-100" type="submit">
                  Crear Binding
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default RabbitMqAdmin;
