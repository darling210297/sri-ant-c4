# Sistema de Consulta SRI–ANT (Arquitectura C4)

ISWZ2202 – Diseño y Arquitectura de Software.

- **Frontend:** React 18 + Vite (`frontend/`)
- **Backend:** Java 17 + Spring Boot 3 (`backend/`)
- **Caché cloud:** Redis administrado (Upstash / Redis Cloud) con patrón **Cache-Aside + stale-if-error**
- **Diagrama C4 (IcePanel):** `<pega aquí el enlace de solo lectura de IcePanel>`

## Flujo

1. El usuario ingresa **correo** y **RUC**. Se valida el formato (correo, 13 dígitos, termina en 001, cédula válida).
2. La API consulta al SRI si **existe el contribuyente** y luego **obtiene sus datos**; verifica que sea **persona natural**.
3. El usuario ingresa la **placa**. La API consulta el **vehículo** (SRI) y, con la cédula (10 primeros dígitos del RUC), los **puntos de la licencia** (ANT), en paralelo.
4. Como la ANT tiene baja disponibilidad, cada respuesta adecuada se guarda en caché; si la ANT falla se devuelve el último dato bueno con un aviso.

## Contrato de la API

| Método | Ruta | Éxito | Errores |
|---|---|---|---|
| GET | `/api/v1/contribuyentes/{ruc}` | 200 | 400 `VALIDACION`/`RUC_INVALIDO`, 404 `NO_ES_CONTRIBUYENTE`, 422 `NO_ES_PERSONA_NATURAL`, 503 `SRI_NO_DISPONIBLE` |
| GET | `/api/v1/vehiculos/{placa}` | 200 | 400 `PLACA_INVALIDA`, 404 `VEHICULO_NO_ENCONTRADO`, 503 `SRI_NO_DISPONIBLE` |
| GET | `/api/v1/licencias/{cedula}/puntos?placa=` | 200 | 400 `CEDULA_INVALIDA`, 503 `ANT_NO_DISPONIBLE` / `ANT_RESPUESTA_INVALIDA` |

Respuesta exitosa:

```json
{ "data": { ... }, "meta": { "fuente": "EXTERNO | CACHE | CACHE_RESPALDO", "desactualizado": false,
  "obtenidoEn": "...", "vigenteHasta": "...", "aviso": "solo cuando el dato no es reciente" } }
```

Error (formato único):

```json
{ "estado": 422, "codigo": "NO_ES_PERSONA_NATURAL",
  "mensaje": "El RUC pertenece a un contribuyente de tipo 'SOCIEDAD', no a una persona natural.",
  "ruta": "/api/v1/contribuyentes/1790011674001", "timestamp": "2026-11-17T22:00:00Z" }
```

## Caché

Ver [`docs/cache.md`](docs/cache.md) (patrón, TTL y justificación).

| Dato | Vigente | Respaldo |
|---|---|---|
| Contribuyente | 24 h | 7 días |
| Vehículo | 12 h | 7 días |
| Puntos ANT | 1 h | 3 días |

## Ejecutar

### Backend

```bash
cd backend
# Caché en la nube (Upstash): copia la URL rediss:// de tu consola
export REDIS_URL="rediss://default:<password>@<host>.upstash.io:6379"
mvn spring-boot:run           # http://localhost:8080
mvn test                      # pruebas del patrón de caché y validaciones
```

Sin cuenta cloud, para desarrollo: `docker run -p 6379:6379 redis:7` (usa `redis://localhost:6379` por defecto).

### Frontend

```bash
cd frontend
npm install
npm run dev                   # http://localhost:5173 (proxy /api → :8080)
```

### Todo con Docker

```bash
REDIS_URL="rediss://..." docker compose up --build
```

## Arquitectura C4

- Guía para IcePanel con elementos, conexiones y enlaces a GitHub: [`docs/c4-icepanel.md`](docs/c4-icepanel.md)
- Modelo equivalente en Structurizr DSL: [`docs/workspace.dsl`](docs/workspace.dsl)

> Nota: los servicios del SRI y la ANT son públicos pero pueden bloquear tráfico desde fuera de Ecuador; ejecuta el backend desde una red ecuatoriana para la demo.
