package us.timinc.mc.cobblemon.cobblemonlinkie

import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.reactive.EventObservable
import com.cobblemon.mod.common.api.reactive.Observable.Companion.filter
import com.cobblemon.mod.common.api.reactive.Observable.Companion.map
import com.cobblemon.mod.common.api.text.onHover
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.util.party
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import us.timinc.mc.cobblemon.cobblemonlinkie.event.ChatReceivedOnServerEvent
import us.timinc.mc.cobblemon.timcore.AbstractConfig
import us.timinc.mc.cobblemon.timcore.AbstractMod

const val MOD_ID: String = "cobblemon_linkie"

val GENDER_MAP = mapOf(
    Gender.GENDERLESS to "⚲",
    Gender.MALE to "♂",
    Gender.FEMALE to "♀"
)

val ELEMENT_MAP = mapOf(
    ElementalTypes.NORMAL to ChatFormatting.WHITE,
    ElementalTypes.FIRE to ChatFormatting.RED,
    ElementalTypes.WATER to ChatFormatting.BLUE,
    ElementalTypes.ELECTRIC to ChatFormatting.YELLOW,
    ElementalTypes.GRASS to ChatFormatting.GREEN,
    ElementalTypes.ICE to ChatFormatting.AQUA,
    ElementalTypes.FIGHTING to ChatFormatting.DARK_RED,
    ElementalTypes.POISON to ChatFormatting.DARK_PURPLE,
    ElementalTypes.GROUND to ChatFormatting.GOLD,
    ElementalTypes.FLYING to ChatFormatting.GRAY,
    ElementalTypes.PSYCHIC to ChatFormatting.LIGHT_PURPLE,
    ElementalTypes.BUG to ChatFormatting.DARK_GREEN,
    ElementalTypes.ROCK to ChatFormatting.DARK_GRAY,
    ElementalTypes.GHOST to ChatFormatting.DARK_AQUA,
    ElementalTypes.DRAGON to ChatFormatting.DARK_BLUE,
    ElementalTypes.DARK to ChatFormatting.BLACK,
    ElementalTypes.STEEL to ChatFormatting.GRAY,
    ElementalTypes.FAIRY to ChatFormatting.LIGHT_PURPLE,
)

object CobblemonLinkie : AbstractMod<CobblemonLinkie.CobblemonLinkieConfig>(MOD_ID, CobblemonLinkieConfig::class.java) {

    class CobblemonLinkieConfig : AbstractConfig() {
        val teamPattern: String = "\\[team#\\]"
    }

    object Events {
        val CHAT_RECEIVED_ON_SERVER = EventObservable<ChatReceivedOnServerEvent>()

        val PLAYER_CHAT_RECEIVED_ON_SERVER = CHAT_RECEIVED_ON_SERVER.pipe(
            filter { it.sender != null },
            map { it.withPlayer() }
        )
    }

    init {
        Events.PLAYER_CHAT_RECEIVED_ON_SERVER.subscribe { evt ->
            val teamRegex = config.teamPattern.replace("#", "([1-6])").toRegex()
            val stringMsg = evt.message.string
            if (!teamRegex.containsMatchIn(stringMsg)) return@subscribe

            val resultGroups = teamRegex.find(stringMsg)?.groups ?: return@subscribe
            val completeGroup = resultGroups[0] ?: return@subscribe
            val indexGroup = resultGroups[1] ?: return@subscribe
            val targetedIndex = indexGroup.value.toIntOrNull() ?: return@subscribe
            val targetedPokemon = evt.sender.party().get(targetedIndex - 1) ?: return@subscribe

            val baseString =
                $$"%1$s %10$s Lvl. %2$s\n%3$s\n%11$s\n%12$s\n---IVS---\nHP§0__§r %4$s | ATK§0_§r %5$s | DEF§0_§r%6$s\nSATK %7$s | SDEF %8$s | SPD§0_§r%9$s\n---EVS---\nHP§0__§r %13$s | ATK§0_§r %14$s | DEF§0_§r%15$s\nSATK %16$s | SDEF %17$s | SPD§0_§r%18$s"
            val pokemonDetail = Component.translatable(
                baseString,
                targetedPokemon.form.name.takeIf { it != "Normal" }
                    ?.let { Component.translatable($$"%1$s %2$s", it, targetedPokemon.species.translatedName) }
                    ?: targetedPokemon.species.translatedName,
                targetedPokemon.level,
                targetedPokemon.secondaryType?.let {
                    Component.translatable(
                        $$"%1$s/%2$s",
                        targetedPokemon.primaryType.displayName.copy().withStyle(ELEMENT_MAP[targetedPokemon.primaryType] ?: ChatFormatting.WHITE),
                        it.displayName.copy().withStyle(ELEMENT_MAP[it] ?: ChatFormatting.WHITE)
                    )
                } ?: targetedPokemon.primaryType.displayName.copy().withStyle(ELEMENT_MAP[targetedPokemon.primaryType] ?: ChatFormatting.WHITE),
                *(Stats.PERMANENT.map { stat ->
                    Component.literal(targetedPokemon.ivs[stat].toString().padStart(2, '0'))
                        .withStyle(
                            when(stat) {
                                targetedPokemon.nature.decreasedStat -> ChatFormatting.RED
                                targetedPokemon.nature.increasedStat -> ChatFormatting.GREEN
                                else -> ChatFormatting.GRAY
                            }
                        )
                }.toTypedArray()),
                GENDER_MAP[targetedPokemon.gender],
                Component.translatable(targetedPokemon.ability.displayName),
                Component.translatable(targetedPokemon.nature.displayName),
                *(Stats.PERMANENT.map { stat ->
                    Component.literal(targetedPokemon.evs[stat].toString().padStart(2, '0'))
                        .withStyle(
                            when(stat) {
                                targetedPokemon.nature.decreasedStat -> ChatFormatting.RED
                                targetedPokemon.nature.increasedStat -> ChatFormatting.GREEN
                                else -> ChatFormatting.GRAY
                            }
                        )
                }.toTypedArray()),
            )

            evt.message = Component.translatable(
                $$"%1$s%2$s%3$s",
                Component.literal(stringMsg.substring(0, completeGroup.range.first)),
                Component.translatable(
                    $$"[%1$s]",
                    targetedPokemon.getDisplayName(true)
                )
                    .withStyle(ChatFormatting.GOLD)
                    .onHover(pokemonDetail),
                Component.literal(stringMsg.substring(completeGroup.range.last + 1))
            )
        }
    }
}