package br.com.melembra.agenda.notification;

import br.com.melembra.agenda.appointment.entity.Appointment;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReminderMessageFactory {
    private final ZoneId zone;
    public ReminderMessageFactory(@Value("${app.time-zone:America/Sao_Paulo}") String timeZone) {
        this.zone = ZoneId.of(timeZone);
    }
    public ReminderMessage create(Appointment appointment, ReminderKind kind) {
        return create(appointment, kind, "");
    }
    public ReminderMessage create(Appointment appointment, ReminderKind kind, String travelInsight) {
        var time = appointment.getStartAt().atZone(zone)
            .format(DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("pt-BR")));
        var icon = switch (appointment.getCategory()) {
            case HEALTH -> "🩺";
            case FINANCE -> "🏦";
            case WORK -> "💼";
            case PERSONAL -> "✨";
            case OTHER -> "📌";
        };
        var event = appointment.getTitle();
        var suggestion = switch (appointment.getCategory()) {
            case HEALTH -> "Separe documentos e o que precisar levar.";
            case FINANCE -> "Confira seus documentos e o horário de atendimento.";
            case WORK -> "Revise os materiais e a pauta.";
            case PERSONAL -> "Reserve esse tempinho na sua rotina.";
            case OTHER -> "Confira se está tudo organizado.";
        };
        var body = switch (kind) {
            case DAY_BEFORE -> "Amanhã, às " + time + ", você tem: " + event + ". " + suggestion;
            case DAY_OF -> "É hoje! " + event + " está marcado para " + time + ". Confira sua programação.";
            case THREE_HOURS -> "Faltam 3 horas para " + event + " (" + time + "). " + suggestion;
            case ONE_HOUR -> "Falta 1 hora para " + event + " (" + time + "). "
                + (travelInsight == null || travelInsight.isBlank() ? "Hora de se preparar!" : travelInsight);
        };
        if ((travelInsight == null || travelInsight.isBlank() || kind != ReminderKind.ONE_HOUR)
            && appointment.getLocation() != null && !appointment.getLocation().isBlank()) {
            body += " Local: " + appointment.getLocation() + ".";
        }
        var heading = switch (kind) {
            case DAY_BEFORE -> "Prepare-se para amanhã";
            case DAY_OF -> "Seu compromisso é hoje";
            case THREE_HOURS -> "Seu compromisso está chegando";
            case ONE_HOUR -> "Está quase na hora!";
        };
        return new ReminderMessage(icon + " " + heading, body);
    }
    public record ReminderMessage(String title, String body) { }
}
