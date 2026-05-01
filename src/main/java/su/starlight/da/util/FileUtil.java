package su.starlight.da.util;

import com.mojang.datafixers.util.Pair;
import su.starlight.da.CorePlugin;

import java.io.*;
import java.security.MessageDigest;
import java.util.jar.JarFile;

public final class FileUtil {

    private FileUtil() {}

    public static void extractData(String path, File destination) {
        try (JarFile jar = new JarFile(new File(CorePlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI()))) {
            jar.stream()
                    .filter(entry -> entry.getName().startsWith(path) && !entry.isDirectory())
                    .forEach(entry -> {
                        File outFile = new File(destination, entry.getName());
                        try {
                            if (outFile.getParentFile() != null && !outFile.getParentFile().exists()) outFile.getParentFile().mkdirs();
                            try (InputStream is = jar.getInputStream(entry);
                                 OutputStream os = new FileOutputStream(outFile)) {
                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    os.write(buffer, 0, bytesRead);
                                }
                            }
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getExtension(File file) {
        return getNameAndExtension(file).getSecond();
    }

    public static Pair<String, String> getNameAndExtension(File file) {
        String name = file.getName();
        int index = name.lastIndexOf('.');
        String extension = index == -1 ? "" : name.substring(index + 1);
        return new Pair<>(name.substring(0, index), extension);
    }

    public static byte[] sha1(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            InputStream fis = new FileInputStream(file);
            int n = 0;
            byte[] buffer = new byte[8192];
            while (n != -1) {
                n = fis.read(buffer);
                if (n > 0) {
                    digest.update(buffer, 0, n);
                }
            }
            fis.close();

            return digest.digest();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
