package com.backlog.service.autenticacion.services;

import com.backlog.service.autenticacion.dto.GoogleAccountInfo;
import com.backlog.service.autenticacion.dto.GoogleLoginRequest;
import com.backlog.service.autenticacion.dto.LoginRequest;
import com.backlog.service.autenticacion.dto.LoginResponse;
import com.backlog.service.registro.entitys.Usuario;
import com.backlog.service.registro.repository.UsuarioRepository;
import com.backlog.service.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lógica de autenticación de usuarios y generación del token de acceso.
 */
@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private GoogleTokenVerifier googleTokenVerifier;

    /**
     * Bloqueos por correo para evitar altas duplicadas cuando dos primeros
     * inicios de sesión con Google llegan al mismo tiempo.
     */
    private final Map<String, Object> emailLocks = new ConcurrentHashMap<>();

    /**
     * Valida las credenciales y devuelve un token JWT recién generado.
     *
     * @param request credenciales del usuario
     * @return respuesta con el token generado y el nombre de usuario
     */
    public LoginResponse login(LoginRequest request) {
        validateRequest(request);

        String username = request.getUsername().trim();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        String token = jwtService.generateToken(usuario.getUsername());
        return new LoginResponse(token, usuario.getUsername());
    }

    /**
     * Autentica al usuario con un ID token de Google ya verificado.
     * Si es el primer inicio de sesión, crea la cuenta local asociada al correo.
     *
     * @param request ID token entregado por Google Identity Services
     * @return respuesta con el token generado y el nombre de usuario
     */
    public LoginResponse loginWithGoogle(GoogleLoginRequest request) {
        if (request == null || request.getCredential() == null || request.getCredential().isBlank()) {
            throw new IllegalArgumentException("La credencial de Google es obligatoria");
        }

        GoogleAccountInfo account = googleTokenVerifier.verify(request.getCredential());
        String email = account.email().trim().toLowerCase(Locale.ROOT);

        Usuario usuario = findOrCreateGoogleUser(email, account);
        String token = jwtService.generateToken(usuario.getUsername());
        return new LoginResponse(token, usuario.getUsername());
    }

    /**
     * Busca la cuenta por correo y la crea si todavía no existe.
     * El bloqueo por correo evita dos altas simultáneas de la misma cuenta.
     */
    private Usuario findOrCreateGoogleUser(String email, GoogleAccountInfo account) {
        Object lock = emailLocks.computeIfAbsent(email, key -> new Object());
        try {
            synchronized (lock) {
                return usuarioRepository.findByEmailIgnoreCase(email)
                        .orElseGet(() -> createGoogleUser(email, account));
            }
        } finally {
            emailLocks.remove(email, lock);
        }
    }

    /**
     * Crea una cuenta local para un usuario de Google.
     * La contraseña es aleatoria porque esta cuenta solo se autentica con Google.
     */
    private Usuario createGoogleUser(String email, GoogleAccountInfo account) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setUsername(buildUniqueUsername(email));
        usuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        return usuarioRepository.save(usuario);
    }

    /**
     * Genera un nombre de usuario único a partir de la parte local del correo.
     */
    private String buildUniqueUsername(String email) {
        int atIndex = email.indexOf('@');
        String localPart = atIndex > 0 ? email.substring(0, atIndex) : email;

        String base = localPart.replaceAll("[^a-zA-Z0-9._-]", "").toLowerCase(Locale.ROOT);
        if (base.isBlank()) {
            base = "usuario";
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }

        String candidate = base;
        int suffix = 1;
        while (usuarioRepository.existsByUsername(candidate)) {
            candidate = base + suffix;
            suffix++;
            if (suffix > 10000) {
                throw new IllegalArgumentException("No se pudo generar un nombre de usuario");
            }
        }
        return candidate;
    }

    private void validateRequest(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos del inicio de sesión son obligatorios");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
    }
}
