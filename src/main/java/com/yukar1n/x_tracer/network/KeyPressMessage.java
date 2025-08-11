package com.yukar1n.x_tracer.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class KeyPressMessage implements IMessage{

    //没有额外数据，构造函数为空
    public void keyPressMessage(){}

    @Override
    public void fromBytes(ByteBuf buf){
        //无数据，不实现
    }

    @Override
    public void toBytes(ByteBuf buf){
        //无数据，不实现
    }

    public static class Handler implements IMessageHandler<KeyPressMessage, IMessage>{
        @Override
        public IMessage onMessage(KeyPressMessage message, MessageContext ctx){
            System.out.println("Got key input at server.");
            return null;//没有返回包
        }
    }
}
