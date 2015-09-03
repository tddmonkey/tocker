package com.shazam.tocker;

import java.util.function.Supplier;

public class AliveStrategies {
    public static AliveStrategy retrying(Supplier<Boolean> upCheck, int timesToTry, int millisBetweenRetry) {
        return () -> {
            while (timesToTry > 0 && upCheck.get() != true) {
                try {
                    Thread.sleep(millisBetweenRetry);
                } catch (InterruptedException e) {
                    // oh really?!
                }
            }
        };
    }

    public static AliveStrategy alwaysAlive() {
        return () -> { };
    }
}
