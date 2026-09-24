package com.palliser.backend;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Stores request metadata in SQL; never records bodies, cookies, auth headers or query values. */
@Component
public class RequestLogFilter extends OncePerRequestFilter {
    private static final Logger log=LoggerFactory.getLogger(RequestLogFilter.class);
    private final JdbcTemplate jdbc;
    public RequestLogFilter(JdbcTemplate jdbc) {this.jdbc=jdbc;}

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/") || "OPTIONS".equals(request.getMethod());
    }

    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        long start=System.nanoTime();
        String requestId=UUID.randomUUID().toString();
        request.setAttribute("palliserRequestId",requestId);
        response.setHeader("X-Request-Id",requestId);
        String errorCode=null;
        try {chain.doFilter(request,response);}
        catch(Exception e) {errorCode="UNHANDLED";throw e;}
        finally {
            long duration=(System.nanoTime()-start)/1_000_000;
            String path=request.getRequestURI();
            if (path.length()>512) path=path.substring(0,512);
            if (errorCode==null && response.getStatus()>=400) errorCode="HTTP_"+response.getStatus();
            try {
                jdbc.update("INSERT INTO api_request_log(request_id,method,path,status,duration_ms,error_code) VALUES (?,?,?,?,?,?)",
                    requestId,request.getMethod(),path,response.getStatus(),duration,errorCode);
            } catch(Exception e) {log.warn("Request log database write failed: {}",e.toString());}
        }
    }
}
