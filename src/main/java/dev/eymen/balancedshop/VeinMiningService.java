package dev.eymen.balancedshop;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.*;
/** Coordinates, speed, drops and destruction are authoritative on the server. */
public final class VeinMiningService {
    private static ShopConfig config;
    private static final Map<UUID,Session> sessions=new HashMap<>();
    private static final class Session {
        int heartbeat;boolean started;BlockPos origin;Block block;ItemStack tool;net.minecraft.world.level.Level level;
        final ArrayDeque<BlockPos> queue=new ArrayDeque<>();float progress;
    }
    public static void configure(ShopConfig value){config=value;sessions.clear();}
    public static void clear(){config=null;sessions.clear();}
    public static void register(){
        PayloadTypeRegistry.playC2S().register(VeinPackets.Held.TYPE,VeinPackets.Held.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(VeinPackets.Held.TYPE,(p,c)->{
            if(!p.held()){sessions.remove(c.player().getUUID());return;}
            if(config==null||!config.veinMiningEnabled)return;
            sessions.computeIfAbsent(c.player().getUUID(),id->new Session()).heartbeat=c.player().level().getServer().getTickCount();
        });
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->sessions.remove(h.getPlayer().getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(server->{
            var it=sessions.entrySet().iterator();
            while(it.hasNext()){
                var e=it.next();ServerPlayer player=server.getPlayerList().getPlayer(e.getKey());Session s=e.getValue();
                if(player==null||!player.isAlive()||player.isSpectator()||player.containerMenu!=player.inventoryMenu||server.getTickCount()-s.heartbeat>20){it.remove();continue;}
                if(!s.started){
                    var eye=player.getEyePosition();var hit=player.level().clip(new ClipContext(eye,eye.add(player.getLookAngle().scale(Math.min(6,player.blockInteractionRange()))),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
                    if(hit.getType()!=HitResult.Type.BLOCK)continue;
                    s.origin=((BlockHitResult)hit).getBlockPos();s.block=player.level().getBlockState(s.origin).getBlock();s.tool=player.getMainHandItem().copy();s.level=player.level();s.started=true;
                    s.queue.addAll(plan(player,s.origin,config.veinMiningMaxBlocks,config.veinMiningRadius));
                }
                if(s.started&&s.level!=player.level()){it.remove();continue;}
                if(s.queue.isEmpty())continue; // One bounded batch per press.
                if(player.getMainHandItem().getItem()!=s.tool.getItem()){it.remove();continue;}
                BlockPos pos=s.queue.peek();var state=player.level().getBlockState(pos);
                if(state.getBlock()!=s.block||!eligible(player,pos)){s.queue.remove();s.progress=0;continue;}
                s.progress+=state.getDestroyProgress(player,player.level(),pos);
                if(s.progress>=1){player.gameMode.destroyBlock(pos);s.queue.remove();s.progress=0;}
            }
        });
    }
    public static boolean eligible(ServerPlayer player,BlockPos pos){
        var level=player.level();if(!level.hasChunkAt(pos)||player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>144)return false;
        var state=level.getBlockState(pos);
        return !state.isAir()&&!state.hasBlockEntity()&&state.getDestroySpeed(level,pos)>=0&&level.mayInteract(player,pos)
            &&(!state.requiresCorrectToolForDrops()||player.hasCorrectToolForDrops(state));
    }
    public static List<BlockPos> plan(ServerPlayer player,BlockPos origin,int maximum,int radius){
        if(!eligible(player,origin))return List.of();Block type=player.level().getBlockState(origin).getBlock();
        List<BlockPos> result=new ArrayList<>();Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> pending=new ArrayDeque<>();pending.add(origin);seen.add(origin);
        while(!pending.isEmpty()&&result.size()<maximum){
            BlockPos pos=pending.remove();if(!eligible(player,pos)||player.level().getBlockState(pos).getBlock()!=type)continue;result.add(pos);
            for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++){
                if(dx==0&&dy==0&&dz==0)continue;BlockPos next=pos.offset(dx,dy,dz);
                if(Math.abs(next.getX()-origin.getX())<=radius&&Math.abs(next.getY()-origin.getY())<=radius&&Math.abs(next.getZ()-origin.getZ())<=radius&&seen.add(next))pending.add(next);
            }
        }
        return List.copyOf(result);
    }
}
