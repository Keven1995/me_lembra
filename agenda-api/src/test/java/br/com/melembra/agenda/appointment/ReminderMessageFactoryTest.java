package br.com.melembra.agenda.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.melembra.agenda.appointment.entity.Appointment;
import br.com.melembra.agenda.appointment.entity.EventCategory;
import br.com.melembra.agenda.notification.ReminderKind;
import br.com.melembra.agenda.notification.ReminderMessageFactory;
import br.com.melembra.agenda.user.entity.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReminderMessageFactoryTest {
    private final ReminderMessageFactory messages = new ReminderMessageFactory("America/Sao_Paulo");

    @Test
    void enrichesOnlyOneHourNotice() {
        var user = new User(UUID.randomUUID(), "google", "teste@example.com", "Usuário");
        var start = Instant.parse("2026-12-02T17:00:00Z");
        var event = Appointment.scheduled(user, "Consulta no dentista", EventCategory.HEALTH,
            "Clínica", null, start, start.plusSeconds(3600));

        var near = messages.create(event, ReminderKind.ONE_HOUR, "🌤️ 26°C, chuva 10%. 🚗 20min.");
        var early = messages.create(event, ReminderKind.DAY_BEFORE, "🌤️ 26°C, chuva 10%.");

        assertThat(near.body()).contains("26°C", "20min", "dentista");
        assertThat(early.body()).doesNotContain("26°C").contains("Clínica");
    }
}
