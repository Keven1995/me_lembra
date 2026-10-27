package br.com.melembra.agenda.appointment.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException() { super("Agendamento nao encontrado."); }
}
