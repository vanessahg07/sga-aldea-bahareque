# Casos de uso — SGA Aldea Bahareque

Cada caso indica el actor, las reglas que aplica y el corte en que se expuso por API. Todos están implementados en la API (Corte 2 y 4), tienen pantalla en el frontend (Corte 3) y están disponibles en el despliegue. El dominio que los soporta se implementó desde el Corte 1.

## Huésped

| ID | Caso de uso | Reglas | Corte |
|---|---|---|---|
| CU-H01 | Registrarse e iniciar sesión | 7.2 (correo único, contraseña segura) | 2 |
| CU-H02 | Buscar apartamentos disponibles por fechas, grupo y características | RN-01, RN-02, RN-07, RN-20, paginación de 10 | 2 |
| CU-H03 | Ver el detalle de un apartamento (galería, dotación, calendario, mapa) | — | 3 |
| CU-H04 | Cotizar una estancia con desglose noche por noche | RN-05, RN-06, RP-02, RP-03 | 2 |
| CU-H05 | Crear una reserva por el canal PORTAL | Seis validaciones, RP-01, RP-03, RN-22 | 2 |
| CU-H06 | Cancelar su propia reserva | RN-08, RN-12, RN-13 | 2 |
| CU-H07 | Consultar su historial de reservas | Solo las propias | 2 |
| CU-H08 | Consultar el folio y el saldo de sus reservas | Solo las propias | 2 |
| CU-H09 | Recuperar la contraseña con un código enviado por correo | Código de 6 dígitos, válido 15 minutos y de un solo uso | 2 y 4 |
| CU-H10 | Actualizar su perfil y cambiar su contraseña | El correo no es editable; exige la contraseña actual | 2 |

## Recepcionista

| ID | Caso de uso | Reglas | Corte |
|---|---|---|---|
| CU-R01 | Crear una reserva por el canal DIRECTO para un titular sin cuenta | Igual que CU-H05 | 2 |
| CU-R02 | Modificar fechas, grupo o apartamento de una reserva no iniciada | RN-14, RN-22 | 2 |
| CU-R03 | Confirmar una reserva | RN-08, RN-09, anticipo | 2 |
| CU-R04 | Cancelar una reserva | RN-12, RN-13 | 2 |
| CU-R05 | Declarar no-show | RN-08, RN-13, hora límite | 2 |
| CU-R06 | Registrar la llegada | RN-10, RN-11 | 2 |
| CU-R07 | Registrar la salida | RN-17, estado operativo | 2 |
| CU-R08 | Consultar las llegadas y salidas del día | — | 2 |
| CU-R09 | Registrar cargos y pagos, revertirlos y cerrar el folio | RN-15, RN-16, RN-17 | 2 |
| CU-R10 | Consultar el calendario de ocupación | — | 2 |

## Administrador

Puede realizar todos los casos del recepcionista y, además:

| ID | Caso de uso | Reglas | Corte |
|---|---|---|---|
| CU-A01 | Gestionar los datos, horarios, normas y servicios del alojamiento | — | 2 |
| CU-A02 | Configurar los parámetros de la Ficha | Validación de `ParametrosAlojamiento` | 2 |
| CU-A03 | Gestionar apartamentos (CRUD con eliminación lógica) | Activar solo con tarifas completas; no eliminar con reservas activas | 2 |
| CU-A04 | Registrar bloqueos | No sobre noches reservadas | 2 |
| CU-A05 | Declarar un apartamento FUERA_DE_SERVICIO o rehabilitarlo | Transiciones de estado operativo | 2 |
| CU-A06 | Gestionar temporadas | Sin solapamiento, temporada base obligatoria | 2 |
| CU-A07 | Gestionar tarifas y consultar su histórico | Tarifa positiva | 2 |
| CU-A08 | Definir o actualizar la política de cancelación | Versionado, mínimo dos tramos | 2 |
| CU-A09 | Gestionar canales y resolver conflictos | RN-18 | 4 |
| CU-A10 | Gestionar usuarios y roles | Eliminación lógica, restricción por reservas activas | 2 |
| CU-A11 | Autorizar el cierre de un folio con saldo | RN-17 | 2 |
| CU-A12 | Consultar reportes de ocupación, ingresos y canales | 7.9 | 4 |
| CU-A13 | Subir imágenes de los apartamentos | JPG, PNG o WEBP de hasta 5 MB; entre 1 y 10 por apartamento | 4 |
| CU-A14 | Consultar y enviar recordatorios | Un recordatorio por reserva y tipo | 4 |

## Personal de servicio

| ID | Caso de uso | Reglas | Corte |
|---|---|---|---|
| CU-S01 | Consultar los apartamentos pendientes de preparación | No accede a reservas ni folios | 2 |
| CU-S02 | Cambiar el estado entre PENDIENTE_PREPARACION, EN_PREPARACION y PREPARADO | No puede declarar FUERA_DE_SERVICIO | 2 |
| CU-S03 | Registrar novedades (con sugerencia de gravedad por IA) | IA no bloqueante | 2 y 4 |

## Canal externo (sistema)

| ID | Caso de uso | Reglas | Corte |
|---|---|---|---|
| CU-C01 | Consultar la disponibilidad de un rango de fechas | Credencial del canal | 4 |
| CU-C02 | Crear una reserva externa | RN-18, RN-19 | 4 |
| CU-C03 | Cancelar una reserva externa | RN-13 | 4 |

## Sistema (procesos automáticos)

| ID | Caso de uso | Reglas | Corte |
|---|---|---|---|
| CU-X01 | Vencer las reservas PENDIENTE fuera del plazo | RN-21 | 2 |
| CU-X02 | Notificar al titular la confirmación y la cancelación | Integración no bloqueante | 4 |
| CU-X03 | Enviar recordatorios antes de la llegada y de la salida | Funcionalidad opcional | 4 |
