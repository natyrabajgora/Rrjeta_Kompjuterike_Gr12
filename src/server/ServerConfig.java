package server;

public class ServerConfig {
    public static final int SERVER_PORT = 5000;
    public static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    public static final int DEFAULT_SERVER_PORT = 5000;
    public static final int BUFFER_SIZE = 4096;

    public static final String ENV_SERVER_HOST = "UDP_SERVER_HOST";
    public static final String ENV_SERVER_PORT = "UDP_SERVER_PORT";
    public static final String PROP_SERVER_HOST = "udp.server.host";
    public static final String PROP_SERVER_PORT = "udp.server.port";

    public static final int MAX_CLIENTS = 4;
    public static final long CLIENT_TIMEOUT_MS = 20_000L;

    public static final String LOGS_DIR = "logs";
    public static final String STATS_LOG_FILE = LOGS_DIR + "/server_stats.txt";
    public static final String MSG_LOG_FILE = LOGS_DIR + "/messages.log";

    public static final String DATA_DIR = "data";
    public static final String SERVER_FILES_DIR = DATA_DIR + "/server_files";
    public static final String UPLOADS_DIR = DATA_DIR + "/uploads";
    public static final String DOWNLOADS_DIR = DATA_DIR + "/downloads";

    public static final String CMD_HELLO = "HELLO";
    public static final String CMD_STATS = "STATS";

    public static final String CMD_LIST = "/list";
    public static final String CMD_READ = "/read";
    public static final String CMD_UPLOAD = "/upload";
    public static final String CMD_DOWNLOAD = "/download";
    public static final String CMD_DELETE = "/delete";
    public static final String CMD_SEARCH = "/search";
    public static final String CMD_INFO = "/info";
    public static final String CMD_EXIT = "/exit";


    public static String resolveServerHost() {
        String prop = System.getProperty(PROP_SERVER_HOST);
        if (prop != null && !prop.isBlank()) {
            return prop.trim();
        }
        String env = System.getenv(ENV_SERVER_HOST);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return DEFAULT_SERVER_HOST;
    }

    public static int resolveServerPort() {
        String prop = System.getProperty(PROP_SERVER_PORT);
        Integer parsed = tryParsePort(prop);
        if (parsed != null) {
            return parsed;
        }
        String env = System.getenv(ENV_SERVER_PORT);
        parsed = tryParsePort(env);
        if (parsed != null) {
            return parsed;
        }
        return DEFAULT_SERVER_PORT;
    }
    private static Integer tryParsePort(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0 || parsed > 65535) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
