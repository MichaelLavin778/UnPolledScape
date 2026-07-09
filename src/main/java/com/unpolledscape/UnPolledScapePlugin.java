package com.unpolledscape;

import com.google.inject.Provides;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Renderable;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PlayerChanged;
// import net.runelite.api.events.PostItemComposition;
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
// import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(name = "UnPolledScape")
public class UnPolledScapePlugin extends Plugin {
    // ITEMS TO REPLACE
    private static final int[] FLOWER_CROWN_IDS = {
        27035, 27141, 27143, 27145, 27147, 27149, 27151, 27153, 27155
    };
    private static final int[] RAINBOW_SCARF_IDS = {
        21314, 28108, 28109, 28110, 28111, 28112, 28113, 28114, 28115
    };
    private static final int[] RAINBOW_JUMPER_IDS = {
        28116, 28118, 28119, 28120, 28121, 28122, 28123, 28124, 28125,
    };
    private static final int[] RAINBOW_CAPE_IDS = {
        29489, 29491, 29493, 29495, 29497, 29499, 29501, 29503, 29505
    };
    private static final int[] RAINBOW_CROWN_SHIRT_IDS = {
        29507, 29509, 29510, 29511, 29512, 29513, 29514, 29515, 29516
    };
    private static final int LOVE_CROSSBOW_ID = 28128;
    private static final int POETS_JACKET_ID = 28126;

    // ITEMS TO REPLACE WITH
    private static final int HELM_OF_RAEDWALD_ID = 19687;
    private static final int GNOME_SCARF_ID = 9470;
    private static final int CLUE_HUNTER_GARB_ID = 19689;
    private static final int CLUE_HUNTER_CLOAK_ID = 19697;
    private static final int DORGESHUUN_CROSSBOW_ID = 8880;

    private static final Map<Integer, Integer> REPLACEMENTS = createReplacementMap();

    Map<Integer, Integer> replacementMap(Client client)
    {
        return REPLACEMENTS;
    }

    boolean isReplacementSourceItem(int itemId)
    {
        return REPLACEMENTS.containsKey(itemId);
    }

    /**
     * The legacy item a modern item is reskinned as, or {@code null} if the item is not replaced.
     * Used by {@link ItemAppearanceOverlay} to paint the correctly-scaled slot icon.
     */
    static Integer getReplacementItemId(int itemId)
    {
        return REPLACEMENTS.get(itemId);
    }

    private static Map<Integer, Integer> createReplacementMap()
    {
        Map<Integer, Integer> replacements = new LinkedHashMap<>();

        for (int flowerCrownId : FLOWER_CROWN_IDS)
        {
            replacements.put(flowerCrownId, HELM_OF_RAEDWALD_ID);
        }
        for (int rainbowScarfId : RAINBOW_SCARF_IDS)
        {
            replacements.put(rainbowScarfId, GNOME_SCARF_ID);
        }
        for (int rainbowJumperId : RAINBOW_JUMPER_IDS)
        {
            replacements.put(rainbowJumperId, CLUE_HUNTER_GARB_ID);
        }
        for (int rainbowCapeId : RAINBOW_CAPE_IDS)
        {
            replacements.put(rainbowCapeId, CLUE_HUNTER_CLOAK_ID);
        }
        for (int rainbowCrownShirtId : RAINBOW_CROWN_SHIRT_IDS)
        {
            replacements.put(rainbowCrownShirtId, CLUE_HUNTER_GARB_ID);
        }
        replacements.put(LOVE_CROSSBOW_ID, DORGESHUUN_CROSSBOW_ID);
        replacements.put(POETS_JACKET_ID, CLUE_HUNTER_GARB_ID);

        return replacements;
    }

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

    // @Inject
    // private OverlayManager overlayManager;

    // @Inject
    // private ItemAppearanceOverlay itemAppearanceOverlay;

    private final MakeoverReplacements makeoverReplacements = new MakeoverReplacements();
    // private final ItemAppearanceReplacements itemAppearanceReplacements = new ItemAppearanceReplacements();
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
        // overlayManager.add(itemAppearanceOverlay);
        clientThread.invoke(this::checklist);
        log.debug("UnPolledScape started");
    }

    @Override
    protected void shutDown() {
        renderCallbackManager.unregister(renderCallback);
        // overlayManager.remove(itemAppearanceOverlay);
        clientThread.invoke(() -> {
            makeoverReplacements.restore(client);
            // itemAppearanceReplacements.restore(client);
            playerAppearanceReplacements.restore(client, REPLACEMENTS);
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

        if (config.items()
            && (isHiddenReplacedItemMenuEntry(event.getMenuEntry())
                || isHiddenEnamourMenuEntry(event.getMenuEntry()))) {
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
                if (isHiddenNpcMenuEntry(menuEntry)
                    || isHiddenReplacedItemMenuEntry(menuEntry)
                    || (config.items() && isHiddenEnamourMenuEntry(menuEntry))) {
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

    // @Subscribe
    // public void onPostItemComposition(PostItemComposition event)
    // {
    //     if (config.items())
    //     {
    //         itemAppearanceReplacements.applyTo(client, event.getItemComposition());
    //     }
    // }

    @Subscribe
    public void onPlayerChanged(PlayerChanged event)
    {
        if (!config.players())
        {
            return;
        }

        playerAppearanceReplacements.apply(client, REPLACEMENTS);
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        // The makeover/hairdresser interface builds its options dynamically and repopulates on body
        // type / category changes, so re-apply while it is open (config-change alone isn't enough).
        if (config.makeover())
        {
            makeoverReplacements.apply(client);
        }
    }

    private void checklist() {
        if (config.makeover()) {
            makeoverReplacements.apply(client);
        } else {
            makeoverReplacements.restore(client);
        }

        // if (config.items()) {
        //     itemAppearanceReplacements.apply(client);
        // } else {
        //     itemAppearanceReplacements.restore(client);
        // }

        if (config.players()) {
            playerAppearanceReplacements.apply(client, REPLACEMENTS);
        }
        else {
            playerAppearanceReplacements.restore(client, REPLACEMENTS);
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
            && isReplacementSourceItem(menuEntry.getItemId());
    }

    private boolean isHiddenEnamourMenuEntry(MenuEntry menuEntry)
    {
        if (menuEntry == null)
        {
            return false;
        }

        String option = menuEntry.getOption();
        if (option == null)
        {
            return false;
        }

        return "Enamour".equalsIgnoreCase(TAG_PATTERN.matcher(option).replaceAll("").trim());
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
