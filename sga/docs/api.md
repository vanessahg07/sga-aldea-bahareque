# API REST — SGA Aldea Bahareque

| Entorno | URL base |
|---|---|
| Desarrollo | `http://localhost:9090/api` |
| Despliegue (Render) | `https://sga-api-ip10.onrender.com/api` |

En el despliegue, la primera petición después de un rato sin uso puede tardar unos 2 minutos mientras el servicio arranca (ver [despliegue.md](despliegue.md#la-primera-petición-tarda-arranque-en-frío)).

JSON en todas las peticiones y respuestas. Las fechas van en formato ISO (`2026-11-10`) y las horas como `16:00`.

## Autenticación

Se inicia sesión en `POST /api/auth/login` y se envía el token en cada petición:

```
Authorization: Bearer <token>
```

El token es un JWT firmado por el SGA (HMAC-SHA256, emisor `sga-aldea-bahareque`), con el id del usuario, su nombre y su rol. Vence a los 60 minutos (`SGA_JWT_MINUTOS_EXPIRACION`); después hay que iniciar sesión de nuevo.

| Rol | Alcance |
|---|---|
| `HUESPED` | Reserva por el portal. Solo accede a sus propias reservas y folios. |
| `RECEPCIONISTA` | Reservas directas, modificaciones, confirmación, cancelación, no-show, llegadas, salidas y folios. |
| `ADMINISTRADOR` | Todo lo de recepción, más la configuración del alojamiento, el inventario, las tarifas, las políticas y los usuarios. Es el único que autoriza cerrar un folio con saldo. |
| `PERSONAL_SERVICIO` | Apartamentos pendientes de preparación, ciclo de preparación y novedades. No declara fuera de servicio, no registra bloqueos y no consulta ni opera reservas ni folios. |

## Errores

Todo error devuelve un código de negocio estable y un mensaje legible, nunca una traza técnica:

```json
{ "codigo": "NOCHES_NO_DISPONIBLES",
  "mensaje": "El apartamento ya tiene una reserva activa en alguna de las noches solicitadas",
  "detalles": [], "fecha": "2026-09-23T06:47:07Z" }
```

| HTTP | Cuándo |
|---|---|
| 400 | La solicitud tiene un formato inválido o le faltan campos (`SOLICITUD_INVALIDA`, con el detalle por campo). |
| 401 | Sin sesión (`NO_AUTENTICADO`) o credenciales incorrectas (`CREDENCIALES_INVALIDAS`). |
| 403 | El rol no permite la acción, o el recurso pertenece a otro huésped (`ACCESO_DENEGADO`). |
| 404 | El recurso no existe (`RESERVA_NO_ENCONTRADA`, `APARTAMENTO_NO_ENCONTRADO`, …). |
| 409 | Conflicto con el estado actual: doble venta, transición inválida, folio con saldo, correo repetido… |
| 422 | Se viola una regla de negocio (capacidad, anticipo, estancia mínima, contraseña insegura…). |

## Paginación

Todos los listados devuelven páginas de 10 elementos. La página se pide con `?pagina=0` (empieza en 0):

```json
{ "contenido": [...], "pagina": 0, "tamano": 10, "totalElementos": 23, "totalPaginas": 3 }
```

## Endpoints

### Autenticación y perfil

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/auth/registro` | Público | Registro de huésped. Devuelve la sesión iniciada. |
| POST | `/auth/login` | Público | Inicio de sesión. |
| POST | `/auth/recuperacion` | Público | Envía un código de 6 dígitos, válido 15 minutos y de un solo uso. |
| POST | `/auth/recuperacion/restablecimiento` | Público | Restablece la contraseña con el código. |
| GET | `/perfil` | Autenticado | Perfil propio. |
| PUT | `/perfil` | Autenticado | Actualiza nombre, teléfono y foto. El correo no es editable. |
| PUT | `/perfil/contrasena` | Autenticado | Cambio de contraseña; exige la actual. |

### Usuarios (administrador)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/usuarios` | Listado paginado. |
| GET | `/usuarios/{id}` | Detalle. |
| POST | `/usuarios` | Crea un usuario con cualquier rol. |
| PUT | `/usuarios/{id}/rol` | Cambia el rol. |
| DELETE | `/usuarios/{id}` | Eliminación lógica. No procede si es titular de reservas activas. |

### Alojamiento

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/alojamiento` | Público | Datos, normas, servicios, medios de pago y parámetros. |
| PUT | `/alojamiento` | Administrador | Datos y normas. |
| PUT | `/alojamiento/parametros` | Administrador | Umbral de edad, horas, preparación, plazos, anticipo, mascotas y descuento. |
| PUT | `/alojamiento/servicios` | Administrador | Servicios adicionales y su cargo. |
| PUT | `/alojamiento/medios-pago` | Administrador | Medios de pago aceptados (mínimo 2). |

### Apartamentos

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/apartamentos` | Público | Apartamentos a la venta. |
| GET | `/apartamentos/{codigo}` | Público | Detalle con galería. |
| GET | `/apartamentos/{codigo}/calendario?desde&hasta` | Público | Estado de cada noche: disponible, reservada o bloqueada. |
| GET | `/apartamentos/{codigo}/tarifas` | Público | Tarifas vigentes por temporada. |
| GET | `/apartamentos/gestion` | Administración, recepción | Todos, incluidos los inactivos. |
| POST | `/apartamentos` | Administrador | Crea el apartamento inactivo. |
| PUT | `/apartamentos/{codigo}` | Administrador | Actualiza datos. Si baja la capacidad, advierte las reservas que quedan por encima. |
| PUT | `/apartamentos/{codigo}/imagenes` | Administrador | Entre 1 y 10 imágenes, con exactamente una principal. |
| POST | `/apartamentos/{codigo}/activacion` | Administrador | Requiere imágenes y tarifa en todas las temporadas. |
| POST | `/apartamentos/{codigo}/desactivacion` | Administrador | Retira de la venta. |
| DELETE | `/apartamentos/{codigo}` | Administrador | Eliminación lógica, solo sin reservas activas ni futuras. |
| GET | `/apartamentos/{codigo}/bloqueos` | Administración, recepción | Bloqueos vigentes. |
| POST | `/apartamentos/{codigo}/bloqueos` | Administrador | Registra un bloqueo; no puede cubrir noches reservadas. |
| DELETE | `/apartamentos/bloqueos/{id}` | Administrador | Levanta el bloqueo (eliminación lógica). |
| GET | `/apartamentos/pendientes-preparacion` | Personal interno | Apartamentos por preparar o en preparación. |
| PUT | `/apartamentos/{codigo}/estado-operativo` | Administrador, servicio | El servicio solo mueve el ciclo de preparación. |
| POST | `/apartamentos/{codigo}/novedades` | Personal interno | Registra una novedad con gravedad. |
| GET | `/apartamentos/{codigo}/novedades` | Personal interno | Historial de novedades. |

### Temporadas, tarifas y política

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/temporadas` | Público | Temporadas con sus rangos y estancia mínima. |
| POST | `/temporadas` | Administrador | Agrega una temporada; no puede solaparse con otra. |
| PUT | `/temporadas/{nombre}` | Administrador | Cambia rangos y estancia mínima. |
| DELETE | `/temporadas/{nombre}` | Administrador | Retira una temporada (lógica). La base no se retira. |
| POST | `/tarifas` | Administrador | Nueva tarifa vigente desde ahora; la anterior queda en el histórico. |
| GET | `/tarifas/historico/{codigo}` | Administrador | Histórico de tarifas de un apartamento. |
| GET | `/tarifas/incompletas` | Administrador | Apartamentos activos sin tarifa en alguna temporada. |
| GET | `/politicas/vigente` | Público | Política de cancelación vigente. |
| GET | `/politicas` | Administrador | Todas las versiones. |
| POST | `/politicas` | Administrador | Publica una nueva versión; las reservas existentes conservan la suya. |

### Disponibilidad y cotización (públicas)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/disponibilidad/busqueda?pagina` | Busca por fechas y grupo (fechas de nacimiento o cantidad de personas), con filtros de capacidad, precio y características. Devuelve tarjetas con el valor estimado. |
| POST | `/cotizaciones` | Cotiza una estancia con el desglose noche por noche e indica si se puede vender. |

### Reservas

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/reservas/portal` | Huésped | Crea una reserva por el portal; el titular es el huésped. |
| POST | `/reservas/directas` | Recepción, administración | Crea una reserva directa para un titular que puede no tener cuenta. |
| GET | `/reservas?desde&hasta&estado&apartamento&canal&titular&pagina` | Recepción, administración | Listado de la más reciente a la más antigua. |
| GET | `/reservas/mias` | Huésped | Historial propio. |
| GET | `/reservas/{id}` | Huésped titular, recepción, administración | Detalle; el huésped solo ve las suyas. |
| PUT | `/reservas/{id}` | Recepción, administración | Modifica fechas, grupo o apartamento; registra el ajuste. |
| PUT | `/reservas/{id}/hora-llegada` | Huésped titular, recepción, administración | Registra la hora estimada de llegada. |
| POST | `/reservas/{id}/confirmacion` | Recepción, administración | Exige hora de llegada y anticipo. |
| POST | `/reservas/{id}/cancelacion` | Huésped titular, recepción, administración | El huésped cancela solo las suyas; aplica la política congelada. |
| POST | `/reservas/{id}/no-show` | Recepción, administración | Desde la hora límite del día de entrada. |
| POST | `/reservas/{id}/llegada` | Recepción, administración | Exige fecha de entrada alcanzada y apartamento preparado. |
| POST | `/reservas/{id}/salida` | Recepción, administración | Cierra el folio; con saldo, solo el administrador autoriza (`motivoAutorizacion`). |
| GET | `/reservas/llegadas?fecha` | Recepción, administración | Llegadas previstas del día. |
| GET | `/reservas/salidas?fecha` | Recepción, administración | Salidas previstas del día. |
| GET | `/reservas/ocupacion?desde&hasta` | Recepción, administración | Calendario de ocupación. |

### Folio

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/reservas/{id}/folio` | Huésped titular, recepción, administración | Cargos, pagos y saldo; el huésped solo ve los suyos. |
| POST | `/reservas/{id}/folio/pagos` | Recepción, administración | Pago con un medio aceptado y fecha (si no se indica, ahora). |
| POST | `/reservas/{id}/folio/servicios` | Recepción, administración | Cargo por un servicio adicional configurado. |
| POST | `/reservas/{id}/folio/ajustes` | Recepción, administración | Ajuste manual con descripción. |
| POST | `/reservas/{id}/folio/cargos/{idCargo}/reverso` | Recepción, administración | Corrección con un cargo inverso. |
| POST | `/reservas/{id}/folio/pagos/{idPago}/reverso` | Recepción, administración | Corrección con un pago inverso. |
| POST | `/reservas/{id}/folio/cierre` | Recepción, administración | Cierre; con saldo, solo el administrador (`motivoAutorizacion`). |

### Canal externo (autenticación por credencial del canal)

Encabezados obligatorios: `X-Canal` (código del canal) y `X-Credencial-Canal`. Sin ellos o con una credencial errada: 401 `CREDENCIAL_CANAL_INVALIDA`; canal inactivo: 403 `CANAL_INACTIVO`. Detalle del contrato en [integraciones.md](integraciones.md).

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/canal/disponibilidad?entrada&salida&personas` | Apartamentos disponibles con su valor total. Alternativa a `personas`: `fechasNacimiento` separadas por coma. |
| POST | `/canal/reservas` | `{idExterno, codigoApartamento, entrada, salida, horaEstimadaLlegada, titular, acompanantes, mascotas}`. 201 `ACEPTADA`, 200 `DUPLICADA` (RN-19), 409 `RECHAZADA_CONFLICTO` (RN-18). |
| DELETE | `/canal/reservas/{idExterno}` | 200 `CANCELADA` o `YA_CANCELADA` si el mensaje se repite. |

### Canales, conflictos y bitácora (administrador)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/canales` | Canales registrados (sin credenciales). |
| POST | `/canales` | Registra un canal `{codigo, nombre, tipo}` y devuelve su credencial una sola vez. |
| PUT | `/canales/{codigo}` | Cambia nombre y tipo. |
| POST | `/canales/{codigo}/credencial` | Renueva la credencial (la anterior deja de servir). |
| PUT | `/canales/{codigo}/activo` | `{activo}`: un canal inactivo no puede operar. |
| GET | `/canales/conflictos?pendientes&pagina` | Reservas externas rechazadas por conflicto. |
| POST | `/canales/conflictos/{id}/revision` | `{nota}`: deja constancia de la revisión (una sola vez). |
| GET | `/canales/bitacora?canal&pagina` | Eventos intercambiados con los canales, del más reciente al más antiguo. |

### Reportes (administrador)

Parámetros `desde` y `hasta` (ambas fechas incluidas, máximo 366 días; si no: 422 `PERIODO_INVALIDO`).

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/reportes/ocupacion` | Noches vendidas, disponibles, porcentaje y detalle por apartamento. |
| GET | `/reportes/ingresos` | Pagos del periodo por medio de pago. |
| GET | `/reportes/canales` | Reservas creadas en el periodo por canal y estado. |
| GET | `/reportes/indicadores` | Tarifa promedio por noche, ingreso por noche disponible, tasas de no-show y cancelación, antelación promedio. |
| GET | `/reportes/{tipo}/csv` | Cualquiera de los anteriores en CSV (`ocupacion`, `ingresos`, `canales`, `indicadores`). |

### Recordatorios, IA e imágenes

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/recordatorios?pagina` | Recepción, administración | Recordatorios enviados. |
| POST | `/recordatorios/envios` | Recepción, administración | Envía ya los recordatorios de hoy (sin duplicar). |
| GET | `/novedades/ia` | Personal | Si la IA está habilitada y con qué proveedor. |
| POST | `/novedades/sugerencia-gravedad` | Personal | `{descripcion}` → `{disponible, gravedad, justificacion, proveedor, mensaje}`. Nunca falla por la IA. |
| POST | `/imagenes` | Administrador | Multipart `archivo` (JPG, PNG o WEBP, máx. 5 MB) → `{url}`. 503 si el servicio externo no responde. |

## Procesos automáticos

- **Vencimiento de reservas pendientes (RN-21):** cada minuto (`sga.reservas.intervalo-vencimiento-ms`) se cancelan las reservas PENDIENTE que superan el plazo de confirmación. No generan penalidad y liberan sus noches.
- **Recordatorios:** todos los días a las 8:00 (`sga.recordatorios.cron`, hora de Colombia) se envían los recordatorios de llegada y de salida.

## Colección de peticiones

La carpeta `bruno/` contiene una colección de [Bruno](https://www.usebruno.com/) con el entorno `local` y todos los flujos anteriores. Para usarla contra el despliegue basta con cambiar la URL base del entorno por la de Render. Las peticiones de login guardan los tokens por rol y las fechas se calculan relativas al día actual.
