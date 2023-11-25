package me.Thelnfamous1.clone_army;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Collection;

public class EntityToggleRemoteArgument implements ArgumentType<ResourceLocation> {
   private static final Collection<String> EXAMPLES = Arrays.asList("minecraft:pig", "cow");
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_ENTITY = new DynamicCommandExceptionType((o) -> {
      return Component.translatable("entity.notFound", o);
   });

   public static EntityToggleRemoteArgument id() {
      return new EntityToggleRemoteArgument();
   }

   public static ResourceLocation getSummonableEntity(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      return verifyCanSummon(pContext.getArgument(pName, ResourceLocation.class));
   }

   private static ResourceLocation verifyCanSummon(ResourceLocation pId) throws CommandSyntaxException {
      Registry.ENTITY_TYPE.getOptional(pId).filter(CloneArmy::isCloneType).orElseThrow(() -> ERROR_UNKNOWN_ENTITY.create(pId));
      return pId;
   }

   public ResourceLocation parse(StringReader pReader) throws CommandSyntaxException {
      return verifyCanSummon(ResourceLocation.read(pReader));
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}