# SGA — Aldea Bahareque

**SGA - Sistema de Gestión de Alojamiento** para *Aldea Bahareque*, un conjunto de siete apartamentos turísticos independientes en Filandia, Quindío. El sistema gestiona inventario, tarifas por temporada, disponibilidad, reservas, operación diaria, folios y venta por tres canales (portal, directo y externo).

Proyecto final de **Programación Avanzada** — Programa de Ingeniería de Sistemas y Computación, Universidad del Quindío.

## Integrantes

| Nombre | Usuario GitHub |
|---|---|
| _Vanessa Henao Gomez_ |vanessahg07 |

## Entrega 1 — Modelado del dominio

| Elemento | Dónde está |
|---|---|
| Documento de la entrega: Ficha del Alojamiento (Anexo A) y reglas propias, lenguaje ubicuo, modelo del dominio (agregados, invariantes y servicios), diagrama de clases, mapa de agregados, casos de uso, resumen de la API y de las pruebas | [docs/SGA-Aldea-Bahareque-Entrega-1.docx](docs/SGA-Aldea-Bahareque-Entrega-1.docx) |
| Libro de identificación (Identificación, Relación de Reglas, Catálogo de Servicios del Dominio y Matriz de Trazabilidad) | [docs/libro-de-identificacion.xlsx](docs/libro-de-identificacion.xlsx) |
| Imágenes y fuentes editables de los diagramas | [docs/diagramas/](docs/diagramas) |
| Documentación de la API (OpenAPI 3.1) | [docs/api/openapi.yaml](docs/api/openapi.yaml) — se abre en [Swagger Editor](https://editor.swagger.io) |
| Dominio (entidades, value objects, servicios y la excepción de dominio) | `src/main/java/co/edu/uniquindio/sga/domain` |
| Repositorios en memoria (HashMap) | `src/main/java/co/edu/uniquindio/sga/infrastructure/persistencia/memoria` |
| Pruebas unitarias (patrón AAA, fechas fijas) | `src/test/java/co/edu/uniquindio/sga` |

## Requisitos

- JDK 25
- No se requiere instalar Gradle: el proyecto incluye el wrapper (`gradlew`).

## Ejecutar las pruebas

```bash
./gradlew test           # Windows: gradlew.bat test
```

Las pruebas del dominio no usan Spring ni base de datos, y usan fechas fijas (el "hoy" de las pruebas es el 1 de octubre de 2026). Cada regla invariante (RN-01 a RN-22) y cada regla propia (RP-01 a RP-03) tiene al menos un caso que la cumple y uno que la viola. El reporte queda en `build/reports/tests/test/index.html`.

## Ejecutar la aplicación

```bash
./gradlew bootRun        # Windows: gradlew.bat bootRun
```

- Servidor en `http://localhost:9090`.
- Consola H2 en `http://localhost:9090/h2-console` — URL JDBC `jdbc:h2:mem:sgadb`, usuario `sa`, sin contraseña.

En esta entrega la aplicación solo arranca la estructura; la API REST se implementa en el Corte 2 a partir del diseño de `docs/api/openapi.yaml`.

## Arquitectura

Arquitectura hexagonal (puertos y adaptadores). Las dependencias apuntan hacia el dominio:

```
co.edu.uniquindio.sga
├── domain          # Reglas y conceptos del negocio, y puertos (Java puro, sin Spring ni JPA)
├── application     # Casos de uso (Corte 2)
└── infrastructure
    └── persistencia
        └── memoria # Repositorios en memoria con HashMap
```
