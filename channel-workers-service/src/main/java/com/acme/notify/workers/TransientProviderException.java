package com.acme.notify.workers;

/** Vendor-side transient failure (5xx, timeout): the router tries the next vendor. */
public class TransientProviderException extends RuntimeException {
    public TransientProviderException(String message) { super(message); }
}
