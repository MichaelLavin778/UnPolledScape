package com.unpolledscape;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
// import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

final class MakeoverReplacements
{
    private static final int BODY_TYPE_B = 1;
    private static final int MAX_HAIR_STYLE_SKIPS = 64;
    private static final String MODERN_CHARACTER_PROMPT = "Select skin colour, body type and pronouns";
    private static final String LEGACY_CHARACTER_PROMPT = "Select skin colour and gender";
    private static final String MODERN_BODY_TYPE_SINGULAR = "Body type";
    private static final String MODERN_BODY_TYPE_PLURAL = "Body types";
    private static final String LEGACY_BODY_TYPE = "Gender";
    private static final String FACIAL_HAIR_CATEGORY_LABEL = "facialhair";
    private static final String SHAVE_MENU_OPTION = "shave";
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern STYLE_NAME_NORMALIZER = Pattern.compile("[^a-z0-9]");

    // private static final int[] LEGACY_MASCULINE_HAIR_ROWS = {
    //     DBTableID.HairStyles.Row.BALD,
    //     DBTableID.HairStyles.Row.DREADLOCKS,
    //     DBTableID.HairStyles.Row.LONG,
    //     DBTableID.HairStyles.Row.MEDIUM,
    //     DBTableID.HairStyles.Row.TONSURE,
    //     DBTableID.HairStyles.Row.SHORT,
    //     DBTableID.HairStyles.Row.CROPPED,
    //     DBTableID.HairStyles.Row.WILDSPIKES,
    //     DBTableID.HairStyles.Row.SPIKES,
    //     DBTableID.HairStyles.Row.MOHAWK,
    //     DBTableID.HairStyles.Row.WINDBRAIDS,
    //     DBTableID.HairStyles.Row.QUIFF,
    //     DBTableID.HairStyles.Row.SAMURAI,
    //     DBTableID.HairStyles.Row.PRINCELY,
    //     DBTableID.HairStyles.Row.CURTAINS,
    //     DBTableID.HairStyles.Row.LONGCURTAINS,
    //     DBTableID.HairStyles.Row.FRONTSPLIT,
    //     DBTableID.HairStyles.Row.TOUSLED,
    //     DBTableID.HairStyles.Row.SIDEWEDGE,
    //     DBTableID.HairStyles.Row.FRONTWEDGE,
    //     DBTableID.HairStyles.Row.FRONTSPIKES,
    //     DBTableID.HairStyles.Row.FROHAWK,
    //     DBTableID.HairStyles.Row.REARSKIRT,
    //     DBTableID.HairStyles.Row.WARRIORMONK
    // };

    // private static final int[] LEGACY_FEMININE_HAIR_ROWS = {
    //     DBTableID.HairStyles.Row.BALD,
    //     DBTableID.HairStyles.Row.LONG,
    //     DBTableID.HairStyles.Row.MEDIUM,
    //     DBTableID.HairStyles.Row.SHORT,
    //     DBTableID.HairStyles.Row.CROPPED,
    //     DBTableID.HairStyles.Row.BUN,
    //     DBTableID.HairStyles.Row.PIGTAILS,
    //     DBTableID.HairStyles.Row.EARMUFFS,
    //     DBTableID.HairStyles.Row.SIDEPONY,
    //     DBTableID.HairStyles.Row.CURLS,
    //     DBTableID.HairStyles.Row.PONYTAIL,
    //     DBTableID.HairStyles.Row.BRAIDS,
    //     DBTableID.HairStyles.Row.BUNCHES,
    //     DBTableID.HairStyles.Row.BOB,
    //     DBTableID.HairStyles.Row.LAYERED,
    //     DBTableID.HairStyles.Row.STRAIGHT
    // };

    private static final int[] PLAYER_DESIGN_PRONOUN_WIDGETS = {
        InterfaceID.PlayerDesign.DROPDOWN_CONTAINER,
        InterfaceID.PlayerDesign.CONTENTS_MIDDLE,
        InterfaceID.PlayerDesign.CONTENTS_MIDDLE_HEADER,
        InterfaceID.PlayerDesign.PRONOUNS,
        InterfaceID.PlayerDesign.DROPDOWN_CLOSE,
        InterfaceID.PlayerDesign.DROPDOWN_PANEL,
        InterfaceID.PlayerDesign.BACKGROUND,
        InterfaceID.PlayerDesign.PRONOUNS_BUTTONS,
        InterfaceID.PlayerDesign.DROPDOWN_SCROLLBAR
    };

    private static final int[] PLAYER_DESIGN_FACIAL_HAIR_WIDGETS = {
        InterfaceID.PlayerDesign.JAW,
        InterfaceID.PlayerDesign.JAW_TEXT,
        InterfaceID.PlayerDesign.JAW_LEFT,
        InterfaceID.PlayerDesign.JAW_RIGHT
    };

    private static final int[] MAKEOVER_MAGE_PRONOUN_WIDGETS = {
        InterfaceID.MakeoverMage.PRONOUNS,
        InterfaceID.MakeoverMage.PRONOUNS_RECT0,
        InterfaceID.MakeoverMage.PRONOUNS_TITLE,
        InterfaceID.MakeoverMage.HE_HIM,
        InterfaceID.MakeoverMage.RADIO_HE,
        InterfaceID.MakeoverMage.PRONOUNS_HE,
        InterfaceID.MakeoverMage.THEY_THEM,
        InterfaceID.MakeoverMage.RADIO_THEY,
        InterfaceID.MakeoverMage.PRONOUNS_THEY,
        InterfaceID.MakeoverMage.SHE_HER,
        InterfaceID.MakeoverMage.RADIO_SHE,
        InterfaceID.MakeoverMage.PRONOUNS_SHE
    };

    private final Map<Integer, WidgetSnapshot> snapshots = new HashMap<>();
    private final Set<Integer> forcedHiddenWidgetIds = new HashSet<>();
    private final Set<String> knownHairStyles = new HashSet<>();
    private final Set<String> legacyMasculineHairStyles = new HashSet<>();
    private final Set<String> legacyFeminineHairStyles = new HashSet<>();

    void apply(Client client)
    {
        boolean interfaceOpen = applyPlayerDesign(client);
        interfaceOpen |= applyMakeoverMage(client);
        interfaceOpen |= applyMakeover(client);

        if (!interfaceOpen)
        {
            snapshots.clear();
            forcedHiddenWidgetIds.clear();
        }
    }

    boolean handleMenuOptionClicked(Client client, MenuOptionClicked event)
    {
        return handlePlayerDesignHairClick(client, event) || handleMakeoverHairClick(client, event);
    }

    void handleMenuEntryAdded(Client client, MenuEntryAdded event)
    {
        if (!isShaveMenuOption(event.getOption()) || !isLocalPlayerFemale(client))
        {
            return;
        }

        client.getMenu().removeMenuEntry(event.getMenuEntry());
    }

    void restore(Client client)
    {
        for (Map.Entry<Integer, WidgetSnapshot> entry : snapshots.entrySet())
        {
            Widget widget = client.getWidget(entry.getKey());
            if (widget != null)
            {
                entry.getValue().restore(widget);
            }
        }

        snapshots.clear();
        forcedHiddenWidgetIds.clear();
    }

    private boolean applyPlayerDesign(Client client)
    {
        if (client.getWidget(InterfaceID.PlayerDesign.UNIVERSE) == null)
        {
            return false;
        }

        replaceInterfaceText(client, InterfaceID.PlayerDesign.UNIVERSE);
        setText(client, InterfaceID.PlayerDesign.CONTENTS_BOTTOM_HEADER, "Gender");
        setText(client, InterfaceID.PlayerDesign.GENDER, "Gender");
        setText(client, InterfaceID.PlayerDesign.GENDER_MALE, "Male");
        setText(client, InterfaceID.PlayerDesign.GENDER_FEMALE, "Female");
        setNameAndAction(client, InterfaceID.PlayerDesign.GENDER_MALE, "Male");
        setNameAndAction(client, InterfaceID.PlayerDesign.GENDER_FEMALE, "Female");
        setWidgetsHidden(client, PLAYER_DESIGN_PRONOUN_WIDGETS, true);
        setWidgetsHidden(client, PLAYER_DESIGN_FACIAL_HAIR_WIDGETS, isPlayerDesignFemale(client));
        enforcePlayerDesignHair(client, true);
        return true;
    }

    private boolean applyMakeoverMage(Client client)
    {
        if (client.getWidget(InterfaceID.MakeoverMage.UNIVERSE) == null)
        {
            return false;
        }

        replaceInterfaceText(client, InterfaceID.MakeoverMage.UNIVERSE);
        setText(client, InterfaceID.MakeoverMage.A_TITLE, "Male");
        setText(client, InterfaceID.MakeoverMage.B_TITLE, "Female");
        setNameAndAction(client, InterfaceID.MakeoverMage.BODYTYPE_A, "Male");
        setNameAndAction(client, InterfaceID.MakeoverMage.BODYTYPE_B, "Female");
        setWidgetsHidden(client, MAKEOVER_MAGE_PRONOUN_WIDGETS, true);
        return true;
    }

    private boolean applyMakeover(Client client)
    {
        if (client.getWidget(InterfaceID.Makeover.UNIVERSE) == null)
        {
            return false;
        }

        replaceInterfaceText(client, InterfaceID.Makeover.UNIVERSE);
        boolean female = isMakeoverFemale(client);
        hideDisallowedMakeoverHairWidgets(client, female);
        hideMakeoverFacialHairCategory(client, female);
        return true;
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
        clickPlayerDesignHairArrow(client, widget, isPlayerDesignFemale(client));
        return true;
    }

    private boolean handleMakeoverHairClick(Client client, MenuOptionClicked event)
    {
        if (client.getWidget(InterfaceID.Makeover.UNIVERSE) == null)
        {
            return false;
        }

        boolean female = isMakeoverFemale(client);
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
        if (isCurrentPlayerDesignHairAllowed(client, isPlayerDesignFemale(client)))
        {
            return;
        }

        Widget arrow = client.getWidget(forward ? InterfaceID.PlayerDesign.HEAD_RIGHT : InterfaceID.PlayerDesign.HEAD_LEFT);
        if (arrow != null)
        {
            clickPlayerDesignHairArrow(client, arrow, isPlayerDesignFemale(client));
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

    private void hideDisallowedMakeoverHairWidgets(Client client, boolean female)
    {
        Widget selectContainer = client.getWidget(InterfaceID.Makeover.SELECT_CONTAINER);
        if (selectContainer != null)
        {
            updateMakeoverHairWidget(selectContainer, female, true);
        }
    }

    private boolean updateMakeoverHairWidget(Widget widget, boolean female, boolean root)
    {
        boolean ownDisallowed = isDisallowedHairStyle(widget.getText(), female)
            || isDisallowedHairStyle(widget.getName(), female);

        boolean childDisallowed = updateMakeoverHairWidgets(widget.getStaticChildren(), female)
            | updateMakeoverHairWidgets(widget.getDynamicChildren(), female)
            | updateMakeoverHairWidgets(widget.getNestedChildren(), female);

        if (!root)
        {
            boolean hide = ownDisallowed || (childDisallowed && hasInteraction(widget));
            if (hide || forcedHiddenWidgetIds.contains(widget.getId()))
            {
                setHidden(widget, hide);
            }
        }

        return ownDisallowed || childDisallowed;
    }

    private boolean updateMakeoverHairWidgets(Widget[] widgets, boolean female)
    {
        if (widgets == null)
        {
            return false;
        }

        boolean disallowed = false;
        for (Widget widget : widgets)
        {
            if (widget != null)
            {
                disallowed |= updateMakeoverHairWidget(widget, female, false);
            }
        }

        return disallowed;
    }

    private void hideMakeoverFacialHairCategory(Client client, boolean female)
    {
        Widget universe = client.getWidget(InterfaceID.Makeover.UNIVERSE);
        if (universe != null)
        {
            updateMakeoverFacialHairWidget(universe, female, true);
        }
    }

    private boolean updateMakeoverFacialHairWidget(Widget widget, boolean female, boolean root)
    {
        boolean ownMatch = isFacialHairCategoryLabel(widget.getText())
            || isFacialHairCategoryLabel(widget.getName());

        boolean childMatch = updateMakeoverFacialHairWidgets(widget.getStaticChildren(), female)
            | updateMakeoverFacialHairWidgets(widget.getDynamicChildren(), female)
            | updateMakeoverFacialHairWidgets(widget.getNestedChildren(), female);

        if (!root)
        {
            boolean hide = female && (ownMatch || (childMatch && hasInteraction(widget)));
            if (hide || forcedHiddenWidgetIds.contains(widget.getId()))
            {
                setHidden(widget, hide);
            }
        }

        return ownMatch || childMatch;
    }

    private boolean updateMakeoverFacialHairWidgets(Widget[] widgets, boolean female)
    {
        if (widgets == null)
        {
            return false;
        }

        boolean matched = false;
        for (Widget widget : widgets)
        {
            if (widget != null)
            {
                matched |= updateMakeoverFacialHairWidget(widget, female, false);
            }
        }

        return matched;
    }

    private boolean hasInteraction(Widget widget)
    {
        if (widget.getOnOpListener() != null)
        {
            return true;
        }

        String[] actions = widget.getActions();
        if (actions == null)
        {
            return false;
        }

        for (String action : actions)
        {
            if (action != null)
            {
                return true;
            }
        }

        return false;
    }

    private String getWidgetText(Client client, int widgetId)
    {
        Widget widget = client.getWidget(widgetId);
        return widget == null ? null : widget.getText();
    }

    private boolean isPlayerDesignFemale(Client client)
    {
        return client.getVarbitValue(VarbitID.PLAYER_DESIGN_BODYTYPE) == BODY_TYPE_B;
    }

    private boolean isMakeoverFemale(Client client)
    {
        return client.getVarbitValue(VarbitID.MAKEOVER_BODYTYPE) == BODY_TYPE_B;
    }

    private boolean isLocalPlayerFemale(Client client)
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return false;
        }

        PlayerComposition composition = localPlayer.getPlayerComposition();
        return composition != null && composition.getGender() == BODY_TYPE_B;
    }

    private void setWidgetsHidden(Client client, int[] widgetIds, boolean hidden)
    {
        for (int widgetId : widgetIds)
        {
            Widget widget = client.getWidget(widgetId);
            if (widget == null)
            {
                continue;
            }

            setHidden(widget, hidden);
        }
    }

    private void replaceInterfaceText(Client client, int rootWidgetId)
    {
        replaceInterfaceText(client.getWidget(rootWidgetId));
    }

    private void replaceInterfaceText(Widget widget)
    {
        if (widget == null)
        {
            return;
        }

        String currentText = widget.getText();
        String replacement = restoreCharacterCreationText(currentText);
        if (replacement != null && !replacement.equals(currentText))
        {
            snapshot(widget);
            widget.setText(replacement);
        }

        replaceInterfaceText(widget.getStaticChildren());
        replaceInterfaceText(widget.getDynamicChildren());
        replaceInterfaceText(widget.getNestedChildren());
    }

    private void replaceInterfaceText(Widget[] widgets)
    {
        if (widgets == null)
        {
            return;
        }

        for (Widget widget : widgets)
        {
            replaceInterfaceText(widget);
        }
    }

    private void setText(Client client, int widgetId, String text)
    {
        Widget widget = client.getWidget(widgetId);
        if (widget == null)
        {
            return;
        }

        snapshot(widget);
        if (!text.equals(widget.getText()))
        {
            widget.setText(text);
        }
    }

    private void setNameAndAction(Client client, int widgetId, String name)
    {
        Widget widget = client.getWidget(widgetId);
        if (widget == null)
        {
            return;
        }

        snapshot(widget);
        if (!name.equals(widget.getName()))
        {
            widget.setName(name);
        }
        widget.setAction(0, "Select");
    }

    static String restoreCharacterCreationText(String text)
    {
        if (text == null || text.isEmpty())
        {
            return text;
        }

        String replacement = text.replace(MODERN_CHARACTER_PROMPT, LEGACY_CHARACTER_PROMPT);
        replacement = replacement.replace(MODERN_BODY_TYPE_PLURAL, LEGACY_BODY_TYPE);
        replacement = replacement.replace(MODERN_BODY_TYPE_SINGULAR, LEGACY_BODY_TYPE);
        return replacement;
    }

    static boolean isShaveMenuOption(String option)
    {
        return SHAVE_MENU_OPTION.equals(normalizeStyleName(option));
    }

    static boolean isFacialHairCategoryLabel(String text)
    {
        return FACIAL_HAIR_CATEGORY_LABEL.equals(normalizeStyleName(text));
    }

    private void setHidden(Widget widget, boolean hidden)
    {
        snapshot(widget);

        int widgetId = widget.getId();
        if (hidden)
        {
            forcedHiddenWidgetIds.add(widgetId);
        }
        else
        {
            forcedHiddenWidgetIds.remove(widgetId);
        }

        WidgetSnapshot snapshot = snapshots.get(widgetId);
        boolean targetHidden = hidden || snapshot.hidden;
        if (widget.isSelfHidden() != targetHidden)
        {
            widget.setHidden(targetHidden);
        }
    }

    private void snapshot(Widget widget)
    {
        snapshots.computeIfAbsent(widget.getId(), id -> WidgetSnapshot.from(widget));
    }

    private boolean isDisallowedHairStyle(String text, boolean female)
    {
        String styleName = normalizeStyleName(text);
        if (styleName.isEmpty() || !knownHairStyles.contains(styleName))
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
        return female ? legacyFeminineHairStyles : legacyMasculineHairStyles;
    }

    private static String normalizeStyleName(String text)
    {
        if (text == null)
        {
            return "";
        }

        String stripped = TAG_PATTERN.matcher(text)
            .replaceAll(" ")
            .replace("&nbsp;", " ")
            .replace('\u00a0', ' ');
        return STYLE_NAME_NORMALIZER.matcher(stripped.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private static final class WidgetSnapshot
    {
        private final String text;
        private final String name;
        private final String[] actions;
        private final boolean hidden;

        private WidgetSnapshot(String text, String name, String[] actions, boolean hidden)
        {
            this.text = text;
            this.name = name;
            this.actions = actions;
            this.hidden = hidden;
        }

        private static WidgetSnapshot from(Widget widget)
        {
            String[] actions = widget.getActions();
            return new WidgetSnapshot(
                widget.getText(),
                widget.getName(),
                actions == null ? null : actions.clone(),
                widget.isSelfHidden()
            );
        }

        private void restore(Widget widget)
        {
            if (!Objects.equals(text, widget.getText()))
            {
                widget.setText(text == null ? "" : text);
            }

            if (!Objects.equals(name, widget.getName()))
            {
                widget.setName(name == null ? "" : name);
            }

            widget.clearActions();
            if (actions != null)
            {
                for (int i = 0; i < actions.length; i++)
                {
                    if (actions[i] != null)
                    {
                        widget.setAction(i, actions[i]);
                    }
                }
            }

            if (widget.isSelfHidden() != hidden)
            {
                widget.setHidden(hidden);
            }
        }
    }
}
