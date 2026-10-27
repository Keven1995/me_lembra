package br.com.melembra.agenda.notification;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationPreferenceRepository extends JpaRepository<LocationPreference, UUID> { }
