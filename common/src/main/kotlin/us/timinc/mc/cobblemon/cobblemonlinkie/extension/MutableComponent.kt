package us.timinc.mc.cobblemon.cobblemonlinkie.extension

import net.minecraft.network.chat.MutableComponent

fun MutableComponent.withOptionalColor(color: Int?): MutableComponent = if (color == null) this else withColor(color)