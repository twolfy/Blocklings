package com.blocklings.network;

import com.blocklings.entities.EntityBlockling;
import com.blocklings.main.Blocklings;
import com.blocklings.util.BlocklingType;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class BlocklingTypeMessage implements IMessage
{
    BlocklingType value;
    int id;

    public BlocklingTypeMessage()
    {
    }

    public BlocklingTypeMessage(BlocklingType value, int entityID)
    {
        this.value = value;
        this.id = entityID;
    }

    public void fromBytes(ByteBuf buf)
    {
        this.value = BlocklingType.getTypeFromTextureName(ByteBufUtils.readUTF8String(buf));
        this.id = buf.readInt();
    }

    public void toBytes(ByteBuf buf)
    {
        ByteBufUtils.writeUTF8String(buf, this.value.textureName);
        buf.writeInt(this.id);
    }

    public static class Handler implements net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler<BlocklingTypeMessage, IMessage>
    {
        public IMessage onMessage(BlocklingTypeMessage message, MessageContext ctx)
        {
            Entity entity = null;

            if ((ctx.side.isClient()) && (Blocklings.proxy.getPlayer(ctx) != null))
            {
                entity = Blocklings.proxy.getPlayer(ctx).world.getEntityByID(message.id);

                if (entity instanceof EntityBlockling)
                {
                    EntityBlockling blockling = (EntityBlockling) entity;

                    blockling.blocklingType = message.value;
                }
            }
            else if (ctx.side.isServer() && Blocklings.proxy.getPlayer(ctx) != null)
            {
                entity = Blocklings.proxy.getPlayer(ctx).world.getEntityByID(message.id);

                if ((entity instanceof EntityBlockling))
                {
                    EntityBlockling blockling = (EntityBlockling) entity;

                    blockling.setBlocklingTypeFromPacketOnServer(message.value);
                }
            }

            return null;
        }
    }
}