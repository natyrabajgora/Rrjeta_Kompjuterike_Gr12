package common;

/**
 * Konstanta të përbashkëta për serverin dhe klientët.
 * Këtu i mbajmë IP-në, portin, madhësitë e buffer-it, timeout-et, etj.
 */
public final class Constants {

    // ============ KONFIGURIMI I RRJETIT ============

    // IP e serverit (mund ta ndryshoni sipas rrjetit tuaj real)
    // "0.0.0.0" do të thotë: dëgjo në të gjitha interfacat (Server side)
    public static final String SERVER_HOST = "127.0.0.1";   // për klientët lokal (test)
    public static final int SERVER_PORT = 5000;             // porti ku dëgjon serveri

    // Madhësia e buffer-it të UDP paketave
    public static final int BUFFER_SIZE = 4096;


    // ============ LIMITET E SERVERIT ============

    // Numri maksimal i klientëve aktiv në të njëjtën kohë
    public static final int MAX_CLIENTS = 10;

    // Sa gjatë (në milisekonda) lejohet klienti të jetë pa aktivitet
    // p.sh. 20 sekonda
    public static final long CLIENT_TIMEOUT_MS = 20_000L;


    // ============ FILE & LOGS ============

    // Folder ku ruhen log-et
    public static final String LOGS_DIR = "logs";

    // File ku ruhen statistikat e serverit (STATS)
    public static final String STATS_LOG_FILE = LOGS_DIR + "/server_stats.txt";

    // File ku ruhen mesazhet e klientëve për monitorim
    public static final String MSG_LOG_FILE = LOGS_DIR + "/messages.log";

    // Folderat kryesorë për menaxhimin e file-ve në server
    public static final String DATA_DIR = "data";
    public static final String SERVER_FILES_DIR = DATA_DIR + "/server_files";
    public static final String UPLOADS_DIR = DATA_DIR + "/uploads";
    public static final String DOWNLOADS_DIR = DATA_DIR + "/downloads";


    // ============ KOMANDA BAZË ============

    // Mund t'i përdorni si string konstant në server & klient
    public static final String CMD_LIST     = "LIST";
    public static final String CMD_READ     = "READ";
    public static final String CMD_UPLOAD   = "UPLOAD";
    public static final String CMD_DOWNLOAD = "DOWNLOAD";
    public static final String CMD_DELETE   = "DELETE";
    public static final String CMD_SEARCH   = "SEARCH";
    public static final String CMD_INFO     = "INFO";
    public static final String CMD_STATS    = "STATS";
    public static final String CMD_HELLO    = "HELLO";

    private Constants() {
        // Konstruktor privat – nuk lejohet instancim
    }
}
