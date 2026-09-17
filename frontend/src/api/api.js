import axios from "axios";

// Un cliente axios por microservicio, cada uno con su propio puerto.
export const inventarioApi = axios.create({
  baseURL: "http://localhost:8082/api/inventario",
});

export const compraApi = axios.create({
  baseURL: "http://localhost:8081/api/compras",
});

export const usuarioApi = axios.create({
  baseURL: "http://localhost:8083/api/usuarios",
});
