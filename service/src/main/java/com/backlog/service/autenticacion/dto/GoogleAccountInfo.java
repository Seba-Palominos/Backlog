package com.backlog.service.autenticacion.dto;

/**
 * Datos confiables extraídos de un ID token de Google ya verificado.
 *
 * @param email    correo de la cuenta de Google (ya verificado por Google)
 * @param subject  identificador único e inmutable de la cuenta de Google
 * @param fullName nombre visible de la cuenta, si Google lo entrega
 */
public record GoogleAccountInfo(String email, String subject, String fullName) {
}
