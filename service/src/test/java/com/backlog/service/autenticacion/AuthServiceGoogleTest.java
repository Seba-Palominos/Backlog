package com.backlog.service.autenticacion;

import com.backlog.service.autenticacion.dto.GoogleAccountInfo;
import com.backlog.service.autenticacion.dto.GoogleLoginRequest;
import com.backlog.service.autenticacion.dto.LoginResponse;
import com.backlog.service.autenticacion.services.AuthService;
import com.backlog.service.autenticacion.services.GoogleTokenVerifier;
import com.backlog.service.registro.entitys.Usuario;
import com.backlog.service.registro.repository.UsuarioRepository;
import com.backlog.service.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas del inicio de sesión con Google en el servicio de autenticación.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceGoogleTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginWithGoogleReutilizaLaCuentaExistentePorCorreo() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setUsername("jdoe");
        usuario.setEmail("jdoe@gmail.com");
        usuario.setPassword("hash-existente");

        when(googleTokenVerifier.verify("id-token"))
                .thenReturn(new GoogleAccountInfo("jdoe@gmail.com", "sub-1", "John Doe"));
        when(usuarioRepository.findByEmailIgnoreCase("jdoe@gmail.com"))
                .thenReturn(Optional.of(usuario));
        when(jwtService.generateToken("jdoe")).thenReturn("jwt-app");

        LoginResponse response = authService.loginWithGoogle(request("id-token"));

        assertEquals("jwt-app", response.getToken());
        assertEquals("jdoe", response.getUsername());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void loginWithGoogleCreaLaCuentaEnElPrimerAcceso() {
        when(googleTokenVerifier.verify("id-token"))
                .thenReturn(new GoogleAccountInfo("Jane.Doe@Gmail.com", "sub-2", "Jane Doe"));
        when(usuarioRepository.findByEmailIgnoreCase("jane.doe@gmail.com"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.existsByUsername("jane.doe")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash-aleatorio");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario guardado = invocation.getArgument(0);
            guardado.setId(9L);
            return guardado;
        });
        when(jwtService.generateToken("jane.doe")).thenReturn("jwt-nuevo");

        LoginResponse response = authService.loginWithGoogle(request("id-token"));

        assertEquals("jwt-nuevo", response.getToken());
        assertEquals("jane.doe", response.getUsername());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario creado = captor.getValue();
        assertEquals("jane.doe@gmail.com", creado.getEmail());
        assertEquals("jane.doe", creado.getUsername());
        assertEquals("hash-aleatorio", creado.getPassword());
    }

    @Test
    void loginWithGoogleGeneraUnNombreDeUsuarioUnico() {
        when(googleTokenVerifier.verify("id-token"))
                .thenReturn(new GoogleAccountInfo("jane.doe@gmail.com", "sub-3", "Jane Doe"));
        when(usuarioRepository.findByEmailIgnoreCase("jane.doe@gmail.com"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.existsByUsername("jane.doe")).thenReturn(true);
        when(usuarioRepository.existsByUsername("jane.doe1")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash-aleatorio");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken("jane.doe1")).thenReturn("jwt-nuevo");

        LoginResponse response = authService.loginWithGoogle(request("id-token"));

        assertEquals("jane.doe1", response.getUsername());
        assertNotEquals("", response.getToken());
    }

    @Test
    void loginWithGoogleRechazaUnTokenNoVerificado() {
        when(googleTokenVerifier.verify("token-invalido"))
                .thenThrow(new IllegalArgumentException("No se pudo verificar la cuenta de Google"));

        assertThrows(IllegalArgumentException.class,
                () -> authService.loginWithGoogle(request("token-invalido")));

        verifyNoInteractions(jwtService);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void loginWithGoogleExigeLaCredencial() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setCredential("   ");

        assertThrows(IllegalArgumentException.class,
                () -> authService.loginWithGoogle(request));

        verifyNoInteractions(googleTokenVerifier, jwtService);
    }

    private GoogleLoginRequest request(String credential) {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setCredential(credential);
        return request;
    }
}
