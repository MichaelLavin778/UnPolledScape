package com.unpolledscape;

import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.events.GameObjectSpawned;

/**
 * Hides selected game objects (the rainbow flowers in the Pride flower field south-east of
 * Barbarian Village) by removing them from the scene as they spawn and while sweeping the loaded
 * scene. Because a removed object can only be brought back by rebuilding the scene, {@link #restore}
 * forces a scene reload once when something was actually hidden.
 *
 * IMPORTANT: mutates live scene state and MUST only be invoked on the client thread.
 */
final class GameObjectReplacements
{
    // Rainbow flower game object ids from the Pride flower field south-east of Barbarian Village.
    // Only the ids actually observed in the in-game object inspector are listed (44803 was not
    // visible, so it is omitted; the 10xxx ids in the same area are rocks/trees and left alone).
    private static final Set<Integer> HIDDEN_OBJECT_IDS = Set.of(
        44802, 44804, 44805, 44806, 44807, 44808, 44809, 44810, 44811, 44812,
        44813, 44814, 44815, 44816, 44817, 44818, 44819, 44820, 44821, 44822);

    private boolean removedAny;

    void apply(Client client)
    {
        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null)
        {
            return;
        }

        Scene scene = worldView.getScene();
        if (scene == null)
        {
            return;
        }

        Tile[][][] tiles = scene.getTiles();
        if (tiles == null)
        {
            return;
        }

        for (Tile[][] plane : tiles)
        {
            if (plane == null)
            {
                continue;
            }

            for (Tile[] column : plane)
            {
                if (column == null)
                {
                    continue;
                }

                for (Tile tile : column)
                {
                    if (tile != null)
                    {
                        removeHiddenObjects(scene, tile);
                    }
                }
            }
        }
    }

    boolean handleGameObjectSpawned(Client client, GameObjectSpawned event)
    {
        GameObject gameObject = event.getGameObject();
        if (gameObject == null || !HIDDEN_OBJECT_IDS.contains(gameObject.getId()))
        {
            return false;
        }

        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null)
        {
            return false;
        }

        worldView.getScene().removeGameObject(gameObject);
        removedAny = true;
        return true;
    }

    void restore(Client client)
    {
        if (!removedAny)
        {
            return;
        }

        removedAny = false;

        // Removed objects can only reappear by rebuilding the scene; reload the current one.
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            client.setGameState(GameState.LOADING);
        }
    }

    private void removeHiddenObjects(Scene scene, Tile tile)
    {
        GameObject[] gameObjects = tile.getGameObjects();
        if (gameObjects == null)
        {
            return;
        }

        for (GameObject gameObject : gameObjects)
        {
            if (gameObject != null && HIDDEN_OBJECT_IDS.contains(gameObject.getId()))
            {
                scene.removeGameObject(gameObject);
                removedAny = true;
            }
        }
    }
}
