package br.com.melembra.agenda.user.repository;

import br.com.melembra.agenda.user.entity.User;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByGoogleId(String googleId);
}
