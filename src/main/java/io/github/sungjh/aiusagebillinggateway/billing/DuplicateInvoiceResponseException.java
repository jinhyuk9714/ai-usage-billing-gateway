package io.github.sungjh.aiusagebillinggateway.billing;

public class DuplicateInvoiceResponseException extends RuntimeException {

    private final InvoiceResponse response;

    public DuplicateInvoiceResponseException(InvoiceResponse response) {
        this.response = response;
    }

    public InvoiceResponse getResponse() {
        return response;
    }
}
