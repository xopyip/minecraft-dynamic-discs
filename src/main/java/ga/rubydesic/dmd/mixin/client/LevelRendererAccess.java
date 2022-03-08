package ga.rubydesic.dmd.mixin.client;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(WorldRenderer.class)
public interface LevelRendererAccess {

    @Accessor
    Map<BlockPos, SoundInstance> getPlayingSongs();

    @Accessor
    ClientWorld getWorld();

    @Invoker
    void callUpdateEntitiesForSong(World level, BlockPos blockPos, boolean bl);
}
