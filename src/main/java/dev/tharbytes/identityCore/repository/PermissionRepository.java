package dev.tharbytes.identityCore.repository;

import dev.tharbytes.identityCore.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {
    Optional<PermissionEntity> findByPermissionKey(String permissionKey);
    List<PermissionEntity> findAllByOrderByPermissionKeyAsc();
}
