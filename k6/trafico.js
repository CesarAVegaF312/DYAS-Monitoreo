// Generador de tráfico para el taller de monitoreo.
// No es una prueba de carga con umbrales: su objetivo es que haya datos en Prometheus, Grafana y Jaeger.
//
// Uso (desde la raíz del repositorio, con el stack arriba):
//   docker compose run --rm k6
//   docker compose run --rm -e DURACION=10m -e TASA=20 k6
import http from 'k6/http';
import { check, sleep } from 'k6';

const PEDIDOS_URL = __ENV.PEDIDOS_URL || 'http://localhost:8081';
const INVENTARIO_URL = __ENV.INVENTARIO_URL || 'http://localhost:8082';
const DURACION = __ENV.DURACION || '5m';
const TASA = Number(__ENV.TASA || 10); // iteraciones por segundo

// La mayoría de pedidos son de productos con stock. "portatil" no tiene stock (409)
// y "tablet" no existe (404): así aparecen errores 4xx, que no son fallas del servicio.
const PRODUCTOS = [
  'teclado', 'teclado', 'mouse', 'mouse', 'monitor', 'audifonos', 'audifonos',
  'teclado', 'mouse', 'monitor', 'audifonos', 'portatil', 'tablet',
];

export const options = {
  scenarios: {
    trafico: {
      executor: 'constant-arrival-rate',
      rate: TASA,
      timeUnit: '1s',
      duration: DURACION,
      preAllocatedVUs: 10,
      maxVUs: 100,
    },
  },
};

function elegir(lista) {
  return lista[Math.floor(Math.random() * lista.length)];
}

export default function () {
  const producto = elegir(PRODUCTOS);

  // 1. Consultar el stock directamente en el servicio de inventario
  http.get(`${INVENTARIO_URL}/api/inventario/${producto}`, {
    tags: { name: 'GET /api/inventario/{producto}' },
  });

  // 2. Crear un pedido: pedidos llama a inventario (traza con spans de los dos servicios)
  const cuerpo = JSON.stringify({ producto, cantidad: 1 + Math.floor(Math.random() * 3) });
  const respuesta = http.post(`${PEDIDOS_URL}/api/pedidos`, cuerpo, {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'POST /api/pedidos' },
  });
  check(respuesta, { 'el pedido no terminó en 5xx': (r) => r.status < 500 });

  // 3. Consultar el pedido recién creado
  if (respuesta.status === 201) {
    http.get(`${PEDIDOS_URL}/api/pedidos/${respuesta.json('id')}`, {
      tags: { name: 'GET /api/pedidos/{id}' },
    });
  }

  sleep(0.1);
}
