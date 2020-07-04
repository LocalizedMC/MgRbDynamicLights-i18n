/*
 * Copyright © 2020 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package me.lambdaurora.lambdynlights;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Represents a utility class for compatibility.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public final class LambDynLightsCompat
{
    /**
     * Returns whether Sodium is installed.
     *
     * @return True if Sodium is installed, else false.
     */
    public static boolean isSodiumInstalled()
    {
        return FabricLoader.getInstance().isModLoaded("sodium");
    }
}