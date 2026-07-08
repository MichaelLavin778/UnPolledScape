package com.unpolledscape;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashSet;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.kit.KitType;

final class PlayerAppearanceReplacements
{
    private static final int FEMALE_GENDER = 1;

    private final Map<PlayerComposition, CompositionOverrideState> compositionOverrides = new IdentityHashMap<>();
    private final Map<PlayerComposition, Integer> femaleJawSnapshots = new IdentityHashMap<>();
    private final Set<Integer> femaleBeardJawEquipmentIds = new HashSet<>();
    private Integer femaleNoneJawEquipmentId;

    void apply(Client client, Map<Integer, Integer> replacements)
    {
        applyPlayerEquipmentOverrides(client, replacements);
        applyFemaleBeardSuppression(client);
    }

    void restore(Client client, Map<Integer, Integer> replacements)
    {
        restorePlayerEquipmentOverrides(replacements);
        restoreFemaleBeards();
    }

    private void applyPlayerEquipmentOverrides(Client client, Map<Integer, Integer> replacements)
    {
        if (replacements.isEmpty())
        {
            return;
        }

        for (Player player : client.getTopLevelWorldView().players())
        {
            if (player == null)
            {
                continue;
            }

            PlayerComposition composition = player.getPlayerComposition();
            if (composition == null)
            {
                continue;
            }

            int[] equipment = composition.getEquipmentIds();
            if (equipment == null)
            {
                continue;
            }

            CompositionOverrideState state = compositionOverrides.computeIfAbsent(composition, c -> new CompositionOverrideState());
            boolean changed = false;

            for (int slot = 0; slot < equipment.length; slot++)
            {
                int current = equipment[slot];
                Integer replacementEquipmentId = toReplacementEquipmentId(current, replacements);
                if (replacementEquipmentId != null)
                {
                    state.originalBySlot.putIfAbsent(slot, current);
                    if (current != replacementEquipmentId)
                    {
                        equipment[slot] = replacementEquipmentId;
                        changed = true;
                    }

                    continue;
                }

                Integer original = state.originalBySlot.get(slot);
                if (original == null)
                {
                    continue;
                }

                Integer expectedReplacement = toReplacementEquipmentId(original, replacements);
                if (expectedReplacement == null || current != expectedReplacement)
                {
                    state.originalBySlot.remove(slot);
                }
            }

            if (state.originalBySlot.isEmpty())
            {
                compositionOverrides.remove(composition);
            }

            if (changed)
            {
                composition.setHash();
            }
        }
    }

    private void restorePlayerEquipmentOverrides(Map<Integer, Integer> replacements)
    {
        for (Entry<PlayerComposition, CompositionOverrideState> entry : compositionOverrides.entrySet())
        {
            PlayerComposition composition = entry.getKey();
            CompositionOverrideState state = entry.getValue();
            int[] equipment = composition.getEquipmentIds();
            if (equipment == null)
            {
                continue;
            }

            boolean changed = false;
            for (Entry<Integer, Integer> slotEntry : state.originalBySlot.entrySet())
            {
                int slot = slotEntry.getKey();
                if (slot < 0 || slot >= equipment.length)
                {
                    continue;
                }

                int original = slotEntry.getValue();
                Integer expectedReplacement = toReplacementEquipmentId(original, replacements);
                if (expectedReplacement != null && equipment[slot] == expectedReplacement)
                {
                    equipment[slot] = original;
                    changed = true;
                }
            }

            if (changed)
            {
                composition.setHash();
            }
        }

        compositionOverrides.clear();
    }

    private static Integer toReplacementEquipmentId(int equipmentId, Map<Integer, Integer> replacements)
    {
        if (equipmentId < PlayerComposition.ITEM_OFFSET)
        {
            return null;
        }

        int itemId = equipmentId - PlayerComposition.ITEM_OFFSET;
        Integer replacementItemId = replacements.get(itemId);
        if (replacementItemId == null)
        {
            return null;
        }

        return replacementItemId + PlayerComposition.ITEM_OFFSET;
    }

    private void applyFemaleBeardSuppression(Client client)
    {
        Integer femaleNoneJaw = resolveFemaleNoneJawEquipmentId(client);
        if (femaleNoneJaw == null)
        {
            return;
        }

        for (Player player : client.getTopLevelWorldView().players())
        {
            if (player == null)
            {
                continue;
            }

            PlayerComposition composition = player.getPlayerComposition();
            if (composition == null || composition.getGender() != FEMALE_GENDER)
            {
                continue;
            }

            int[] equipment = composition.getEquipmentIds();
            int jawIndex = KitType.JAW.getIndex();
            if (equipment == null || jawIndex < 0 || jawIndex >= equipment.length)
            {
                continue;
            }

            int jaw = equipment[jawIndex];
            if (!femaleBeardJawEquipmentIds.contains(jaw) || jaw == femaleNoneJaw)
            {
                continue;
            }

            femaleJawSnapshots.putIfAbsent(composition, jaw);
            equipment[jawIndex] = femaleNoneJaw;
            composition.setHash();
        }
    }

    private Integer resolveFemaleNoneJawEquipmentId(Client client)
    {
        if (femaleNoneJawEquipmentId != null)
        {
            return femaleNoneJawEquipmentId;
        }

        femaleBeardJawEquipmentIds.clear();
        try
        {
            for (int row : client.getDBTableRows(DBTableID.FacialHairStyles.ID))
            {
                Object[] values = client.getDBTableField(
                    row,
                    DBTableID.FacialHairStyles.COL_PLAYER_KIT_ID_TYPE_B,
                    0);
                if (values.length == 0 || !(values[0] instanceof Number))
                {
                    continue;
                }

                int idkit = ((Number) values[0]).intValue();
                if (idkit < 0)
                {
                    continue;
                }

                int equipmentId = idkit >= PlayerComposition.KIT_OFFSET ? idkit : idkit + PlayerComposition.KIT_OFFSET;
                if (row == DBTableID.FacialHairStyles.Row.NONE_FACIAL_HAIR)
                {
                    femaleNoneJawEquipmentId = equipmentId;
                }
                else
                {
                    femaleBeardJawEquipmentIds.add(equipmentId);
                }
            }

            return femaleNoneJawEquipmentId;
        }
        catch (RuntimeException ex)
        {
            return null;
        }
    }

    private void restoreFemaleBeards()
    {
        int jawIndex = KitType.JAW.getIndex();
        for (Map.Entry<PlayerComposition, Integer> entry : femaleJawSnapshots.entrySet())
        {
            PlayerComposition composition = entry.getKey();
            int[] equipment = composition.getEquipmentIds();
            if (equipment == null || jawIndex < 0 || jawIndex >= equipment.length)
            {
                continue;
            }

            int originalJaw = entry.getValue();
            if (equipment[jawIndex] != originalJaw)
            {
                equipment[jawIndex] = originalJaw;
                composition.setHash();
            }
        }

        femaleJawSnapshots.clear();
    }

    private static final class CompositionOverrideState
    {
        private final Map<Integer, Integer> originalBySlot = new HashMap<>();
    }
}
