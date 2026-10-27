package br.com.melembra.agenda.notification;

import jakarta.validation.constraints.NotBlank;

public record PushSubscriptionRequest(
    @NotBlank String endpoint,
    @NotBlank String p256dh,
    @NotBlank String auth
) { }
