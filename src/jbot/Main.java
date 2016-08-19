package jbot;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Input syntax: 
 *  -press [-key]: press a given key. 
 * 
 *      (optional) -every [metric]: specifies to press the key in intervals of the given metric. Maximum value is 255. 
 * 
 *          (required) -[units]: possible options are -seconds, -minutes, -hours.
 * 
 *  -hold [-key]: hold a given key; expects a time to hold. 
 * 
 *      (required) -for [metric] specifies how long to hold the key for. Maximum value is 255.
 * 
 *          (required) -[units]: possible options are -seconds, -minutes, -hours.
 * 
 *  (required) -on [key]: toggles the operation on or off. 
 * 
 * Examples: 
 * 
 *  -Press -F4 -every -4 -minutes -on -F12 
 * 
 *  -Hold -3 -for -8 -seconds -on -T
 *
 * Capitalization is ignored.
 *
 * @author Ceno
 */
public class Main {

    static String instruction = "";
    static int[] keyCodes;
    static boolean[] holdCodes;
    
    public static void main(String[] args) {
        instruction = "-hold -4 -for -1 -second -on -f12\n";
        String[] codes = null;
        try{
            codes = codeBot(instruction);  
        } catch (SecurityException | NoSuchFieldException | IllegalAccessException ex) {
            System.exit(0);
        }
        int j = codes.length;
        for(String s : codes)
        {
            if(s == null || s.equals(""))
                j--;
        }
        CyclicBarrier gate = new CyclicBarrier(j + 1);
        HashMap<Thread,Integer> threads = new HashMap<>();
        for(int i = 0; i < codes.length; i++)
        {
            if(codes[i] == null)
                continue;
            jBot jb = new jBot("Bot_" + i, codes[i], gate, -1, keyCodes[i], holdCodes[i]);
            threads.put(jb, keyCodes[i]);
            jb.start();
        }
        try{
            gate.await();
            System.out.println("Threads running!");
        } catch (InterruptedException | BrokenBarrierException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String[] codeBot(String instr) throws SecurityException, NoSuchFieldException, IllegalAccessException {
        String[] instructions = instr.toLowerCase().replace(" ","").split("\n");
        StringBuilder sb = new StringBuilder();
        String[] encoded = new String[instructions.length];
        keyCodes = new int[encoded.length];
        holdCodes = new boolean[encoded.length];
        for (int i = 0; i < instructions.length; i++) {
            String str = instructions[i];
            if (!str.contains("on") || !str.contains("-") || !str.contains("on") || !(str.contains("press") || str.contains("hold"))) {
                invalidCode(encoded);
                continue;
            }
            if (str.contains("hold") && !str.contains("for")) {
                invalidCode(encoded);
                continue;
            }
            if ((str.contains("every") || str.contains("for")) && !(str.contains("seconds") || str.contains("minutes") || str.contains("hours") || str.contains("second") || str.contains("minute") || str.contains("hour"))) {
                invalidCode(encoded);
                continue;
            }
            String[] pieces = str.split("-");
            if (pieces[1].contains("press")) {
                holdCodes[i] = false;
                String key = pieces[2];
                sb.append("try {\n" + " java.awt.Robot rob = new java.awt.Robot();\n rob.keyPress(java.awt.event.KeyEvent.VK_")
                        .append(key).append(");\n rob.delay(2);\n rob.keyRelease(java.awt.event.KeyEvent.VK_")
                        .append(key).append(");\n");
                if(pieces[3].contains("every"))
                {
                    int timer = Integer.parseInt(pieces[4].replace(" ", ""));
                    int multiplier;
                    switch(pieces[5])
                    {
                        case "seconds" : multiplier = 1000;
                            break;
                        case "minutes" : multiplier = 60000;
                            break;
                        case "hours" : multiplier = 36000000;
                            break;
                        case "second" : multiplier = 1000;
                            break;
                        case "minute" : multiplier = 60000;
                            break;
                        case "hour" : multiplier = 36000000;
                            break;
                        default : invalidCode(encoded); 
                            continue;
                    }
                    sb.append("rob.delay(").append(timer * multiplier).append(");\n");
                }
            } else if (pieces[1].contains("hold")) {
                holdCodes[i] = true;
                String key = pieces[2].replace(" ","");
                sb.append("try {\n" + " java.awt.Robot rob = new java.awt.Robot();\n rob.keyPress(java.awt.event.KeyEvent.VK_")
                        .append(key).append(");\n");
                if(pieces[3].contains("for"))
                {
                    int timer = Integer.parseInt(pieces[4].replace(" ", ""));
                    int multiplier;
                    switch(pieces[5])
                    {
                        case "seconds" : multiplier = 1000;
                            break;
                        case "minutes" : multiplier = 60000;
                            break;
                        case "hours" : multiplier = 36000000;
                            break;
                        case "second" : multiplier = 1000;
                            break;
                        case "minute" : multiplier = 60000;
                            break;
                        case "hour" : multiplier = 36000000;
                            break;
                        default : invalidCode(encoded); 
                            continue;
                    }
                    sb.append("rob.delay(").append(timer * multiplier).append(");\n");
                    sb.append("rob.keyRelease(java.awt.event.KeyEvent.VK_").append(key).append(");\n");
                }
                else
                {
                    invalidCode(encoded);
                    continue;
                }
            } else {
                invalidCode(encoded);
                continue;
            }
            if(pieces[6].contains("on"))
            {
                String code = "VK_" + pieces[7].replace(" ","").toUpperCase();
                keyCodes[i] = (int)KeyEvent.class.getField(code).getInt(null);
            }
            else
            {
                invalidCode(encoded);
                continue;
            }
            sb.append(" } catch (java.awt.AWTException ex){}");
            encoded[i] = sb.toString();
            sb.setLength(0);
        }
        return encoded;
    }
    
    private static void invalidCode(String[] str)
    {
        if(str.length == 1)
            return;
        String[] tmp = new String[str.length - 1];
        int j = 0;
        while(str[j] != null)
            tmp[j] = str[j++];
        str = tmp;
        int[] tmp2 = new int[str.length];
        j = 0;
        while(str[j] != null)
            tmp2[j] = keyCodes[j++];
        keyCodes = tmp2;
        boolean[] tmp3 = new boolean[str.length];
        j = 0;
        while(str[j] != null)
            tmp3[j] = holdCodes[j++];
        holdCodes = tmp3;
    }
}
