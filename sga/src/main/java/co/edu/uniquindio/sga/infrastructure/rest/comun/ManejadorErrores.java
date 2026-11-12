package co.edu.uniquindio.sga.infrastructure.rest.comun;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Traduce las excepciones a respuestas HTTP con significado de negocio. Nunca expone
 * trazas técnicas al cliente.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    private static final Set<CodigoError> NO_ENCONTRADO = EnumSet.of(
            CodigoError.APARTAMENTO_NO_ENCONTRADO, CodigoError.RESERVA_NO_ENCONTRADA,
            CodigoError.BLOQUEO_NO_ENCONTRADO, CodigoError.TEMPORADA_NO_ENCONTRADA,
            CodigoError.USUARIO_NO_ENCONTRADO, CodigoError.SERVICIO_NO_ENCONTRADO,
            CodigoError.MOVIMIENTO_NO_ENCONTRADO, CodigoError.CANAL_NO_ENCONTRADO, CodigoError.CONFLICTO_NO_ENCONTRADO);

    private static final Set<CodigoError> CONFLICTO = EnumSet.of(
            CodigoError.NOCHES_NO_DISPONIBLES, CodigoError.APARTAMENTO_BLOQUEADO,
            CodigoError.TIEMPO_PREPARACION_INSUFICIENTE, CodigoError.BLOQUEO_SOBRE_RESERVAS,
            CodigoError.APARTAMENTO_YA_EXISTE, CodigoError.TEMPORADA_YA_EXISTE, CodigoError.CORREO_YA_REGISTRADO,
            CodigoError.TRANSICION_INVALIDA, CodigoError.TRANSICION_ESTADO_OPERATIVO_INVALIDA,
            CodigoError.APARTAMENTO_NO_PREPARADO, CodigoError.APARTAMENTO_CON_RESERVAS_ACTIVAS,
            CodigoError.USUARIO_CON_RESERVAS_ACTIVAS, CodigoError.FOLIO_CERRADO, CodigoError.FOLIO_CON_SALDO,
            CodigoError.MOVIMIENTO_YA_REVERTIDO, CodigoError.RESERVA_NO_MODIFICABLE,
            CodigoError.TEMPORADAS_SOLAPADAS, CodigoError.SIN_TEMPORADA_BASE,
            CodigoError.ALOJAMIENTO_NO_CONFIGURADO, CodigoError.POLITICA_NO_CONFIGURADA,
            CodigoError.CANAL_YA_EXISTE, CodigoError.CONFLICTO_YA_REVISADO);

    private static final Set<CodigoError> PROHIBIDO = EnumSet.of(
            CodigoError.ACCESO_DENEGADO, CodigoError.OPERACION_NO_PERMITIDA_PERSONAL_SERVICIO, CodigoError.CANAL_INACTIVO);

    static HttpStatus estadoPara(CodigoError codigo) {
        if (NO_ENCONTRADO.contains(codigo)) return HttpStatus.NOT_FOUND;
        if (CONFLICTO.contains(codigo)) return HttpStatus.CONFLICT;
        if (PROHIBIDO.contains(codigo)) return HttpStatus.FORBIDDEN;
        if (codigo == CodigoError.CREDENCIALES_INVALIDAS || codigo == CodigoError.CREDENCIAL_CANAL_INVALIDA)
            return HttpStatus.UNAUTHORIZED;
        if (codigo == CodigoError.SERVICIO_EXTERNO_NO_DISPONIBLE) return HttpStatus.SERVICE_UNAVAILABLE;
        return HttpStatus.UNPROCESSABLE_CONTENT;
    }

    @ExceptionHandler(ExcepcionNegocio.class)
    public ResponseEntity<ErrorRespuesta> negocio(ExcepcionNegocio e) {
        return ResponseEntity.status(estadoPara(e.getCodigo()))
                .body(ErrorRespuesta.de(e.getCodigo().name(), e.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException e) {
        List<String> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorRespuesta.de("SOLICITUD_INVALIDA", "La solicitud tiene datos inválidos", detalles));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ErrorRespuesta> formato(Exception e) {
        LOG.debug("Solicitud con formato inválido: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ErrorRespuesta.de("SOLICITUD_INVALIDA",
                "La solicitud no tiene el formato esperado (revise fechas, números y campos obligatorios)", List.of()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorRespuesta> archivoGrande(MaxUploadSizeExceededException e) {
        return negocio(new ExcepcionNegocio(CodigoError.IMAGEN_INVALIDA));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorRespuesta> accesoDenegado(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorRespuesta.de(CodigoError.ACCESO_DENEGADO.name(),
                CodigoError.ACCESO_DENEGADO.mensaje(), List.of()));
    }

    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ErrorRespuesta> rutaInexistente(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorRespuesta.de("RECURSO_INEXISTENTE", "La ruta solicitada no existe", List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception e) {
        LOG.error("Error inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorRespuesta.de("ERROR_INTERNO",
                "Ocurrió un error inesperado. Intente de nuevo más tarde.", List.of()));
    }
}
