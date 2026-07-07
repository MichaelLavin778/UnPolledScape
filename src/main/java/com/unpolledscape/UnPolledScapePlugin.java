package com.unpolledscape;

import com.google.inject.Provides;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PostClientTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(name = "UnPolledScape")
public class UnPolledScapePlugin extends Plugin {
    private static final int[] DIALOGUE_WIDGETS = {
            InterfaceID.ChatLeft.NAME,
            InterfaceID.ChatLeft.TEXT,
            InterfaceID.ChatRight.UNIVERSE,
            InterfaceID.ChatRight.TEXT,
            InterfaceID.Chatmenu.UNIVERSE,
            InterfaceID.Chatmenu.OPTIONS,
            InterfaceID.Objectbox.TEXT,
            InterfaceID.ObjectboxDouble.TEXT
    };

    @Inject
    private Client client;

    @Inject
    private UnPolledScapeConfig config;

    @Inject
    private ClientThread clientThread;

    private final CharacterReplacements characterReplacements = new CharacterReplacements();
    private final NpcAppearanceReplacements npcAppearanceReplacements = new NpcAppearanceReplacements();
    private boolean warnedLadyKeliAppearance;

    @Override
    protected void startUp() {
        log.debug("UnPolledScape started");
    }

    @Override
    protected void shutDown() {
        clientThread.invoke(() -> {
            characterReplacements.restore(client);
            npcAppearanceReplacements.restoreLadyKeli(client);
            log.debug("UnPolledScape stopped");
        });
    }

    @Subscribe
    public void onPostClientTick(PostClientTick event) {
        if (config.character()) {
            characterReplacements.apply(client);
        } else {
            characterReplacements.restore(client);
        }

        if (!config.npcs()) {
            npcAppearanceReplacements.restoreLadyKeli(client);
            return;
        }

        if (!npcAppearanceReplacements.applyLadyKeli(client) && !warnedLadyKeliAppearance) {
            warnedLadyKeliAppearance = true;
            log.warn("Unable to restore Lady Keli's legacy appearance");
        }

        Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int widgetId : DIALOGUE_WIDGETS) {
            replaceWidgetText(client.getWidget(widgetId), visited);
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (config.character()) {
            characterReplacements.handleMenuOptionClicked(client, event);
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (config.character()) {
            characterReplacements.handleMenuEntryAdded(client, event);
        }

        if (config.npcs()) {
            replaceMenuEntryText(event.getMenuEntry());
        }
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        if (!config.npcs()) {
            for (MenuEntry menuEntry : event.getMenuEntries()) {
                replaceMenuEntryText(menuEntry);
            }
        }
    }

    private void replaceWidgetText(Widget widget, Set<Widget> visited) {
        if (widget == null || !visited.add(widget)) {
            return;
        }

        String text = widget.getText();
        if (text != null && !text.isEmpty()) {
            String replacement = NpcReplacements.restoreNpcText(text);
            if (!replacement.equals(text)) {
                widget.setText(replacement);
            }
        }

        replaceWidgetText(widget.getStaticChildren(), visited);
        replaceWidgetText(widget.getDynamicChildren(), visited);
        replaceWidgetText(widget.getNestedChildren(), visited);
    }

    private void replaceWidgetText(Widget[] widgets, Set<Widget> visited) {
        if (widgets == null) {
            return;
        }

        for (Widget widget : widgets) {
            replaceWidgetText(widget, visited);
        }
    }

    private void replaceMenuEntryText(MenuEntry menuEntry) {
        if (menuEntry == null || !isNpcMenuAction(menuEntry.getType())) {
            return;
        }

        String target = menuEntry.getTarget();
        String replacementTarget = NpcReplacements.restoreNpcText(target);
        if (replacementTarget != null && !replacementTarget.equals(target)) {
            menuEntry.setTarget(replacementTarget);
            target = replacementTarget;
        }

        String option = menuEntry.getOption();
        String replacementOption = NpcReplacements.restoreNpcMenuOption(option, target);
        if (replacementOption != null && !replacementOption.equals(option)) {
            menuEntry.setOption(replacementOption);
        }
    }

    private boolean isNpcMenuAction(MenuAction action) {
        if (action == null) {
            return false;
        }

        switch (action) {
            case WIDGET_TARGET_ON_NPC:
            case NPC_FIRST_OPTION:
            case NPC_SECOND_OPTION:
            case NPC_THIRD_OPTION:
            case NPC_FOURTH_OPTION:
            case NPC_FIFTH_OPTION:
            case EXAMINE_NPC:
                return true;
            default:
                return false;
        }
    }

    @Provides
    UnPolledScapeConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(UnPolledScapeConfig.class);
    }
}
