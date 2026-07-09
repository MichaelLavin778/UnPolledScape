package com.unpolledscape;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Draws the correctly-scaled icon of the replacement item over inventory, bank and equipment slots.
 *
 * <p>This is the "correct appearance" half of the hybrid item-replacement feature. The composition
 * swap ({@link ItemAppearanceReplacements}) makes the underlying item render as the replacement
 * everywhere (chat/quest dialogs, ground, and the slot beneath this overlay) but cannot control the
 * icon's zoom/scale because RuneLite exposes no setter for it. {@link ItemManager#getImage} renders
 * the replacement with its own correct scale, so painting it here yields a pixel-perfect slot icon.
 * Because the item underneath is already the replacement, nothing of the original shows through.
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

        Integer replacementId = UnPolledScapePlugin.getReplacementItemId(itemId);
        if (replacementId == null)
        {
            return;
        }

        Image image = itemManager.getImage(replacementId);
        if (image == null)
        {
            return;
        }

        Rectangle bounds = widgetItem.getCanvasBounds();
        graphics.drawImage(image, bounds.x, bounds.y, null);
    }
}
