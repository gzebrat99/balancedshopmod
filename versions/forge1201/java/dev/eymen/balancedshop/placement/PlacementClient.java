package dev.eymen.balancedshop.placement;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.*;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

/** Choosing a size enables a persistent preview; one right-click places one plane. */
public final class PlacementClient {
    public static final KeyMapping MENU=new KeyMapping("Alan yerleştirme boyutu",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_P,"Balanced Shop");
    private static int size=3;
    private static boolean useWasDown,enabled;
    private static int previewCells;
    private PlacementClient(){}
    public static int size(){return size;}
    public static boolean enabled(){return enabled;}
    public static int previewCells(){return previewCells;}
    public static void disable(){enabled=false;previewCells=0;}
    public static void select(int value){if(AreaPlacement.validSize(value)){size=value;enabled=true;}}
    public static void install(IEventBus bus){
        bus.addListener((RegisterKeyMappingsEvent e)->e.register(MENU));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e)->{
            if(e.phase!=TickEvent.Phase.END)return;var client=Minecraft.getInstance();
            while(MENU.consumeClick())if(client.player!=null&&client.screen==null)client.setScreen(new PlacementScreen());
            if(client.screen!=null||client.player==null||!client.options.keyUse.isDown())useWasDown=false;
            if(client.player==null)disable();
        });
        MinecraftForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered e)->{
            var client=Minecraft.getInstance();
            if(!e.isUseItem()||client.screen!=null||client.player==null||!enabled||!(client.player.getMainHandItem().getItem() instanceof BlockItem))return;
            e.setCanceled(true);e.setSwingHand(false);
            // A press, rather than vanilla's repeated held-use ticks, places exactly one plane.
            if(!useWasDown&&e.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND&&client.hitResult instanceof BlockHitResult hit&&hit.getType()==HitResult.Type.BLOCK){
                AreaPlacement.CHANNEL.sendToServer(new AreaPlacement.Request(size));useWasDown=true;
            }
        });
        MinecraftForge.EVENT_BUS.addListener((InputEvent.MouseButton.Post e)->{if(e.getButton()==GLFW.GLFW_MOUSE_BUTTON_RIGHT&&e.getAction()==GLFW.GLFW_RELEASE)useWasDown=false;});
        MinecraftForge.EVENT_BUS.addListener((InputEvent.Key e)->{if(e.getAction()==GLFW.GLFW_RELEASE&&!Minecraft.getInstance().options.keyUse.isDown())useWasDown=false;});
        MinecraftForge.EVENT_BUS.addListener(PlacementClient::preview);
        MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent.Post e)->{
            var client=Minecraft.getInstance();
            if(enabled&&client.screen==null&&client.player!=null&&client.player.getMainHandItem().getItem() instanceof BlockItem)
                e.getGuiGraphics().drawCenteredString(client.font,"Alan: "+size+"×"+size+" · Sağ tık: yerleştir · "+MENU.getTranslatedKeyMessage().getString()+": boyut / kapat",client.getWindow().getGuiScaledWidth()/2,client.getWindow().getGuiScaledHeight()-65,0xE8C46E);
        });
    }
    private static void preview(RenderLevelStageEvent e){
        var client=Minecraft.getInstance();
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;
        previewCells=0;
        if(client.player==null||client.screen!=null||!enabled||!(client.player.getMainHandItem().getItem() instanceof BlockItem)||!(client.hitResult instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK)return;
        var context=new net.minecraft.world.item.context.BlockPlaceContext(new net.minecraft.world.item.context.UseOnContext(client.player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
        var positions=AreaPlacement.plane(context.getClickedPos(),hit.getDirection(),size);
        var pose=e.getPoseStack();var camera=e.getCamera().getPosition();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        var buffers=client.renderBuffers().bufferSource();var lines=buffers.getBuffer(RenderType.lines());
        for(var pos:positions){
            var cellHit=new BlockHitResult(Vec3.atCenterOf(pos),hit.getDirection(),pos,false);
            var cellContext=new net.minecraft.world.item.context.BlockPlaceContext(new net.minecraft.world.item.context.UseOnContext(client.player,net.minecraft.world.InteractionHand.MAIN_HAND,cellHit));
            boolean available=client.level.getWorldBorder().isWithinBounds(pos)&&!client.level.isOutsideBuildHeight(pos)&&cellContext.getClickedPos().equals(pos)&&client.level.getBlockState(pos).canBeReplaced(cellContext);
            LevelRenderer.renderLineBox(pose,lines,new AABB(pos).inflate(0.002),available?0.95f:0.95f,available?0.77f:0.22f,available?0.35f:0.22f,0.85f);previewCells++;
        }
        buffers.endBatch(RenderType.lines());pose.popPose();
    }
}
