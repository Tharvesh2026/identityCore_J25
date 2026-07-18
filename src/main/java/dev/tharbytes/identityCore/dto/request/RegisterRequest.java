package dev.tharbytes.identityCore.dto.request;

public class RegisterRequest {
    private String name;
    private String username;
    private String email;
    private String password;
    private String businessName;
    private String location;
    private String awsAccountId;
    private String gcpProjectId;
    private String azureSubscriptionId;

    public RegisterRequest() {}

    public RegisterRequest(String name, String username, String email, String password,
                           String businessName, String location,
                           String awsAccountId, String gcpProjectId, String azureSubscriptionId) {
        this.name = name;
        this.username = username;
        this.email = email;
        this.password = password;
        this.businessName = businessName;
        this.location = location;
        this.awsAccountId = awsAccountId;
        this.gcpProjectId = gcpProjectId;
        this.azureSubscriptionId = azureSubscriptionId;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getAwsAccountId() {
        return awsAccountId;
    }

    public void setAwsAccountId(String awsAccountId) {
        this.awsAccountId = awsAccountId;
    }

    public String getGcpProjectId() {
        return gcpProjectId;
    }

    public void setGcpProjectId(String gcpProjectId) {
        this.gcpProjectId = gcpProjectId;
    }

    public String getAzureSubscriptionId() {
        return azureSubscriptionId;
    }

    public void setAzureSubscriptionId(String azureSubscriptionId) {
        this.azureSubscriptionId = azureSubscriptionId;
    }
}
