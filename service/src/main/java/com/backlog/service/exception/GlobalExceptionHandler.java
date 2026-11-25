package com.backlog.service.exception;

import jakarta.persistence.OptimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo global de excepciones de la API.
 * Traduce los errores de validación y de negocio a respuestas HTTP claras
 * para que el frontend pueda mostrar mensajes al usuario.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Devuelve un error 400 cuando falla la validación de un DTO de entrada.
     *
     * @param exception excepción con los errores de validación
     * @return respuesta con el mensaje general y el detalle por campo
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Los datos enviados no son válidos");
        body.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Devuelve un error 400 cuando una regla de negocio no se cumple.
     *
     * @param exception excepción con el mensaje de negocio
     * @return respuesta con el mensaje de la excepción
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Devuelve un error 403 cuando el usuario no tiene el permiso requerido.
     *
     * @param exception excepción de permiso denegado
     * @return respuesta con el mensaje del permiso faltante
     */
    @ExceptionHandler(PermisoDenegadoException.class)
    public ResponseEntity<Map<String, Object>> handlePermisoDenegado(
            PermisoDenegadoException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * Devuelve un error 409 cuando el elemento ya fue modificado por otro usuario.
     *
     * @param exception excepción de conflicto de concurrencia
     * @return respuesta con el mensaje del conflicto
     */
    @ExceptionHandler(ConflictoConcurrenciaException.class)
    public ResponseEntity<Map<String, Object>> handleConflictoConcurrencia(
            ConflictoConcurrenciaException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Devuelve un error 409 cuando Hibernate detecta una versión desactualizada.
     *
     * @param exception excepción de bloqueo optimista del proveedor JPA
     * @return respuesta con un mensaje para el usuario
     */
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(Exception exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "El elemento fue modificado por otro usuario. Recarga la página para ver los cambios.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Devuelve un error 500 controlado ante cualquier fallo no previsto.
     *
     * @param exception excepción inesperada
     * @return respuesta con un mensaje genérico
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception exception) {
        // Se registra la causa real para poder diagnosticar el fallo.
        logger.error("Error no controlado en la API", exception);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Ocurrió un error interno en el servidor");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
