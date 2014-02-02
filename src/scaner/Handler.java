package scaner;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Title: Основной класс-сканер
 * Description:
 * <p/>
 * User: valentina
 * Date: 30.01.14
 * Time: 23:46
 */
public class Handler implements Runnable {

    public Handler(String inputDir, String outputDir, SimpleFileNameFilter filter, boolean includeSubfolders, boolean autoDelete, int interval) throws IOException {
        setInputDir(inputDir);
        setOutputDir(outputDir);
        setMaskFilter(filter);
        setIncludeSubfolders(includeSubfolders);
        setAutoDelete(autoDelete);
        setInterval(interval);
    }

    protected static boolean cancel = false;
    public static void cancel(){
        cancel = true;
    }

    public void run() {
        while(!cancel){
            try {
                System.out.println("start " + inputDir + " " + outputDir);
                File source = new File(inputDir);
                File dest = new File(outputDir);

                try {
                    if(!source.exists())
                        throw new IOException("Папка " + source.getAbsolutePath() + " не существует");
                    for(File f: source.listFiles(maskFilter))
                        copy(new File(source, f.getName()), new File(dest, f.getName()));

                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

                System.out.println("end " + inputDir + " " + outputDir);


                Thread.sleep(interval);
            } catch (InterruptedException e) {
                System.out.println(inputDir + " " + outputDir + " interrupted");
            }
        }
    }

    protected String inputDir;
    protected String outputDir;
    protected boolean includeSubfolders;
    protected boolean autoDelete;
    protected SimpleFileNameFilter maskFilter = null;

    protected int interval;

    public String getInputDir() {
        return inputDir;
    }

    public void setInputDir(String inputDir) {
        this.inputDir = inputDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public SimpleFileNameFilter getMaskFilter() {
        return maskFilter;
    }

    public void setMaskFilter(SimpleFileNameFilter maskFilter) {
        this.maskFilter = maskFilter;
    }

    public boolean isIncludeSubfolders() {
        return includeSubfolders;
    }

    public void setIncludeSubfolders(boolean includeSubfolders) {
        this.includeSubfolders = includeSubfolders;
    }

    public boolean isAutoDelete() {
        return autoDelete;
    }

    public void setAutoDelete(boolean autoDelete) {
        this.autoDelete = autoDelete;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    protected void doCopy(File source, File dest) throws IOException {
        if(source.isDirectory()){
            if(!dest.exists())
                dest.mkdir();
            if(includeSubfolders){
                for(File f: source.listFiles(maskFilter))
                    copy(new File(source, f.getName()), new File(dest, f.getName()));
                if(autoDelete)
                    source.delete();
            }

        }
        else{
            FileChannel sourceChannel = new FileInputStream(source).getChannel();
            try{
                FileChannel destChannel = new FileOutputStream(dest).getChannel();
                try {
                    destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
                } finally {
                    destChannel.close();
                }
            } finally {
                sourceChannel.close();
            }
            if(autoDelete)
                source.delete();
        }
    }


    protected void copy(File source, File dest) throws IOException {
        //Мы работаем с объектами Lock.
        //Логика следующая: каждому объекту File однозначно соответствует один объект Lock.
        //Поскольку мы работаем каждый раз с двумя объектами (source, dest), нужно блокировать ОБА
        //Т.е. необходима конструкция с tryLock: возможно осуществить копирование только в том случае, если оба лока были успешно получены.
        Lock sourceLock = getLock(source);
        Lock destLock = getLock(source);
        if(sourceLock.tryLock()){
            try{
                if(destLock.tryLock()){
                    try{
                        doCopy(source, dest);
                    }
                    finally{
                        destLock.unlock();
                    }
                }
            }
            finally{
                sourceLock.unlock();
            }
        };
    }


    protected Lock getLock(File file){
        synchronized (locks){
            locks.putIfAbsent(file.getAbsolutePath(), new ReentrantLock());
            return locks.get(file.getAbsolutePath());
        }
    }

    private static final ConcurrentMap<String, Lock> locks = new ConcurrentHashMap<String, Lock>();

}
