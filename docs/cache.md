# Caché y resiliencia

## Patrón

**Cache-Aside con respaldo "stale-if-error"** (implementado en `ResilientCache`):

1. Se busca la clave en Redis. Si el dato tiene menos de `fresh` de antigüedad → se responde desde la caché (`fuente: CACHE`).
2. Si no existe o ya no es reciente → se consulta el servicio externo.
   - **Respuesta adecuada** → se guarda en Redis como `{data, cachedAt}` con TTL físico `stale` y se responde (`fuente: EXTERNO`).
   - **Error de negocio** (no es contribuyente, no es persona natural, placa no existe) → se responde 404/422 y **no** se cachea.
3. Si el servicio externo falla (timeout, 5xx, respuesta no reconocible):
   - Hay dato guardado → se responde **200 con el último dato bueno** y `meta.desactualizado = true` + `meta.aviso` (`fuente: CACHE_RESPALDO`). El frontend muestra el aviso en amarillo.
   - No hay dato → **503** con `codigo` `ANT_NO_DISPONIBLE` / `SRI_NO_DISPONIBLE`.
4. Si Redis no está disponible, la API sigue funcionando sin caché (se registra una advertencia).

Ejemplo de respuesta en respaldo:

```json
{
  "data": { "cedula": "1710034065", "placa": "PBX1234", "puntos": 30, "totalCitaciones": 0 },
  "meta": {
    "fuente": "CACHE_RESPALDO",
    "desactualizado": true,
    "obtenidoEn": "2026-11-16T14:03:11Z",
    "vigenteHasta": "2026-11-16T15:03:11Z",
    "aviso": "El servicio ANT no está disponible. Se muestra el último dato válido obtenido el 2026-11-16T14:03:11Z (UTC); puede no estar actualizado."
  }
}
```

## Tiempo de vida (TTL) y justificación

| Dato | `fresh` (vigente) | `stale` (respaldo) | Justificación |
|---|---|---|---|
| Contribuyente (SRI) | 24 h | 7 días | El estado tributario, régimen y tipo de contribuyente cambian muy poco; consultar una vez al día reduce la carga sobre el SRI. |
| Vehículo (SRI) | 12 h | 7 días | Matrícula, propietario y características cambian con poca frecuencia (traspasos, renovación anual). |
| Puntos de licencia (ANT) | 1 h | 3 días | Una citación puede restar puntos el mismo día, así que el dato "reciente" dura poco. Como la ANT cae con frecuencia, se guarda 3 días para poder mostrar el último dato bueno, siempre con aviso. Pasados 3 días el dato ya no es confiable y se prefiere el error 503. |

Los valores son configurables en `application.yml` (`app.cache.*`).

## Caché en la nube

Se usa Redis administrado. Recomendado: **Upstash Redis** (plan gratuito, TLS):

1. Crear base en https://console.upstash.com → copiar la URL `rediss://default:<password>@<host>:6379`.
2. Arrancar el backend con `REDIS_URL=rediss://...`.

Alternativas: Redis Cloud, Azure Cache for Redis, AWS ElastiCache.
