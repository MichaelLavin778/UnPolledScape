package com.unpolledscape;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;

/**
 * Reverts cosmetic item recolors (ground/inventory/bank icon appearance) back to their
 * pre-recolor look. Mutates {@link ItemComposition} visual fields in place via the public
 * setter API (inventoryModel, recolor/retexture arrays, 2D icon rotation) and snapshots the
 * original values so changes can be cleanly reverted.
 *
 * IMPORTANT: {@link #apply(Client)} and {@link #restore(Client)} mutate shared client state
 * (ItemComposition instances are cached/shared) and MUST only be invoked on the client thread.
 * Calling from any other thread (e.g. the AWT/UI thread during startUp()/onConfigChanged())
 * races with the render thread reading these same fields and can corrupt the item's rendered
 * model/colors mid-update.
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

    // Tracks the original visual state of each source item so apply()/restore() are reversible.
    private final Map<Integer, ItemSnapshot> snapshots = new HashMap<>();

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

    void apply(Client client)
    {
        boolean anyChanged = false;

        for (Map.Entry<Integer, Integer> replacement : REPLACEMENTS.entrySet())
        {
            ItemComposition source = client.getItemDefinition(replacement.getKey());
            anyChanged |= applyTo(client, source);
        }

        if (anyChanged)
        {
            invalidateItemCaches(client);
        }
    }

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

        // Snapshot once, before the first mutation, so restore() can revert to the true original.
        ItemSnapshot snapshot = snapshots.computeIfAbsent(source.getId(), id -> ItemSnapshot.capture(source));
        return copyVisuals(source, target, snapshot);
    }

    void restore(Client client)
    {
        if (snapshots.isEmpty())
        {
            return;
        }

        boolean anyChanged = false;

        for (Map.Entry<Integer, ItemSnapshot> entry : snapshots.entrySet())
        {
            ItemComposition source = client.getItemDefinition(entry.getKey());
            if (source == null)
            {
                continue;
            }

            anyChanged |= entry.getValue().applyTo(source);
        }

        snapshots.clear();

        if (anyChanged)
        {
            invalidateItemCaches(client);
        }
    }

    /**
     * Copies the visual fields that control an item's ground/inventory/bank icon appearance
     * from {@code target} onto {@code source}. Falls back to the item's own original snapshot
     * value when the target doesn't provide one, so unrelated fields aren't zeroed out.
     * Returns true if anything actually changed.
     */
    private static boolean copyVisuals(ItemComposition source, ItemComposition target, ItemSnapshot snapshot)
    {
        boolean changed = false;

        String targetName = target.getName();
        if (targetName != null && !targetName.equals(source.getName()))
        {
            source.setName(targetName);
            changed = true;
        }

        changed |= applyInt(target.getInventoryModel(), source::getInventoryModel, source::setInventoryModel);
        changed |= applyShortArray(target.getColorToReplace(), source::getColorToReplace, source::setColorToReplace);
        changed |= applyShortArray(target.getColorToReplaceWith(), source::getColorToReplaceWith, source::setColorToReplaceWith);
        changed |= applyShortArray(target.getTextureToReplace(), source::getTextureToReplace, source::setTextureToReplace);
        changed |= applyShortArray(target.getTextureToReplaceWith(), source::getTextureToReplaceWith, source::setTextureToReplaceWith);
        changed |= applyInt(target.getXan2d(), source::getXan2d, source::setXan2d);
        changed |= applyInt(target.getYan2d(), source::getYan2d, source::setYan2d);
        changed |= applyInt(target.getZan2d(), source::getZan2d, source::setZan2d);

        return changed;
    }

    private static boolean applyInt(int desired, java.util.function.IntSupplier current, java.util.function.IntConsumer setter)
    {
        if (current.getAsInt() == desired)
        {
            return false;
        }

        setter.accept(desired);
        return true;
    }

    private static boolean applyShortArray(short[] desired, java.util.function.Supplier<short[]> current, java.util.function.Consumer<short[]> setter)
    {
        short[] desiredCopy = desired == null ? null : desired.clone();
        if (Arrays.equals(current.get(), desiredCopy))
        {
            return false;
        }

        setter.accept(desiredCopy);
        return true;
    }

    private static void invalidateItemCaches(Client client)
    {
        // These caches hold pre-rendered models/sprites keyed by item id; without resetting them
        // the client keeps drawing the old appearance even though the composition data changed.
        client.getItemModelCache().reset();
        client.getItemSpriteCache().reset();
    }

    /**
     * Immutable capture of an item's original visual fields, taken the first time apply()
     * touches it, so restore() can put things back exactly as they were.
     */
    private static final class ItemSnapshot
    {
        private final String name;
        private final int inventoryModel;
        private final short[] colorToReplace;
        private final short[] colorToReplaceWith;
        private final short[] textureToReplace;
        private final short[] textureToReplaceWith;
        private final int xan2d;
        private final int yan2d;
        private final int zan2d;

        private ItemSnapshot(
            String name,
            int inventoryModel,
            short[] colorToReplace,
            short[] colorToReplaceWith,
            short[] textureToReplace,
            short[] textureToReplaceWith,
            int xan2d,
            int yan2d,
            int zan2d)
        {
            this.name = name;
            this.inventoryModel = inventoryModel;
            this.colorToReplace = colorToReplace;
            this.colorToReplaceWith = colorToReplaceWith;
            this.textureToReplace = textureToReplace;
            this.textureToReplaceWith = textureToReplaceWith;
            this.xan2d = xan2d;
            this.yan2d = yan2d;
            this.zan2d = zan2d;
        }

        private static ItemSnapshot capture(ItemComposition composition)
        {
            return new ItemSnapshot(
                composition.getName(),
                composition.getInventoryModel(),
                cloneOrNull(composition.getColorToReplace()),
                cloneOrNull(composition.getColorToReplaceWith()),
                cloneOrNull(composition.getTextureToReplace()),
                cloneOrNull(composition.getTextureToReplaceWith()),
                composition.getXan2d(),
                composition.getYan2d(),
                composition.getZan2d()
            );
        }

        private boolean applyTo(ItemComposition composition)
        {
            boolean changed = false;

            if (name != null && !name.equals(composition.getName()))
            {
                composition.setName(name);
                changed = true;
            }

            changed |= applyInt(inventoryModel, composition::getInventoryModel, composition::setInventoryModel);
            changed |= applyShortArray(colorToReplace, composition::getColorToReplace, composition::setColorToReplace);
            changed |= applyShortArray(colorToReplaceWith, composition::getColorToReplaceWith, composition::setColorToReplaceWith);
            changed |= applyShortArray(textureToReplace, composition::getTextureToReplace, composition::setTextureToReplace);
            changed |= applyShortArray(textureToReplaceWith, composition::getTextureToReplaceWith, composition::setTextureToReplaceWith);
            changed |= applyInt(xan2d, composition::getXan2d, composition::setXan2d);
            changed |= applyInt(yan2d, composition::getYan2d, composition::setYan2d);
            changed |= applyInt(zan2d, composition::getZan2d, composition::setZan2d);

            return changed;
        }

        private static short[] cloneOrNull(short[] value)
        {
            return value == null ? null : value.clone();
        }
    }
}