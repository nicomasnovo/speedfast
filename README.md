![Duoc UC](https://www.duoc.cl/wp-content/uploads/2022/09/logo-0.png)

# 🚚 SpeedFast

Aplicación Java de consola que simula la gestión de pedidos de una empresa de
despachos, desarrollada para la asignatura
**Programación Orientada a Objetos 2**.

## 👤 Autor del proyecto

- **Nombre completo:** Nicolás Masnovo
- **Carrera:** Analista Programador Computacional
- **Sede:** Sede Online
- **Asignatura:** Programación Orientada a Objetos 2
- **Docente:** Pablo Andrés Vilches Valenzuela

## 📁 Contenido del repositorio

| Carpeta | Descripción |
| --- | --- |
| `semana3/` **(sumativa 1)** | Entrega evaluada: jerarquía de pedidos, `ControladorDeEnvios` y las interfaces `Despachable`, `Cancelable` y `Rastreable` |
| `semana4/` **(formativa)** | Entregas concurrentes: la clase `Repartidor` (`Runnable`) ejecutada con `ExecutorService` |

Cada carpeta es un módulo Maven con paquete base `cl.duocuc`.

---
### Cálculo del tiempo de entrega

Cada subclase implementa `calcularTiempoEntrega()` con su propia regla:

| Tipo de pedido | Regla | Ejemplo |
| --- | --- | --- |
| `PedidoComida` | 15 min base + 2 min por km | 4 km → 23 min |
| `PedidoEncomienda` | 20 min base + 1,5 min por km | 7 km → 30 min |
| `PedidoExpress` | 10 min base, +5 min si supera los 5 km | 7 km → 15 min |

### Conceptos de POO demostrados

- **Clase abstracta:** `Pedido` define los datos comunes de la entrega y deja
  `calcularTiempoEntrega()` como método abstracto.
- **Herencia:** `PedidoComida`, `PedidoEncomienda` y `PedidoExpress` extienden
  `Pedido` y agregan sus propios atributos.
- **Polimorfismo:** se recorre un arreglo `Pedido[]` invocando
  `mostrarResumen()` y `asignarRepartidor()`, y cada objeto responde con su
  propio comportamiento.
- **Sobrecarga:** `asignarRepartidor()` convive con `asignarRepartidor(String
  nombre)`, que permite indicar el repartidor a cargo de la entrega.
- **Encapsulamiento:** todos los atributos son privados y se exponen mediante
  métodos de acceso.
- **Interfaces:** `ControladorDeEnvios` implementa `Despachable`, `Cancelable`
  y `Rastreable`, separando los contratos de despacho, cancelación y rastreo.

---

## 🧵 Semana 4: entregas en paralelo con hilos

La carpeta `semana4/` reutiliza la jerarquía de `Pedido` y las tres interfaces, y
agrega la clase `Repartidor`, que implementa `Runnable` además de `Despachable`,
`Cancelable` y `Rastreable`:

- Cada repartidor tiene su nombre y su lista de pedidos, y entrega de forma
  secuencial dentro de su propio hilo (`run()` recorre los pedidos y llama a
  `despachar()`).
- El traslado se simula con `Thread.sleep()` usando valores aleatorios entre 1 y
  3 segundos, la `InterruptedException` se maneja restaurando el estado de
  interrupción del hilo.
- `Main` instancia tres repartidores con dos pedidos cada uno y los ejecuta en
  paralelo con `Executors.newFixedThreadPool(3)`, cerrando el pool con
  `shutdown()` y `awaitTermination()`.
- Al terminar se muestra el historial de cada repartidor y una comparación entre
  el tiempo total secuencial y el tiempo real en paralelo, para evidenciar el
  impacto de la concurrencia en el rendimiento.
- Un último caso ejecuta un repartidor en un `Thread` directo y lo detiene con
  `cancelar()`: la entrega en curso se completa y las restantes se descartan.

---

© Duoc UC | Escuela de Informática y Telecomunicaciones
