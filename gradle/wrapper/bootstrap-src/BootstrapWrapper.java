/*
 * Minimal, auditable distribution bootstrap for environments without Gradle.
 * This is NOT Gradle's official wrapper JAR; use `gradle wrapper` to replace
 * it with the official JAR after opening the project in Android Studio.
 */
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

public class BootstrapWrapper {
    private static final String VERSION = "9.6.0";
    private static final String SHA256 = "bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01";
    private static final String URL = "https://services.gradle.org/distributions/gradle-9.6.0-bin.zip";
    public static void main(String[] args) throws Exception {
        final Path home = Paths.get(System.getProperty("user.home"), ".gradle", "wrapper", "dists", "phone-diagnostic-" + VERSION);
        final Path gradleDir = home.resolve("gradle-" + VERSION);
        final Path cmd = gradleDir.resolve("bin").resolve(System.getProperty("os.name").startsWith("Windows") ? "gradle.bat" : "gradle");
        if(!Files.isRegularFile(cmd)) {
            Files.createDirectories(home);
            final Path zip = home.resolve("gradle-"+VERSION+"-bin.zip");
            if(!Files.exists(zip)) {
                System.err.println("Downloading Gradle " + VERSION + " from " + URL);
                HttpURLConnection connection = (HttpURLConnection)new URL(URL).openConnection();
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);
                connection.setInstanceFollowRedirects(true);
                try(InputStream in=connection.getInputStream()) { Files.copy(in,zip,StandardCopyOption.REPLACE_EXISTING); }
            }
            MessageDigest md=MessageDigest.getInstance("SHA-256");
            try(InputStream in=Files.newInputStream(zip)) {
                byte[] buf = new byte[65536]; int n;
                while((n=in.read(buf))!=-1) md.update(buf,0,n);
            }
            StringBuilder digest=new StringBuilder();
            for(byte b:md.digest()) digest.append(String.format("%02x",b&0xff));
            if(!SHA256.equals(digest.toString())) {
                Files.deleteIfExists(zip);
                throw new SecurityException("Gradle distribution checksum mismatch; archive deleted");
            }
            try(ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
                ZipEntry entry;
                while((entry=in.getNextEntry())!=null) {
                    Path out=home.resolve(entry.getName()).normalize();
                    if(!out.startsWith(home)) throw new SecurityException("Unsafe ZIP entry");
                    if(entry.isDirectory()) Files.createDirectories(out);
                    else {
                        Files.createDirectories(out.getParent());
                        Files.copy(in,out,StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            if(!Files.isRegularFile(cmd)) throw new IOException("Gradle distribution lacks executable");
            cmd.toFile().setExecutable(true);
        }
        ArrayList<String> command=new ArrayList<>(); command.add(cmd.toAbsolutePath().toString()); command.addAll(Arrays.asList(args));
        ProcessBuilder pb=new ProcessBuilder(command).inheritIO();
        pb.directory(new File(System.getProperty("user.dir")));
        int exit=pb.start().waitFor();
        System.exit(exit);
    }
}
