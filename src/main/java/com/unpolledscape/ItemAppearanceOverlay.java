package com.unpolledscape;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Visually replaces cosmetic items with their legacy equivalents by drawing the replacement item's
 * icon on top of the real item wherever it appears as a widget: inventory, worn equipment, bank,
 * deposit boxes, trade, GE, etc.
 *
 * This is a pure rendering overlay (modeled after the Animated Icons plugin) and never mutates the
 * shared {@link net.runelite.api.ItemComposition} state, so there is nothing to "restore": when the
 * feature is disabled the overlay simply stops drawing and the real item shows through again. That
 * makes enabling/disabling instant and glitch-free, unlike the previous cache-mutation approach.
 */
class ItemAppearanceOverlay extends WidgetItemOverlay
{
    private final ItemManager itemManager;
    private final UnPolledScapeConfig config;

    @Inject
    ItemAppearanceOverlay(ItemManager itemManager, UnPolledScapeConfig config)
    {
        this.itemManager = itemManager;
        this.config = config;
        showOnInventory();
        showOnEquipment();
        showOnBank();
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
    {
        if (!config.items())
        {
            return;
        }

        Integer replacementId = ItemAppearanceReplacements.getReplacementItemId(itemId);
        if (replacementId == null)
        {
            return;
        }

        Image replacement = itemManager.getImage(replacementId);
        if (replacement == null)
        {
            return;
        }

        Rectangle bounds = widgetItem.getCanvasBounds();
        graphics.drawImage(replacement, bounds.x, bounds.y, null);
    }
}
