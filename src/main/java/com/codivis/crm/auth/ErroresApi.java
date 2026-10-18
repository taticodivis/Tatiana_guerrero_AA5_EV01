package com.codivis.crm.auth;

import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Mantiene el formato JSON del contrato tambien en los errores HTTP. */
@RestControllerAdvice
public class ErroresApi {
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiController.Resultado> formato() {
        return ApiController.respuesta(415, false, "FORMATO_NO_ADMITIDO", "Usa application/x-www-form-urlencoded.");
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiController.Resultado> metodo(HttpRequestMethodNotSupportedException ex) {
        var respuesta = ApiController.respuesta(405, false, "METODO_NO_PERMITIDO", "Metodo HTTP no permitido.");
        return ResponseEntity.status(405).headers(respuesta.getHeaders())
                .header("Allow", ex.getSupportedMethods() == null ? "" : String.join(", ", ex.getSupportedMethods()))
                .body(respuesta.getBody());
    }
    @ExceptionHandler(IOException.class)
    ResponseEntity<ApiController.Resultado> almacenamiento(IOException ex) {
        java.util.logging.Logger.getLogger(ErroresApi.class.getName()).log(
                java.util.logging.Level.SEVERE, "Error guardando cuenta", ex);
        return ApiController.respuesta(500, false, "ERROR_INTERNO", "No se pudo guardar el usuario.");
    }
}
