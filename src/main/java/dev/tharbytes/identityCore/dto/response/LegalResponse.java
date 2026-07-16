package dev.tharbytes.identityCore.dto.response;

public class LegalResponse {
    private String lastUpdated;
    private String contactEmail;
    private String companyName;
    private String cookiePolicyUpdated;

    public LegalResponse() {}

    public LegalResponse(String lastUpdated, String contactEmail, String companyName, String cookiePolicyUpdated) {
        this.lastUpdated = lastUpdated;
        this.contactEmail = contactEmail;
        this.companyName = companyName;
        this.cookiePolicyUpdated = cookiePolicyUpdated;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCookiePolicyUpdated() {
        return cookiePolicyUpdated;
    }

    public void setCookiePolicyUpdated(String cookiePolicyUpdated) {
        this.cookiePolicyUpdated = cookiePolicyUpdated;
    }
}
