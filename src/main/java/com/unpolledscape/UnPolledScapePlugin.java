package com.unpolledscape;

import com.google.inject.Provides;
import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Renderable;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PlayerChanged;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(name = "UnPolledScape")
public class UnPolledScapePlugin extends Plugin {
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");
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

    @Inject
    private RenderCallbackManager renderCallbackManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ItemAppearanceOverlay itemAppearanceOverlay;

    private final MakeoverReplacements makeoverReplacements = new MakeoverReplacements();
    private final ItemAppearanceReplacements itemAppearanceReplacements = new ItemAppearanceReplacements();
    private final PlayerAppearanceReplacements playerAppearanceReplacements = new PlayerAppearanceReplacements();
    private final NpcAppearanceReplacements npcAppearanceReplacements = new NpcAppearanceReplacements();
    private final NpcDialogueReplacement npcDialogueReplacement = new NpcDialogueReplacement();
    private final RenderCallback renderCallback = new RenderCallback()
    {
        @Override
        public boolean addEntity(Renderable renderable, boolean drawingUi)
        {
            return shouldDraw(renderable, drawingUi);
        }
    };

    @Override
    protected void startUp() {
        renderCallbackManager.register(renderCallback);
        overlayManager.add(itemAppearanceOverlay);
        clientThread.invoke(this::checklist);
        log.debug("UnPolledScape started");
    }

    @Override
    protected void shutDown() {
        renderCallbackManager.unregister(renderCallback);
        overlayManager.remove(itemAppearanceOverlay);
        clientThread.invoke(() -> {
            makeoverReplacements.restore(client);
            playerAppearanceReplacements.restore(client, itemAppearanceReplacements.replacementMap(client));
            npcAppearanceReplacements.restoreLadyKeli(client);
            log.debug("UnPolledScape stopped");
        });
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (config.makeover()) {
            makeoverReplacements.handleMenuOptionClicked(client, event);
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!"unpolledscape".equals(event.getGroup()))
        {
            return;
        }

        clientThread.invoke(this::checklist);
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (config.makeover()) {
            makeoverReplacements.handleMenuEntryAdded(client, event);
        }

        if (config.items() && isHiddenReplacedItemMenuEntry(event.getMenuEntry())) {
            client.getMenu().removeMenuEntry(event.getMenuEntry());
            return;
        }

        if (config.npcs()) {
            if (isHiddenNpcMenuEntry(event.getMenuEntry())) {
                client.getMenu().removeMenuEntry(event.getMenuEntry());
                return;
            }

            replaceMenuEntryText(event.getMenuEntry());
        }
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        if (config.npcs() || config.items()) {
            MenuEntry[] menuEntries = event.getMenuEntries();
            int kept = 0;
            for (MenuEntry menuEntry : menuEntries) {
                if (isHiddenNpcMenuEntry(menuEntry) || isHiddenReplacedItemMenuEntry(menuEntry)) {
                    continue;
                }

                if (config.npcs()) {
                    replaceMenuEntryText(menuEntry);
                }
                menuEntries[kept++] = menuEntry;
            }

            if (kept != menuEntries.length) {
                MenuEntry[] filtered = new MenuEntry[kept];
                System.arraycopy(menuEntries, 0, filtered, 0, kept);
                event.setMenuEntries(filtered);
            }
        }
    }

    @Subscribe
    public void onPlayerChanged(PlayerChanged event)
    {
        if (!config.players())
        {
            return;
        }

        playerAppearanceReplacements.apply(client, itemAppearanceReplacements.replacementMap(client));
    }

    private void checklist() {
        if (config.makeover()) {
            makeoverReplacements.apply(client);
        } else {
            makeoverReplacements.restore(client);
        }

        Map<Integer, Integer> replacementMap = itemAppearanceReplacements.replacementMap(client);
        // Item icon replacement is handled by ItemAppearanceOverlay, which gates itself on
        // config.items() and requires no apply/restore since it never mutates client state.

        if (config.players()) {
            playerAppearanceReplacements.apply(client, replacementMap);
        }
        else {
            playerAppearanceReplacements.restore(client, replacementMap);
        }

        if (config.npcs()) {
            npcDialogueReplacement.replaceDialogueWidgets(client, DIALOGUE_WIDGETS);
            npcAppearanceReplacements.applyLadyKeli(client);
        } else {
            npcAppearanceReplacements.restoreLadyKeli(client);
        }
    }

    private boolean isHiddenNpcMenuEntry(MenuEntry menuEntry) {
        return menuEntry != null
            && isNpcMenuAction(menuEntry.getType())
            && isHiddenNpcTarget(menuEntry.getTarget());
    }

    private boolean isHiddenReplacedItemMenuEntry(MenuEntry menuEntry)
    {
        return menuEntry != null
            && menuEntry.isItemOp()
            && "Change-style".equalsIgnoreCase(menuEntry.getOption())
            && itemAppearanceReplacements.isReplacementSourceItem(menuEntry.getItemId());
    }

    private boolean isHiddenNpcTarget(String target) {
        if (target == null || target.isEmpty()) {
            return false;
        }

        String normalized = TAG_PATTERN.matcher(target).replaceAll(" ").toLowerCase();
        return normalized.contains("gilbert") || normalized.contains("sir kit breaker");
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

    private boolean shouldDraw(Renderable renderable, boolean drawingUi)
    {
        if (!config.npcs())
        {
            return true;
        }

        if (renderable instanceof NPC)
        {
            int npcId = ((NPC) renderable).getId();
            return npcId != NpcID.PRIDE22_GILBERT && npcId != NpcID.PRIDE24_KIT_GLADE;
        }

        return true;
    }

    @Provides
    UnPolledScapeConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(UnPolledScapeConfig.class);
    }
}
