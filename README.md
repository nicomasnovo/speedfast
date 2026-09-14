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
| `semana2/` | Jerarquía de pedidos y conceptos base de POO |
| `semana3/` **(sumativa 1)** | `ControladorDeEnvios` y las interfaces `Despachable`, `Cancelable` y `Rastreable` |
| `semana4/` | Entregas concurrentes con `Repartidor` (`Runnable`) y `ExecutorService` |
| `semana5/` **(entrega actual)** | Sincronización del acceso a la `ZonaDeCarga` como recurso compartido |

Cada carpeta es un módulo Maven con paquete base `cl.duocuc`. Este README
documenta la entrega de **semana 5**.

---

## 🔒 Semana 5: sincronización del acceso a la zona de carga

Varios repartidores de SpeedFast acceden **al mismo tiempo** a una única zona de
carga para retirar pedidos. El objetivo de la actividad es sincronizar ese acceso
para que **dos repartidores nunca retiren el mismo pedido** y no se produzcan
condiciones de carrera.

### Clases del módulo

| Clase | Rol |
| --- | --- |
| `Pedido` | Datos de la entrega: `id`, `direccionEntrega` y `estado`, con constructor, getters, setters, `setEstado(EstadoPedido)`, la sobrecarga `setEstado(String)` y `toString()` |
| `EstadoPedido` (enum) | `PENDIENTE`, `EN_REPARTO`, `ENTREGADO`: limita el estado a valores válidos y evita los errores de tipeo de un `String` |
| `ZonaDeCarga` | Recurso compartido entre los hilos, con `agregarPedido()` y `retirarPedido()` |
| `Repartidor` | `Runnable` con su `nombre` y la referencia a la `ZonaDeCarga`: retira un pedido, lo pasa a `EN_REPARTO`, simula el traslado y lo deja `ENTREGADO` |
| `Main` | Crea la zona de carga, deja 6 pedidos y lanza 3 repartidores concurrentes |

### Ciclo de vida del pedido

```
PENDIENTE  ──(el repartidor lo retira)──▶  EN_REPARTO  ──(fin del traslado)──▶  ENTREGADO
```

### Mecanismos de sincronización aplicados

- **`BlockingQueue<Pedido>`:** los pedidos disponibles viven en un
  `LinkedBlockingQueue` dentro de `ZonaDeCarga`. Su operación `poll()` es
  atómica, de modo que cada pedido se entrega a un solo hilo: nunca hay retiro
  doble ni entregas duplicadas. El `poll()` con tiempo de espera también permite
  que un repartidor aguarde por un pedido y termine cuando la zona queda vacía.
- **`synchronized`:** protege el registro de pedidos y el contador de entregas de
  `ZonaDeCarga`, y los accesos al `estado` de `Pedido`, que se escribe desde el
  hilo del repartidor y se lee desde el hilo principal.
- **`ExecutorService`:** `Main` lanza los tres repartidores con
  `Executors.newFixedThreadPool(3)` y espera a que todos terminen con
  `shutdown()` y `awaitTermination()` antes de mostrar el resumen.

### ▶️ Cómo ejecutar

```bash
mvn -pl semana5 -am clean compile
java -cp semana5/target/classes cl.duocuc.app.Main
```

En IntelliJ IDEA también está disponible la configuración de ejecución
**«Main semana5»**.

### Salida esperada

Los pedidos ingresan en estado `PENDIENTE` y los tres repartidores se intercalan
—el orden cambia en cada ejecución, porque depende de la planificación de los
hilos—:

```
[ZonaDeCarga] Pedido #101 disponible
[Repartidor: Luis] Retirando pedido #101...
[Repartidor: Camila] Retirando pedido #102...
[Repartidor: Pedro] Retirando pedido #103...
[Repartidor: Luis] Entregando pedido #101...
[Repartidor: Camila] Entregando pedido #102...
[Repartidor: Pedro] Entregando pedido #103...
[Repartidor: Luis] Pedido #101 entregado ✓
[Repartidor: Camila] Pedido #102 entregado ✓
[Repartidor: Pedro] Pedido #103 entregado ✓
```

Al finalizar, `Main` muestra cuántos pedidos entregó cada repartidor, el estado
final de cada pedido y confirma el resultado con el mensaje
**«Todos los pedidos han sido entregados correctamente.»**, que solo se imprime
si las entregas registradas coinciden con los pedidos ingresados y todos quedaron
en estado `ENTREGADO`.

---

© Duoc UC | Escuela de Informática y Telecomunicaciones
