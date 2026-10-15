package dev.eymen.balancedshop.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.eymen.balancedshop.*;
import dev.eymen.balancedshop.data.EconomyData;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import java.math.BigDecimal;

public final class EconomyCommands {
    private EconomyCommands(){}
    /** Exact decimal coin amounts; 0.01 coin = 1 cent. No rounding, exponent notation or negatives. */
    public static long amount(String input,boolean zero){
        if(!input.matches("[0-9]{1,14}(\\.[0-9]{1,2})?"))throw new IllegalArgumentException("Miktar: 12 veya 12.50 biçiminde olmalı.");
        long n=new BigDecimal(input).movePointRight(2).longValueExact();
        if(n<(zero?0:1)||n>Money.MAX)throw new IllegalArgumentException("Miktar sınır dışında.");return n;
    }
    private static EconomyData data(){if(BalancedShop.wallets()==null)throw new IllegalArgumentException("Ekonomi henüz hazır değil.");return BalancedShop.wallets().data();}
    public static void register(RegisterCommandsEvent event){
        var d=event.getDispatcher();
        d.register(Commands.literal("bal").executes(c->{
            try {long value=data().balance(c.getSource().getPlayerOrException().getUUID());c.getSource().sendSuccess(()->Component.literal("Bakiye: "+Money.format(value)+" coin"),false);return 1;}
            catch(IllegalArgumentException e){return fail(c.getSource(),e);}
        }));
        d.register(Commands.literal("pay").then(Commands.argument("oyuncu",EntityArgument.player()).then(Commands.argument("miktar",StringArgumentType.word()).executes(c->{
            try {
                var from=c.getSource().getPlayerOrException();var to=EntityArgument.getPlayer(c,"oyuncu");
                long n=amount(StringArgumentType.getString(c,"miktar"),false);
                if(!data().transfer(from.getUUID(),to.getUUID(),n))throw new IllegalArgumentException("Transfer yapılamadı: bakiye, alıcı limiti veya aynı oyuncu.");
                c.getSource().sendSuccess(()->Component.literal(to.getScoreboardName()+" oyuncusuna "+Money.format(n)+" coin gönderildi."),false);
                to.sendSystemMessage(Component.literal(from.getScoreboardName()+" sana "+Money.format(n)+" coin gönderdi."));return 1;
            }catch(IllegalArgumentException e){return fail(c.getSource(),e);}
        }))));
        var root=Commands.literal("economy").requires(source->source.hasPermission(2));
        for(String operation:new String[]{"add","remove","set"})root.then(Commands.literal(operation).then(Commands.argument("oyuncu",EntityArgument.player()).then(Commands.argument("miktar",StringArgumentType.word()).executes(c->{
            try {
                var target=EntityArgument.getPlayer(c,"oyuncu");var data=data();long n=amount(StringArgumentType.getString(c,"miktar"),operation.equals("set")),old=data.balance(target.getUUID());
                long value=switch(operation){case "add"->Money.credit(old,n);case "remove"->old-n;default->n;};
                if(value<0)throw new IllegalArgumentException("Bakiye sıfırın altına inemez.");data.setBalance(target.getUUID(),value);
                c.getSource().sendSuccess(()->Component.literal(target.getScoreboardName()+" bakiyesi: "+Money.format(value)),true);return 1;
            }catch(IllegalArgumentException e){return fail(c.getSource(),e);}
        }))));d.register(root);
    }
    private static int fail(CommandSourceStack source,Exception e){source.sendFailure(Component.literal(e.getMessage()));return 0;}
}
