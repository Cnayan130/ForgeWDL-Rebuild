package top.principlecreativity.fgwdlrb.common.key;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import top.principlecreativity.fgwdlrb.common.WorldDownloader;

public class WDLKeybinding {
    public static final KeyBinding DOWNLOAD_HOTKEY = new KeyBinding("key.my_mod.hotkey_1", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, Keyboard.KEY_L, "key.category.fgwdlrb");

    // 注册快捷键。
    // 没有调用的时间限制，但建议在 FMLInitializationEvent 发布时调用。
    public static void init() {
        ClientRegistry.registerKeyBinding(DOWNLOAD_HOTKEY);
    }

    @SubscribeEvent
    public static void onKeyPressed(InputEvent.KeyInputEvent event) {
        if (DOWNLOAD_HOTKEY.isPressed()) {
            WorldDownloader.startDownload();
        }
    }
}
