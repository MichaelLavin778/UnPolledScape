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
            CharacterReplacements.restoreCharacterCreationText("Select skin colour, body type and pronouns")
        );
        assertEquals(
            "<col=ffffff>Select skin colour and gender</col>",
            CharacterReplacements.restoreCharacterCreationText("<col=ffffff>Select skin colour, body type and pronouns</col>")
        );
        assertEquals("Gender", CharacterReplacements.restoreCharacterCreationText("Body type"));
        assertEquals("Gender", CharacterReplacements.restoreCharacterCreationText("Body types"));
        assertEquals("Choose Gender", CharacterReplacements.restoreCharacterCreationText("Choose Body type"));
    }

    @Test
    public void matchesShaveMenuOption()
    {
        assertEquals(true, CharacterReplacements.isShaveMenuOption("Shave"));
        assertEquals(true, CharacterReplacements.isShaveMenuOption("<col=ff9040>Shave</col>"));
        assertEquals(false, CharacterReplacements.isShaveMenuOption("Talk-to"));
        assertEquals(false, CharacterReplacements.isShaveMenuOption(null));
    }

    @Test
    public void matchesFacialHairCategoryLabel()
    {
        assertEquals(true, CharacterReplacements.isFacialHairCategoryLabel("Facial hair"));
        assertEquals(true, CharacterReplacements.isFacialHairCategoryLabel("<col=ff9040>Facial hair</col>"));
        assertEquals(false, CharacterReplacements.isFacialHairCategoryLabel("Hair"));
        assertEquals(false, CharacterReplacements.isFacialHairCategoryLabel(null));
    }
}
