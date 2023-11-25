package me.Thelnfamous1.clone_army.datagen;

import me.Thelnfamous1.clone_army.CACommands;
import me.Thelnfamous1.clone_army.CloneArmy;
import me.Thelnfamous1.clone_army.CloneRemoteItem;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = CloneArmy.MODID)
public class DatagenHandler {

    @SubscribeEvent
    static void onGatherData(GatherDataEvent event){
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new LanguageProvider(generator, CloneArmy.MODID, "en_us") {
            @Override
            protected void addTranslations() {
                this.add(CloneArmy.CLONE_REMOTE.get(), "Clone Remote");
                this.add(CloneRemoteItem.REMOTE_ENTITY_TYPE_KEY, "Clone: %s");
                this.add(CACommands.NOT_HOLDING_REMOTE_EXCEPTION_KEY, "You need to be holding a clone remote");
                this.add(CACommands.TOGGLE_REMOTE_SUCCESS_KEY, "Toggled %s to %s");
                this.add(CACommands.TOGGLE_HOSTILITY_SUCCESS_KEY, "Toggled %s hostility to %s");
                this.add(CACommands.TOGGLE_HOSTILITY_CLEAR_SUCCESS_KEY, "%ss no longer have a hostility toggle");
                this.add(CACommands.TARGET_PLAYER_SUCCESS_KEY, "%ss will now target player %s");
                this.add(CACommands.TARGET_PLAYER_CLEAR_SUCCESS_KEY, "%ss will no longer target a specific player");
                this.add(CACommands.TARGET_PLAYER_CLEAR_ALL_SUCCESS_KEY, "All mobs will no longer target specific players");
                this.add(CACommands.MOB_COMBAT_SUCCESS_KEY, "%ss will now target %ss");
                this.add(CACommands.MOB_COMBAT_CLEAR_SUCCESS_KEY, "%ss will no longer target specific mobs");
                this.add(CACommands.MOB_COMBAT_CLEAR_ALL_SUCCESS_KEY, "All mobs will no longer target specific mobs");
            }
        });

        generator.addProvider(event.includeClient(), new ItemModelProvider(generator, CloneArmy.MODID, existingFileHelper) {
            @Override
            protected void registerModels() {
                this.basicItem(CloneArmy.CLONE_REMOTE.get());
            }
        });
    }
}
