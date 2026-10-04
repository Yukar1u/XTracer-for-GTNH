package com.yukar1n.x_tracer;

import com.yukar1n.x_tracer.client.ClientRender;
import com.yukar1n.x_tracer.client.gui.GuiXTracerTerminal;
import com.yukar1n.x_tracer.item.ItemXTracerTerminal;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import com.yukar1n.x_tracer.network.KeyPressMessage;
import com.yukar1n.x_tracer.block.BlockXTracerContainer;
import com.yukar1n.x_tracer.network.TerminalBindingMessage;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    private static KeyBinding keySearch;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event); // 调用父类的 preInit（保留公共逻辑）

        // 注册按键（类别是 "key.categories.x_trace"，会在控制设置中分组）
        keySearch = new KeyBinding("key.x_tracer.search", Keyboard.KEY_X, "key.categories.x_tracer");
        ClientRegistry.registerKeyBinding(keySearch);

        // 注册客户端事件监听器
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);

    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.entityPlayer.getHeldItem() == null
            || event.entityPlayer.getHeldItem().getItem() != ItemXTracerTerminal.INSTANCE) {
            return;
        }
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            if (event.entityPlayer.isSneaking()
                && event.entityPlayer.worldObj.getBlock(event.x, event.y, event.z) == BlockXTracerContainer.INSTANCE) {
                event.setCanceled(true);
                network.sendToServer(new TerminalBindingMessage(true, event.x, event.y, event.z));
            } else {
                event.setCanceled(true);
                openTerminalGui();
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (keySearch.isPressed()){
            //按键按下，发送请求到服务端
            network.sendToServer(new KeyPressMessage());
        }
    }
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        // 注册客户端渲染器
        MinecraftForge.EVENT_BUS.register(new ClientRender());
    }

    @Override
    public void openTerminalGui() {
        Minecraft.getMinecraft().displayGuiScreen(new GuiXTracerTerminal());
    }
}
