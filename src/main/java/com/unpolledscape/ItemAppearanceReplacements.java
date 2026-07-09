package com.unpolledscape;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;

/**
 * Reverts cosmetic item recolors (ground/inventory/bank icon appearance) back to their
 * pre-recolor look by copying the visual fields of a legacy "target" item onto each modern
 * "source" item.
 *
 * The mutation is applied reactively in {@link #applyTo(Client, ItemComposition)}, which the
 * plugin invokes from the {@code PostItemComposition} event. That event fires every time an
 * item composition is (re)created, so it is the single source of truth for the swap.
 *
 * {@link #apply(Client)} / {@link #restore(Client)} simply flush the client's item caches. This
 * evicts every cached composition (so it is reloaded, re-firing {@code PostItemComposition}) and
 * clears the pre-rendered model/sprite caches (so the icon/ground model is rebuilt). When the
 * feature is disabled the reload restores the untouched originals straight from the game cache
 * because {@code PostItemComposition} no longer mutates them, so no manual snapshotting is needed.
 *
 * IMPORTANT: every method here touches shared client caches/compositions and MUST only be invoked
 * on the client thread.
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

    Map<Integer, Integer> replacementMap(Client client)
    {
        return REPLACEMENTS;
    }

    boolean isReplacementSourceItem(int itemId)
    {
        return REPLACEMENTS.containsKey(itemId);
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

    /**
     * Enables the swap. Flushing the caches forces every affected item to be reloaded (which
     * re-fires {@code PostItemComposition} and re-applies the swap via {@link #applyTo}) and
     * re-rendered.
     */
    void apply(Client client)
    {
        invalidateItemCaches(client);
    }

    /**
     * Disables the swap. Flushing the caches reloads the untouched originals from the game cache;
     * because {@code PostItemComposition} is gated on the feature being enabled, the reload leaves
     * them unmodified.
     */
    void restore(Client client)
    {
        invalidateItemCaches(client);
    }

    /**
     * Copies the legacy target item's visual fields onto {@code source}. Invoked for every freshly
     * created composition; a no-op for items that aren't part of the replacement set.
     */
    boolean applyTo(Client client, ItemComposition source)
    {
        if (source == null)
        {
            return false;
        }

        Integer replacementId = REPLACEMENTS.get(source.getId());
        if (replacementId == null)
        {
            return false;
        }

        ItemComposition target = client.getItemDefinition(replacementId);
        if (target == null)
        {
            return false;
        }

        return copyVisuals(source, target);
    }

    /**
     * Copies the visual fields that control an item's ground/inventory/bank icon appearance from
     * {@code target} onto {@code source}. Returns true if anything actually changed.
     */
    private static boolean copyVisuals(ItemComposition source, ItemComposition target)
    {
        boolean changed = false;

        String targetName = target.getName();
        if (targetName != null && !targetName.equals(source.getName()))
        {
            source.setName(targetName);
            changed = true;
        }

        changed |= applyInt(target.getInventoryModel(), source.getInventoryModel(), source::setInventoryModel);
        changed |= applyShortArray(target.getColorToReplace(), source.getColorToReplace(), source::setColorToReplace);
        changed |= applyShortArray(target.getColorToReplaceWith(), source.getColorToReplaceWith(), source::setColorToReplaceWith);
        changed |= applyShortArray(target.getTextureToReplace(), source.getTextureToReplace(), source::setTextureToReplace);
        changed |= applyShortArray(target.getTextureToReplaceWith(), source.getTextureToReplaceWith(), source::setTextureToReplaceWith);
        changed |= applyInt(target.getXan2d(), source.getXan2d(), source::setXan2d);
        changed |= applyInt(target.getYan2d(), source.getYan2d(), source::setYan2d);
        changed |= applyInt(target.getZan2d(), source.getZan2d(), source::setZan2d);

        return changed;
    }

    private static boolean applyInt(int desired, int current, IntConsumer setter)
    {
        if (current == desired)
        {
            return false;
        }

        setter.accept(desired);
        return true;
    }

    private static boolean applyShortArray(short[] desired, short[] current, Consumer<short[]> setter)
    {
        short[] desiredCopy = desired == null ? null : desired.clone();
        if (Arrays.equals(current, desiredCopy))
        {
            return false;
        }

        setter.accept(desiredCopy);
        return true;
    }

    private static void invalidateItemCaches(Client client)
    {
        // Evict cached compositions so they reload (re-firing PostItemComposition), and drop the
        // pre-rendered models/sprites keyed by item id so the new appearance is actually drawn.
        client.getItemCompositionCache().reset();
        client.getItemModelCache().reset();
        client.getItemSpriteCache().reset();
    }
}
