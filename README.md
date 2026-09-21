![Duoc UC](https://www.duoc.cl/wp-content/uploads/2022/09/logo-0.png)

# 🚚 SpeedFast

Aplicación Java que simula la gestión de pedidos de una empresa de despachos,
desarrollada para la asignatura **Programación Orientada a Objetos 2**. Empezó
como un ejercicio de consola y ahora tiene una interfaz gráfica hecha con Swing.

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
| `semana5/` | Sincronización del acceso a la `ZonaDeCarga` como recurso compartido |
| `semana6/` **(entrega actual)** | Interfaz gráfica Swing sobre la lógica de las semanas anteriores |

Cada carpeta es un módulo Maven con paquete base `cl.duocuc`. Este README
documenta la entrega de **semana 6**.

---

## 🖥️ Semana 6: de la consola a la interfaz gráfica

Hasta la semana 5 todo el sistema se manejaba por consola: los pedidos venían
escritos en el código y el resultado se leía en la salida estándar. La idea de
esta semana es que un usuario pueda operar SpeedFast desde ventanas, sin perder
nada de lo que ya funcionaba: el reparto sigue siendo concurrente y la zona de
carga sigue siendo el recurso compartido que se sincroniza.

Para lograrlo la aplicación quedó separada en tres capas, siguiendo el patrón
MVC. El modelo no sabe que existe Swing y las ventanas no manipulan pedidos
directamente: todo pasa por los controladores.

| Paquete | Qué hay ahí |
| --- | --- |
| `cl.duocuc.model` | `Pedido`, `Repartidor`, `ZonaDeCarga` y los enums `EstadoPedido` y `TipoPedido` |
| `cl.duocuc.controllers` | `PedidoController` y `RepartidorController`: validan los datos y coordinan el reparto |
| `cl.duocuc.view` | Las cuatro ventanas Swing |
| `cl.duocuc.Main` | Crea la zona de carga, los controladores y levanta la ventana principal |

### Las ventanas

- **Ventana principal:** reemplaza al viejo menú de consola. Tiene los cuatro
  accesos del sistema y desde ahí se abre todo lo demás.
- **Registrar Pedido:** formulario con ID, dirección y tipo de pedido. El ID
  viene propuesto por el controlador (`sugerirId()`) para no repetirlo, y los
  errores se avisan con un `JOptionPane` en lugar de reventar la aplicación.
- **Listar Pedidos:** una `JTable` de solo lectura con el ID, la dirección, el
  tipo, el estado y el repartidor a cargo de cada pedido, más un resumen con el
  total de entregados.
- **Asignar Repartidor:** permite elegir un pedido pendiente y dejárselo
  reservado a un repartidor antes de que empiece el despacho.

### Lo que se agregó al modelo

- `TipoPedido` (enum) para que el tipo de pedido se elija de una lista y no se
  escriba a mano, con su etiqueta legible para mostrarla en la interfaz.
- `Pedido` ahora guarda también su `tipo` y el `repartidorAsignado`.
- `ZonaDeCarga` mantiene la cola general de pedidos y suma una cola por
  repartidor para las asignaciones manuales. Cada pedido vive en una sola cola,
  así que el retiro sigue siendo atómico y nadie entrega el mismo pedido dos
  veces.

### Concurrencia e interfaz conviviendo

Este fue el punto más delicado de la entrega. `iniciarEntregas()` es bloqueante
porque espera a que el `ExecutorService` termine, y si se llamara directamente
desde el botón dejaría la ventana congelada. Por eso la vista lo ejecuta dentro
de un `SwingWorker`: el reparto corre en segundo plano, el Event Dispatch Thread
queda libre para seguir dibujando, y cuando todo termina el resumen aparece en
un diálogo y las ventanas abiertas se refrescan solas.

El resto de los cuidados vienen de la semana anterior y siguen vigentes: la
`BlockingQueue` que reparte cada pedido a un solo hilo, los métodos
`synchronized` de `ZonaDeCarga` y `Pedido`, y el `volatile` en el contador de
entregas de cada repartidor, que se escribe en su hilo y se lee desde la
interfaz.

### Qué esperar al usarla

La aplicación parte con seis pedidos ya cargados en estado `PENDIENTE`. Se
pueden agregar más desde el formulario, asignar algunos a un repartidor concreto
y después pulsar **Iniciar Entregas**: los tres repartidores trabajan en
paralelo y la consola va mostrando el avance —el orden cambia en cada ejecución,
porque depende de la planificación de los hilos—:

```
[ZonaDeCarga] Pedido #101 disponible
[ZonaDeCarga] Pedido #103 asignado a Camila
[Repartidor: Luis] Retirando pedido #101...
[Repartidor: Camila] Retirando pedido #103...
[Repartidor: Pedro] Retirando pedido #102...
[Repartidor: Luis] Entregando pedido #101...
[Repartidor: Luis] Pedido #101 entregado ✓
```

Al terminar, el diálogo de resumen indica cuántos pedidos entregó cada
repartidor y cierra con **«Todos los pedidos han sido entregados
correctamente.»**, mensaje que solo aparece si las entregas registradas
coinciden con los pedidos ingresados y todos quedaron en estado `ENTREGADO`. En
el listado se puede confirmar el estado final de cada pedido y quién lo llevó.

---

© Duoc UC | Escuela de Informática y Telecomunicaciones
