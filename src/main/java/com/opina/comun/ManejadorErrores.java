package com.opina.comun;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Los errores salen en formato Problem Details (RFC 9457). En los de validación se añade "errores",
 * con el mensaje de cada campo, para que el formulario pueda marcarlo.
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    /** Llega aquí desde Spring Security (ver SeguridadConfig.sinSesion). */
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail sinSesion(AuthenticationException ex) {
        String detalle = ex instanceof InsufficientAuthenticationException
                ? "Inicia sesión para ver esto"
                : "La sesión no es válida o ha caducado. Vuelve a iniciar sesión";
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detalle);
    }

    /** Dos peticiones a la vez que crean lo mismo (un email, un slug): la base de datos frena la segunda. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail duplicado(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Ya existe. Revisa los datos e inténtalo de nuevo");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(problemaDeValidacion(errores));
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(resultado -> errores.putIfAbsent(
                resultado.getMethodParameter().getParameterName(),
                resultado.getResolvableErrors().getFirst().getDefaultMessage()));
        return ResponseEntity.badRequest().body(problemaDeValidacion(errores));
    }

    private static ProblemDetail problemaDeValidacion(Map<String, String> errores) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Hay datos que no son válidos");
        problema.setProperty("errores", errores);
        return problema;
    }
}
