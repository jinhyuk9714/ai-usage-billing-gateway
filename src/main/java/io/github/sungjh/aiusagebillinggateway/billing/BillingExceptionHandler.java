package io.github.sungjh.aiusagebillinggateway.billing;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BillingExceptionHandler {

    @ExceptionHandler(DuplicateInvoiceResponseException.class)
    ResponseEntity<InvoiceResponse> duplicate(DuplicateInvoiceResponseException exception) {
        return ResponseEntity.ok(exception.getResponse());
    }
}
