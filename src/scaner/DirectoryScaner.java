package scaner;

import com.sun.deploy.util.ArrayUtil;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Title:
 * Description:
 * <p/>
 * User: valentina
 * Date: 30.01.14
 * Time: 23:20
 */
public class DirectoryScaner {

    protected static Boolean getBoolean(String s){
        if(s.toLowerCase().equals("true")){
            return true;
        }
        else if(s.toLowerCase().equals("false")){
            return false;
        }
        return null;
    }

    protected static boolean validatePath(String path){
        Pattern pattern = Pattern.compile("^\"[A-Za-z\\s\\.\\/\\:]+\"$");
        return pattern.matcher(path).find();
    }

    protected static Map<String, Object> processScanArgs(String[] args) throws IllegalArgsException {
        Map<String, Object> result = new HashMap<String, Object>(){
            {
                put("input", null);
                put("output", null);
                put("mask", null);
                put("includeSubfolders", false);
                put("autoDelete", false);
                put("waitInterval", 0);
            }
        };

        for(int i=0; i<args.length; i=i+2){
            if(i == args.length -1)
                continue;

            if(args[i].equals("-input")){
                if(!validatePath(args[i+1]))
                    throw new IllegalArgsException("Некорректное значение аргумента -input: " + args[i+1]);
                result.put("input", args[i+1].replace("\"",""));
            }
            else if(args[i].equals("-output")){
                if(!validatePath(args[i+1]))
                    throw new IllegalArgsException("Некорректное значение аргумента -input: " + args[i+1]);

                result.put("output", args[i+1].replace("\"",""));
            }
            else if(args[i].equals("-mask")){
                String mask = args[i+1].replace("\"","");
                if(!SimpleFileNameFilter.checkMask(mask))
                    throw new IllegalArgsException("Некорректное значение агрумента -mask: " + args[i+1]);
                result.put("mask", mask);
            }
            else if(args[i].equals("-waitInterval")){
                try{
                    Integer interval = Integer.parseInt(args[i+1]);
                    result.put("waitInterval", interval);
                }
                catch(NumberFormatException e){
                    throw new IllegalArgsException("Некорректное значение аргумента -waitInterval: " + args[i+1]);
                }
            }
            else if(args[i].equals("-includeSubfolders")){
                Boolean includeSubfolders = getBoolean(args[i+1]);
                if(includeSubfolders == null)
                    throw new IllegalArgsException("Некорректное значение аргумента -includeSubfolders: " + args[i+1]);
                result.put("includeSubfolders", includeSubfolders);
            }
            else if(args[i].equals("-autoDelete")){
                Boolean autoDelete = getBoolean(args[i+1]);
                if(autoDelete == null)
                    throw new IllegalArgsException("Некорректное значение аргумента -autoDelete: " + args[i+1]);
                result.put("autoDelete", autoDelete);
            }
            else i--;
        }
        return result;
    }

    protected static void showHelp(){

    }

    public static class IllegalArgsException extends Exception{

        public IllegalArgsException(String message) {
            super(message);
        }

        public IllegalArgsException(String message, Throwable cause) {
            super(message, cause);
        }

        public IllegalArgsException(Throwable cause) {
            super(cause);
        }

        protected IllegalArgsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }


    public static void main(String[] args) throws IOException, IllegalArgsException {
        System.out.println("Введите команду (help - помощь) : ");
        boolean exit = false;
        Scanner scanIn = new Scanner(System.in);

        ExecutorService executorService = Executors.newCachedThreadPool();

        while(!exit){
            String command;
            command = scanIn.nextLine();

            String[] command_args = command.split(" ");
            if(command_args[0].equals(COMMAIND_EXIT))
                exit = true;
            else if(command_args[0].equals(COMMAIND_HELP))
                showHelp();
            else if(command_args[0].equals(COMMAIND_SCAN)){
                Map<String, Object> scanArgs = processScanArgs(Arrays.copyOfRange(command_args, 1, command_args.length));
                executorService.execute(new Handler(
                    (String)scanArgs.get("input"),
                    (String)scanArgs.get("output"),
                    scanArgs.get("mask") == null?null:new SimpleFileNameFilter((String)scanArgs.get("mask")),
                    (Boolean)scanArgs.get("includeSubfolders"),
                    (Boolean)scanArgs.get("autoDelete"),
                    (Integer) scanArgs.get("waitInterval")
                ));
            }
            else System.out.println("Неизвестная команда: " + command_args[0]);
        }
        Handler.cancel();
        executorService.shutdown();
        scanIn.close();
    }

    public static final String COMMAIND_HELP = "help";
    public static final String COMMAIND_SCAN = "scan";
    public static final String COMMAIND_EXIT = "exit";
}
