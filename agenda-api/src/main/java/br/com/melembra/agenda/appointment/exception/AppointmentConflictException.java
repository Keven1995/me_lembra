package br.com.melembra.agenda.appointment.exception;

public class AppointmentConflictException extends RuntimeException {
    public AppointmentConflictException() { super("Ja existe um atendimento neste periodo."); }
}
