![Duoc UC](https://www.duoc.cl/wp-content/uploads/2022/09/logo-0.png)

# 🚚 SpeedFast

Aplicación Java que simula la gestión de pedidos de una empresa de despachos,
desarrollada para la asignatura **Programación Orientada a Objetos 2**. Empezó
como un ejercicio de consola y hoy tiene una interfaz gráfica hecha con Swing y
sus datos guardados en MySQL.

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
| `semana6/` | Interfaz gráfica Swing sobre la lógica de las semanas anteriores |
| `semana7/` **(entrega actual)** | Persistencia en MySQL con JDBC, patrón DAO y una interfaz de una sola ventana |

Cada carpeta es un módulo Maven con paquete base `cl.duocuc`. Este README
documenta la entrega de **semana 7**.

---

## 💾 Semana 7: qué cambió

La semana 6 dejó la aplicación funcionando con ventanas, pero los datos seguían
viviendo solo en memoria: los pedidos venían escritos en `Main` y al cerrar la
aplicación se perdía todo. Esta entrega agrega la base de datos y reordena el
proyecto para que el acceso a ella quede en su propia capa.

### Persistencia en MySQL

Los pedidos y los repartidores ahora se guardan en MySQL con JDBC. Los datos
iniciales ya no se crean desde Java, sino desde `semana7/db/script_estructura.sql`,
y cada registro, edición, asignación o entrega queda escrita en la base.

### Patrón DAO

Todo el SQL se sacó de los controladores y se agrupó en dos paquetes nuevos:

| Paquete | Qué hay ahí |
| --- | --- |
| `cl.duocuc.dao` | `PedidoDAO` y `RepartidorDAO`: el CRUD, las consultas y la conversión de `ResultSet` a objetos del modelo |
| `cl.duocuc.conexion` | `ConexionBD`: entrega las conexiones y valida la base al iniciar |

Los controladores conservan su rol de siempre, validar los datos de la vista,
crear los objetos y mantener la zona de carga al día, pero ya no conocen
`Connection` ni SQL. El flujo quedó vista → controlador → DAO → `ConexionBD` →
MySQL.

### Una sola ventana

Las cuatro ventanas independientes se reemplazaron por una ventana con barra
lateral fija y un `CardLayout` que cambia el contenido central, así que navegar
entre secciones ya no abre ni cierra ventanas. Las secciones son **Registrar
Pedido**, **Registrar Repartidor** (nueva en esta entrega), **Registrar Entrega**
y **Ver Pedidos**.

### Lo que no cambió

El reparto sigue siendo concurrente y con los mismos cuidados de las semanas
anteriores: un hilo por repartidor dentro del `ExecutorService`, la
`BlockingQueue` de la `ZonaDeCarga` para que nadie entregue el mismo pedido dos
veces y el `SwingWorker` que evita congelar la interfaz mientras corren las
entregas.

---

© Duoc UC | Escuela de Informática y Telecomunicaciones
