package io.github.sungjh.aiusagebillinggateway.usage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UsageExceptionHandler {

    @ExceptionHandler(DuplicateUsageResponseException.class)
    ResponseEntity<UsageEventResponse> duplicate(DuplicateUsageResponseException exception) {
        return ResponseEntity.ok(exception.getResponse());
    }
}
