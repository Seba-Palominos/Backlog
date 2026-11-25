package com.backlog.service.registro.controller;

import com.backlog.service.registro.dto.RegistroRequest;
import com.backlog.service.registro.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de la funcionalidad de registro de usuarios.
 */
@RestController
@RequestMapping("/api/registro")
public class RegistroController {

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Recibe los datos del formulario de registro en formato JSON.
     *
     * @param request datos del formulario de registro
     * @return respuesta con el estado de creación
     */
    @PostMapping
    public ResponseEntity<Void> registro(@Valid @RequestBody RegistroRequest request) {
        usuarioService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
