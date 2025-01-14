/*
 * Copyright © 2020 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.api.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.lambdaurora.lambdynlights.LambDynLights;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Represents an item light source.
 *
 * @author LambdAurora
 * @version 2.1.0
 * @since 1.3.0
 */
public abstract class ItemLightSource {
	private final ResourceLocation id;
	private final Item item;
	private final boolean waterSensitive;

	public ItemLightSource(ResourceLocation id, Item item, boolean waterSensitive) {
		this.id = id;
		this.item = item;
		this.waterSensitive = waterSensitive;
	}

	public ItemLightSource(ResourceLocation id, Item item) {
		this(id, item, false);
	}

	public ResourceLocation id() {
		return this.id;
	}

	public Item item() {
		return this.item;
	}

	public boolean waterSensitive() {
		return this.waterSensitive;
	}

	/**
	 * Gets the luminance of the item.
	 *
	 * @param stack the item stack
	 * @param submergedInWater {@code true} if submerged in water, else {@code false}.
	 * @return the luminance value between {@code 0} and {@code 15}
	 */
	public int getLuminance(ItemStack stack, boolean submergedInWater) {
		if (this.waterSensitive() && submergedInWater)
			return 0; // Don't emit light with water sensitive items while submerged in water.

		return this.getLuminance(stack);
	}

	/**
	 * Gets the luminance of the item.
	 *
	 * @param stack the item stack
	 * @return the luminance value between {@code 0} and {@code 15}
	 */
	public abstract int getLuminance(ItemStack stack);

	@Override
	public String toString() {
		return "ItemLightSource{" +
				"id=" + this.id() +
				"item=" + this.item() +
				", water_sensitive=" + this.waterSensitive() +
				'}';
	}

	public static @NotNull Optional<ItemLightSource> fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
		if (!json.has("item") || !json.has("luminance")) {
			LambDynLights.get().warn("Failed to parse item light source \"" + id + "\", invalid format: missing required fields.");
			return Optional.empty();
		}

		var affectId = new ResourceLocation(json.get("item").getAsString());
		var item = Registry.ITEM.get(affectId);

		if (item == Items.AIR)
			return Optional.empty();

		boolean waterSensitive = false;
		if (json.has("water_sensitive"))
			waterSensitive = json.get("water_sensitive").getAsBoolean();

		var luminanceElement = json.get("luminance").getAsJsonPrimitive();
		if (luminanceElement.isNumber()) {
			return Optional.of(new StaticItemLightSource(id, item, luminanceElement.getAsInt(), waterSensitive));
		} else if (luminanceElement.isString()) {
			var luminanceStr = luminanceElement.getAsString();
			if (luminanceStr.equals("block")) {
				if (item instanceof BlockItem blockItem) {
					return Optional.of(new BlockItemLightSource(id, item, blockItem.getBlock().defaultBlockState(), waterSensitive));
				}
			} else {
				var blockId = ResourceLocation.tryParse(luminanceStr);
				if (blockId != null) {
					var block = Registry.BLOCK.get(blockId);
					if (block != Blocks.AIR)
						return Optional.of(new BlockItemLightSource(id, item, block.defaultBlockState(), waterSensitive));
				}
			}
		} else {
			LambDynLights.get().warn("Failed to parse item light source \"" + id + "\", invalid format: \"luminance\" field value isn't string or integer.");
		}

		return Optional.empty();
	}

	public static class StaticItemLightSource extends ItemLightSource {
		private final int luminance;

		public StaticItemLightSource(ResourceLocation id, Item item, int luminance, boolean waterSensitive) {
			super(id, item, waterSensitive);
			this.luminance = luminance;
		}

		public StaticItemLightSource(ResourceLocation id, Item item, int luminance) {
			super(id, item);
			this.luminance = luminance;
		}

		@Override
		public int getLuminance(ItemStack stack) {
			return this.luminance;
		}
	}

	public static class BlockItemLightSource extends ItemLightSource {
		private final BlockState mimic;

		public BlockItemLightSource(ResourceLocation id, Item item, BlockState block, boolean waterSensitive) {
			super(id, item, waterSensitive);
			this.mimic = block;
		}

		@Override
		public int getLuminance(ItemStack stack) {
			return getLuminance(stack, this.mimic);
		}

		static int getLuminance(ItemStack stack, BlockState state) {
			var nbt = stack.getTag();
			if (nbt != null) {
				var blockStateTag = nbt.getCompound("BlockStateTag");
				var stateManager = state.getBlock().getStateDefinition();

				for (var key : blockStateTag.getAllKeys()) {
					var property = stateManager.getProperty(key);
					if (property != null) {
						var value = blockStateTag.get(key).getAsString();
						state = with(state, property, value);
					}
				}
			}
			return state.getLightEmission();
		}

		private static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, String name) {
			return property.getValue(name).map(value -> state.setValue(property, value)).orElse(state);
		}
	}
}
