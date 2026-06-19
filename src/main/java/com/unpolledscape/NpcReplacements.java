package com.unpolledscape;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NpcReplacements
{
    private static final Replacement[] NPC_TEXT_REPLACEMENTS = {
        new Replacement("a complete imbecile", "mentally deficient"),
        new Replacement("You've got an outrageous accent, you silly thing!", "You do know those clothes make you look fat!"),
        new Replacement("You've got an outrageous accent, you silly thing", "You do know those clothes make you look fat"),
        new Replacement("through a gap in the creature's chitin", "through a chink in the creature's chitin"),
        Replacement.npcName("Tormented Warriors", "Jungle savages"),
        Replacement.npcName("Tormented Warrior", "Jungle savage"),
        Replacement.npcName("Faisal the Barman", "Ali the Barman"),
        Replacement.npcName("Aris", "Gypsy Aris"),
        Replacement.npcName("Okina", "Enoch"),
        Replacement.npcName("Zembo", "Zambo"),
        Replacement.npcName("Ethereal Being", "Ethereal Man"),
        Replacement.npcName("Warriors", "Warrior women"),
        Replacement.npcName("Warrior", "Warrior woman", "Warrior women")
    };

    private NpcReplacements()
    {
    }

    static String restoreNpcText(String text)
    {
        if (text == null)
        {
            return null;
        }

        String restored = text;
        for (Replacement replacement : NPC_TEXT_REPLACEMENTS)
        {
            restored = replacement.apply(restored);
        }
        return restored;
    }

    static String restoreNpcMenuOption(String option, String target)
    {
        if (option == null)
        {
            return null;
        }

        if ("Pat".equals(option) && target != null && isFrogRoyalty(target))
        {
            return "Kiss";
        }

        return option;
    }

    private static boolean isFrogRoyalty(String target)
    {
        String lowerTarget = target.toLowerCase();
        return lowerTarget.contains("frog prince") || lowerTarget.contains("frog princess");
    }

    private static final class Replacement
    {
        private final String currentText;
        private final String legacyText;
        private final Pattern pattern;
        private final String[] skipIfPresent;

        private Replacement(String currentText, String legacyText)
        {
            this(currentText, legacyText, null);
        }

        private Replacement(String currentText, String legacyText, Pattern pattern, String... skipIfPresent)
        {
            this.currentText = currentText;
            this.legacyText = legacyText;
            this.pattern = pattern;
            this.skipIfPresent = skipIfPresent;
        }

        private static Replacement npcName(String currentText, String legacyText, String... additionalSkipText)
        {
            String[] skipIfPresent = new String[additionalSkipText.length + 1];
            skipIfPresent[0] = legacyText;
            System.arraycopy(additionalSkipText, 0, skipIfPresent, 1, additionalSkipText.length);

            Pattern pattern = Pattern.compile("(?<![A-Za-z])" + Pattern.quote(currentText) + "(?![A-Za-z])");
            return new Replacement(currentText, legacyText, pattern, skipIfPresent);
        }

        private String apply(String text)
        {
            for (String skipText : skipIfPresent)
            {
                if (text.contains(skipText))
                {
                    return text;
                }
            }

            if (pattern != null)
            {
                return pattern.matcher(text).replaceAll(Matcher.quoteReplacement(legacyText));
            }

            return text.replace(currentText, legacyText);
        }
    }
}
