package com.forgeessentials.core.misc;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;

import com.forgeessentials.api.APIRegistry;
import com.forgeessentials.api.UserIdent;
import com.forgeessentials.api.permissions.FEPermissions;
import com.forgeessentials.api.permissions.GroupEntry;
import com.forgeessentials.commons.selections.WarpPoint;
import com.forgeessentials.commons.selections.WorldPoint;
import com.forgeessentials.util.PlayerInfo;

public class RespawnHandler
{

    public RespawnHandler()
    {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
    }

    public static WarpPoint getPlayerSpawn(EntityPlayer player, WarpPoint location, boolean doDefaultSpawn)
    {
        UserIdent ident = UserIdent.get(player);
        if (location == null)
            location = new WarpPoint(player);

        boolean bedEnabled = APIRegistry.perms.checkUserPermission(ident, FEPermissions.SPAWN_BED);
        if (bedEnabled)
        {
            BlockPos spawn = player.getBedLocation(player.dimension);
            if (spawn != null)
                spawn = EntityPlayer.func_180467_a(player.worldObj, spawn, true);
            if (spawn != null)
            {
                // Bed seems OK, so just return null to let default MC code handle respawn
                if (doDefaultSpawn)
                    return null;
                return new WarpPoint(player.dimension, spawn, player.cameraYaw, player.cameraPitch);
            }
        }

        String spawnProperty = APIRegistry.perms.getPermission(ident, location.toWorldPoint(), null,
                GroupEntry.toList(APIRegistry.perms.getPlayerGroups(ident)), FEPermissions.SPAWN_LOC, true);
        if (spawnProperty != null)
        {
            WorldPoint point = WorldPoint.fromString(spawnProperty);
            if (point != null)
                return new WarpPoint(point, player.cameraYaw, player.cameraPitch);
        }

        if (doDefaultSpawn)
            return null;
        else
            return new WarpPoint(player.dimension, player.worldObj.getSpawnPoint(), 0, 0);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPlayerDeath(LivingDeathEvent e)
    {
        if (e.entityLiving instanceof EntityPlayer)
        {
            EntityPlayerMP player = (EntityPlayerMP) e.entityLiving;
            PlayerInfo pi = PlayerInfo.get(player.getPersistentID());
            pi.setLastDeathLocation(new WarpPoint(player));
            pi.setLastTeleportOrigin(pi.getLastDeathLocation());
        }
    }

    @SubscribeEvent
    public void doFirstRespawn(EntityJoinWorldEvent e)
    {
        if (!e.entity.getClass().equals(EntityPlayerMP.class))
            return;

        EntityPlayerMP player = (EntityPlayerMP) e.entity;
        if (!PlayerInfo.exists(player.getPersistentID()))
        {
            WarpPoint p = getPlayerSpawn(player, null, true);
            if (p != null)
                TeleportHelper.doTeleport(player, p);
        }
    }

    @SubscribeEvent
    public void doRespawn(PlayerRespawnEvent e)
    {
        WarpPoint lastDeathLocation = PlayerInfo.get(e.player.getPersistentID()).getLastDeathLocation();
        if (lastDeathLocation == null)
            lastDeathLocation = new WarpPoint(e.player);

        WarpPoint p = getPlayerSpawn(e.player, lastDeathLocation, true);
        if (p != null)
            TeleportHelper.doTeleport((EntityPlayerMP) e.player, p);
    }

}
