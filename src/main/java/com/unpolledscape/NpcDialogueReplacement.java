package com.unpolledscape;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;

final class NpcDialogueReplacement
{
    void replaceDialogueWidgets(Client client, int[] widgetIds)
    {
        Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int widgetId : widgetIds)
        {
            replaceWidgetText(client.getWidget(widgetId), visited);
        }
    }

    private void replaceWidgetText(Widget widget, Set<Widget> visited)
    {
        if (widget == null || !visited.add(widget))
        {
            return;
        }

        String text = widget.getText();
        if (text != null && !text.isEmpty())
        {
            String replacement = NpcReplacements.restoreNpcText(text);
            if (!replacement.equals(text))
            {
                widget.setText(replacement);
            }
        }

        replaceWidgetText(widget.getStaticChildren(), visited);
        replaceWidgetText(widget.getDynamicChildren(), visited);
        replaceWidgetText(widget.getNestedChildren(), visited);
    }

    private void replaceWidgetText(Widget[] widgets, Set<Widget> visited)
    {
        if (widgets == null)
        {
            return;
        }

        for (Widget widget : widgets)
        {
            replaceWidgetText(widget, visited);
        }
    }
}