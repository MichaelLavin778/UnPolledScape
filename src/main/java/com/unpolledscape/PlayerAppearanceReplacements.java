package com.unpolledscape;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.kit.KitType;

/**
 * Hides selected worn items on player characters (self and others) while keeping the underlying
 * body visible.
 *
 * A player's {@link PlayerComposition#getEquipmentIds()} entry for a slot is either {@code 0}
 * (nothing), a kit ({@link PlayerComposition#KIT_OFFSET}..{@link PlayerComposition#ITEM_OFFSET})
 * or an item (&gt;= {@link PlayerComposition#ITEM_OFFSET}). For equipment-only slots (head, cape,
 * amulet, weapon, ...) simply clearing the slot to {@code 0} hides the item. For body slots such
 * as the torso, the item replaces the base body kit and the client no longer knows the character's
 * real body model, so clearing it to {@code 0} would make the torso (and its sleeve-covered arms)
 * disappear entirely. To avoid that we substitute a plain default body kit for the affected
 * gender, leaving a visible body with no item.
 *
 * IMPORTANT: mutates live composition state and MUST only be invoked on the client thread.
 */
final class PlayerAppearanceReplacements
{
    private static final int FEMALE_GENDER = 1;

    // Plain "naked" default body kit ids used to reveal a body when a covering item is hidden.
    // Values taken from OSRS character-creation kit data (TorsoKit.PLAIN / ArmsKit.REGULAR).
    private static final int MALE_TORSO_KIT = 18;
    private static final int FEMALE_TORSO_KIT = 56;
    private static final int MALE_ARMS_KIT = 26;
    private static final int FEMALE_ARMS_KIT = 61;

    private final Map<PlayerComposition, CompositionOverrideState> compositionOverrides = new IdentityHashMap<>();
    private final Map<PlayerComposition, Integer> femaleJawSnapshots = new IdentityHashMap<>();
    private final Set<Integer> femaleBeardJawEquipmentIds = new HashSet<>();
    private Integer femaleNoneJawEquipmentId;

    void apply(Client client, Map<Integer, Integer> replacements)
    {
        reapplyEquipmentHiding(client, replacements.keySet());
        applyFemaleBeardSuppression(client);
    }

    void restore(Client client, Map<Integer, Integer> replacements)
    {
        restoreHiddenEquipment();
        restoreFemaleBeards();
    }

    /**
     * Reverts any previous overrides using the current live values, then re-hides from scratch.
     * Player appearance is rebuilt by the game whenever it changes, so recomputing on every
     * {@code PlayerChanged} keeps the overrides in sync without leaving stale state behind.
     */
    private void reapplyEquipmentHiding(Client client, Set<Integer> itemsToHide)
    {
        restoreHiddenEquipment();

        if (itemsToHide.isEmpty())
        {
            return;
        }

        hideEquipment(client, itemsToHide);
    }

    private void hideEquipment(Client client, Set<Integer> itemsToHide)
    {
        int torsoIndex = KitType.TORSO.getIndex();
        int armsIndex = KitType.ARMS.getIndex();

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

            int gender = composition.getGender();
            CompositionOverrideState state = null;
            boolean changed = false;

            for (int slot = 0; slot < equipment.length; slot++)
            {
                if (!isHiddenItem(equipment[slot], itemsToHide))
                {
                    continue;
                }

                if (state == null)
                {
                    state = compositionOverrides.computeIfAbsent(composition, c -> new CompositionOverrideState());
                }

                state.originalBySlot.put(slot, equipment[slot]);
                equipment[slot] = hiddenValueForSlot(slot, gender);
                changed = true;

                // A hidden torso item usually also covered the arms, leaving the arms slot empty.
                // Reveal a default arms kit so the freshly shown body isn't missing its arms.
                if (slot == torsoIndex
                    && armsIndex >= 0
                    && armsIndex < equipment.length
                    && equipment[armsIndex] == 0)
                {
                    state.originalBySlot.put(armsIndex, equipment[armsIndex]);
                    equipment[armsIndex] = hiddenValueForSlot(armsIndex, gender);
                }
            }

            if (changed)
            {
                composition.setHash();
            }
        }
    }

    private void restoreHiddenEquipment()
    {
        for (Entry<PlayerComposition, CompositionOverrideState> entry : compositionOverrides.entrySet())
        {
            PlayerComposition composition = entry.getKey();
            int[] equipment = composition.getEquipmentIds();
            if (equipment == null)
            {
                continue;
            }

            int gender = composition.getGender();
            boolean changed = false;

            for (Entry<Integer, Integer> slotEntry : entry.getValue().originalBySlot.entrySet())
            {
                int slot = slotEntry.getKey();
                if (slot < 0 || slot >= equipment.length)
                {
                    continue;
                }

                // Only restore slots the game hasn't since changed on its own, i.e. that still
                // hold the value we substituted.
                if (equipment[slot] == hiddenValueForSlot(slot, gender))
                {
                    equipment[slot] = slotEntry.getValue();
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

    private static boolean isHiddenItem(int equipmentId, Set<Integer> itemsToHide)
    {
        if (equipmentId < PlayerComposition.ITEM_OFFSET)
        {
            return false;
        }

        return itemsToHide.contains(equipmentId - PlayerComposition.ITEM_OFFSET);
    }

    /**
     * The equipment value to substitute for a hidden item. Body slots get a plain default kit so
     * the body stays visible; every other slot is cleared to nothing.
     */
    private static int hiddenValueForSlot(int slot, int gender)
    {
        if (slot == KitType.TORSO.getIndex())
        {
            return PlayerComposition.KIT_OFFSET + (gender == FEMALE_GENDER ? FEMALE_TORSO_KIT : MALE_TORSO_KIT);
        }

        if (slot == KitType.ARMS.getIndex())
        {
            return PlayerComposition.KIT_OFFSET + (gender == FEMALE_GENDER ? FEMALE_ARMS_KIT : MALE_ARMS_KIT);
        }

        return 0;
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

                // The column stores a raw kit id; a player's equipment slot holds it offset by
                // KIT_OFFSET, so always add the offset to match against getEquipmentIds().
                int equipmentId = idkit + PlayerComposition.KIT_OFFSET;
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
