package com.yitianys.BlockZ.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@SuppressWarnings({"deprecation", "removal"})
public class NetworkHandler {
    public static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("blockz", "channel"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    public static void init() {
        int id = 0;
        CHANNEL.messageBuilder(LootPickupC2S.class, id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(LootPickupC2S::encode)
                .decoder(LootPickupC2S::decode)
                .consumerMainThread(LootPickupC2S::handle)
                .add();
        id++;
        CHANNEL.messageBuilder(OpenDayZMenuC2S.class, id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenDayZMenuC2S::encode)
                .decoder(OpenDayZMenuC2S::decode)
                .consumerMainThread(OpenDayZMenuC2S::handle)
                .add();
        id++;
        CHANNEL.messageBuilder(OpenDayZContainerC2S.class, id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenDayZContainerC2S::encode)
                .decoder(OpenDayZContainerC2S::decode)
                .consumerMainThread(OpenDayZContainerC2S::handle)
                .add();
        id++;
        CHANNEL.messageBuilder(SyncBackpackS2C.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncBackpackS2C::encode)
                .decoder(SyncBackpackS2C::decode)
                .consumerMainThread(SyncBackpackS2C::handle)
                .add();
        id++;
        CHANNEL.messageBuilder(RotateItemC2S.class, id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RotateItemC2S::encode)
                .decoder(RotateItemC2S::decode)
                .consumerMainThread(RotateItemC2S::handle)
                .add();
        id++;
        CHANNEL.messageBuilder(DayzToggleRequestC2S.class, id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DayzToggleRequestC2S::encode)
                .decoder(DayzToggleRequestC2S::decode)
                .consumerMainThread(DayzToggleRequestC2S::handle)
                .add();
        id++;
        CHANNEL.messageBuilder(DayzToggleStateS2C.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DayzToggleStateS2C::encode)
                .decoder(DayzToggleStateS2C::decode)
                .consumerMainThread(DayzToggleStateS2C::handle)
                .add();
        id++;
        CHANNEL.messageBuilder(DayzTogglePermissionS2C.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DayzTogglePermissionS2C::encode)
                .decoder(DayzTogglePermissionS2C::decode)
                .consumerMainThread(DayzTogglePermissionS2C::handle)
                .add();
        id++;
        CHANNEL.messageBuilder(PacketReloadConfigS2C.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketReloadConfigS2C::toBytes)
                .decoder(PacketReloadConfigS2C::new)
                .consumerMainThread(PacketReloadConfigS2C::handle)
                .add();
        id++;
        CHANNEL.messageBuilder(SyncGridRulesS2C.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncGridRulesS2C::encode)
                .decoder(SyncGridRulesS2C::new)
                .consumerMainThread(SyncGridRulesS2C::handle)
                .add();
        id++;
        CHANNEL.messageBuilder(SyncPlayerStatusS2C.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPlayerStatusS2C::encode)
                .decoder(SyncPlayerStatusS2C::decode)
                .consumerMainThread(SyncPlayerStatusS2C::handle)
                .add();
        id++;
        CHANNEL.messageBuilder(RequestSwitchToDayZMenuC2S.class, id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestSwitchToDayZMenuC2S::encode)
                .decoder(RequestSwitchToDayZMenuC2S::decode)
                .consumerMainThread(RequestSwitchToDayZMenuC2S::handle)
                .add();
        id++;
        CHANNEL.messageBuilder(SyncConfigS2C.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncConfigS2C::encode)
                .decoder(SyncConfigS2C::new)
                .consumerMainThread(SyncConfigS2C::handle)
                .add();
        id++;
        CHANNEL.messageBuilder(LeanUpdateC2S.class, id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(LeanUpdateC2S::encode)
                .decoder(LeanUpdateC2S::decode)
                .consumerMainThread(LeanUpdateC2S::handle)
                .add();
        id++;
        CHANNEL.messageBuilder(LeanSyncS2C.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LeanSyncS2C::encode)
                .decoder(LeanSyncS2C::decode)
                .consumerMainThread(LeanSyncS2C::handle)
                .add();
        id++;
        CHANNEL.messageBuilder(ProneUpdateC2S.class, id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ProneUpdateC2S::encode)
                .decoder(ProneUpdateC2S::decode)
                .consumerMainThread(ProneUpdateC2S::handle)
                .add();
        id++;
        CHANNEL.messageBuilder(ProneSyncS2C.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ProneSyncS2C::encode)
                .decoder(ProneSyncS2C::decode)
                .consumerMainThread(ProneSyncS2C::handle)
                .add();
        id++;
    }
}
