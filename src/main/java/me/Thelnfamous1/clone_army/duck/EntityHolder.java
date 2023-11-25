package me.Thelnfamous1.clone_army.duck;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface EntityHolder {

    @Nullable Entity getHeldEntity();

    void setHeldEntity(@Nullable Entity heldEntity);
}
