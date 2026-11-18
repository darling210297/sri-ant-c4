# Modelo C4 para IcePanel

Guía para dibujar el modelo en IcePanel (icepanel.io) con exactamente lo que está implementado en este repositorio.
Reemplaza `<usuario>/<repo>` por la URL real de tu repositorio en GitHub.

---

## Nivel 1 – Contexto

| Tipo IcePanel | Nombre | Descripción | Tecnología |
|---|---|---|---|
| Actor | **Usuario** | Persona natural que consulta su información tributaria, de su vehículo y de su licencia. | — |
| System (interno) | **Sistema de Consulta SRI–ANT** | Verifica que el usuario sea contribuyente persona natural, muestra los datos de su vehículo y los puntos de su licencia. | React, Java, Redis |
| System (externo) | **SRI – Catastro de Contribuyentes** | API REST pública: `existePorNumeroRuc`, `obtenerPorNumerosRuc`. | REST/JSON |
| System (externo) | **SRI – Matriculación Vehicular** | API REST pública: `BaseVehiculo/obtenerPorNumeroPlacaOPorNumeroCampvOPorNumeroCpn`. | REST/JSON |
| System (externo) | **ANT – Portal de Citaciones** | Portal web `clp_grid_citaciones.jsp`. **Baja disponibilidad.** | HTTPS/HTML |

### Conexiones (Contexto)

| Origen → Destino | Descripción |
|---|---|
| Usuario → Sistema de Consulta SRI–ANT | Ingresa correo, RUC y placa; ve los resultados |
| Sistema → SRI – Catastro | Verifica si es contribuyente y si es persona natural |
| Sistema → SRI – Matriculación | Obtiene la información del vehículo por placa |
| Sistema → ANT – Portal | Consulta los puntos de la licencia (con caché por baja disponibilidad) |

---

## Nivel 2 – Contenedores (dentro de "Sistema de Consulta SRI–ANT")

| Tipo IcePanel | Nombre | Descripción | Tecnología | Enlace GitHub |
|---|---|---|---|---|
| App | **Aplicación Web** | SPA: formulario de correo/RUC, placa; muestra datos, origen del dato y avisos de datos no recientes. | React 18, Vite | `https://github.com/<usuario>/<repo>/tree/main/frontend` |
| App / API | **API de Consultas** | API REST `/api/v1`. Valida entradas, orquesta las integraciones y aplica Cache-Aside + stale-if-error. | Java 17, Spring Boot 3 | `https://github.com/<usuario>/<repo>/tree/main/backend` |
| Store | **Caché distribuida** | Guarda cada respuesta adecuada con su fecha; sirve el último dato bueno si el servicio externo cae. | Redis en la nube (Upstash / Redis Cloud) | `https://github.com/<usuario>/<repo>/blob/main/backend/src/main/java/ec/edu/udla/sriant/cache/ResilientCache.java` |

### Conexiones (Contenedores)

| Origen → Destino | Descripción | Tecnología |
|---|---|---|
| Usuario → Aplicación Web | Usa en el navegador | HTTPS |
| Aplicación Web → API de Consultas | `GET /api/v1/contribuyentes/{ruc}`, `GET /api/v1/vehiculos/{placa}`, `GET /api/v1/licencias/{cedula}/puntos?placa=` | REST/JSON |
| API de Consultas → Caché distribuida | Lee/escribe entradas `{data, cachedAt}` con TTL | RESP/TLS (rediss://) |
| API de Consultas → SRI – Catastro | Verifica contribuyente y persona natural | HTTPS REST |
| API de Consultas → SRI – Matriculación | Consulta vehículo | HTTPS REST |
| API de Consultas → ANT – Portal | Consulta puntos (respuesta adecuada → caché; fallo → último dato bueno) | HTTPS (HTML/JSON) |

---

## Nivel 3 – Componentes de la API (opcional, suma puntos)

| Componente | Responsabilidad | Archivo |
|---|---|---|
| ConsultaController | Contrato REST y validación de parámetros | `web/ConsultaController.java` |
| GlobalExceptionHandler | Errores estructurados `{estado, codigo, mensaje, ruta, timestamp, detalles}` | `error/GlobalExceptionHandler.java` |
| ContribuyenteService | "Microservicio" de verificación de contribuyente / persona natural | `service/ContribuyenteService.java` |
| VehiculoService | "Microservicio" de vehículo | `service/VehiculoService.java` |
| LicenciaService | "Microservicio" de puntos de licencia | `service/LicenciaService.java` |
| ResilientCache | Patrón Cache-Aside + stale-if-error | `cache/ResilientCache.java` |
| SriClient / AntClient | Adaptadores a los servicios externos | `client/` |

---

## Cómo enlazar GitHub en IcePanel

1. En IcePanel, abre el objeto (p. ej. **API de Consultas**) → panel lateral → **Links** → **+ Add link** → pega la URL de la carpeta `backend` en GitHub.
2. Repite para **Aplicación Web** (`frontend`) y **Caché distribuida** (`ResilientCache.java`).
3. (Opcional) En **Integrations → GitHub** conecta el repositorio para que los enlaces se validen.
4. Para el "enlace de lectura": **Share → Share link (view only)** y copia el enlace en el README del repo y en la entrega.
