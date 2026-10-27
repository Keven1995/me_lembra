package br.com.melembra.agenda.notification;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderJobRepository extends JpaRepository<ReminderJob, UUID> {
    @Modifying
    @Query("delete from ReminderJob r where r.appointment.id = :appointmentId")
    void deleteForAppointment(@Param("appointmentId") UUID appointmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReminderJob r join fetch r.appointment a where r.sentAt is null and r.dueAt <= :now order by r.dueAt")
    List<ReminderJob> due(@Param("now") Instant now, org.springframework.data.domain.Pageable limit);
}
