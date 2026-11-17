# Anexo A — Ficha del Alojamiento: Aldea Bahareque

> Todo lo definido aquí es **configuración del sistema** (parámetros, temporadas, tarifas, política), nunca valores quemados en el código.

## A.1 Identidad

| Campo | Valor |
|---|---|
| Nombre del alojamiento | **Aldea Bahareque** — Apartamentos turísticos |
| Ciudad y ubicación | Filandia, Quindío, Colombia. A tres cuadras del parque principal, camino al mirador. Coordenadas aprox.: 4.6746, -75.6582 |
| Descripción breve | Siete apartamentos independientes en una edificación de estilo bahareque, pensados para parejas, familias y grupos que visitan el Paisaje Cultural Cafetero. |
| Integrantes del equipo | _(por definir)_ |

## A.2 Inventario

| ID | Nombre | Dormitorios | Capacidad | Características |
|---|---|---|---|---|
| BH-101 | Guadua | 1 | 2 | Balcón con vista al valle, cama queen. Admite mascotas |
| BH-102 | Yarumo | 1 | 3 | Primer piso sin escaleras (accesible), sofá cama |
| BH-201 | Cafetal | 2 | 4 | Cocina completa, lavadora |
| BH-202 | Barranquero | 2 | 4 | Patio privado. Admite mascotas |
| BH-203 | Palma de Cera | 2 | 5 | Vista panorámica, sofá cama |
| BH-301 | Mirador | 3 | 6 | Terraza privada con jacuzzi |
| BH-302 | Quimbaya | 3 | 8 | Familiar, dos baños, comedor para ocho |

Siete apartamentos con seis capacidades distintas (2, 3, 4, 5, 6 y 8). Las áreas comunes (parqueadero, terraza común, accesos) no son inventario vendible.

**Por qué alguien pagaría más por uno que por otro:** los de un dormitorio son para parejas y tienen la mejor relación privacidad/vista; el Mirador es la unidad premium por el jacuzzi; el Quimbaya baja el valor por persona para atraer grupos grandes. El Yarumo es el único sin escaleras y se advierte al reservar.

## A.3 Temporadas y tarifas

Las fechas se expresan como rangos `[inicio, fin)`: la fecha final no pertenece a la temporada.

| Temporada | Fechas que cubre | Criterio de negocio | Estancia mínima (RP-01) |
|---|---|---|---|
| BASE | Resto del año | Demanda regular del Eje Cafetero | 1 noche |
| MITAD_DE_ANIO | 15-jun al 15-jul (`[15-jun, 16-jul)`) | Vacaciones escolares de mitad de año | 1 noche |
| SEMANA_SANTA | 21-mar al 28-mar de 2027 (`[21-mar, 29-mar)`) | Pico de turismo nacional; cambia cada año | 2 noches |
| FIN_DE_ANIO | 15-dic al 15-ene (`[15-dic, 16-ene)`) | Temporada más alta de la región | 2 noches |

Tarifa **por ocupante facturable por noche** (COP):

| Apartamento | BASE | MITAD_DE_ANIO | SEMANA_SANTA | FIN_DE_ANIO |
|---|---|---|---|---|
| BH-101 Guadua | 95.000 | 120.000 | 140.000 | 140.000 |
| BH-102 Yarumo | 95.000 | 120.000 | 140.000 | 140.000 |
| BH-201 Cafetal | 85.000 | 105.000 | 125.000 | 125.000 |
| BH-202 Barranquero | 85.000 | 105.000 | 125.000 | 125.000 |
| BH-203 Palma de Cera | 85.000 | 105.000 | 125.000 | 125.000 |
| BH-301 Mirador | 95.000 | 120.000 | 140.000 | 140.000 |
| BH-302 Quimbaya | 75.000 | 95.000 | 110.000 | 110.000 |

Criterio: los apartamentos grandes bajan el valor por persona (economía de grupo). El Mirador cobra un 25 % más que su categoría por el jacuzzi.

## A.4 Parámetros y políticas

| Parámetro | Valor | Justificación |
|---|---|---|
| Umbral de edad facturable | 5 años | Práctica común en la región: los niños pequeños comparten cama con sus padres |
| Hora de entrada | 15:00 | |
| Hora de salida | 11:00 | Deja una ventana de 4 h para preparar |
| Tiempo de preparación (horas) | 3 | Cabe en la ventana, así que se admite una entrada el mismo día de una salida |
| Plazo de confirmación (horas) | 24 | Tiempo razonable para enviar el anticipo por transferencia |
| Hora límite para no-show | 22:00 | Última llegada razonable desde Armenia o Pereira |
| ¿Se exige anticipo? ¿De cuánto? | Sí, **30 %** del valor de la estancia, requisito para confirmar | Reduce los no-show |
| ¿Se aceptan mascotas? Límites y cargo | Solo en BH-101 y BH-202, máximo 1 por reserva, cargo fijo de 40.000 por estancia | Aseo adicional |
| Normas de convivencia | Silencio de 22:00 a 7:00; prohibido fumar dentro; no se permiten fiestas ni eventos; los visitantes no registrados no pernoctan | |

**Política de cancelación (versión 1):**

| Antelación (días antes de la entrada) | Retención | Devolución |
|---|---|---|
| 15 días o más | 0 % | 100 % de lo pagado |
| De 7 a 14 días | 30 % del valor de la estancia | Lo pagado menos la retención |
| Menos de 7 días | 60 % del valor de la estancia | Lo pagado menos la retención |

| Consecuencia del no-show | Se retiene el **30 %** del valor de la estancia (equivale al anticipo) y se liberan las noches. |
|---|---|

## A.5 Canales, pagos y servicios

| Campo | Valor |
|---|---|
| Canales de venta habilitados | PORTAL, DIRECTO, EXTERNO ("ViajaEje", canal simulado) |
| Medios de pago aceptados | EFECTIVO, TRANSFERENCIA, TARJETA |
| Servicios adicionales y su cargo | Desayuno típico: 25.000 por persona por día · Tour de café: 80.000 por persona · Traslado desde la terminal de Armenia: 90.000 por trayecto |

## A.6 Diferenciación del equipo

| Elemento | Definición |
|---|---|
| Regla propia 1 (RP-01) | **Estancia mínima por temporada.** Cada temporada define su estancia mínima; FIN_DE_ANIO y SEMANA_SANTA exigen 2 noches. Se rechaza una reserva más corta si alguna de sus noches cae en esa temporada. |
| Regla propia 2 (RP-02) | **Descuento por estancia larga.** Las estancias de 7 noches o más reciben un 10 % de descuento sobre el cargo de alojamiento. El umbral y el porcentaje son configurables y el redondeo se aplica al final. |
| Regla propia 3 (RP-03) | **Mascotas.** Solo se admiten en apartamentos que las aceptan, con un máximo configurable por reserva, y generan un cargo fijo configurable por estancia. |
| Caso de uso del modelo de IA | Sugerir la **gravedad de una novedad** (BAJA, MEDIA, ALTA o CRÍTICA) a partir de su descripción cuando la registra el personal de servicio. |
| Comportamiento con la IA deshabilitada | Con `sga.ia.habilitada=false`, o si el modelo tarda más de 5 s, no hay sugerencia: el personal elige la gravedad a mano y ve el aviso "Sugerencia de IA no disponible". |
| Integración de salida elegida y proveedor | **Notificaciones por correo (opción A).** Adaptador SMTP con clave de aplicación o un servicio de captura de correo para pruebas, más un adaptador en memoria para trabajar sin conexión. |
| Comportamiento del sistema si la integración falla | La notificación queda en la bitácora con estado FALLIDA y la operación de negocio continúa. Tiempo de espera: 10 s. |
| Funcionalidad opcional elegida | **Recordatorios automáticos** antes de la llegada y antes de la salida. |
