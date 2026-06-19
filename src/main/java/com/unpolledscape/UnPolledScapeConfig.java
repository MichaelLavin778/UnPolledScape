package com.unpolledscape;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unpolledscape")
public interface UnPolledScapeConfig extends Config
{
    @ConfigItem(
        keyName = "npcs",
        name = "NPCs",
        description = "Enable legacy NPC names, dialogue, and interaction text."
    )
    default boolean npcs()
    {
        return false;
    }
}
