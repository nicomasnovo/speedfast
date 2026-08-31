![Duoc UC](https://www.duoc.cl/wp-content/uploads/2022/09/logo-0.png)

# 🚚 SpeedFast

Aplicación Java de consola que simula la gestión de pedidos de una empresa de
despachos, desarrollada como **evaluación sumativa 1** de la asignatura
**Programación Orientada a Objetos 2**. El proyecto aplica herencia,
polimorfismo, sobrecarga, clases abstractas e interfaces sobre la jerarquía de
`Pedido`.

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

La carpeta `semana3/` (sumativa 1) es un proyecto Maven con paquete base
`cl.duocuc`.

---

## 🧱 Estructura del código

```
cl.duocuc
├── app
│   ├── Main.java                  # Punto de entrada y demostración por consola
│   └── ControladorDeEnvios.java   # Despacho, cancelación e historial
├── model
│   ├── Pedido.java                # Clase abstracta base
│   ├── PedidoComida.java          # Restaurante + tiempo de preparación
│   ├── PedidoEncomienda.java      # Peso + volumen del paquete
│   └── PedidoExpress.java         # Tienda de origen, entrega reducida
└── interfaces
    ├── Despachable.java           # despachar()
    ├── Cancelable.java            # cancelar()
    └── Rastreable.java            # verHistorial()
```

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

© Duoc UC | Escuela de Informática y Telecomunicaciones
