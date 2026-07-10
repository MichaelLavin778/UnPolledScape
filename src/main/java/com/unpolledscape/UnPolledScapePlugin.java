package com.unpolledscape;

import com.google.inject.Provides;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.Client;
import net.runelite.api.IterableHashTable;
import net.runelite.api.NPC;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Node;
import net.runelite.api.Projectile;
import net.runelite.api.Renderable;
import net.runelite.api.events.AreaSoundEffectPlayed;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PlayerChanged;
import net.runelite.api.events.PostItemComposition;
import net.runelite.api.events.SoundEffectPlayed;
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

@Slf4j
@PluginDescriptor(
    name = "UnPolledScape",
    description = "Restores selected legacy NPC, dialogue, character, item, and game object elements that have been altered or removed in the due to unpolled game updates.",
    tags = {"unpolled", "integrity", "legacy", "modern", "poll", "vote", "pride", "diversity", "inclusion"}
)
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

    // The love crossbow's "Enamour" special fires a heart projectile (PRIDE23_ENAMOUR_ARROW).
    private static final int ENAMOUR_PROJECTILE_ID = 2366;

    // Heart spotanims shown on the firer (when fired) and the target (on impact) by the "Enamour"
    // special (PRIDE23_ENAMOUR_ARROW / PRIDE23_ENAMOUR_IMPACT).
    private static final int ENAMOUR_ARROW_SPOTANIM_ID = 2366;
    private static final int ENAMOUR_IMPACT_SPOTANIM_ID = 2367;

    // Sound effects played by the "Enamour" special (6998 = area sound, 6999 = sound effect).
    private static final int ENAMOUR_AREA_SOUND_ID = 6998;
    private static final int ENAMOUR_SOUND_ID = 6999;

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
    // private ItemAppearanceOverlay itemAppearanceOverlay;

    private final MakeoverReplacements makeoverReplacements = new MakeoverReplacements();
    // private final ItemAppearanceReplacements itemAppearanceReplacements = new ItemAppearanceReplacements();
    private final Experimental experimental = new Experimental();
    private final GameObjectReplacements gameObjectReplacements = new GameObjectReplacements();
    private final ItemNameReplacements itemNameReplacements = new ItemNameReplacements();
    private final PlayerAppearanceReplacements playerAppearanceReplacements = new PlayerAppearanceReplacements();
    // private final NpcAppearanceReplacements npcAppearanceReplacements = new NpcAppearanceReplacements();
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
            experimental.restore(client);
            gameObjectReplacements.restore(client);
            itemNameReplacements.restore(client);
            playerAppearanceReplacements.restore(client, REPLACEMENTS);
            // npcAppearanceReplacements.restoreLadyKeli(client);
            log.debug("UnPolledScape stopped");
        });
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (config.experimental()) {
            experimental.handleMenuOptionClicked(client, event);
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

        if (config.npcs() && isHiddenFrogPatMenuEntry(event.getMenuEntry())) {
            client.getMenu().removeMenuEntry(event.getMenuEntry());
            return;
        }

        if (config.experimental()
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
        if (config.npcs() || config.experimental()) {
            MenuEntry[] menuEntries = event.getMenuEntries();
            int kept = 0;
            for (MenuEntry menuEntry : menuEntries) {
                if (isHiddenNpcMenuEntry(menuEntry)
                    || isHiddenFrogPatMenuEntry(menuEntry)
                    || isHiddenReplacedItemMenuEntry(menuEntry)
                    || (config.experimental() && isHiddenEnamourMenuEntry(menuEntry))) {
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
    public void onPostItemComposition(PostItemComposition event)
    {
        if (config.experimental())
        {
            itemNameReplacements.applyTo(event.getItemComposition());
        }
    }

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
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        if (config.gameObjects())
        {
            gameObjectReplacements.handleGameObjectSpawned(client, event);
        }
    }

    @Subscribe
    public void onSoundEffectPlayed(SoundEffectPlayed event)
    {
        if (config.players() && isEnamourSound(event.getSoundId()))
        {
            event.consume();
        }
    }

    @Subscribe
    public void onAreaSoundEffectPlayed(AreaSoundEffectPlayed event)
    {
        if (config.players() && isEnamourSound(event.getSoundId()))
        {
            event.consume();
        }
    }

    private static boolean isEnamourSound(int soundId)
    {
        return soundId == ENAMOUR_AREA_SOUND_ID || soundId == ENAMOUR_SOUND_ID;
    }

    @Subscribe
    public void onGraphicChanged(GraphicChanged event)
    {
        if (!config.players())
        {
            return;
        }

        removeEnamourSpotAnims(event.getActor());
    }

    private void removeEnamourSpotAnims(Actor actor)
    {
        if (actor == null)
        {
            return;
        }

        IterableHashTable<ActorSpotAnim> spotAnims = actor.getSpotAnims();
        if (spotAnims == null)
        {
            return;
        }

        // Collect keys first; the spotanim table must not be modified while iterating it.
        List<Integer> keysToRemove = null;
        for (ActorSpotAnim spotAnim : spotAnims)
        {
            if (isEnamourSpotAnim(spotAnim.getId()))
            {
                if (keysToRemove == null)
                {
                    keysToRemove = new ArrayList<>();
                }
                keysToRemove.add((int) ((Node) spotAnim).getHash());
            }
        }

        if (keysToRemove != null)
        {
            for (int key : keysToRemove)
            {
                actor.removeSpotAnim(key);
            }
        }
    }

    private static boolean isEnamourSpotAnim(int spotAnimId)
    {
        return spotAnimId == ENAMOUR_ARROW_SPOTANIM_ID || spotAnimId == ENAMOUR_IMPACT_SPOTANIM_ID;
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

        if (config.experimental())
        {
            experimental.apply(client);
        }
    }

    private void checklist() {
        if (config.makeover()) {
            makeoverReplacements.apply(client);
        } else {
            makeoverReplacements.restore(client);
        }

        if (config.experimental()) {
            experimental.apply(client);
            itemNameReplacements.apply(client);
        } else {
            experimental.restore(client);
            itemNameReplacements.restore(client);
        }

        // if (config.items()) {
        //     itemAppearanceReplacements.apply(client);
        // } else {
        //     itemAppearanceReplacements.restore(client);
        // }

        if (config.players()) {
            playerAppearanceReplacements.apply(client, REPLACEMENTS);
        } else {
            playerAppearanceReplacements.restore(client, REPLACEMENTS);
        }

        if (config.gameObjects()) {
            gameObjectReplacements.apply(client);
        } else {
            gameObjectReplacements.restore(client);
        }

        if (config.npcs()) {
            npcDialogueReplacement.replaceDialogueWidgets(client, DIALOGUE_WIDGETS);
            // npcAppearanceReplacements.applyLadyKeli(client);
        } else {
            // npcAppearanceReplacements.restoreLadyKeli(client);
        }
    }

    private boolean isHiddenNpcMenuEntry(MenuEntry menuEntry) {
        return menuEntry != null
            && isNpcMenuAction(menuEntry.getType())
            && isHiddenNpcTarget(menuEntry.getTarget());
    }

    private boolean isHiddenFrogPatMenuEntry(MenuEntry menuEntry)
    {
        if (menuEntry == null)
        {
            return false;
        }

        String option = menuEntry.getOption();
        if (option == null || !"Pat".equals(option))
        {
            return false;
        }

        return isFrogRoyalty(menuEntry.getTarget());
    }

    private static boolean isFrogRoyalty(String target)
    {
        if (target == null)
        {
            return false;
        }

        String lowerTarget = target.toLowerCase();
        return lowerTarget.contains("frog prince") || lowerTarget.contains("frog princess");
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
        if (config.players()
            && renderable instanceof Projectile
            && ((Projectile) renderable).getId() == ENAMOUR_PROJECTILE_ID)
        {
            return false;
        }

        if (config.npcs() && renderable instanceof NPC)
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
