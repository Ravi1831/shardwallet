package com.ravi.mds.shardedsagawallet.exception;

public class WalletInactiveException extends RuntimeException {
    public WalletInactiveException(String message) {
        super(message);
    }
}
