package com.unpolledscape;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;

/**
 * Restores legacy item names that were changed by the "Diversity & Inclusion" updates, by mutating
 * the item's {@link ItemComposition#setName(String)}. The name is read live from the composition on
 * every surface (right-click menus, tooltips, bank, etc.), so mutating it in place is enough; the
 * composition cache is deliberately NOT reset (that would evict the mutation). Compositions loaded
 * or reloaded later are handled via {@link #applyTo(ItemComposition)} on the PostItemComposition
 * event.
 *
 * IMPORTANT: mutates live composition state and MUST only be invoked on the client thread.
 */
final class ItemNameReplacements
{
    // "skin paste" was renamed to "paste" (Prince Ali Rescue, 27 April 2022 D&I update).
    private static final int PASTE_ITEM_ID = 2424;

    private static final Map<Integer, String> LEGACY_ITEM_NAMES = createLegacyNameMap();

    /** Original in-game names captured the first time each item is renamed, used to restore them. */
    private final Map<Integer, String> originalNames = new HashMap<>();

    private static Map<Integer, String> createLegacyNameMap()
    {
        Map<Integer, String> names = new HashMap<>();
        names.put(PASTE_ITEM_ID, "Skin paste");
        return names;
    }

    void apply(Client client)
    {
        for (int itemId : LEGACY_ITEM_NAMES.keySet())
        {
            applyTo(client.getItemDefinition(itemId));
        }
    }

    void applyTo(ItemComposition composition)
    {
        if (composition == null)
        {
            return;
        }

        String legacyName = LEGACY_ITEM_NAMES.get(composition.getId());
        if (legacyName == null || legacyName.equals(composition.getName()))
        {
            return;
        }

        originalNames.putIfAbsent(composition.getId(), composition.getName());
        composition.setName(legacyName);
    }

    void restore(Client client)
    {
        if (originalNames.isEmpty())
        {
            return;
        }

        for (Map.Entry<Integer, String> entry : originalNames.entrySet())
        {
            ItemComposition composition = client.getItemDefinition(entry.getKey());
            if (composition != null)
            {
                composition.setName(entry.getValue());
            }
        }

        originalNames.clear();
    }
}
