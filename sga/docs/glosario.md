# Glosario — Lenguaje ubicuo del SGA

Los términos de la sección 4 del enunciado son obligatorios y no se renombran. Este glosario los relaciona con su representación en el código y agrega los términos propios de Aldea Bahareque.

## Términos del enunciado

| Término | Significado | En el código |
|---|---|---|
| Alojamiento | El negocio completo, bajo una misma administración | `domain.alojamiento`, `ParametrosAlojamiento` |
| Apartamento | Unidad vendible, entregada en exclusiva a un grupo | `Apartamento` |
| Dormitorio | Espacio para dormir; atributo descriptivo, no se reserva por separado | `Apartamento.dormitorios` |
| Capacidad | Máximo de personas que admite un apartamento; es un tope rígido | `Apartamento.capacidad`, `validarCapacidad` |
| Noche | Unidad mínima de venta | `Estancia.listaNoches()` |
| Estancia | Rango `[entrada, salida)` de noches de una reserva | `Estancia` |
| Reserva | Compromiso de ocupar un apartamento durante una estancia | `Reserva` |
| Titular | Responsable de la reserva y de su pago; siempre es facturable | `Titular` |
| Ocupante | Persona incluida en la reserva, con su fecha de nacimiento | `Ocupante` |
| Ocupante facturable | Ocupante que alcanza el umbral de edad a la fecha de entrada | `Ocupante.esFacturable` |
| Tarifa | Valor por ocupante facturable, por noche, para un apartamento en una temporada | `Tarifa`, `TablaTarifas` |
| Temporada | Periodo del calendario con tarifas propias | `Temporada` |
| Temporada base | Temporada que cubre todas las fechas no asignadas a otra | `Temporada.esBase`, `CalendarioTemporadas` |
| Disponibilidad | Cálculo que dice si se pueden vender unas noches | `VerificadorDisponibilidad` |
| Bloqueo | Registro administrativo que impide vender un rango de noches | `Bloqueo` |
| Estado operativo | Condición física presente del apartamento | `EstadoOperativo` |
| Tiempo de preparación | Horas requeridas entre una salida y la siguiente entrada | `ParametrosAlojamiento.horasPreparacion` |
| Registro | Momento en que el grupo toma posesión del apartamento | `Reserva.registrarLlegada` |
| Salida | Momento en que el apartamento se libera | `Reserva.registrarSalida` |
| No-show | El titular no se presenta antes de la hora límite | `Reserva.declararNoShow`, `EstadoReserva.NO_SHOW` |
| Política de cancelación | Regla única y versionada de retenciones y devoluciones | `PoliticaCancelacion`, `TramoCancelacion` |
| Folio | Cuenta de la reserva: cargos, pagos y saldo | `Folio` |
| Cargo | Concepto que suma al folio | `Cargo`, `TipoCargo` |
| Pago | Abono con medio y fecha | `Pago`, `MedioPago` |
| Saldo | Cargos menos pagos | `Folio.saldo()` |
| Canal | Origen de la reserva: portal, directo o externo | `Canal` |
| Conflicto de canal | Reserva externa rechazada por colisionar con una vigente | `ConflictoCanal` |
| Novedad | Reporte de un daño, faltante o situación en un apartamento, con una gravedad (baja, media, alta o crítica) | `Novedad`, `Novedad.Gravedad` |

## Términos propios de Aldea Bahareque

| Término | Significado | En el código |
|---|---|---|
| Acompañante | Ocupante de la reserva distinto del titular | `SolicitudReserva.acompanantes` |
| Cotización | Valor calculado de una estancia con su desglose noche por noche; al crear la reserva queda congelada | `CotizacionEstancia`, `DetalleNoche` |
| Anticipo | Porcentaje del valor de la estancia que se exige pagar para confirmar | `ParametrosAlojamiento.porcentajeAnticipo`, `Reserva.anticipoRequerido` |
| Retención | Valor que el alojamiento conserva al cancelar o ante un no-show, según la política congelada | `PoliticaCancelacion.retencionPor*` |
| Tramo de cancelación | Antelación mínima y porcentaje de retención dentro de una política | `TramoCancelacion` |
| Estancia mínima | Noches mínimas que exige una temporada (RP-01) | `Temporada.estanciaMinimaNoches` |
| Descuento por estancia larga | Descuento sobre el alojamiento a partir de cierto número de noches (RP-02) | `ParametrosAlojamiento.nochesDescuentoLargo` |
| Mascota | Animal de compañía admitido según el apartamento, con cargo por estancia (RP-03) | `Apartamento.admiteMascotas`, `TipoCargo.MASCOTA` |
| Reverso | Movimiento inverso que corrige un cargo o un pago sin borrarlo | `Folio.revertirCargo`, `Folio.revertirPago` |
| Autorización de cierre | Permiso del administrador, con autor y motivo, para cerrar un folio con saldo | `AutorizacionCierre` |
| Referencia externa | Canal más identificador propio de una reserva externa, único en el sistema | `ReferenciaExterna` |
| Vencimiento | Cancelación automática de una reserva pendiente al superar el plazo de confirmación | `Reserva.vencerSiCorresponde` |
| Recordatorio | Aviso automático antes de la llegada o de la salida; cada reserva recibe cada uno una sola vez | `Recordatorio`, `PlanificadorRecordatorios` |
| Sugerencia de gravedad | Gravedad de una novedad propuesta por la IA, no vinculante | `SugeridorGravedad.SugerenciaGravedad` |
| Canal de venta | Agencia o plataforma externa registrada, con tipo y credencial propia | `CanalVenta` |
| Bitácora de canal | Registro de todo lo intercambiado con los canales, incluidos los intentos rechazados | `EventoCanal` |
| Noche vendida | Noche de una reserva confirmada, en curso o finalizada; base del reporte de ocupación | `CalculadoraReportes` |
| Periodo | Rango de fechas de un reporte, con ambas fechas incluidas | `Periodo` |
