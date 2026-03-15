package us.timinc.mc.cobblemon.cobblemonlinkie.handler

import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.text.onHover
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.PokemonStats
import com.cobblemon.mod.common.util.party
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import us.timinc.mc.cobblemon.cobblemonlinkie.CobblemonLinkie.config
import us.timinc.mc.cobblemon.cobblemonlinkie.event.ChatReceivedOnServerEvent
import us.timinc.mc.cobblemon.cobblemonlinkie.extension.withOptionalColor
import us.timinc.mc.cobblemon.timcore.AbstractHandler
import us.timinc.mc.cobblemon.timcore.PokemonMatcher

object MessageParser : AbstractHandler<ChatReceivedOnServerEvent.WithPlayer>() {
    override fun handle(evt: ChatReceivedOnServerEvent.WithPlayer) {
        val teamRegex = config.teamPattern.replace("#", "([1-6])").toRegex()
        val stringMsg = evt.message.string
        val matches = teamRegex.findAll(stringMsg).toList()
        if (matches.isEmpty()) return

        val rebuiltMessage = Component.literal("")
        var nextTextIndex = 0

        for (match in matches) {
            val completeGroup = match.groups[0] ?: continue
            val indexGroup = match.groups[1]
            val targetedPokemon = indexGroup
                ?.value
                ?.toIntOrNull()
                ?.let { evt.sender.party().get(it - 1) }

            if (completeGroup.range.first > nextTextIndex) {
                rebuiltMessage.append(
                    Component.literal(
                        stringMsg.substring(
                            nextTextIndex,
                            completeGroup.range.first
                        )
                    )
                )
            }

            val replacementComponent = if (targetedPokemon == null) {
                Component.literal(completeGroup.value)
            } else {
                val baseString = config.baseString
                val pokemonDetail = Component.translatable(
                    baseString,
                    targetedPokemon.form.name.takeIf { it != "Normal" }
                        ?.let { Component.translatable($$"%1$s %2$s", it, targetedPokemon.species.translatedName) }
                        ?: targetedPokemon.species.translatedName,
                    targetedPokemon.level,
                    targetedPokemon.secondaryType?.let {
                        Component.translatable(
                            $$"%1$s/%2$s",
                            getElementComponent(targetedPokemon.primaryType),
                            getElementComponent(it)
                        )
                    } ?: getElementComponent(targetedPokemon.primaryType),
                    config.genderMap[targetedPokemon.gender.name],
                    Component.translatable(targetedPokemon.ability.displayName),
                    Component.translatable(targetedPokemon.nature.displayName),
                    *(Stats.PERMANENT.map { getStatValueComponent(targetedPokemon.ivs, it, config.ivColors, 2) }
                        .toTypedArray()),
                    *(Stats.PERMANENT.map { getStatValueComponent(targetedPokemon.evs, it, config.evColors, 3) }
                        .toTypedArray()),
                    *(Stats.PERMANENT.map {
                        Component.translatable(config.statLabels[it.showdownId] ?: "")
                            .withOptionalColor(
                                getColor(
                                    when (it) {
                                        targetedPokemon.nature.decreasedStat -> config.negativeNatureInfluenceColor
                                        targetedPokemon.nature.increasedStat -> config.positiveNatureInfluenceColor
                                        else -> config.neutralNatureInfluenceColor
                                    }
                                )
                            )
                    }.toTypedArray()),
                    if (targetedPokemon.shiny) config.shinyBit else "",
                )

                Component.translatable(
                    $$"[%1$s]",
                    targetedPokemon.getDisplayName(true)
                )
                    .withOptionalColor(getChatColor(targetedPokemon))
                    .onHover(pokemonDetail)
            }

            rebuiltMessage.append(replacementComponent)
            nextTextIndex = completeGroup.range.last + 1
        }

        if (nextTextIndex < stringMsg.length) {
            rebuiltMessage.append(Component.literal(stringMsg.substring(nextTextIndex)))
        }

        evt.message = rebuiltMessage
    }
}

fun getChatColor(pokemon: Pokemon) = getColor(config.chatColors.firstNotNullOfOrNull { (k, v) ->
    if (PokemonMatcher.parse(k).matches(pokemon)) v else null
} ?: config.defaultChatColor)

fun getColor(color: String) = ChatFormatting.getByName(color)?.color
    ?: runCatching { color.hexToInt() }.getOrNull()

fun getElementComponent(element: ElementalType): MutableComponent =
    element.displayName.plainCopy()
        .withOptionalColor(config.elementColors[element.name]?.let(::getColor))

fun getStatValueComponent(
    stats: PokemonStats,
    stat: Stat,
    colors: Map<Int, String>,
    padding: Int,
): MutableComponent =
    stats[stat].let { statVal ->
        Component.literal(
            statVal.toString()
                .padStart(padding, '0')
        )
            .withOptionalColor(
                colors.keys.sorted().let { sortedKeys ->
                    colors[sortedKeys[(sortedKeys.indexOfFirst { it > (statVal ?: 0) }
                        .takeIf { it != -1 } ?: sortedKeys.size) - 1]]
                }?.let(::getColor)
            )
    }