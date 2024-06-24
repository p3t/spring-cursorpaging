package io.vigier.cursorpaging.example.webapp.api.controller;

import jakarta.validation.ValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

    public record ErrorResponse(
            String message,
            String request ) {

    }

    @ExceptionHandler( value = { ValidationException.class } )
    ResponseEntity<Object> handleBadRequest( final Exception ex, final WebRequest request ) {
        return ResponseEntity.badRequest().body( new ErrorResponse( ex.getMessage(), request.toString() ) );
    }
}
