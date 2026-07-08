package com.unpolledscape;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Locale;
import net.runelite.api.Client;
import net.runelite.api.NPCComposition;
import net.runelite.api.NodeCache;
import net.runelite.api.gameval.NpcID;

final class NpcAppearanceReplacements
{
    private static final int[] LADY_KELI_LEGACY_MODELS = {390, 456, 332, 353, 428, 358};
    private static final int[] LADY_KELI_LEGACY_CHATHEAD_MODELS = {113};
    private static final short[] LADY_KELI_LEGACY_RECOLOR_FROM = {
        (short) 25238,
        (short) 8741,
        (short) 6798
    };
    private static final short[] LADY_KELI_LEGACY_RECOLOR_TO = {
        (short) -2837,
        (short) -10342,
        (short) 6854
    };

    private LadyKeliSnapshot ladyKeliOriginal;

    boolean applyLadyKeli(Client client)
    {
        NPCComposition composition = client.getNpcDefinition(NpcID.LADY_KELI);
        if (composition == null)
        {
            return true;
        }

        if (ladyKeliOriginal == null)
        {
            ladyKeliOriginal = LadyKeliSnapshot.from(composition);
        }

        if (isLadyKeliRestored(composition))
        {
            return true;
        }

        boolean updated = replaceArrayByIdentity(
            composition,
            composition.getModels(),
            LADY_KELI_LEGACY_MODELS.clone()
        ) && replaceArrayByIdentity(
            composition,
            composition.getChatheadModels(),
            LADY_KELI_LEGACY_CHATHEAD_MODELS.clone()
        ) && replaceArrayByIdentity(
            composition,
            composition.getColorToReplace(),
            LADY_KELI_LEGACY_RECOLOR_FROM.clone()
        ) && replaceArrayByIdentity(
            composition,
            composition.getColorToReplaceWith(),
            LADY_KELI_LEGACY_RECOLOR_TO.clone()
        );

        if (!updated)
        {
            restoreLadyKeli(client);
        }
        else
        {
            resetModelCache(composition);
        }

        return updated;
    }

    void restoreLadyKeli(Client client)
    {
        if (ladyKeliOriginal == null)
        {
            return;
        }

        NPCComposition composition = client.getNpcDefinition(NpcID.LADY_KELI);
        if (composition != null)
        {
            replaceArrayByIdentity(composition, composition.getModels(), ladyKeliOriginal.models.clone());
            replaceArrayByIdentity(composition, composition.getChatheadModels(), ladyKeliOriginal.chatheadModels.clone());
            replaceArrayByIdentity(composition, composition.getColorToReplace(), ladyKeliOriginal.recolorFrom.clone());
            replaceArrayByIdentity(composition, composition.getColorToReplaceWith(), ladyKeliOriginal.recolorTo.clone());
            resetModelCache(composition);
        }

        ladyKeliOriginal = null;
    }

    private static boolean isLadyKeliRestored(NPCComposition composition)
    {
        return Arrays.equals(LADY_KELI_LEGACY_MODELS, composition.getModels())
            && Arrays.equals(LADY_KELI_LEGACY_CHATHEAD_MODELS, composition.getChatheadModels())
            && Arrays.equals(LADY_KELI_LEGACY_RECOLOR_FROM, composition.getColorToReplace())
            && Arrays.equals(LADY_KELI_LEGACY_RECOLOR_TO, composition.getColorToReplaceWith());
    }

    private static boolean replaceArrayByIdentity(Object owner, Object currentArray, Object replacementArray)
    {
        if (currentArray == null || replacementArray == null)
        {
            return false;
        }

        Class<?> arrayType = replacementArray.getClass();
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass())
        {
            for (Field field : type.getDeclaredFields())
            {
                if (!field.getType().equals(arrayType))
                {
                    continue;
                }

                try
                {
                    field.setAccessible(true);
                    if (field.get(owner) == currentArray)
                    {
                        field.set(owner, replacementArray);
                        return true;
                    }
                }
                catch (ReflectiveOperationException | RuntimeException ex)
                {
                    return false;
                }
            }
        }

        return false;
    }

    private static void resetModelCache(NPCComposition composition)
    {
        for (Class<?> type = composition.getClass(); type != null; type = type.getSuperclass())
        {
            for (Field field : type.getDeclaredFields())
            {
                if (!Modifier.isStatic(field.getModifiers())
                    || !NodeCache.class.isAssignableFrom(field.getType())
                    || !field.getName().toLowerCase(Locale.ROOT).contains("model"))
                {
                    continue;
                }

                try
                {
                    field.setAccessible(true);
                    NodeCache cache = (NodeCache) field.get(null);
                    if (cache != null)
                    {
                        cache.reset();
                    }
                }
                catch (ReflectiveOperationException | RuntimeException ex)
                {
                    return;
                }
            }
        }
    }

    private static final class LadyKeliSnapshot
    {
        private final int[] models;
        private final int[] chatheadModels;
        private final short[] recolorFrom;
        private final short[] recolorTo;

        private LadyKeliSnapshot(
            int[] models,
            int[] chatheadModels,
            short[] recolorFrom,
            short[] recolorTo
        )
        {
            this.models = models;
            this.chatheadModels = chatheadModels;
            this.recolorFrom = recolorFrom;
            this.recolorTo = recolorTo;
        }

        private static LadyKeliSnapshot from(NPCComposition composition)
        {
            return new LadyKeliSnapshot(
                copy(composition.getModels()),
                copy(composition.getChatheadModels()),
                copy(composition.getColorToReplace()),
                copy(composition.getColorToReplaceWith())
            );
        }

        private static int[] copy(int[] values)
        {
            return values == null ? new int[0] : values.clone();
        }

        private static short[] copy(short[] values)
        {
            return values == null ? new short[0] : values.clone();
        }
    }

}
