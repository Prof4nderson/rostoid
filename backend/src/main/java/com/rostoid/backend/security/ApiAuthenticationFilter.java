package com.rostoid.backend.security;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*; import jakarta.servlet.http.*; import org.springframework.http.MediaType; import org.springframework.stereotype.Component; import org.springframework.web.filter.OncePerRequestFilter; import java.io.IOException; import java.util.Map;
@Component public class ApiAuthenticationFilter extends OncePerRequestFilter {
 private final SecurityService service; private final ObjectMapper mapper;
 public ApiAuthenticationFilter(SecurityService service,ObjectMapper mapper){this.service=service;this.mapper=mapper;}
 @Override protected boolean shouldNotFilter(HttpServletRequest r){String p=r.getRequestURI();return !p.startsWith("/api/")||p.equals("/api/health")||p.equals("/api/security/state")||p.equals("/api/security/register")||p.equals("/api/security/login");}
 @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain c)throws ServletException,IOException{String a=r.getHeader("Authorization"), t=a!=null&&a.startsWith("Bearer ")?a.substring(7):null, user=service.authenticate(t); if(user==null){s.setStatus(401);s.setContentType(MediaType.APPLICATION_JSON_VALUE);mapper.writeValue(s.getWriter(),Map.of("error","Sessão expirada ou inválida"));return;} try{AuthenticatedUser.set(user);c.doFilter(r,s);}finally{AuthenticatedUser.clear();}}
}
