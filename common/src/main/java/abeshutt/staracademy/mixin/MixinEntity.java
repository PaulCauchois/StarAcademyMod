package abeshutt.staracademy.mixin;

import abeshutt.staracademy.StarAcademyMod;
import abeshutt.staracademy.data.adapter.Adapters;
import abeshutt.staracademy.init.ModConfigs;
import abeshutt.staracademy.init.ModWorldData;
import abeshutt.staracademy.util.ProxyEntity;
import abeshutt.staracademy.world.data.EntityState;
import abeshutt.staracademy.world.data.SafariData;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class MixinEntity implements ProxyEntity {

    @Unique private boolean inSafariPortal;
    @Unique private boolean safariPortalCooldown;

    @Shadow public abstract World getWorld();
    @Shadow public abstract UUID getUuid();

    @Shadow protected abstract void checkBlockCollision();

    @Shadow public abstract boolean hasPortalCooldown();

    @Override
    public boolean isInSafariPortal() {
        return this.inSafariPortal;
    }

    @Override
    public boolean hasSafariPortalCooldown() {
        return this.safariPortalCooldown;
    }

    @Override
    public void setInSafariPortal(boolean inSafariPortal) {
        this.inSafariPortal = inSafariPortal;
    }

    @Override
    public void setSafariPortalCooldown(boolean safariPortalCooldown) {
        this.safariPortalCooldown = safariPortalCooldown;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tickHead(CallbackInfo ci) {
        this.setInSafariPortal(false);

        if(this.hasSafariPortalCooldown() && !this.hasPortalCooldown()) {
            this.checkBlockCollision();

            if(!this.isInSafariPortal()) {
                this.setSafariPortalCooldown(false);
            }
        }
    }

    @Inject(method = "getTeleportTarget", at = @At("HEAD"), cancellable = true)
    protected void getTeleportTarget(ServerWorld destination, CallbackInfoReturnable<TeleportTarget> ci) {
        if(destination.getRegistryKey() == StarAcademyMod.SAFARI) {
            BlockPos pos = ModConfigs.SAFARI.getPlacementOffset().add(ModConfigs.SAFARI.getRelativeSpawnPosition());

            ci.setReturnValue(new TeleportTarget(new Vec3d(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D),
                    Vec3d.ZERO, ModConfigs.SAFARI.getSpawnYaw(), ModConfigs.SAFARI.getSpawnPitch()));
        } else if(this.getWorld().getRegistryKey() == StarAcademyMod.SAFARI) {
            SafariData.Entry entry = ModWorldData.SAFARI.getGlobal(this.getWorld()).get(this.getUuid()).orElseThrow();
            EntityState state = entry.getLastState();
            ci.setReturnValue(new TeleportTarget(state.getPos(),Vec3d.ZERO, state.getYaw(), state.getPitch()));
        }
    }

    @Inject(method = "writeNbt", at = @At("RETURN"))
    public void writeNbt(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> ci) {
        Adapters.BOOLEAN.writeNbt(this.inSafariPortal).ifPresent(tag -> nbt.put("inSafariPortal", tag));
        Adapters.BOOLEAN.writeNbt(this.safariPortalCooldown).ifPresent(tag -> nbt.put("safariPortalCooldown", tag));
    }

    @Inject(method = "readNbt", at = @At("RETURN"))
    public void readNbt(NbtCompound nbt, CallbackInfo ci) {
        this.inSafariPortal = Adapters.BOOLEAN.readNbt(nbt.get("inSafariPortal")).orElse(false);
        this.safariPortalCooldown = Adapters.BOOLEAN.readNbt(nbt.get("safariPortalCooldown")).orElse(false);
    }

}
