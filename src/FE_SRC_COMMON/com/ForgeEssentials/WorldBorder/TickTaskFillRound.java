package com.ForgeEssentials.WorldBorder;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;

import com.ForgeEssentials.util.OutputHandler;

/**
 * Does the actual filling, with limited chuncks per tick.
 * 
 * @author Dries007
 * 
 */

public class TickTaskFillRound extends TickTaskFill
{
	public TickTaskFillRound(WorldServer world)
	{
		super(world);
	}

	@Override
	public void tick()
	{
		super.tick();
	}

	@Override
	public void genList()
	{
		for (int X = this.minX; X <= this.maxX; X = X + 16)
		{
			for (int Z = this.minZ; Z <= this.maxZ; Z = Z + 16)
			{
				if ((rad + ModuleWorldBorder.overGenerate) < ModuleWorldBorder.getDistanceRound(centerX, centerZ, X, Z))
				{
					toDo.add(new ChunkCoordIntPair(X, Z));
				}
			}
		}
		OutputHandler.fine(toDo.size() + " chunks to generate.");
	}
}
