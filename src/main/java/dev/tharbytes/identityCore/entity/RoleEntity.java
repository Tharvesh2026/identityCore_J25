package dev.tharbytes.identityCore.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", unique = true, nullable = false, length = 100)
    private String roleName;

    @Column(length = 20, nullable = false)
    private String status = "ACTIVE";

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<PermissionEntity> permissions = new HashSet<>();

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private List<UserEntity> users = new ArrayList<>();

    public RoleEntity() {}

    public RoleEntity(String roleName, String status) {
        this.roleName = roleName;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Set<PermissionEntity> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<PermissionEntity> permissions) {
        this.permissions = permissions;
    }

    public List<UserEntity> getUsers() {
        return users;
    }

    public void setUsers(List<UserEntity> users) {
        this.users = users;
    }
}
