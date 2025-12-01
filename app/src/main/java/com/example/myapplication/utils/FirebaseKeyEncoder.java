package com.example.myapplication.utils;

import android.util.Log;

/**
 * Utility class for encoding and decoding strings to be used as Firebase Database keys.
 * Firebase Database paths cannot contain '.', '#', '$', '[', or ']' characters.
 * This class provides methods to safely encode/decode these characters.
 */
public class FirebaseKeyEncoder {
    private static final String TAG = "FirebaseKeyEncoder";

    /**
     * Encodes a string to be safe for use as a Firebase Database key.
     * Replaces invalid characters with URL-encoded equivalents.
     *
     * @param key The original key string
     * @return Encoded string safe for Firebase paths
     */
    public static String encode(String key) {
        if (key == null) {
            return null;
        }
        // Replace invalid Firebase characters with URL-encoded equivalents
        return key
                .replace(".", "%2E")
                .replace("#", "%23")
                .replace("$", "%24")
                .replace("[", "%5B")
                .replace("]", "%5D");
    }

    /**
     * Decodes a Firebase Database key back to its original form.
     *
     * @param encodedKey The encoded key string
     * @return Decoded string
     */
    public static String decode(String encodedKey) {
        if (encodedKey == null) {
            return null;
        }
        // Reverse the encoding
        return encodedKey
                .replace("%2E", ".")
                .replace("%23", "#")
                .replace("%24", "$")
                .replace("%5B", "[")
                .replace("%5D", "]");
    }

    /**
     * Checks if a string contains characters that need encoding for Firebase.
     *
     * @param key The string to check
     * @return true if encoding is needed, false otherwise
     */
    public static boolean needsEncoding(String key) {
        if (key == null) {
            return false;
        }
        return key.contains(".") || key.contains("#") || key.contains("$") 
                || key.contains("[") || key.contains("]");
    }
}

