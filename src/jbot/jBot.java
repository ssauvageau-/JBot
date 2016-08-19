package jbot;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 *
 * @author Ceno
 */
public class jBot extends Thread {
    protected String instruction = "";
    CyclicBarrier cb;
    private volatile boolean stop = false;
    private final Integer haltCode;
    private final Integer toggle;
    private boolean running = false;
    private final boolean holder;
    public jBot(String fn, String instr, CyclicBarrier gate, Integer hardStop, Integer keyCode, boolean hold)
    {
        super(fn);
        this.cb = gate;
        this.instruction = instr;
        this.haltCode = hardStop;
        this.toggle = keyCode;
        this.holder = hold;
    }
    
    @Override
    public void run()
    {
        try{
            cb.await();
            runCode(instruction, this.getName());
        } catch (Exception ex) {
            Logger.getLogger(jBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void runCode(String s, String fn) throws Exception{
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager sjfm = jc.getStandardFileManager(null, null, null);
        File jf = new File(fn + ".java"); //create file in current working directory
        PrintWriter pw = new PrintWriter(jf);
        StringBuilder sb = new StringBuilder();
        sb.append("public class ").append(fn).append(" {public static void main(){").append(s).append("}}");
        pw.println(sb.toString());
        pw.close();
        Iterable fO = sjfm.getJavaFileObjects(jf);
        if(!jc.getTask(null,sjfm,null,null,null,fO).call()) { //compile the code
            throw new Exception("compilation failed");
        }
        URL[] urls = new URL[]{new File("").toURI().toURL()}; //use current working directory
        URLClassLoader ucl = new URLClassLoader(urls);
        Object o = ucl.loadClass(fn).newInstance();
        Method m = o.getClass().getMethod("main");
        running = true;
        if(!holder)
            while(!stop)
                while(running)
                    m.invoke(o);
        else
            m.invoke(o);
    }
    
    public void testBot()
    {
        try {
            java.awt.Robot rob = new java.awt.Robot();
            rob.keyPress(java.awt.event.KeyEvent.VK_0);
            rob.delay(2);
            rob.keyRelease(java.awt.event.KeyEvent.VK_0);
            
        } catch (java.awt.AWTException ex){}
    }
    
    public void stopOperation(Integer i)
    {
        if(haltCode.equals(i))
            stop = true;
    }
    
    public void toggleRunning(Integer i)
    {
        if(toggle.equals(i))
            running = !running;
    }
}
