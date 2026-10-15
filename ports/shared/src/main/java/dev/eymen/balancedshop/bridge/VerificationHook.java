package dev.eymen.balancedshop.bridge;
/** Development harness lives on the run classpath and is excluded from distributable JARs. */
public final class VerificationHook {
    public static void call(String className,String method){
        if(!Boolean.getBoolean("balancedshop.verify"))return;
        try{Class.forName("dev.eymen.balancedshop.verification."+className).getMethod(method).invoke(null);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Development verification harness unavailable",e);}
    }
}
