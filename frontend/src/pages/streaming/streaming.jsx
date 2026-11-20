import { useEffect, useState, useCallback } from "react";
import { compraApi, inventarioApi, usuarioApi } from "../../api/api";

function formatearMiles(valor) {
  if (valor === "" || valor === null || valor === undefined) return "0";
  return Number(valor).toLocaleString("es-CL");
}

function formatearHora(iso) {
  if (!iso) return "-";
  try {
    return new Date(iso).toLocaleTimeString("es-CL");
  } catch {
    return iso;
  }
}

function badgeSeveridad(sev) {
  if (sev === "CRITICAL") return "bg-danger";
  if (sev === "WARNING") return "bg-warning text-dark";
  return "bg-info text-dark";
}

function Streaming() {
  const [resumen, setResumen] = useState(null);
  const [productosInventario, setProductosInventario] = useState([]);
  const [estadoConexion, setEstadoConexion] = useState("Verificando...");
  const [ultimoEnvio, setUltimoEnvio] = useState(null);
  const [error, setError] = useState("");
  const [enviando, setEnviando] = useState(false);

  const [form, setForm] = useState({
    producto: "",
    cantidad: 1,
    precioUnitario: 25000,
    persistirCompra: true,
  });

  const cargarDatos = useCallback(async () => {
    try {
      const [resAnalitica, resInv] = await Promise.all([
        usuarioApi.get("/analitica/resumen"),
        inventarioApi.get("").catch(() => ({ data: [] })),
      ]);
      setResumen(resAnalitica.data);
      setProductosInventario(Array.isArray(resInv.data) ? resInv.data : []);
      setEstadoConexion("Conectado a Kafka Streaming y Analítica");
    } catch (err) {
      console.error("Error consultando analítica Kafka:", err);
      setEstadoConexion("Sin conexión con el servicio de Analítica (usuario:8083)");
    }
  }, []);

  useEffect(() => {
    cargarDatos();
    const id = setInterval(cargarDatos, 3000);
    return () => clearInterval(id);
  }, [cargarDatos]);

  const seleccionarProducto = (nombre) => {
    const encontrado = productosInventario.find((p) => p.producto === nombre);
    setForm((prev) => ({
      ...prev,
      producto: nombre,
      precioUnitario: encontrado ? encontrado.precio : prev.precioUnitario,
    }));
  };

  const publicarEvento = async (e) => {
    e.preventDefault();
    if (!form.producto.trim()) {
      setError("Ingresa o selecciona un producto.");
      return;
    }
    setError("");
    setEnviando(true);
    try {
      const res = await compraApi.post("/kafka/publicar", {
        producto: form.producto.trim(),
        cantidad: Number(form.cantidad),
        precioUnitario: Number(form.precioUnitario),
        persistirCompra: form.persistirCompra,
      });
      setUltimoEnvio(
        `Evento publicado en ${res.data.topic} [Partición ${res.data.partition}, Offset ${res.data.offset}, Key="${res.data.key}"]`
      );
      setTimeout(cargarDatos, 700);
    } catch (err) {
      console.error(err);
      setError(
        "No se pudo publicar en Kafka. Verifica haber iniciado sesión y que Compra (8081) y el clúster Kafka estén corriendo."
      );
    } finally {
      setEnviando(false);
    }
  };

  const lanzarRafaga = async () => {
    setError("");
    setEnviando(true);
    try {
      const nombresInv = productosInventario.map((p) => p.producto).filter(Boolean);
      const productosPayload =
        nombresInv.length >= 3
          ? nombresInv
          : [...nombresInv, "Laptop Pro", "Mouse Gamer", "Teclado Mecanico", "Monitor 4K"];

      const res = await compraApi.post("/kafka/rafaga", {
        cantidadEventos: 9,
        productos: productosPayload,
      });
      const dist = Object.entries(res.data.distribucionParticiones || {})
        .map(([p, c]) => `P${p}: ${c}`)
        .join(" | ");
      setUltimoEnvio(
        `Ráfaga de ${res.data.totalEventos} eventos enviada a compras.eventos.stream (${dist})`
      );
      setTimeout(cargarDatos, 1000);
    } catch (err) {
      console.error(err);
      setError("No se pudo enviar la ráfaga a Kafka. Revisa sesión y servicios activos.");
    } finally {
      setEnviando(false);
    }
  };

  const reiniciarMetricas = async () => {
    setError("");
    try {
      await usuarioApi.delete("/analitica/reset");
      setUltimoEnvio("Métricas en memoria reiniciadas.");
      cargarDatos();
    } catch (err) {
      console.error(err);
      setError("No se pudieron reiniciar las métricas.");
    }
  };

  const distCompras = resumen?.distribucionComprasParticiones || { P0: 0, P1: 0, P2: 0 };
  const distAlertas = resumen?.distribucionAlertasParticiones || { P0: 0, P1: 0, P2: 0 };
  const totalDistCompras = Math.max(
    1,
    Object.values(distCompras).reduce((acc, v) => acc + Number(v || 0), 0)
  );

  return (
    <div className="container my-3">
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-2">
        <h2 className="mb-0">Streaming y Analítica (Apache Kafka)</h2>
        <a
          href="http://localhost:8090"
          target="_blank"
          rel="noreferrer"
          className="btn btn-outline-dark btn-sm"
        >
          Abrir Kafka UI (localhost:8090)
        </a>
      </div>

      <p
        style={{
          color: estadoConexion.includes("Conectado") ? "green" : "#b91c1c",
          fontWeight: "bold",
        }}
      >
        {estadoConexion}
      </p>

      <div className="alert alert-light border mb-3">
        <strong>Arquitectura Dual:</strong> RabbitMQ sigue operando de forma independiente en la pestaña{" "}
        <em>Eventos</em> (colas transaccionales + DLQ). Kafka complementa el sistema con 2 tópicos de{" "}
        <strong>3 particiones y 3 réplicas</strong>: <code>compras.eventos.stream</code> (retención 7d) e{" "}
        <code>inventario.alertas.stream</code> (retención 24h), consumidos en paralelo por{" "}
        <code>inventario-stock-group</code>, <code>analitica-ventas-group</code> y{" "}
        <code>monitoreo-alertas-group</code>.
      </div>

      {error && <div className="alert alert-danger">{error}</div>}
      {ultimoEnvio && <div className="alert alert-success">{ultimoEnvio}</div>}

      {/* Panel productor */}
      <div className="card mb-4">
        <div className="card-header fw-bold">Publicar Eventos al Stream de Compras</div>
        <div className="card-body">
          <form onSubmit={publicarEvento} className="row g-2 align-items-end">
            <div className="col-md-4">
              <label className="form-label">Producto (key de partición)</label>
              <input
                className="form-control"
                list="lista-productos-inv"
                placeholder="Ej: Laptop Pro"
                value={form.producto}
                onChange={(e) => seleccionarProducto(e.target.value)}
                required
              />
              <datalist id="lista-productos-inv">
                {productosInventario.map((p) => (
                  <option key={p.id} value={p.producto}>
                    Stock actual: {p.stock}
                  </option>
                ))}
              </datalist>
            </div>
            <div className="col-md-2">
              <label className="form-label">Cantidad</label>
              <input
                type="number"
                min="1"
                className="form-control"
                value={form.cantidad}
                onChange={(e) => setForm({ ...form, cantidad: e.target.value })}
                required
              />
            </div>
            <div className="col-md-2">
              <label className="form-label">Precio Unitario</label>
              <input
                type="number"
                min="1"
                className="form-control"
                value={form.precioUnitario}
                onChange={(e) => setForm({ ...form, precioUnitario: e.target.value })}
                required
              />
            </div>
            <div className="col-md-4 d-flex gap-2 flex-wrap">
              <button type="submit" className="btn btn-primary" disabled={enviando}>
                Publicar Compra
              </button>
              <button
                type="button"
                className="btn btn-success"
                onClick={lanzarRafaga}
                disabled={enviando}
              >
                Simular Ráfaga (9)
              </button>
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={reiniciarMetricas}
              >
                Reset
              </button>
            </div>
            <div className="col-12 mt-2">
              <div className="form-check">
                <input
                  id="chkPersistir"
                  className="form-check-input"
                  type="checkbox"
                  checked={form.persistirCompra}
                  onChange={(e) => setForm({ ...form, persistirCompra: e.target.checked })}
                />
                <label className="form-check-label" htmlFor="chkPersistir">
                  Guardar también en tabla <code>compra</code> (Supabase) y descontar stock real en{" "}
                  <code>inventario</code>
                </label>
              </div>
            </div>
          </form>
        </div>
      </div>

      {/* KPIs en tiempo real */}
      <div className="row g-3 mb-4">
        <div className="col-md-3">
          <div className="card text-center p-3 h-100">
            <small className="text-muted">Compras en Stream</small>
            <h3 className="mb-0">{resumen?.totalComprasProcesadas ?? 0}</h3>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card text-center p-3 h-100">
            <small className="text-muted">Unidades Vendidas</small>
            <h3 className="mb-0">{resumen?.totalUnidadesVendidas ?? 0}</h3>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card text-center p-3 h-100">
            <small className="text-muted">Ingresos Acumulados</small>
            <h3 className="mb-0">${formatearMiles(resumen?.ingresosAcumulados ?? 0)}</h3>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card text-center p-3 h-100">
            <small className="text-muted">Alertas de Stock</small>
            <h3 className="mb-0">
              <span className="text-danger">{resumen?.alertasCriticas ?? 0}</span> /{" "}
              <span className="text-warning">{resumen?.alertasAdvertencia ?? 0}</span>
            </h3>
            <small className="text-muted">Críticas / Stock bajo</small>
          </div>
        </div>
      </div>

      {/* Distribución por particiones + Ranking productos */}
      <div className="row g-3 mb-4">
        <div className="col-md-5">
          <div className="card h-100">
            <div className="card-header fw-bold">
              Distribución por Particiones (Paralelismo)
            </div>
            <div className="card-body">
              <p className="small text-muted mb-2">
                <code>compras.eventos.stream</code> (key = producto)
              </p>
              {Object.entries(distCompras).map(([part, count]) => {
                const pct = Math.round((Number(count || 0) / totalDistCompras) * 100);
                return (
                  <div key={part} className="mb-2">
                    <div className="d-flex justify-content-between small">
                      <span>Partición {part}</span>
                      <strong>{count} eventos</strong>
                    </div>
                    <div className="progress" style={{ height: "10px" }}>
                      <div className="progress-bar" style={{ width: `${pct}%` }} />
                    </div>
                  </div>
                );
              })}
              <hr />
              <p className="small text-muted mb-1">
                <code>inventario.alertas.stream</code>:
              </p>
              <div className="d-flex gap-2">
                {Object.entries(distAlertas).map(([part, count]) => (
                  <span key={part} className="badge bg-secondary">
                    {part}: {count}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="col-md-7">
          <div className="card h-100">
            <div className="card-header fw-bold">
              Analítica de Ventas por Producto (<code>analitica-ventas-group</code>)
            </div>
            <div className="card-body p-0">
              <table className="table table-sm table-hover mb-0">
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th>Compras</th>
                    <th>Unidades</th>
                    <th>Ingresos</th>
                  </tr>
                </thead>
                <tbody>
                  {!resumen?.ventasPorProducto?.length ? (
                    <tr>
                      <td colSpan="4" className="text-center text-muted py-3">
                        Aún no hay eventos analizados. Publica una compra o lanza una ráfaga.
                      </td>
                    </tr>
                  ) : (
                    resumen.ventasPorProducto.map((p) => (
                      <tr key={p.producto}>
                        <td>{p.producto}</td>
                        <td>{p.compras}</td>
                        <td>{p.unidades}</td>
                        <td>${formatearMiles(p.ingresos)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      {/* Alertas de inventario en tiempo real */}
      <div className="card mb-4">
        <div className="card-header fw-bold">
          Reacción Automática de Inventario y Alertas (<code>inventario.alertas.stream</code>)
        </div>
        <div className="card-body p-0">
          <table className="table table-sm table-hover mb-0">
            <thead>
              <tr>
                <th>Hora</th>
                <th>Severidad</th>
                <th>Tipo</th>
                <th>Producto</th>
                <th>Stock</th>
                <th>Detalle</th>
                <th>Part / Offset</th>
              </tr>
            </thead>
            <tbody>
              {!resumen?.ultimasAlertas?.length ? (
                <tr>
                  <td colSpan="7" className="text-center text-muted py-3">
                    Sin alertas todavía. Al comprar un producto del inventario, el stock se descontará automáticamente aquí.
                  </td>
                </tr>
              ) : (
                resumen.ultimasAlertas.map((a) => (
                  <tr key={a.eventId}>
                    <td>{formatearHora(a.timestamp)}</td>
                    <td>
                      <span className={`badge ${badgeSeveridad(a.severidad)}`}>{a.severidad}</span>
                    </td>
                    <td><code>{a.eventType}</code></td>
                    <td>{a.producto}</td>
                    <td>
                      {a.stockAnterior !== null && a.stockAnterior !== undefined
                        ? `${a.stockAnterior} -> ${a.stockActual}`
                        : "N/A"}
                    </td>
                    <td>{a.mensaje}</td>
                    <td>P{a.partition} / #{a.offset}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Últimos eventos del stream de compras */}
      <div className="card mb-4">
        <div className="card-header fw-bold">
          Últimos Eventos Consumidos (<code>compras.eventos.stream</code>)
        </div>
        <div className="card-body p-0">
          <table className="table table-sm table-hover mb-0">
            <thead>
              <tr>
                <th>Hora</th>
                <th>Tipo</th>
                <th>Producto</th>
                <th>Cant.</th>
                <th>Total</th>
                <th>Origen</th>
                <th>Part / Offset</th>
              </tr>
            </thead>
            <tbody>
              {!resumen?.ultimasCompras?.length ? (
                <tr>
                  <td colSpan="7" className="text-center text-muted py-3">
                    Sin eventos recibidos todavía.
                  </td>
                </tr>
              ) : (
                resumen.ultimasCompras.map((c) => (
                  <tr key={c.eventId}>
                    <td>{formatearHora(c.timestamp)}</td>
                    <td><code>{c.eventType}</code></td>
                    <td>{c.producto}</td>
                    <td>{c.cantidad}</td>
                    <td>${formatearMiles(c.montoTotal)}</td>
                    <td>{c.origen}</td>
                    <td>P{c.partition} / #{c.offset}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default Streaming;
