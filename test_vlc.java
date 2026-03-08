import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.binding.LibVlc;

public class test_vlc {
    public static void main(String[] args) {
        System.out.println("=== VLCJ Test ===");
        System.out.println();

        // Show system properties
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Architecture: " + System.getProperty("os.arch"));
        System.out.println();

        // Try to find VLC
        System.out.println("Attempting to find VLC...");
        NativeDiscovery discovery = new NativeDiscovery();

        boolean found = discovery.discover();
        System.out.println("Discovery result: " + found);

        if (!found) {
            System.out.println("\n=== VLC NOT FOUND ===");
            System.out.println("Trying manual discovery...");

            String[] paths = {
                "C:\\Program Files\\VideoLAN\\VLC",
                "C:\\Program Files (x86)\\VideoLAN\\VLC"
            };

            for (String path : paths) {
                System.out.println("\nChecking: " + path);
                java.io.File vlcDir = new java.io.File(path);
                java.io.File libvlc = new java.io.File(path, "libvlc.dll");
                java.io.File libvlccore = new java.io.File(path, "libvlccore.dll");

                System.out.println("  Directory exists: " + vlcDir.exists());
                System.out.println("  libvlc.dll exists: " + libvlc.exists());
                System.out.println("  libvlccore.dll exists: " + libvlccore.exists());

                if (vlcDir.exists() && libvlc.exists() && libvlccore.exists()) {
                    System.out.println("  Found! Setting system properties...");
                    System.setProperty("java.library.path", path);
                    System.setProperty("vlc.plugin.path", path + "\\plugins");

                    System.out.println("\n  Trying to load libvlccore.dll...");
                    try {
                        System.load(new java.io.File(path, "libvlccore.dll").getAbsolutePath());
                        System.out.println("    SUCCESS!");
                    } catch (Exception e) {
                        System.out.println("    FAILED: " + e.getMessage());
                    }

                    System.out.println("\n  Trying to load libvlc.dll...");
                    try {
                        System.load(new java.io.File(path, "libvlc.dll").getAbsolutePath());
                        System.out.println("    SUCCESS!");
                    } catch (Exception e) {
                        System.out.println("    FAILED: " + e.getMessage());
                    }

                    break;
                }
            }
        }

        // Try to create MediaPlayerFactory
        System.out.println("\n=== Attempting to create MediaPlayerFactory ===");
        try {
            MediaPlayerFactory factory = new MediaPlayerFactory("--no-video-title-show");
            System.out.println("SUCCESS! MediaPlayerFactory created");
            System.out.println("VLC version: " + factory.libvlc().version());
            factory.release();
            System.out.println("\n=== VLCJ WORKS CORRECTLY ===");
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getClass().getName());
            System.out.println("Message: " + e.getMessage());
            e.printStackTrace();
            System.out.println("\n=== VLCJ INITIALIZATION FAILED ===");
        }
    }
}
