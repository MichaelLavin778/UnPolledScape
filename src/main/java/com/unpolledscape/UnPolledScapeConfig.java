package com.unpolledscape;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unpolledscape")
public interface UnPolledScapeConfig extends Config
{
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
        keyName = "character",
        name = "Character",
        description = "Enable legacy character creation and Make-over Mage gender screens."
    )
    default boolean character()
    {
        return true;
    }
}
