package su.starlight.da;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProfanityFilter {

    private final Set<String> bannedWords;
    private final Map<String, String[]> leetSpeakMap;
    private final Set<String> russianHomoglyphs;
    private final String replacementChar;

    public ProfanityFilter(Set<String> bannedWords, String replacementChar) {
        this.bannedWords = bannedWords;
        this.leetSpeakMap = new HashMap<>();
        this.russianHomoglyphs = Set.of("а", "е", "о", "с", "у", "р", "х", "к", "м", "т", "в", "н", "г");
        this.replacementChar = replacementChar;

        addLeetSpeak("4", "a", "а");
        addLeetSpeak("1", "i", "и");
        addLeetSpeak("3", "e", "е");
        addLeetSpeak("0", "o", "о");
        addLeetSpeak("@", "a", "а");
        addLeetSpeak("8", "b");
        addLeetSpeak("!", "i");
        addLeetSpeak("5", "s");
        addLeetSpeak("7", "t");
        addLeetSpeak("2", "z");
    }

    private void addLeetSpeak(String key, String... values) {
        leetSpeakMap.put(key, values);
    }

    public String filterText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String filteredText = text;

        filteredText = normalizeText(filteredText);
        filteredText = filterDirectProfanity(filteredText);
        filteredText = filterLeetSpeakProfanity(filteredText);
        filteredText = filterRussianHomoglyphProfanity(filteredText);
        filteredText = filterWithSeparators(filteredText);

        return filteredText;
    }

    private String normalizeText(String text) {
        return text.toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String filterDirectProfanity(String text) {
        String filtered = text;
        for (String bannedWord : bannedWords) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(bannedWord) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(filtered);
            filtered = matcher.replaceAll(generateReplacement(bannedWord.length()));
        }
        return filtered;
    }

    private String filterLeetSpeakProfanity(String text) {
        String normalizedText = text.toLowerCase();

        for (String bannedWord : bannedWords) {
            String leetPattern = createLeetSpeakPattern(bannedWord);
            Pattern pattern = Pattern.compile(leetPattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(normalizedText);

            if (matcher.find()) {
                normalizedText = matcher.replaceAll(generateReplacement(bannedWord.length()));
            }
        }

        return normalizedText;
    }

    private String createLeetSpeakPattern(String word) {
        StringBuilder pattern = new StringBuilder();

        for (char c : word.toCharArray()) {
            String charStr = String.valueOf(c);
            pattern.append("(?:");

            pattern.append(Pattern.quote(charStr));

            for (Map.Entry<String, String[]> entry : leetSpeakMap.entrySet()) {
                for (String value : entry.getValue()) {
                    if (value.equals(charStr.toLowerCase())) {
                        pattern.append("|").append(Pattern.quote(entry.getKey()));
                    }
                }
            }

            pattern.append(")");
        }

        return pattern.toString();
    }

    private String filterRussianHomoglyphProfanity(String text) {
        String filtered = text;

        for (String bannedWord : bannedWords) {
            String mixedPattern = createMixedScriptPattern(bannedWord);
            Pattern pattern = Pattern.compile(mixedPattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher matcher = pattern.matcher(filtered);

            if (matcher.find()) {
                filtered = matcher.replaceAll(generateReplacement(bannedWord.length()));
            }
        }

        return filtered;
    }

    private String createMixedScriptPattern(String word) {
        StringBuilder pattern = new StringBuilder();

        for (char c : word.toCharArray()) {
            String charStr = String.valueOf(c);
            pattern.append("(?:");

            pattern.append(Pattern.quote(charStr));

            if (russianHomoglyphs.contains(charStr.toLowerCase())) pattern.append("|").append(getLatinEquivalent(charStr));
            else if (isLatinCharacter(charStr)) pattern.append("|").append(getRussianHomoglyph(charStr));

            pattern.append(")");
        }

        return pattern.toString();
    }

    private String getLatinEquivalent(String cyrillicChar) {
        Map<String, String> equivalents = new HashMap<>();
        equivalents.put("а", "a");
        equivalents.put("е", "e");
        equivalents.put("о", "o");
        equivalents.put("с", "c");
        equivalents.put("у", "y");
        equivalents.put("р", "p");
        equivalents.put("х", "x");
        equivalents.put("к", "k");
        equivalents.put("м", "m");
        equivalents.put("т", "t");
        equivalents.put("в", "b");
        equivalents.put("н", "h");
        equivalents.put("г", "r");

        return equivalents.getOrDefault(cyrillicChar.toLowerCase(), cyrillicChar);
    }

    private String getRussianHomoglyph(String latinChar) {
        Map<String, String> homoglyphs = new HashMap<>();
        homoglyphs.put("a", "а");
        homoglyphs.put("e", "е");
        homoglyphs.put("o", "о");
        homoglyphs.put("c", "с");
        homoglyphs.put("y", "у");
        homoglyphs.put("p", "р");
        homoglyphs.put("x", "х");
        homoglyphs.put("k", "к");
        homoglyphs.put("m", "м");
        homoglyphs.put("t", "т");
        homoglyphs.put("b", "в");
        homoglyphs.put("h", "н");
        homoglyphs.put("r", "г");

        return homoglyphs.getOrDefault(latinChar.toLowerCase(), latinChar);
    }

    private boolean isLatinCharacter(String str) {
        return str.matches("[a-zA-Z]");
    }

    private String filterWithSeparators(String text) {
        String filtered = text;

        for (String bannedWord : bannedWords) {
            String patternStr = createSeparatorPattern(bannedWord);
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(filtered);

            if (matcher.find()) {
                filtered = matcher.replaceAll(generateReplacement(bannedWord.length()));
            }
        }

        return filtered;
    }

    private String createSeparatorPattern(String word) {
        if (word.length() < 2) {
            return Pattern.quote(word);
        }

        StringBuilder pattern = new StringBuilder();
        pattern.append("(?:");

        for (int i = 0; i < word.length(); i++) {
            if (i > 0) pattern.append("[\\s\\*\\-\\_\\.\\,]*");
            pattern.append(Pattern.quote(String.valueOf(word.charAt(i))));
        }

        pattern.append(")");
        return pattern.toString();
    }

    private String generateReplacement(int length) {
        return replacementChar.repeat(Math.max(1, length));
    }

}