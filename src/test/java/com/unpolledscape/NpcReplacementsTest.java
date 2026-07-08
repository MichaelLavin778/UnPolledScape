package com.unpolledscape;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NpcReplacementsTest
{
    @Test
    public void restoresCurrentNpcDialogueSnippets()
    {
        assertEquals(
            "King Roald calls the player mentally deficient.",
            NpcReplacements.restoreNpcText("King Roald calls the player a complete imbecile.")
        );
        assertEquals(
            "You do know those clothes make you look fat!",
            NpcReplacements.restoreNpcText("You've got an outrageous accent, you silly thing!")
        );
        assertEquals(
            "You slip your weapon through a chink in the creature's chitin, landing a vicious blow.",
            NpcReplacements.restoreNpcText("You slip your weapon through a gap in the creature's chitin, landing a vicious blow.")
        );
    }

    @Test
    public void restoresCurrentNpcNames()
    {
        assertEquals("Jungle savage", NpcReplacements.restoreNpcText("Tormented Warrior"));
        assertEquals("Ali the Barman", NpcReplacements.restoreNpcText("Faisal the Barman"));
        assertEquals("Gypsy Aris", NpcReplacements.restoreNpcText("Aris"));
        assertEquals("Enoch", NpcReplacements.restoreNpcText("Okina"));
        assertEquals("Zambo", NpcReplacements.restoreNpcText("Zembo"));
        assertEquals("Ethereal Man", NpcReplacements.restoreNpcText("Ethereal Being"));
        assertEquals("Warrior women", NpcReplacements.restoreNpcText("Warriors"));
        assertEquals("Warrior woman", NpcReplacements.restoreNpcText("Warrior"));
    }

    @Test
    public void restoresFrogRoyaltyMenuOption()
    {
        assertEquals(
            "Kiss",
            NpcReplacements.restoreNpcMenuOption("Pat", "<col=ffff00>Frog prince<col=ffffff>")
        );
    }

    @Test
    public void leavesUnmatchedTextAlone()
    {
        assertEquals("Unchanged dialogue.", NpcReplacements.restoreNpcText("Unchanged dialogue."));
        assertEquals("Talk-to", NpcReplacements.restoreNpcMenuOption("Talk-to", "Guide"));
    }

    @Test
    public void doesNotRepeatedlyApplyNestedNameReplacements()
    {
        assertEquals("Gypsy Aris", NpcReplacements.restoreNpcText("Gypsy Aris"));
        assertEquals("Warrior women", NpcReplacements.restoreNpcText("Warrior women"));
        assertEquals("Warrior woman", NpcReplacements.restoreNpcText("Warrior woman"));
    }

    @Test
    public void restoresCharacterCreationPrompt()
    {
        assertEquals(
            "Select skin colour and gender",
            MakeoverReplacements.restoreCharacterCreationText("Select skin colour, body type and pronouns")
        );
        assertEquals(
            "<col=ffffff>Select skin colour and gender</col>",
            MakeoverReplacements.restoreCharacterCreationText("<col=ffffff>Select skin colour, body type and pronouns</col>")
        );
        assertEquals("Gender", MakeoverReplacements.restoreCharacterCreationText("Body type"));
        assertEquals("Gender", MakeoverReplacements.restoreCharacterCreationText("Body types"));
        assertEquals("Choose Gender", MakeoverReplacements.restoreCharacterCreationText("Choose Body type"));
    }

    @Test
    public void matchesShaveMenuOption()
    {
        assertEquals(true, MakeoverReplacements.isShaveMenuOption("Shave"));
        assertEquals(true, MakeoverReplacements.isShaveMenuOption("<col=ff9040>Shave</col>"));
        assertEquals(false, MakeoverReplacements.isShaveMenuOption("Talk-to"));
        assertEquals(false, MakeoverReplacements.isShaveMenuOption(null));
    }

    @Test
    public void matchesFacialHairCategoryLabel()
    {
        assertEquals(true, MakeoverReplacements.isFacialHairCategoryLabel("Facial hair"));
        assertEquals(true, MakeoverReplacements.isFacialHairCategoryLabel("<col=ff9040>Facial hair</col>"));
        assertEquals(false, MakeoverReplacements.isFacialHairCategoryLabel("Hair"));
        assertEquals(false, MakeoverReplacements.isFacialHairCategoryLabel(null));
    }
}
