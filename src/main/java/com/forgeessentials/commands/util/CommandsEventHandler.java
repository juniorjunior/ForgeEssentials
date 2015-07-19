package com.forgeessentials.commands.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.permission.PermissionManager;

import com.forgeessentials.commands.player.CommandNoClip;
import com.forgeessentials.util.PlayerUtil;
import com.forgeessentials.util.events.ServerEventHandler;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;

public class CommandsEventHandler extends ServerEventHandler
{

    public static HashMultimap<EntityPlayer, PlayerInvChest> map = HashMultimap.create();

    public static int getWorldHour(World world)
    {
        return (int) ((world.getWorldTime() % 24000) / 1000);
    }

    public static int getWorldDays(World world)
    {
        return (int) (world.getWorldTime() / 24000);
    }

    public static void makeWorldTimeHours(World world, int target)
    {
        world.setWorldTime((getWorldDays(world) + 1) * 24000 + (target * 1000));
    }

    public static void register(PlayerInvChest inv)
    {
        map.put(inv.owner, inv);
    }

    public static void remove(PlayerInvChest inv)
    {
        map.remove(inv.owner, inv);
    }

    public CommandsEventHandler()
    {
        super();
    }

    @SubscribeEvent
    public void playerInteractEvent(PlayerInteractEvent e)
    {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient())
        {
            return;
        }

        /*
         * Jump with compass
         */
        if (e.action == Action.RIGHT_CLICK_AIR || e.action == Action.RIGHT_CLICK_BLOCK)
        {
            if (e.entityPlayer.getCurrentEquippedItem() != null && FMLCommonHandler.instance().getEffectiveSide().isServer())
            {
                if (e.entityPlayer.getCurrentEquippedItem().getItem() == Items.compass)
                {
                    if (PermissionManager.checkPermission(e.entityPlayer, "fe.commands.jump"))
                    {
                        MovingObjectPosition mop = PlayerUtil.getPlayerLookingSpot(e.entityPlayer, 500);
                        if (mop != null)
                        {
                            BlockPos pos1 = mop.func_178782_a();
                            BlockPos pos2 = new BlockPos(pos1.getX(), pos1.getY() + 1, pos1.getZ());
                            while (pos1.getY() < e.entityPlayer.worldObj.getHeight() + 2
                                    && (!e.entityPlayer.worldObj.isAirBlock(pos1) || !e.entityPlayer.worldObj.isAirBlock(pos2)))
                            {
                                pos1 = pos2;
                                pos2 = new BlockPos(pos1.getX(), pos1.getY() + 1, pos1.getZ());
                            }
                            ((EntityPlayerMP) e.entityPlayer).setPositionAndUpdate(pos1.getX() + 0.5, pos1.getY(), pos1.getZ() + 0.5);
                        }
                    }
                }
            }
        }
        if (e.entityPlayer.getCurrentEquippedItem() != null && FMLCommonHandler.instance().getEffectiveSide().isServer())
        {
            ItemStack is = e.entityPlayer.inventory.getCurrentItem();
            if (is != null && is.getTagCompound() != null && is.getTagCompound().hasKey("FEbinding"))
            {
                String cmd = null;
                NBTTagCompound nbt = is.getTagCompound().getCompoundTag("FEbinding");

                if (e.action.equals(Action.LEFT_CLICK_BLOCK))
                {
                    cmd = nbt.getString("left");
                }
                else if (e.action.equals(Action.RIGHT_CLICK_AIR))
                {
                    cmd = nbt.getString("right");
                }

                if (!Strings.isNullOrEmpty(cmd))
                {
                    MinecraftServer.getServer().getCommandManager().executeCommand(e.entityPlayer, cmd);
                    e.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void doWorldTick(TickEvent.WorldTickEvent e)
    {
        /*
         * Time settings
         */
        if (!CommandDataManager.WTmap.containsKey(e.world.provider.getDimensionId()))
        {
            WeatherTimeData wt = new WeatherTimeData(e.world.provider.getDimensionId());
            wt.freezeTime = e.world.getWorldTime();
            CommandDataManager.WTmap.put(e.world.provider.getDimensionId(), wt);
        }
        else
        {
            WeatherTimeData wt = CommandDataManager.WTmap.get(e.world.provider.getDimensionId());
            /*
             * Weather part
             */
            if (wt.weatherSpecified)
            {
                WorldInfo winfo = e.world.getWorldInfo();
                if (!wt.rain)
                {
                    winfo.setRainTime(20 * 300);
                    winfo.setRaining(false);
                    winfo.setThunderTime(20 * 300);
                    winfo.setThundering(false);
                }
                else if (!wt.storm)
                {
                    winfo.setThunderTime(20 * 300);
                    winfo.setThundering(false);
                }
            }

            /*
             * Time part
             */
            if (wt.timeFreeze)
            {
                e.world.setWorldTime(wt.freezeTime);
            }
            else if (wt.timeSpecified)
            {
                int h = getWorldHour(e.world);

                if (wt.day)
                {
                    if (h >= WeatherTimeData.dayTimeEnd)
                    {
                        makeWorldTimeHours(e.world, WeatherTimeData.dayTimeStart);
                    }
                }
                else
                {
                    if (h >= WeatherTimeData.nightTimeEnd)
                    {
                        makeWorldTimeHours(e.world, WeatherTimeData.nightTimeStart);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void tickStart(TickEvent.PlayerTickEvent event)
    {
        if (map.containsKey(event.player))
        {
            for (PlayerInvChest inv : map.get(event.player))
            {
                inv.update();
            }
        }
        if (event.phase == TickEvent.Phase.END)
            CommandNoClip.checkClip(event.player);
    }

}
