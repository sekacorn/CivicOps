package org.civicops.shared.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends CivicOpsException {
    public ResourceNotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", resource + " was not found: " + id);
    }
}
