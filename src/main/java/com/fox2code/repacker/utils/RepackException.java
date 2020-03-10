package com.fox2code.repacker.utils;

import java.io.IOException;

public class RepackException extends IOException {
    public RepackException() {
        super();
    }

    public RepackException(String message) {
        super(ConsoleColors.RED_BOLD + message);
    }

    public RepackException(String message, Throwable cause) {
        super(ConsoleColors.RED_BOLD + message, cause);
    }
}
