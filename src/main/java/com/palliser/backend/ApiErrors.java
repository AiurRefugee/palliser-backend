package com.palliser.backend;

import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger log=LoggerFactory.getLogger(ApiErrors.class);
    private final JdbcTemplate jdbc;
    public ApiErrors(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> known(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",e.getReason()==null?"request failed":e.getReason()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,String>> unknown(Exception e,HttpServletRequest request) {
        log.error("API request failed",e);
        StringWriter trace=new StringWriter();
        e.printStackTrace(new PrintWriter(trace));
        try {
            String path=request.getRequestURI();
            jdbc.update("INSERT INTO api_error_log(request_id,method,path,exception_class,message,stack_trace) VALUES (?,?,?,?,?,?)",
                request.getAttribute("palliserRequestId"),request.getMethod(),
                path.substring(0,Math.min(path.length(),512)),e.getClass().getName(),
                e.getMessage(),trace.toString().substring(0,Math.min(trace.getBuffer().length(),65535)));
        } catch(Exception writeError) {log.warn("Error log database write failed: {}",writeError.toString());}
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error","internal server error"));
    }
}
