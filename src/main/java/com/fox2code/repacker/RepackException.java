package com.fox2code.repacker;

import java.io.IOException;

public class RepackException extends IOException {
    public RepackException() {
        super();
    }

    public RepackException(String message) {
        super(message);
    }
}
