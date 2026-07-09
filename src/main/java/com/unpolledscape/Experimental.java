package com.unpolledscape;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/**
 * Experimental legacy hairstyle filtering: hides opposite-body-type hairstyles in the character
 * creation ("Player Design") and hairdresser ("Makeover") interfaces. Split out from
 * {@link MakeoverReplacements} so it can be toggled independently via the "Experimental" config
 * flag. Reuses the shared widget helpers exposed as package-private statics on
 * {@link MakeoverReplacements}.
 */
final class Experimental
{
    private static final int MAX_HAIR_STYLE_SKIPS = 64;

    // Legacy (pre-"Diversity & Inclusion") gendered hairstyles, by display name. A style that
    // historically belonged to one body type is hidden from the other; styles shared by both
    // (Bald/Long/Medium/Short/Cropped) stay for both. Post-2024 unisex additions we treat as
    // single-gender are listed below; any style not listed here or in the feminine set is treated as
    // unknown and left visible for both body types.
    private static final Set<String> LEGACY_MASCULINE_HAIR_STYLES = normalizeToSet(
        "Bald", "Dreadlocks", "Long", "Medium", "Tonsure", "Short", "Cropped", "Wild spikes", "Spikes",
        "Mohawk", "Wind braids", "Quiff", "Samurai", "Princely", "Curtains", "Long curtains",
        "Front split", "Tousled", "Side wedge", "Front wedge", "Front spikes", "Frohawk", "Rear skirt",
        "Queue", "Mullet", "Undercut", "Low Bun", "Messy Bun", "Pompadour", "Afro", "Short locs",
        "Spiky Mohawk", "Slicked Mohawk");

    private static final Set<String> LEGACY_FEMININE_HAIR_STYLES = normalizeToSet(
        "Bald", "Long", "Medium", "Short", "Cropped", "Bun", "Pigtails", "Earmuffs", "Side pony",
        "Curls", "Ponytail", "Braids", "Bunches", "Bob", "Layered", "Straight", "Long Quiff",
        "Short Choppy", "Side Afro", "Punk", "Half-shaved", "Medium Coils", "High ponytail",
        "Plaits", "High Bunches");

    private static final Set<String> KNOWN_HAIR_STYLES =
        union(LEGACY_MASCULINE_HAIR_STYLES, LEGACY_FEMININE_HAIR_STYLES);

    void apply(Client client)
    {
        applyPlayerDesignHair(client);
        applyMakeoverHair(client);
    }

    void restore(Client client)
    {
        // Un-hide any hairstyle swatches we hid in the selection grid. These are dynamic children
        // that share their container's packed id, so they are not individually tracked; passing a
        // predicate that never hides simply makes every run visible again.
        MakeoverReplacements.hideMakeoverSwatches(client.getWidget(InterfaceID.Makeover.ITEM_AREA), text -> false);
    }

    boolean handleMenuOptionClicked(Client client, MenuOptionClicked event)
    {
        return handlePlayerDesignHairClick(client, event) || handleMakeoverHairClick(client, event);
    }

    private void applyPlayerDesignHair(Client client)
    {
        if (client.getWidget(InterfaceID.PlayerDesign.UNIVERSE) == null)
        {
            return;
        }

        enforcePlayerDesignHair(client, true);
    }

    private void applyMakeoverHair(Client client)
    {
        if (client.getWidget(InterfaceID.Makeover.UNIVERSE) == null)
        {
            return;
        }

        boolean female = MakeoverReplacements.isMakeoverFemale(client);

        // Hide cross-sex hairstyle swatches in the selection grid.
        MakeoverReplacements.hideMakeoverSwatches(client.getWidget(InterfaceID.Makeover.ITEM_AREA),
            text -> isDisallowedHairStyle(text, female));
    }

    private boolean handlePlayerDesignHairClick(Client client, MenuOptionClicked event)
    {
        if (client.getWidget(InterfaceID.PlayerDesign.UNIVERSE) == null)
        {
            return false;
        }

        Widget widget = event.getWidget();
        if (widget == null)
        {
            return false;
        }

        int widgetId = widget.getId();
        if (widgetId != InterfaceID.PlayerDesign.HEAD_LEFT && widgetId != InterfaceID.PlayerDesign.HEAD_RIGHT)
        {
            return false;
        }

        event.consume();
        clickPlayerDesignHairArrow(client, widget, MakeoverReplacements.isPlayerDesignFemale(client));
        return true;
    }

    private boolean handleMakeoverHairClick(Client client, MenuOptionClicked event)
    {
        if (client.getWidget(InterfaceID.Makeover.UNIVERSE) == null)
        {
            return false;
        }

        boolean female = MakeoverReplacements.isMakeoverFemale(client);
        Widget widget = event.getWidget();
        if (isDisallowedHairStyle(event.getMenuTarget(), female)
            || isDisallowedHairStyle(widget == null ? null : widget.getText(), female)
            || isDisallowedHairStyle(widget == null ? null : widget.getName(), female))
        {
            event.consume();
            return true;
        }

        return false;
    }

    private void enforcePlayerDesignHair(Client client, boolean forward)
    {
        if (isCurrentPlayerDesignHairAllowed(client, MakeoverReplacements.isPlayerDesignFemale(client)))
        {
            return;
        }

        Widget arrow = client.getWidget(forward ? InterfaceID.PlayerDesign.HEAD_RIGHT : InterfaceID.PlayerDesign.HEAD_LEFT);
        if (arrow != null)
        {
            clickPlayerDesignHairArrow(client, arrow, MakeoverReplacements.isPlayerDesignFemale(client));
        }
    }

    private void clickPlayerDesignHairArrow(Client client, Widget arrow, boolean female)
    {
        Object[] listener = arrow.getOnOpListener();
        if (listener == null)
        {
            return;
        }

        String previousStyle = getWidgetText(client, InterfaceID.PlayerDesign.HEAD_TEXT);
        for (int i = 0; i < MAX_HAIR_STYLE_SKIPS; i++)
        {
            client.runScript(listener);

            String currentStyle = getWidgetText(client, InterfaceID.PlayerDesign.HEAD_TEXT);
            if (isHairStyleAllowed(currentStyle, female))
            {
                return;
            }

            if (Objects.equals(previousStyle, currentStyle))
            {
                return;
            }

            previousStyle = currentStyle;
        }
    }

    private boolean isCurrentPlayerDesignHairAllowed(Client client, boolean female)
    {
        return isHairStyleAllowed(getWidgetText(client, InterfaceID.PlayerDesign.HEAD_TEXT), female);
    }

    private String getWidgetText(Client client, int widgetId)
    {
        Widget widget = client.getWidget(widgetId);
        return widget == null ? null : widget.getText();
    }

    private boolean isDisallowedHairStyle(String text, boolean female)
    {
        String styleName = MakeoverReplacements.normalizeStyleName(text);
        if (styleName.isEmpty() || !KNOWN_HAIR_STYLES.contains(styleName))
        {
            return false;
        }

        return !getAllowedHairStyles(female).contains(styleName);
    }

    private boolean isHairStyleAllowed(String text, boolean female)
    {
        return !isDisallowedHairStyle(text, female);
    }

    private Set<String> getAllowedHairStyles(boolean female)
    {
        return female ? LEGACY_FEMININE_HAIR_STYLES : LEGACY_MASCULINE_HAIR_STYLES;
    }

    private static Set<String> normalizeToSet(String... names)
    {
        Set<String> set = new HashSet<>();
        for (String name : names)
        {
            String normalized = MakeoverReplacements.normalizeStyleName(name);
            if (!normalized.isEmpty())
            {
                set.add(normalized);
            }
        }
        return set;
    }

    private static Set<String> union(Set<String> first, Set<String> second)
    {
        Set<String> set = new HashSet<>(first);
        set.addAll(second);
        return set;
    }
}
