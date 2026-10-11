package com.cyberopsarena.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<?> validation(MethodArgumentNotValidException ex){return ResponseEntity.badRequest().body(Map.of("timestamp",Instant.now(),"status",400,"error","Invalid request","message","One or more fields are invalid"));}
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> status(ResponseStatusException ex){int status=ex.getStatusCode().value();return ResponseEntity.status(status).body(Map.of("timestamp",Instant.now(),"status",status,"error",ex.getStatusCode().toString(),"message",ex.getReason()==null?"Request could not be completed":ex.getReason()));}
 @ExceptionHandler(Exception.class) public ResponseEntity<?> unexpected(Exception ex){return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("timestamp",Instant.now(),"status",500,"error","Internal server error","message","The request could not be completed"));}
}
