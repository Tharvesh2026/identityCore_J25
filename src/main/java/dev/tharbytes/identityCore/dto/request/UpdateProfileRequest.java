package dev.tharbytes.identityCore.dto.request;

public class UpdateProfileRequest {
    private String name;
    private String username;
    private String email;
    private String businessName;
    private String location;

    public UpdateProfileRequest() {}

    public UpdateProfileRequest(String name, String username, String email, String businessName, String location) {
        this.name = name;
        this.username = username;
        this.email = email;
        this.businessName = businessName;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
