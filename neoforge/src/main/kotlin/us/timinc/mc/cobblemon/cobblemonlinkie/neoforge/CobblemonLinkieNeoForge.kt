package us.timinc.mc.cobblemon.cobblemonlinkie.neoforge

import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.ServerChatEvent
import us.timinc.mc.cobblemon.cobblemonlinkie.CobblemonLinkie
import us.timinc.mc.cobblemon.cobblemonlinkie.MOD_ID
import us.timinc.mc.cobblemon.cobblemonlinkie.event.ChatReceivedOnServerEvent
import us.timinc.mc.cobblemon.timcore.neoforge.AbstractNeoForgeMod

@Mod(MOD_ID)
object CobblemonLinkieNeoForge : AbstractNeoForgeMod(CobblemonLinkie) {
    init {
        with(NeoForge.EVENT_BUS) {
            addListener(::onServerChat)
        }
    }

    fun onServerChat(evt: ServerChatEvent) {
        val extEvt = ChatReceivedOnServerEvent(evt.player, evt.message)
        CobblemonLinkie.Events.CHAT_RECEIVED_ON_SERVER.post(extEvt)
        evt.message = extEvt.message
    }
}