package top.principlecreativity.fgwdlrb;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Main.MOD_ID, name = Main.NAME, version = Main.VERSION, useMetadata = true)
public enum Main {
    INSTANCE;
    public static final String MOD_ID = "fgwdlrb";
    public static final String NAME = "ForgeWDL Rebuild";
    public static final String VERSION = "0.0.1";

    @Mod.InstanceFactory
    public static Main getInstance() {
        return INSTANCE;
    }

    @Mod.EventHandler
    public void preLoad(FMLPreInitializationEvent event) {

    }

    @Mod.EventHandler
    public void load(FMLInitializationEvent event) {

    }
}
