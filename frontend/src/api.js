// URL del backend. En desarrollo se usa el proxy de Vite (/api → localhost:8080).
const BASE = import.meta.env.VITE_API_URL ?? '';

export class ApiError extends Error {
  constructor({ estado, codigo, mensaje, detalles }) {
    super(mensaje);
    this.estado = estado;
    this.codigo = codigo;
    this.detalles = detalles ?? [];
  }
}

async function get(path) {
  let res;
  try {
    res = await fetch(`${BASE}${path}`, { headers: { Accept: 'application/json' } });
  } catch {
    throw new ApiError({ estado: 0, codigo: 'BACKEND_NO_DISPONIBLE', mensaje: 'No se pudo conectar con el servidor.' });
  }
  const body = await res.json().catch(() => null);
  if (!res.ok) {
    throw new ApiError(body ?? { estado: res.status, codigo: 'ERROR', mensaje: `Error ${res.status}` });
  }
  return body; // { data, meta }
}

export const api = {
  contribuyente: (ruc) => get(`/api/v1/contribuyentes/${encodeURIComponent(ruc)}`),
  vehiculo: (placa) => get(`/api/v1/vehiculos/${encodeURIComponent(placa)}`),
  puntos: (cedula, placa) =>
    get(`/api/v1/licencias/${encodeURIComponent(cedula)}/puntos${placa ? `?placa=${encodeURIComponent(placa)}` : ''}`),
};
