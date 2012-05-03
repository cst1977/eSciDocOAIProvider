package de.fiz.karlsruhe.cache;

public class ValidationInfo {

    private ValidationResult result;
    private Throwable failure;
    private long validationDelay;

    public long getValidationDelay() {
        return validationDelay;
    }

    public void setValidationDelay(long validationDelay) {
        this.validationDelay = validationDelay;
    }

    public ValidationResult getResult() {
        return result;
    }

    public void setResult(ValidationResult result) {
        this.result = result;
    }

    public Throwable getFailReason() {
        return failure;
    }

    public void setFailReason(Throwable failure) {
        this.failure = failure;
    }
}
