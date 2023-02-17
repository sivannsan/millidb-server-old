package com.sivannsan.millidb;

import com.sivannsan.foundation.annotation.Nonnull;

//Permission, later
public class MilliDBUsers {
    public static boolean hasUser(@Nonnull String userName, @Nonnull String userPassword) {
        return true;
    }

    public static boolean hasPermission(@Nonnull String userName, @Nonnull String path) {
        return true;
    }
}
