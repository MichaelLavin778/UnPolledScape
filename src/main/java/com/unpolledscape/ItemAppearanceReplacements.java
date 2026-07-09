package com.unpolledscape;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;

/**
 * NOT FUNCTIONAL - TODO TO FIX
 * 
 * Reskins modern cosmetic items so they render as the legacy item they replace, everywhere the game
 * draws an item from its composition: inventory, equipment, bank, ground and chat/quest dialogs.
 *
 * <p>The swap is a real image replacement (the original icon is gone, not painted over) done by
 * copying the legacy item's visual fields onto the modern item's {@link ItemComposition} and then
 * flushing the client's item caches so every surface re-renders.
 *
 * <p>Note: RuneLite exposes no setter for an item's icon zoom/scale/offset, so the reskinned model
 * renders at the original item's scale. On the inventory/bank/equipment slots (where scale is most
 * noticeable) {@link ItemAppearanceOverlay} paints the correctly-scaled icon on top; this class
 * guarantees the item underneath that overlay is already the replacement so nothing of the original
 * shows through, and it is the sole mechanism on model surfaces the overlay cannot reach
 * (chat/quest dialogs, dropped items).
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

    /** Original visuals of every composition we have mutated, keyed by the modern item id. */
    private final Map<Integer, ItemSnapshot> snapshots = new HashMap<>();

    /**
     * Reskin every replaced item that is already loaded, then flush the item caches so the change is
     * visible on every surface. Items loaded/reloaded later are handled by
     * {@link #applyTo(Client, ItemComposition)} via the PostItemComposition event.
     */
    void apply(Client client, Map<Integer, Integer> replacements)
    {
        for (int sourceId : replacements.keySet())
        {
            applyTo(client, client.getItemDefinition(sourceId), replacements);
        }
        invalidateItemCaches(client);
    }

    /**
     * Reskin a single composition (if it is a replacement source), snapshotting its original visuals
     * the first time it is touched so they can be restored later. Safe to call repeatedly.
     */
    void applyTo(Client client, ItemComposition composition, Map<Integer, Integer> replacements)
    {
        if (composition == null)
        {
            return;
        }

        Integer replacementId = replacements.get(composition.getId());
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
}
