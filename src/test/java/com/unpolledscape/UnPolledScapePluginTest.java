package com.unpolledscape;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class UnPolledScapePluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(UnPolledScapePlugin.class);
        RuneLite.main(args);
    }
}
