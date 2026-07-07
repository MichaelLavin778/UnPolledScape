package com.unpolledscape;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class UnPolledScapePluginTest
{
    @SuppressWarnings({"unchecked", "varargs"})
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(UnPolledScapePlugin.class);
        RuneLite.main(args);
    }
}
