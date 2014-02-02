package scaner;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Title:
 * Description:
 * <p/>
 * User: valentina
 * Date: 02.02.14
 * Time: 16:04
 */
public class SimpleFileNameFilter implements FilenameFilter {

    protected Pattern pattern;
    public SimpleFileNameFilter(String mask) throws IOException {
        pattern = Pattern.compile(constructRegExp(mask));
    }

    /**
     * Преобразуем маску в регексп, предварительно проверив на корректность
     * Маска может содержать цифры, буквы, _, . , * (замещение 0-inf символов)
     *
     * @param mask маска
     * @return регексп
     * @throws IOException
     */
    protected String constructRegExp(String mask) throws IOException {
        if(!checkMask(mask))
            throw new IOException("Некорректная маска для поиска файла " + mask);

        String result = "^" + mask.replace(".","\\.")
                                  .replace("*", ".*") + "$";
        return result;
    }


    public static boolean checkMask(String mask){
        Pattern checkValid = Pattern.compile("^[A-Za-z0-9_\\.\\*]+$");
        return checkValid.matcher(mask).find();
    }


    @Override
    public boolean accept(File dir, String name) {
        return pattern.matcher(name).find();

    }
}
