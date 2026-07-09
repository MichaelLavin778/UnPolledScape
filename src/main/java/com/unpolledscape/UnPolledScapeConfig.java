package com.unpolledscape;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("unpolledscape")
public interface UnPolledScapeConfig extends Config
{
    @ConfigSection(
        position = 100,
        name = "Experimental",
        description = "Experimental, potentially unstable features.",
        closedByDefault = true
    )
    String experimentalSection = "experimentalSection";

    @ConfigItem(
        position = 0,
        keyName = "npcs",
        name = "NPCs",
        description = "Enable legacy NPC names, dialogue, and interaction text."
    )
    default boolean npcs()
    {
        return true;
    }

    @ConfigItem(
        position = 1,
        keyName = "makeover",
        name = "Makeover",
        description = "Enable legacy character creation and Make-over Mage gender screens."
    )
    default boolean makeover()
    {
        return true;
    }

    @ConfigItem(
        position = 2,
        keyName = "players",
        name = "Players",
        description = "Enable visual changes applied to other player characters."
    )
    default boolean players()
    {
        return true;
    }

    @ConfigItem(
        position = 3,
        keyName = "gameObjects",
        name = "Game Objects",
        description = "Hide legacy-replaced game objects, such as the rainbow flowers in the Pride flower field."
    )
    default boolean gameObjects()
    {
        return true;
    }

    @ConfigItem(
        position = 4,
        keyName = "experimental",
        name = "Experimental",
        description = "Enable experimental legacy hairstyle filtering and item replacement changes.",
        section = experimentalSection
    )
    default boolean experimental()
    {
        return false;
    }
}
