package dev.tharbytes.identityCore.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "permissions")
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "permission_key", unique = true, nullable = false, length = 100)
    private String permissionKey;

    @Column(length = 255)
    private String description;

    @ManyToMany(mappedBy = "permissions")
    private Set<RoleEntity> roles = new HashSet<>();

    public PermissionEntity() {}

    public PermissionEntity(String permissionKey, String description) {
        this.permissionKey = permissionKey;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPermissionKey() {
        return permissionKey;
    }

    public void setPermissionKey(String permissionKey) {
        this.permissionKey = permissionKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleEntity> roles) {
        this.roles = roles;
    }
}
