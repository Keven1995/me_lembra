package br.com.melembra.agenda.notification;

import br.com.melembra.agenda.appointment.entity.Appointment;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WebPushSender {
    private final String publicKey;
    private final String privateKey;
    private final String subject;
    private final ReminderMessageFactory messages;
    private final ObjectMapper mapper;

    public WebPushSender(@Value("${app.push.vapid-public-key:}") String publicKey,
                         @Value("${app.push.vapid-private-key:}") String privateKey,
                         @Value("${app.push.vapid-subject:mailto:admin@melembra.app}") String subject,
                         ReminderMessageFactory messages, ObjectMapper mapper) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
        this.messages = messages;
        this.mapper = mapper;
    }

    public boolean isConfigured() { return !publicKey.isBlank() && !privateKey.isBlank(); }

    public void send(PushSubscription subscription, Appointment appointment, ReminderKind kind) throws Exception {
        send(subscription, appointment, kind, "");
    }
    public void send(PushSubscription subscription, Appointment appointment, ReminderKind kind,
                     String travelInsight) throws Exception {
        if (!isConfigured()) return;
        var message = messages.create(appointment, kind, travelInsight);
        var payload = mapper.writeValueAsString(Map.of(
            "title", message.title(),
            "body", message.body(),
            "url", "/?event=" + appointment.getId(),
            "tag", appointment.getId() + "-" + kind));
        var pushService = new PushService();
        pushService.setSubject(subject);
        pushService.setPublicKey(publicKey);
        pushService.setPrivateKey(privateKey);
        var notification = new Notification(subscription.getEndpoint(), subscription.getP256dh(), subscription.getAuth(), payload);
        pushService.send(notification);
    }

}
