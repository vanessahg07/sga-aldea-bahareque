package co.edu.uniquindio.sga.domain.comun;

/**
 * Códigos de las reglas de negocio que el dominio puede rechazar.
 * Cada código tiene un mensaje con significado para el negocio.
 */
public enum CodigoError {

    // Estancia y fechas
    ESTANCIA_INVALIDA("La fecha de salida debe ser posterior a la fecha de entrada"),
    FECHA_ENTRADA_PASADA("La fecha de entrada no puede ser anterior a hoy"),

    // Apartamento
    APARTAMENTO_INACTIVO("El apartamento no está activo para la venta"),
    APARTAMENTO_INVALIDO("Los datos del apartamento no son válidos"),
    TARIFAS_INCOMPLETAS("El apartamento no tiene tarifa en todas las temporadas"),
    CAPACIDAD_EXCEDIDA("El número de ocupantes excede la capacidad del apartamento"),
    APARTAMENTO_NO_PREPARADO("El apartamento no está preparado para recibir al grupo"),
    TRANSICION_ESTADO_OPERATIVO_INVALIDA("El cambio de estado operativo no está permitido"),
    OPERACION_NO_PERMITIDA_PERSONAL_SERVICIO("El personal de servicio no puede realizar este cambio de estado"),
    APARTAMENTO_CON_RESERVAS_ACTIVAS("El apartamento tiene reservas activas o futuras"),

    // Disponibilidad
    NOCHES_NO_DISPONIBLES("El apartamento ya tiene una reserva activa en alguna de las noches solicitadas"),
    APARTAMENTO_BLOQUEADO("El apartamento tiene un bloqueo en alguna de las noches solicitadas"),
    TIEMPO_PREPARACION_INSUFICIENTE("No hay tiempo de preparación suficiente entre la salida y la entrada del mismo día"),
    BLOQUEO_SOBRE_RESERVAS("No se puede bloquear noches que ya tienen reservas activas"),
    BLOQUEO_INVALIDO("Los datos del bloqueo no son válidos"),

    // Temporadas y tarifas
    TEMPORADAS_SOLAPADAS("Las temporadas no pueden solaparse entre sí"),
    SIN_TEMPORADA_BASE("Debe existir exactamente una temporada base"),
    TEMPORADA_INVALIDA("Los datos de la temporada no son válidos"),
    TARIFA_INVALIDA("La tarifa debe ser un valor positivo"),

    // Ocupantes y titular
    OCUPANTE_INVALIDO("Los datos del ocupante no son válidos"),
    TITULAR_INVALIDO("Los datos del titular no son válidos"),
    TITULAR_NO_FACTURABLE("El titular debe ser un ocupante facturable"),

    // Reserva
    TRANSICION_INVALIDA("La reserva no puede pasar a ese estado desde su estado actual"),
    HORA_LLEGADA_REQUERIDA("La reserva debe tener hora estimada de llegada antes de confirmarse"),
    ANTICIPO_INSUFICIENTE("No se ha pagado el anticipo requerido para confirmar la reserva"),
    REGISTRO_ANTES_DE_ENTRADA("No se puede registrar la llegada antes de la fecha de entrada"),
    NO_SHOW_ANTICIPADO("Aún no se ha alcanzado la hora límite para declarar no-show"),
    RESERVA_NO_MODIFICABLE("La reserva solo puede modificarse en estado PENDIENTE o CONFIRMADA"),
    RESERVA_INVALIDA("Los datos de la reserva no son válidos"),

    // Reglas propias
    ESTANCIA_MINIMA_NO_CUMPLIDA("La estancia no cumple el mínimo de noches exigido por la temporada"),
    MASCOTAS_NO_PERMITIDAS("El apartamento no admite mascotas"),
    MASCOTAS_EXCEDIDAS("La cantidad de mascotas excede el máximo permitido"),

    // Política
    POLITICA_INVALIDA("La política de cancelación no es válida"),

    // Folio
    FOLIO_CERRADO("El folio está cerrado y no admite movimientos"),
    PAGO_INVALIDO("Todo pago debe tener un valor positivo, un medio de pago y una fecha"),
    CARGO_INVALIDO("Los datos del cargo no son válidos"),
    MOVIMIENTO_NO_ENCONTRADO("El movimiento indicado no existe en el folio"),
    MOVIMIENTO_YA_REVERTIDO("El movimiento ya fue revertido"),
    FOLIO_CON_SALDO("El folio tiene saldo pendiente y requiere autorización del administrador para cerrarse"),
    AUTORIZACION_INVALIDA("La autorización debe indicar autor y motivo"),

    // Canal externo
    RESERVA_EXTERNA_INVALIDA("La reserva externa debe indicar el canal y su identificador propio"),
    CONFLICTO_YA_REVISADO("El conflicto ya fue revisado"),

    // Configuración
    PARAMETROS_INVALIDOS("Los parámetros del alojamiento no son válidos"),
    DINERO_INVALIDO("El valor monetario no es válido"),
    ALOJAMIENTO_INVALIDO("Los datos del alojamiento no son válidos"),
    ALOJAMIENTO_NO_CONFIGURADO("El alojamiento no ha sido configurado"),
    POLITICA_NO_CONFIGURADA("No existe una política de cancelación vigente"),
    MEDIO_PAGO_NO_ACEPTADO("El alojamiento no acepta ese medio de pago"),
    SERVICIO_NO_ENCONTRADO("El servicio adicional no existe"),

    // Recursos inexistentes
    APARTAMENTO_NO_ENCONTRADO("El apartamento no existe"),
    APARTAMENTO_YA_EXISTE("Ya existe un apartamento con ese código"),
    IMAGENES_INVALIDAS("Un apartamento admite entre 1 y 10 imágenes, con exactamente una principal"),
    RESERVA_NO_ENCONTRADA("La reserva no existe"),
    BLOQUEO_NO_ENCONTRADO("El bloqueo no existe"),
    NOVEDAD_INVALIDA("La novedad debe indicar autor, descripción y gravedad");

    private final String mensaje;

    CodigoError(String mensaje) {
        this.mensaje = mensaje;
    }

    public String mensaje() {
        return mensaje;
    }
}
