package dev.tharbytes.identityCore.repository;

import dev.tharbytes.identityCore.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByMailId(String mailId);
    Optional<UserEntity> findByUsername(String username);
    boolean existsByMailId(String mailId);
    boolean existsByUsername(String username);
}
