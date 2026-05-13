package io.github.sungjh.aiusagebillinggateway.usage;

public class DuplicateUsageResponseException extends RuntimeException {

    private final UsageEventResponse response;

    public DuplicateUsageResponseException(UsageEventResponse response) {
        this.response = response;
    }

    public UsageEventResponse getResponse() {
        return response;
    }
}
