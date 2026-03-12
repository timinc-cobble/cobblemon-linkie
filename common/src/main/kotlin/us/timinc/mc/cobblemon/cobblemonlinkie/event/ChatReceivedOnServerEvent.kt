package us.timinc.mc.cobblemon.cobblemonlinkie.event

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

open class ChatReceivedOnServerEvent(
    open val sender: ServerPlayer?,
    open var message: Component,
) {
    class WithPlayer(
        private val parent: ChatReceivedOnServerEvent,
        override val sender: ServerPlayer,
    ) : ChatReceivedOnServerEvent(sender, parent.message) {
        override var message: Component
            get() = parent.message
            set(value) {
                parent.message = value
            }
    }

    fun withPlayer() = WithPlayer(this, sender!!)
}
