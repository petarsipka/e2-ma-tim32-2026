package com.example.slagalica.util;

public class TextNormalizer {

    public static String normalize(String input) {
        if (input == null) return "";

        StringBuilder sb = new StringBuilder();
        String lower = input.toLowerCase().trim();

        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            switch (c) {
                case 'đ':
                    sb.append("dj");
                    break;
                case 'š':
                    sb.append("s");
                    break;
                case 'č':
                case 'ć':
                    sb.append("c");
                    break;
                case 'ž':
                    sb.append("z");
                    break;
                case 'd':
                    if (i + 1 < lower.length() && lower.charAt(i + 1) == 'j') {
                        sb.append("dj");
                        i++;
                    } else {
                        sb.append("d");
                    }
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    public static boolean matches(String input, String correctAnswer) {
        return normalize(input).equals(normalize(correctAnswer));
    }
}