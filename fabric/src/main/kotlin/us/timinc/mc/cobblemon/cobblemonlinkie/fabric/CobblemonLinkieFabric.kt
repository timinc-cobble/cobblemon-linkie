package us.timinc.mc.cobblemon.cobblemonlinkie.fabric

import us.timinc.mc.cobblemon.cobblemonlinkie.CobblemonLinkie
import us.timinc.mc.cobblemon.timcore.fabric.AbstractFabricMod
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import us.timinc.mc.cobblemon.cobblemonlinkie.event.ChatReceivedOnServerEvent

object CobblemonLinkieFabric : AbstractFabricMod(CobblemonLinkie) {
    override fun onInitialize() {
        ServerMessageDecoratorEvent.EVENT.register(
            ServerMessageDecoratorEvent.CONTENT_PHASE
        ) { sender, message ->
            val evt = ChatReceivedOnServerEvent(sender, message)
            CobblemonLinkie.Events.CHAT_RECEIVED_ON_SERVER.post(evt)
            evt.message
        }
    }
}