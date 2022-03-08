package ga.rubydesic.dmd.game

import ga.rubydesic.dmd.analytics.Analytics
import ga.rubydesic.dmd.config
import ga.rubydesic.dmd.download.MusicCache
import ga.rubydesic.dmd.download.MusicSource
import ga.rubydesic.dmd.log
import ga.rubydesic.dmd.util.component1
import ga.rubydesic.dmd.util.component2
import ga.rubydesic.dmd.util.component3
import ga.rubydesic.dmd.util.squared
import io.netty.buffer.Unpooled
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.minecraft.block.Blocks
import net.minecraft.block.JukeboxBlock
import net.minecraft.item.Item
import net.minecraft.item.ItemUsageContext
import net.minecraft.network.PacketByteBuf
import net.minecraft.stat.Stats
import net.minecraft.util.ActionResult

class DynamicRecordItem(properties: Item.Settings?) : Item(properties) {

    private fun playSound(ctx: ItemUsageContext) {
        val item = ctx.stack
        val name = run {
            val d = item.name.string
            d.substring(1, d.length - 1)
        }

        val server = ctx.player?.server

        if (server == null) {
            log.error("Couldn't find a server object for player who used the music disc??")
            return
        }

        GlobalScope.launch(server.asCoroutineDispatcher()) {
            Analytics.event("Search", false, name)
            val id = MusicCache.searchYt(name)

            if (id == null) {
                log.info("Could not find a result for the search: $name")
                return@launch
            }

            val pos = ctx.blockPos!!
            val (x, y, z) = pos
            val maxDistSq = config.attenuationDistance.squared()
            server.playerManager.playerList.forEach { player ->
                val playerDistSq = player.squaredDistanceTo(x.toDouble(), y.toDouble(), z.toDouble())
                if (playerDistSq < maxDistSq) {
                    val data = PacketByteBuf(Unpooled.buffer())
                    ClientboundPlayMusicPacket(MusicSource.YOUTUBE, pos, id).write(data)
                    ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, ClientboundPlayMusicPacket.packetId, data)
                }
            }
        }
    }

    override fun useOnBlock(ctx: ItemUsageContext): ActionResult? {
        val level = ctx.world
        val blockPos = ctx.blockPos
        val blockState = level.getBlockState(blockPos)
        return if (blockState.block === (Blocks.JUKEBOX) && !blockState.get(JukeboxBlock.HAS_RECORD)) {
            val itemStack = ctx.stack
            if (!level.isClient) {
                (Blocks.JUKEBOX as JukeboxBlock).setRecord(level, blockPos, blockState, itemStack)

                playSound(ctx)

                itemStack.count--
                val player = ctx.player
                player?.incrementStat(Stats.PLAY_RECORD)
            }
            Analytics.event("Use Disc", level.isClient)
            ActionResult.success(level.isClient)
        } else {
            ActionResult.PASS
        }
    }

}
