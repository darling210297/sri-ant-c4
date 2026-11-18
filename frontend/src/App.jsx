import { useState } from 'react';
import { api } from './api.js';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const PLACA_RE = /^([A-Z]{3}-?\d{3,4}|[A-Z]{2}-?\d{3}[A-Z])$/;

function validarRucPersonaNatural(ruc) {
  if (!/^\d{13}$/.test(ruc)) return 'El RUC debe tener 13 dígitos.';
  if (!ruc.endsWith('001')) return 'El RUC de persona natural termina en 001.';
  if (Number(ruc[2]) >= 6) return 'El tercer dígito indica que no es una persona natural.';
  return null;
}

const FUENTES = {
  EXTERNO: { texto: 'Dato en línea', clase: 'ok' },
  CACHE: { texto: 'Desde caché (vigente)', clase: 'info' },
  CACHE_RESPALDO: { texto: 'Último dato guardado', clase: 'warn' },
};

function Origen({ meta }) {
  if (!meta) return null;
  const f = FUENTES[meta.fuente] ?? FUENTES.EXTERNO;
  return (
    <div className="origen">
      <span className={`badge ${f.clase}`}>{f.texto}</span>
      <small>Obtenido: {new Date(meta.obtenidoEn).toLocaleString('es-EC')}</small>
      {meta.desactualizado && <div className="aviso">⚠ {meta.aviso}</div>}
    </div>
  );
}

function ErrorBox({ error }) {
  if (!error) return null;
  return (
    <div className="error" role="alert">
      <strong>{error.message}</strong>
      <small> ({error.codigo})</small>
      {error.detalles?.length > 0 && (
        <ul>{error.detalles.map((d, i) => <li key={i}>{d.campo}: {d.mensaje}</li>)}</ul>
      )}
    </div>
  );
}

function Campos({ datos }) {
  return (
    <dl className="campos">
      {datos.filter(([, v]) => v !== null && v !== undefined && v !== '').map(([k, v]) => (
        <div key={k}><dt>{k}</dt><dd>{String(v)}</dd></div>
      ))}
    </dl>
  );
}

export default function App() {
  const [correo, setCorreo] = useState('');
  const [ruc, setRuc] = useState('');
  const [placa, setPlaca] = useState('');
  const [errCampos, setErrCampos] = useState({});

  const [contrib, setContrib] = useState(null);
  const [vehiculo, setVehiculo] = useState(null);
  const [puntos, setPuntos] = useState(null);
  const [errores, setErrores] = useState({});
  const [cargando, setCargando] = useState(null);

  async function consultarContribuyente(e) {
    e.preventDefault();
    const errs = {};
    if (!EMAIL_RE.test(correo.trim())) errs.correo = 'Ingrese un correo electrónico válido.';
    const eRuc = validarRucPersonaNatural(ruc.trim());
    if (eRuc) errs.ruc = eRuc;
    setErrCampos(errs);
    if (Object.keys(errs).length) return;

    setCargando('contribuyente');
    setContrib(null); setVehiculo(null); setPuntos(null); setErrores({});
    try {
      setContrib(await api.contribuyente(ruc.trim()));
    } catch (err) {
      setErrores({ contribuyente: err });
    } finally {
      setCargando(null);
    }
  }

  async function consultarVehiculo(e) {
    e.preventDefault();
    const p = placa.trim().toUpperCase();
    if (!PLACA_RE.test(p)) {
      setErrCampos({ placa: 'Formato de placa inválido (ej. PBX1234 o IB123C).' });
      return;
    }
    setErrCampos({});
    setCargando('vehiculo');
    setVehiculo(null); setPuntos(null);
    setErrores((x) => ({ contribuyente: x.contribuyente }));
    const cedula = contrib.data.cedula;
    // Las dos consultas son independientes: si la ANT falla, igual se muestra el vehículo.
    const [v, pts] = await Promise.allSettled([api.vehiculo(p), api.puntos(cedula, p)]);
    if (v.status === 'fulfilled') setVehiculo(v.value); else setErrores((x) => ({ ...x, vehiculo: v.reason }));
    if (pts.status === 'fulfilled') setPuntos(pts.value); else setErrores((x) => ({ ...x, puntos: pts.reason }));
    setCargando(null);
  }

  const c = contrib?.data;
  const v = vehiculo?.data;
  const pt = puntos?.data;

  return (
    <main>
      <header>
        <h1>Consulta de contribuyente, vehículo y licencia</h1>
        <p>Datos del SRI y la ANT.</p>
      </header>

      <section className="card">
        <h2>1. Datos del usuario</h2>
        <form onSubmit={consultarContribuyente} noValidate>
          <label>
            Correo electrónico
            <input type="email" value={correo} onChange={(e) => setCorreo(e.target.value)} placeholder="usuario@correo.com" />
            {errCampos.correo && <span className="campo-error">{errCampos.correo}</span>}
          </label>
          <label>
            RUC (persona natural)
            <input inputMode="numeric" maxLength={13} value={ruc} onChange={(e) => setRuc(e.target.value.replace(/\D/g, ''))} placeholder="1712345678001" />
            {errCampos.ruc && <span className="campo-error">{errCampos.ruc}</span>}
          </label>
          <button disabled={cargando === 'contribuyente'}>
            {cargando === 'contribuyente' ? 'Verificando en el SRI…' : 'Verificar contribuyente'}
          </button>
        </form>
        <ErrorBox error={errores.contribuyente} />
      </section>

      {c && (
        <section className="card">
          <h2>2. Contribuyente</h2>
          <Origen meta={contrib.meta} />
          <Campos datos={[
            ['Nombre', c.razonSocial], ['RUC', c.ruc], ['Cédula', c.cedula], ['Tipo', c.tipoContribuyente],
            ['Estado', c.estado], ['Régimen', c.regimen], ['Actividad principal', c.actividadEconomicaPrincipal],
            ['Obligado a llevar contabilidad', c.obligadoLlevarContabilidad], ['Inicio de actividades', c.fechaInicioActividades],
            ['Correo registrado en el formulario', correo],
          ]} />

          <h3>3. Matrícula del vehículo</h3>
          <form onSubmit={consultarVehiculo} noValidate className="inline">
            <input value={placa} onChange={(e) => setPlaca(e.target.value.toUpperCase())} placeholder="PBX1234" maxLength={8} />
            <button disabled={cargando === 'vehiculo'}>{cargando === 'vehiculo' ? 'Consultando…' : 'Consultar'}</button>
          </form>
          {errCampos.placa && <span className="campo-error">{errCampos.placa}</span>}
        </section>
      )}

      {(v || errores.vehiculo) && (
        <section className="card">
          <h2>Vehículo</h2>
          <ErrorBox error={errores.vehiculo} />
          {v && (<>
            <Origen meta={vehiculo.meta} />
            <Campos datos={[
              ['Placa', v.placa], ['Marca', v.marca], ['Modelo', v.modelo], ['Año', v.anio], ['Clase', v.clase],
              ['Servicio', v.servicio], ['Color', v.color], ['Cilindraje', v.cilindraje], ['País', v.pais],
              ['Cantón', v.canton], ['Última matrícula', v.fechaUltimaMatricula], ['Caducidad matrícula', v.fechaCaducidadMatricula],
            ]} />
          </>)}
        </section>
      )}

      {(pt || errores.puntos) && (
        <section className="card">
          <h2>Puntos de licencia (ANT)</h2>
          <ErrorBox error={errores.puntos} />
          {pt && (<>
            <Origen meta={puntos.meta} />
            <div className="puntos">
              <span className="numero">{pt.puntos ?? '—'}</span>
              <span>{pt.puntos != null ? 'de 30 puntos' : 'La ANT no informó los puntos en su respuesta'}</span>
            </div>
            <Campos datos={[['Cédula', pt.cedula], ['Citaciones registradas', pt.totalCitaciones]]} />
          </>)}
        </section>
      )}
    </main>
  );
}
