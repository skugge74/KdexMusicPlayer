package com.example.kdexmusicplayer;

import androidx.room.TypeConverter;
import android.text.TextUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Converters {
    @TypeConverter
    public static String fromSet(Set<String> set) {
        return set == null ? "" : TextUtils.join(",", set);
    }

    @TypeConverter
    public static Set<String> toSet(String value) {
        if (TextUtils.isEmpty(value)) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(value.split(",")));
    }
}