package us.timinc.mc.cobblemon.cobblemonlinkie

import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.reactive.EventObservable
import com.cobblemon.mod.common.api.reactive.Observable.Companion.filter
import com.cobblemon.mod.common.api.reactive.Observable.Companion.map
import com.cobblemon.mod.common.api.text.onHover
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.pokemon.PokemonStats
import com.cobblemon.mod.common.util.party
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import us.timinc.mc.cobblemon.cobblemonlinkie.event.ChatReceivedOnServerEvent
import us.timinc.mc.cobblemon.timcore.AbstractConfig
import us.timinc.mc.cobblemon.timcore.AbstractMod

const val MOD_ID: String = "cobblemon_linkie"

object CobblemonLinkie : AbstractMod<CobblemonLinkie.CobblemonLinkieConfig>(MOD_ID, CobblemonLinkieConfig::class.java) {

    class CobblemonLinkieConfig : AbstractConfig() {
        val teamPattern: String = "\\[team#\\]"
        val baseString: String =
            $$"%1$s %4$s Lvl. %2$s\n%3$s\n%5$s\n%6$s\n---IVS---\n%19$s %7$s | %20$s %8$s | %21$s %9$s\n%22$s %10$s | %23$s %11$s | %24$s %12$s\n---EVS---\n%19$s %13$s | %20$s %14$s | %21$s %15$s\n%22$s %16$s | %23$s %17$s | %24$s %18$s"
        val elementColors = mapOf(
            ElementalTypes.NORMAL.name to ChatFormatting.WHITE.name,
            ElementalTypes.FIRE.name to ChatFormatting.RED.name,
            ElementalTypes.WATER.name to ChatFormatting.BLUE.name,
            ElementalTypes.ELECTRIC.name to ChatFormatting.YELLOW.name,
            ElementalTypes.GRASS.name to ChatFormatting.GREEN.name,
            ElementalTypes.ICE.name to ChatFormatting.AQUA.name,
            ElementalTypes.FIGHTING.name to ChatFormatting.DARK_RED.name,
            ElementalTypes.POISON.name to ChatFormatting.DARK_PURPLE.name,
            ElementalTypes.GROUND.name to ChatFormatting.GOLD.name,
            ElementalTypes.FLYING.name to ChatFormatting.GRAY.name,
            ElementalTypes.PSYCHIC.name to ChatFormatting.LIGHT_PURPLE.name,
            ElementalTypes.BUG.name to ChatFormatting.DARK_GREEN.name,
            ElementalTypes.ROCK.name to ChatFormatting.DARK_GRAY.name,
            ElementalTypes.GHOST.name to ChatFormatting.DARK_AQUA.name,
            ElementalTypes.DRAGON.name to ChatFormatting.DARK_BLUE.name,
            ElementalTypes.DARK.name to ChatFormatting.DARK_GRAY.name,
            ElementalTypes.STEEL.name to ChatFormatting.GRAY.name,
            ElementalTypes.FAIRY.name to ChatFormatting.LIGHT_PURPLE.name,
        )
        val genderMap = mapOf(
            Gender.GENDERLESS.name to "⚲",
            Gender.MALE.name to "♂",
            Gender.FEMALE.name to "♀"
        )
        val ivColors = mapOf(
            0 to ChatFormatting.RED.name,
            10 to ChatFormatting.YELLOW.name,
            20 to ChatFormatting.GREEN.name,
            30 to ChatFormatting.AQUA.name,
            31 to ChatFormatting.LIGHT_PURPLE.name,
        )
        val evColors = mapOf(
            0 to ChatFormatting.RED.name,
            100 to ChatFormatting.YELLOW.name,
            200 to ChatFormatting.GREEN.name,
            251 to ChatFormatting.AQUA.name,
            252 to ChatFormatting.LIGHT_PURPLE.name,
        )
        val statLabels = mapOf(
            Stats.HP.showdownId to "HP§0__§r",
            Stats.ATTACK.showdownId to "ATK§0_§r",
            Stats.DEFENCE.showdownId to "DEF§0_§r",
            Stats.SPECIAL_ATTACK.showdownId to "SATK",
            Stats.SPECIAL_DEFENCE.showdownId to "SDEF",
            Stats.SPEED.showdownId to "SPD§0_§r",
        )
        val positiveNatureInfluenceColor = ChatFormatting.GREEN.name
        val negativeNatureInfluenceColor = ChatFormatting.DARK_RED.name
        val neutralNatureInfluenceColor = ChatFormatting.WHITE.name
    }

    object Events {
        val CHAT_RECEIVED_ON_SERVER = EventObservable<ChatReceivedOnServerEvent>()

        val PLAYER_CHAT_RECEIVED_ON_SERVER = CHAT_RECEIVED_ON_SERVER.pipe(
            filter { it.sender != null },
            map { it.withPlayer() }
        )
    }

    fun getElementColor(element: ElementalType) = ChatFormatting.getByName(config.elementColors[element.name])

    fun getElementComponent(element: ElementalType): MutableComponent =
        element.displayName.plainCopy().withStyle(getElementColor(element) ?: ChatFormatting.WHITE)

    fun getStatValueComponent(stats: PokemonStats, stat: Stat, colors: Map<Int, String>): MutableComponent =
        stats[stat].let { statVal ->
            Component.literal(
                statVal.toString()
                    .padStart(2, '0')
            )
                .withStyle(
                    ChatFormatting.getByName(
                        colors.keys.sorted().let { sortedKeys ->
                            colors[sortedKeys[(sortedKeys.indexOfFirst { it > (statVal ?: 0) }
                                .takeIf { it != -1 } ?: sortedKeys.size) - 1]]
                        }
                    ) ?: ChatFormatting.WHITE
                )
        }

    init {
        Events.PLAYER_CHAT_RECEIVED_ON_SERVER.subscribe { evt ->
            val teamRegex = config.teamPattern.replace("#", "([1-6])").toRegex()
            val stringMsg = evt.message.string
            val matches = teamRegex.findAll(stringMsg).toList()
            if (matches.isEmpty()) return@subscribe

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
                    rebuiltMessage.append(Component.literal(stringMsg.substring(nextTextIndex, completeGroup.range.first)))
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
                        *(Stats.PERMANENT.map { getStatValueComponent(targetedPokemon.ivs, it, config.ivColors) }
                            .toTypedArray()),
                        *(Stats.PERMANENT.map { getStatValueComponent(targetedPokemon.evs, it, config.evColors) }
                            .toTypedArray()),
                        *(Stats.PERMANENT.map {
                            Component.translatable(config.statLabels[it.showdownId] ?: "").withStyle(
                                ChatFormatting.getByName(
                                    when (it) {
                                        targetedPokemon.nature.decreasedStat -> config.negativeNatureInfluenceColor
                                        targetedPokemon.nature.increasedStat -> config.positiveNatureInfluenceColor
                                        else -> config.neutralNatureInfluenceColor
                                    }
                                ) ?: ChatFormatting.WHITE
                            )
                        }.toTypedArray())
                    )

                    Component.translatable(
                        $$"[%1$s]",
                        targetedPokemon.getDisplayName(true)
                    )
                        .withStyle(ChatFormatting.GOLD)
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
}