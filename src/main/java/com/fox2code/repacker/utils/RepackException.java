package com.fox2code.repacker.utils;

import com.fox2code.repacker.Main;

import java.io.IOException;

public class RepackException extends IOException {
    public RepackException() {
        super();
    }

    public RepackException(String message) {
        super(Main.colors.RED_BOLD + message);
    }

    public RepackException(String message, Throwable cause) {
        super(Main.colors.RED_BOLD + message, cause);
    }
}
