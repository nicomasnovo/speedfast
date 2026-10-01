![Duoc UC](https://www.duoc.cl/wp-content/uploads/2022/09/logo-0.png)

# 🚚 SpeedFast

Aplicación Java que simula la gestión de pedidos de una empresa de despachos,
desarrollada para la asignatura **Programación Orientada a Objetos 2**. Empezó
como un ejercicio de consola y hoy es una aplicación de escritorio hecha con
Swing, repartida en capas MVC y con sus datos guardados en MySQL.

## 👤 Autor del proyecto

- **Nombre completo:** Nicolás Masnovo
- **Carrera:** Analista Programador Computacional
- **Sede:** Sede Online
- **Asignatura:** Programación Orientada a Objetos 2
- **Docente:** Pablo Andrés Vilches Valenzuela

## 📁 Contenido del repositorio

| Carpeta                         | Descripción |
|---------------------------------| --- |
| `semana2/`                      | Jerarquía de pedidos y conceptos base de POO |
| `semana3/` **(sumativa 1)**     | `ControladorDeEnvios` y las interfaces `Despachable`, `Cancelable` y `Rastreable` |
| `semana4/`                      | Entregas concurrentes con `Repartidor` (`Runnable`) y `ExecutorService` |
| `semana5/` **(sumativa 2)**     | Sincronización del acceso a la `ZonaDeCarga` como recurso compartido |
| `semana6/`                      | Interfaz gráfica Swing sobre la lógica de las semanas anteriores |
| `semana7/`                      | Persistencia en MySQL con JDBC, patrón DAO y una interfaz de una sola ventana |
| `semana8/` **(entrega actual)** | Gestión completa de repartidores, pedidos y entregas sobre MVC, DAO y JDBC |

Cada carpeta es un módulo Maven con paquete base `cl.duocuc`. Este README
documenta la entrega de **semana 8**.

---

## 💾 Semana 8: qué cambió

La semana 7 dejó los datos guardados en MySQL, pero la aplicación todavía se
parecía a la simulación original: se registraban pedidos y se miraba el
resultado del reparto. Esta entrega la convierte en algo más cercano a un sistema
de gestión, con las tres entidades del negocio administrables desde la ventana.

### Todo se administra desde la interfaz

El menú quedó con tres secciones **Repartidores**, **Pedidos** y
**Entregas**, cada una con su tabla y los botones de siempre: Nuevo, Editar,
Eliminar y Actualizar. Los formularios de alta se abren en ventanas emergentes y
las tablas son de solo lectura, así que los datos solo cambian a través de esos
botones. En Pedidos además se puede asignar un repartidor, filtrar por tipo y
estado, e iniciar el reparto.

### Una clase abstracta para no repetir la interfaz

Las tres secciones se parecen mucho entre ellas: mismo título arriba, mismos
márgenes, formularios de etiqueta + campo y la misma barra de botones de colores.
Para no copiar ese código tres veces apareció `PanelSeccion`, una clase abstracta
que extiende `JPanel` y de la que heredan todas las secciones.

Ahí viven las partes compartidas: el título y los márgenes que arma el
constructor, los ayudantes para montar formularios (`crearFormulario`,
`crearRestricciones`, `agregarFila`), la fábrica de botones del CRUD con su color,
su icono y el gris que toman al deshabilitarse, y la barra donde se alinean. Cada
sección hereda todo eso y solo escribe lo suyo: sus columnas, sus botones y qué
le pide al controlador. Es abstracta a propósito, porque por sí sola no
representa ninguna pantalla: solo existe para ser la base de las demás.

### La tabla de entregas

Ahora existe `entrega`, que deja constancia de qué repartidor entregó qué pedido,
y en qué fecha y hora. Está relacionada con `pedido` y `repartidor` por llaves
foráneas, de modo que la base no permite anotar una entrega de alguien que no
existe ni borrar un pedido que ya tiene entregas registradas; cuando eso pasa, la
aplicación lo explica en lugar de mostrar un error de SQL.

Registrar una entrega significa que el pedido llegó a su destino, así que queda
en estado `ENTREGADO` a nombre de quien la hizo. Y al revés: los pedidos que
reparten los hilos de los repartidores quedan anotados en esa misma tabla, para
que la sección Entregas sea siempre la lista de los pedidos entregados. La
estructura y los datos de prueba están en `semana8/db/script_estructura.sql`.

### Las capas, un poco más ordenadas

El proyecto sigue el mismo camino de la semana 7, ahora con las capas completas y
cada una en su paquete:

| Paquete | Qué hay ahí |
| --- | --- |
| `cl.duocuc.model` | `Pedido`, `Repartidor`, `Entrega`, la `ZonaDeCarga` y los enums `TipoPedido` y `EstadoPedido` |
| `cl.duocuc.view` | Las secciones Swing: piden datos y muestran resultados, nada más |
| `cl.duocuc.controllers` | Las validaciones y las reglas del negocio |
| `cl.duocuc.dao` | El CRUD con JDBC: `PedidoDAO`, `RepartidorDAO` y el nuevo `EntregaDAO` |
| `cl.duocuc.conexion` | `ConexionBD`: entrega las conexiones y valida la base al iniciar |

Cualquier operación recorre siempre lo mismo: vista → controlador → DAO →
`ConexionBD` → MySQL. La vista no sabe que existe SQL, el DAO no sabe que existe
Swing, y los problemas de la base vuelven como `PersistenciaException` para que la
ventana los muestre con un mensaje entendible.

### Lo que no cambió

El reparto sigue siendo concurrente y con los mismos cuidados de las semanas
anteriores: un hilo por repartidor dentro del `ExecutorService`, la
`BlockingQueue` de la `ZonaDeCarga` para que nadie entregue el mismo pedido dos
veces y el `SwingWorker` que evita congelar la interfaz mientras corren las
entregas.

---

© Duoc UC | Escuela de Informática y Telecomunicaciones
