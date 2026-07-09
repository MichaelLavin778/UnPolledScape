package com.unpolledscape;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;

/**
 * Reskins modern cosmetic items so they render as the legacy item they replace, everywhere the game
 * draws an item from its composition: inventory, equipment, bank, ground and chat/quest dialogs.
 *
 * <p>The swap is a real image replacement (the original icon is gone, not painted over) done by
 * copying the legacy item's visual fields onto the modern item's {@link ItemComposition} and then
 * flushing the client's item caches so every surface re-renders.
 *
 * <p>Restoration is deliberately belt-and-braces so toggling the feature (or the plugin) always
 * reverts cleanly:
 * <ul>
 *   <li>every mutated composition is restored <em>in place</em> from a snapshot of its original
 *       visuals, which covers compositions still referenced by the rendering caches that a plain
 *       cache reset will not evict; and</li>
 *   <li>the composition, model and sprite caches are all reset so any evicted/reloaded copies come
 *       back original and every rendered icon/model is rebuilt.</li>
 * </ul>
 */
final class ItemAppearanceReplacements
{
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

    /** Original visuals of every composition we have mutated, keyed by the modern item id. */
    private final Map<Integer, ItemSnapshot> snapshots = new HashMap<>();

    Map<Integer, Integer> replacementMap(Client client)
    {
        return REPLACEMENTS;
    }

    boolean isReplacementSourceItem(int itemId)
    {
        return REPLACEMENTS.containsKey(itemId);
    }

    /**
     * Reskin every replaced item that is already loaded, then flush the item caches so the change is
     * visible on every surface. Items loaded/reloaded later are handled by
     * {@link #applyTo(Client, ItemComposition)} via the PostItemComposition event.
     */
    void apply(Client client)
    {
        for (int sourceId : REPLACEMENTS.keySet())
        {
            applyTo(client, client.getItemDefinition(sourceId));
        }
        invalidateItemCaches(client);
    }

    /**
     * Reskin a single composition (if it is a replacement source), snapshotting its original visuals
     * the first time it is touched so they can be restored later. Safe to call repeatedly.
     */
    void applyTo(Client client, ItemComposition composition)
    {
        if (composition == null)
        {
            return;
        }

        Integer replacementId = REPLACEMENTS.get(composition.getId());
        if (replacementId == null)
        {
            return;
        }

        ItemComposition replacement = client.getItemDefinition(replacementId);
        if (replacement == null)
        {
            return;
        }

        snapshots.computeIfAbsent(composition.getId(), id -> ItemSnapshot.of(composition));
        copyVisuals(replacement, composition);
    }

    /**
     * Restore every mutated item to its original appearance and flush the item caches. No-op when
     * nothing has been changed.
     */
    void restore(Client client)
    {
        if (snapshots.isEmpty())
        {
            return;
        }

        for (Map.Entry<Integer, ItemSnapshot> entry : snapshots.entrySet())
        {
            ItemComposition composition = client.getItemDefinition(entry.getKey());
            if (composition != null)
            {
                entry.getValue().restoreTo(composition);
            }
        }
        snapshots.clear();
        invalidateItemCaches(client);
    }

    /** Copy the visual fields that determine how an item's icon and model are drawn. */
    private static void copyVisuals(ItemComposition from, ItemComposition to)
    {
        to.setInventoryModel(from.getInventoryModel());
        to.setColorToReplace(from.getColorToReplace());
        to.setColorToReplaceWith(from.getColorToReplaceWith());
        to.setTextureToReplace(from.getTextureToReplace());
        to.setTextureToReplaceWith(from.getTextureToReplaceWith());
        to.setXan2d(from.getXan2d());
        to.setYan2d(from.getYan2d());
        to.setZan2d(from.getZan2d());
    }

    private static void invalidateItemCaches(Client client)
    {
        client.getItemCompositionCache().reset();
        client.getItemModelCache().reset();
        client.getItemSpriteCache().reset();
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

    /** Immutable copy of the visual fields {@link #copyVisuals} overwrites, used to restore them. */
    private static final class ItemSnapshot
    {
        private final int inventoryModel;
        private final short[] colorToReplace;
        private final short[] colorToReplaceWith;
        private final short[] textureToReplace;
        private final short[] textureToReplaceWith;
        private final int xan2d;
        private final int yan2d;
        private final int zan2d;

        private ItemSnapshot(ItemComposition composition)
        {
            this.inventoryModel = composition.getInventoryModel();
            this.colorToReplace = clone(composition.getColorToReplace());
            this.colorToReplaceWith = clone(composition.getColorToReplaceWith());
            this.textureToReplace = clone(composition.getTextureToReplace());
            this.textureToReplaceWith = clone(composition.getTextureToReplaceWith());
            this.xan2d = composition.getXan2d();
            this.yan2d = composition.getYan2d();
            this.zan2d = composition.getZan2d();
        }

        static ItemSnapshot of(ItemComposition composition)
        {
            return new ItemSnapshot(composition);
        }

        void restoreTo(ItemComposition composition)
        {
            composition.setInventoryModel(inventoryModel);
            composition.setColorToReplace(clone(colorToReplace));
            composition.setColorToReplaceWith(clone(colorToReplaceWith));
            composition.setTextureToReplace(clone(textureToReplace));
            composition.setTextureToReplaceWith(clone(textureToReplaceWith));
            composition.setXan2d(xan2d);
            composition.setYan2d(yan2d);
            composition.setZan2d(zan2d);
        }

        private static short[] clone(short[] array)
        {
            return array == null ? null : array.clone();
        }
    }
}
