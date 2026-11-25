package com.backlog.service.autenticacion.services;

import com.backlog.service.autenticacion.dto.GoogleAccountInfo;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Verifica los ID tokens emitidos por Google Identity Services.
 * Comprueba la firma de Google, la audiencia (Client ID), la expiración
 * y que el correo de la cuenta esté verificado.
 */
@Service
public class GoogleTokenVerifier {

    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    /**
     * Construye el verificador con el Client ID configurado.
     * Si no hay Client ID, la verificación falla con un mensaje claro.
     *
     * @param clientId Client ID de la aplicación web en Google Cloud Console
     */
    public GoogleTokenVerifier(@Value("${security.google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();

        if (this.clientId.isBlank()) {
            this.verifier = null;
        } else {
            this.verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(List.of(this.clientId))
                    .build();
        }
    }

    /**
     * Verifica el ID token recibido del frontend y extrae la cuenta de Google.
     *
     * @param credential ID token en formato JWT entregado por Google
     * @return datos verificados de la cuenta
     * @throws IllegalArgumentException si el token es inválido o la cuenta no es apta
     */
    public GoogleAccountInfo verify(String credential) {
        if (clientId.isBlank()) {
            throw new IllegalArgumentException(
                    "El inicio de sesión con Google no está configurado en el servidor");
        }
        if (credential == null || credential.isBlank()) {
            throw new IllegalArgumentException("La credencial de Google es obligatoria");
        }

        try {
            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                throw new IllegalArgumentException("No se pudo verificar la cuenta de Google");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String subject = payload.getSubject();

            if (email == null || email.isBlank() || subject == null || subject.isBlank()) {
                throw new IllegalArgumentException(
                        "La cuenta de Google no tiene un correo válido");
            }
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new IllegalArgumentException(
                        "El correo de la cuenta de Google no está verificado");
            }

            String fullName = payload.get("name") instanceof String value ? value : null;
            return new GoogleAccountInfo(email, subject, fullName);
        } catch (IllegalArgumentException exception) {
            // Los errores de negocio ya traen un mensaje apto para el usuario.
            throw exception;
        } catch (Exception exception) {
            // Firma inválida, token expirado, error de red o JSON malformado.
            throw new IllegalArgumentException("No se pudo verificar la cuenta de Google");
        }
    }
}
