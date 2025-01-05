package top.principlecreativity.fgwdlrb.common;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenRealmsProxy;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.SaveHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class WorldDownloader {

    public static final String GITHUB_REPO = "Pokechu22/WorldDownloader";

    /**
     * Reference to the Minecraft object.
     */
    public static Minecraft minecraft;
    /**
     * Reference to the World object that WDL uses.
     */
    public static WorldClient worldClient;
    /**
     * Reference to a connection specific object. Used to detect a new
     * connection.
     */
    public static NetworkManager networkManager = null;
    /**
     * The current player. <br/>
     * In 1.7.10, a net.minecraft.client.entity.EntityClientPlayerMP was used
     * here, but now that does not exist, and it appears that the SinglePlayer
     * type is what is supposed to be used instead.
     */
    public static EntityPlayerSP thePlayer;

    /**
     * Reference to the place where all the item stacks end up after receiving
     * them.
     */
    public static Container windowContainer;
    /**
     * The block position clicked most recently.
     *
     * Needed for TileEntity creation.
     */
    public static BlockPos lastClickedBlock;
    /**
     * Last entity clicked (used for non-block tiles like minecarts with chests)
     */
    public static Entity lastEntity;

    /**
     * For player files and the level.dat file.
     */
    public static SaveHandler saveHandler;
    /**
     * For the chunks (despite the name it does also SAVE chunks)
     */

    /**
     * All tile entities that were saved manually, by chunk and then position.
     */
    public static HashMap<ChunkPos, Map<BlockPos, TileEntity>> newTileEntities = new HashMap<>();

    /**
     * All entities that were downloaded, by chunk.
     */
    public static HashMultimap<ChunkPos, Entity> newEntities = HashMultimap.create();

    /**
     * All of the {@link MapData}s that were sent to the client in the current
     * world.
     */
    public static HashMap<Integer, MapData> newMapDatas = new HashMap<>();

    public static boolean downloading = false;
    /**
     * Is this a multiworld server?
     */
    public static boolean isMultiworld = false;
    /**
     * Are there saved properties available?
     */
    public static boolean propsFound = false;
    /**
     * Automatically restart after world changes?
     */
    public static boolean startOnChange = false;
    /**
     * Whether to ignore the check as to whether a player
     * previously modified the world before downloading it.
     */
    public static boolean overrideLastModifiedCheck = false;

    /**
     * Is the world currently being saved?
     */
    public static boolean saving = false;
    /**
     * Has loading the world been delayed while the old one is being saved?
     *
     * Used when going thru portals or otherwise saving data.
     */
    public static boolean worldLoadingDeferred = false;

    // Names:
    /**
     * The current world name, if the world is multiworld.
     */
    public static String worldName = "WorldDownloaderERROR";
    /**
     * The folder in which worlds are being saved.
     */
    public static String baseFolderName = "WorldDownloaderERROR";

    // Properties:
    /**
     * Base properties, shared between each world on a multiworld server.
     */
    public static Properties baseProps;
    /**
     * Properties for a single world on a multiworld server, or all worlds
     * on a single world server.
     */
    public static Properties worldProps;
    /**
     * Default properties used for creating baseProps.  Saved and loaded;
     * shared between all servers.
     */
    public static final Properties globalProps;
    /**
     * Default properties that are used to create the global properites.
     */
    public static final Properties defaultProps;

    private static final Logger LOGGER = LogManager.getLogger();

    // Initialization:
    static {
        minecraft = Minecraft.getMinecraft();
        // Initialize the Properties template:
        defaultProps = new Properties();
        defaultProps.setProperty("ServerName", "");
        defaultProps.setProperty("WorldName", "");
        defaultProps.setProperty("LinkedWorlds", "");
        defaultProps.setProperty("Backup", "ZIP");
        defaultProps.setProperty("AllowCheats", "true");
        defaultProps.setProperty("GameType", "keep");
        defaultProps.setProperty("Time", "keep");
        defaultProps.setProperty("Weather", "keep");
        defaultProps.setProperty("MapFeatures", "false");
        defaultProps.setProperty("RandomSeed", "");
        defaultProps.setProperty("MapGenerator", "void");
        defaultProps.setProperty("GeneratorName", "flat");
        defaultProps.setProperty("GeneratorVersion", "0");
        defaultProps.setProperty("GeneratorOptions", ";0");
        defaultProps.setProperty("Spawn", "player");
        defaultProps.setProperty("SpawnX", "8");
        defaultProps.setProperty("SpawnY", "127");
        defaultProps.setProperty("SpawnZ", "8");
        defaultProps.setProperty("PlayerPos", "keep");
        defaultProps.setProperty("PlayerX", "8");
        defaultProps.setProperty("PlayerY", "127");
        defaultProps.setProperty("PlayerZ", "8");
        defaultProps.setProperty("PlayerHealth", "20");
        defaultProps.setProperty("PlayerFood", "20");

        defaultProps.setProperty("Messages.enableAll", "true");

        //Set up entities.
        defaultProps.setProperty("Entity.TrackDistanceMode", "server");

        //Don't save these entities by default -- they're problematic.
        defaultProps.setProperty("Entity.FireworksRocketEntity.Enabled", "false");
        defaultProps.setProperty("Entity.EnderDragon.Enabled", "false");
        defaultProps.setProperty("Entity.WitherBoss.Enabled", "false");
        defaultProps.setProperty("Entity.PrimedTnt.Enabled", "false");
        defaultProps.setProperty("Entity.null.Enabled", "false"); // :(

        //Groups
        defaultProps.setProperty("EntityGroup.Other.Enabled", "true");
        defaultProps.setProperty("EntityGroup.Hostile.Enabled", "true");
        defaultProps.setProperty("EntityGroup.Passive.Enabled", "true");

        //Last saved time, so that you can tell if the world was modified.
        defaultProps.setProperty("LastSaved", "-1");

        // Whether the 1-time tutorial has been shown.
        defaultProps.setProperty("TutorialShown", "false");

        globalProps = new Properties(defaultProps);

        File dataFile = new File(minecraft.mcDataDir, "WorldDownloader.txt");
        try (FileReader reader = new FileReader(dataFile)) {
            globalProps.load(reader);
        } catch (Exception e) {
            LOGGER.debug("Failed to load global properties", e);
        }
        baseProps = new Properties(globalProps);
        worldProps = new Properties(baseProps);
    }

    public static void startDownload() {
        worldClient = minecraft.world;

        if (!WDLPluginChannels.canDownloadAtAll()) {
            return;
        }

        // 假设默认不启用多世界模式
        isMultiworld = false;
        worldName = "defaultWorld"; // 设置默认世界名称
        propsFound = true; // 假设已找到配置

        worldProps = loadWorldProps(worldName);
        saveHandler = (SaveHandler) minecraft.getSaveLoader().getSaveLoader(
                getWorldFolderName(worldName), true);

        long lastSaved = Long.parseLong(worldProps.getProperty("LastSaved", "-1"));
        long lastPlayed;
        File levelDatFile = new File(saveHandler.getWorldDirectory(), "level.dat");
        try (FileInputStream stream = new FileInputStream(levelDatFile)) {
            NBTTagCompound compound = CompressedStreamTools.readCompressed(stream);
            lastPlayed = compound.getCompoundTag("Data").getLong("LastPlayed");
        } catch (Exception e) {
            LOGGER.warn("Error while checking if the map has been played and " +
                    "needs to be backed up: ", e);
            lastPlayed = -1;
        }
        if (!overrideLastModifiedCheck && lastPlayed > lastSaved) {
            // 如果检测到冲突，直接取消下载
            cancelDownload();
            return;
        }

        runSanityCheck();

        custom_ChunkRenderer.setState(true);

        WorldDownloader.minecraft.displayGuiScreen(null);
        WorldDownloader.minecraft.setIngameFocus();
        newTileEntities = new HashMap<>();
        newEntities = HashMultimap.create();
        newMapDatas = new HashMap<>();

        if (baseProps.getProperty("ServerName").isEmpty()) {
            baseProps.setProperty("ServerName", getServerName());
        }

        startOnChange = true;
        downloading = true;
        WDLMessages.chatMessageTranslated(WDLMessageTypes.INFO,
                "wdl.messages.generalInfo.downloadStarted");
    }

    public static boolean isSpigot() {
        //getClientBrand() returns the server brand; blame MCP.
        if (thePlayer != null) {
            thePlayer.getServerBrand();
            return thePlayer.getServerBrand().toLowerCase().contains("spigot");
        }
        return false;
    }

    public static void cancelDownload() {
        boolean wasDownloading = downloading;

        if (wasDownloading) {
            minecraft.getSaveLoader().flushCache();
            saveHandler.flush();
            startOnChange = false;
            saving = false;
            downloading = false;
            worldLoadingDeferred = false;

            WDLMessages.chatMessageTranslated(WDLMessageTypes.INFO,
                    "wdl.messages.generalInfo.downloadCanceled");
        }
    }

    private static void runSanityCheck() {
        Map<SanityCheck, Exception> failures = Maps.newEnumMap(SanityCheck.class);

        for (SanityCheck check : SanityCheck.values()) {
            try {
                LOGGER.trace("Running {}", check);
                check.run();
            } catch (Exception ex) {
                LOGGER.trace("{} failed", check, ex);
                failures.put(check, ex);
            }
        }
        if (!failures.isEmpty()) {
            WDLMessages.chatMessageTranslated(WDLMessageTypes.ERROR, "wdl.sanity.failed");
            for (Map.Entry<SanityCheck, Exception> failure : failures.entrySet()) {
                WDLMessages.chatMessageTranslated(WDLMessageTypes.ERROR, failure.getKey().errorMessage, failure.getValue());
            }
            if (failures.containsKey(SanityCheck.TRANSLATION)) {
                // Err, we can't put translated stuff into chat.  So redo those messages, without translation.
                // For obvious reasons these messages aren't translated.
                WDLMessages.chatMessage(WDLMessageTypes.ERROR, "----- SANITY CHECKS FAILED! -----");
                for (Map.Entry<SanityCheck, Exception> failure : failures.entrySet()) {
                    WDLMessages.chatMessage(WDLMessageTypes.ERROR, failure.getKey() + ": " + failure.getValue());
                }
                WDLMessages.chatMessage(WDLMessageTypes.ERROR, "Please check the log for more info.");
            }
        }
    }

    public static Properties loadWorldProps(String theWorldName) {
        Properties ret = new Properties(baseProps);

        if (theWorldName.isEmpty()) {
            return ret;
        }

        File savesDir = new File(minecraft.mcDataDir, "saves");

        String folder = getWorldFolderName(theWorldName);
        File worldFolder = new File(savesDir, folder);
        File dataFile = new File(worldFolder, "WorldDownloader.txt");

        try (FileReader reader = new FileReader(dataFile)) {
            ret.load(reader);

            return ret;
        } catch (Exception e) {
            LOGGER.debug("Failed to load world props for " + worldName, e);
            return ret;
        }
    }

    public static String getWorldFolderName(String theWorldName) {
        if (theWorldName.isEmpty()) {
            return baseFolderName;
        } else {
            return baseFolderName + " - " + theWorldName;
        }
    }

    public static String getServerName() {
        try {
            if (minecraft.getCurrentServerData() != null) {
                String name = minecraft.getCurrentServerData().serverName;

                if (name.equals(I18n.format("selectServer.defaultName"))) {
                    // Direct connection using domain name or IP (and port)
                    name = minecraft.getCurrentServerData().serverIP;
                }

                return name;
            } else if (minecraft.isConnectedToRealms()) {
                String realmName = getRealmName();
                if (realmName != null) {
                    return realmName;
                } else {
                    LOGGER.warn("getServerName: getRealmName returned null!");
                }
            } else {
                LOGGER.warn("getServerName: Not connected to either a real server or realms!");
            }
        } catch (Exception e) {
            LOGGER.warn("Exception while getting server name: ", e);
        }

        return "Unidentified Server";
    }

    @Nullable
    public static String getRealmName() {
        if (!minecraft.isConnectedToRealms()) {
            LOGGER.warn("getRealmName: Not currently connected to realms!");
        }
        // Is this the only way to get the name of the Realms server? Really Mojang?
        // If this function turns out to be a pain to update, just remove Realms support completely.
        // I doubt anyone will need this anyway since Realms support downloading the world out of the box.

        // Try to get the value of NetHandlerPlayClient.guiScreenServer:
        GuiScreen screen = ReflectionUtils.findAndGetPrivateField(minecraft.getConnection(), GuiScreen.class);

        // If it is not a GuiScreenRealmsProxy we are not using a Realms server
        if (!(screen instanceof GuiScreenRealmsProxy)) {
            LOGGER.warn("getRealmName: screen {} is not an instance of GuiScreenRealmsProxy", screen);
            return null;
        }

        // Get the proxy's RealmsScreen object
        GuiScreenRealmsProxy screenProxy = (GuiScreenRealmsProxy) screen;
        RealmsScreen rs = screenProxy.getProxy();

        // It needs to be of type RealmsMainScreen (this should always be the case)
        if (!(rs instanceof RealmsMainScreen)) {
            LOGGER.warn("getRealmName: realms screen {} (instance of {}) not an instance of RealmsMainScreen!", rs, (rs != null ? rs.getClass() : null));
            return null;
        }

        RealmsMainScreen rms = (RealmsMainScreen) rs;
        RealmsServer mcos = null;
        try {
            // Find the ID of the selected Realms server. Fortunately unobfuscated names!
            Field selectedServerId = rms.getClass().getDeclaredField("selectedServerId");
            selectedServerId.setAccessible(true);
            if (!selectedServerId.getType().equals(long.class)) {
                LOGGER.warn("getRealmName: RealmsMainScreen selectedServerId field ({}) is not of type `long` ({})!", selectedServerId, selectedServerId.getType());
                return null;
            }
            long id = selectedServerId.getLong(rms);

            // Get the McoServer instance that was selected
            Method findServer = rms.getClass().getDeclaredMethod("findServer", long.class);
            findServer.setAccessible(true);
            Object obj = findServer.invoke(rms, id);
            if (!(obj instanceof RealmsServer)) {
                LOGGER.warn("getRealmName: RealmsMainScreen findServer method ({}) returned something other than a RealmsServer! ({})", findServer, obj);
                return null;
            }
            mcos = (RealmsServer) obj;
        } catch (Exception e) {
            LOGGER.warn("getRealmName: Unexpected exception!", e);
            return null;
        }

        // Return its name. Not sure if this is the best naming scheme...
        return mcos.name;
    }

    public static void saveProps(String theWorldName, Properties theWorldProps) {
        File savesDir = new File(minecraft.mcDataDir, "saves");

        if (theWorldName.length() > 0) {
            String folder = getWorldFolderName(theWorldName);

            File worldFolder = new File(savesDir, folder);
            worldFolder.mkdirs();
            File worldPropsFile = new File(worldFolder, "WorldDownloader.txt");
            try (FileWriter writer = new FileWriter(worldPropsFile)) {
                theWorldProps.store(writer, I18n.format("wdl.props.world.title"));
            } catch (Exception e) {
                LOGGER.warn("Failed to write world props!", e);
            }
        } else if (!isMultiworld) {
            baseProps.putAll(theWorldProps);
        }

        File baseFolder = new File(savesDir, baseFolderName);
        baseFolder.mkdirs();

        File basePropsFile = new File(baseFolder, "WorldDownloader.txt");
        try (FileWriter writer = new FileWriter(basePropsFile)) {
            baseProps.store(writer, I18n.format("wdl.props.base.title"));
        } catch (Exception e) {
            LOGGER.warn("Failed to write base props!", e);
        }

        saveGlobalProps();
    }

    public static void saveGlobalProps() {
        File globalPropsFile = new File(minecraft.mcDataDir, "WorldDownloader.txt");
        try (FileWriter writer = new FileWriter(globalPropsFile)) {
            globalProps.store(writer, I18n.format("wdl.props.global.title"));
        } catch (Exception e) {
            LOGGER.warn("Failed to write globalprops!", e);
        }
    }

}
