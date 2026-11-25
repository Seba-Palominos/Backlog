package com.backlog.service.autenticacion.controller;

import com.backlog.service.autenticacion.dto.GoogleLoginRequest;
import com.backlog.service.autenticacion.dto.LoginRequest;
import com.backlog.service.autenticacion.dto.LoginResponse;
import com.backlog.service.autenticacion.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de la funcionalidad de autenticación de usuarios.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Autentica al usuario y devuelve el token generado para la sesión.
     *
     * @param request credenciales enviadas en formato JSON
     * @return respuesta con el token JWT
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Autentica al usuario con su cuenta de Google.
     *
     * @param request ID token entregado por Google Identity Services
     * @return respuesta con el token JWT del servicio
     */
    @PostMapping("/google")
    public ResponseEntity<LoginResponse> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }
}
