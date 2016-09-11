package com.shazam.tocker;

import java.util.function.Supplier;

public class UpChecks {
    public static Supplier<Boolean> exceptionIsDown(Runnable upCheck) {
        return () -> {
            try {
                upCheck.run();
                return true;
            } catch (Exception e) {
                return false;
            }
        };
    }
}
