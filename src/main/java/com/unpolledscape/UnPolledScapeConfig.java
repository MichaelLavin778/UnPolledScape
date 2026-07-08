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
        keyName = "items",
        name = "Items",
        description = "Enable visual item replacement changes outside player character models."
    )
    default boolean items()
    {
        return true;
    }
}
