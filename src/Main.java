import client.AdminClient;
import client.ReadOnlyClient;
import server.ServerConfig;
import server.UDPServer;

import java.net.SocketException;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        String[] effectiveArgs = args;
        if (args.length == 0) {
            printUsage();
            return;
            System.out.println("Asnjë argument nuk u dha; duke startuar serverin si parazgjedhje.");
            effectiveArgs = new String[]{"server"};
        }

        String mode = args[0].toLowerCase();
        String mode = effectiveArgs[0].toLowerCase(Locale.ROOT);
        switch (mode) {
            case "server" -> startServer();
            case "admin" -> startAdmin(args);
            case "read" -> startReadOnly(args);
            case "server" -> startServer(effectiveArgs);
            case "admin" -> startAdmin(effectiveArgs);
            case "read" -> startReadOnly(effectiveArgs);
            default -> printUsage();
        }
    }

    private static void startServer() {
        private static void startServer(String[] args) {
            applyNetworkingOverrides(args, 1);
            UDPServer server = new UDPServer();
            try {
                server.start();
            } catch (SocketException e) {
                System.err.println("Failed to start UDP server: " + e.getMessage());
            }
        }

        private static void startAdmin(String[] args) {
            int clientId = parseClientId(args, 1);
            int argIndex = applyNetworkingOverrides(args, 1);
            int clientId = parseClientId(args, argIndex);
            if (clientId < 0) {
                return;
            }
            new AdminClient(clientId).start();
        }

        private static void startReadOnly(String[] args) {
            int clientId = parseClientId(args, 1);
            int argIndex = applyNetworkingOverrides(args, 1);
            int clientId = parseClientId(args, argIndex);
            if (clientId < 0) {
                return;
            }
            new ReadOnlyClient(clientId).start();
        }

        private static int applyNetworkingOverrides(String[] args, int startIndex) {
            int i = startIndex;
            while (i < args.length) {
                String arg = args[i];
                if ("--host".equalsIgnoreCase(arg) && i + 1 < args.length) {
                    System.setProperty(ServerConfig.PROP_SERVER_HOST, args[i + 1]);
                    i += 2;
                    continue;
                }
                if ("--port".equalsIgnoreCase(arg) && i + 1 < args.length) {
                    System.setProperty(ServerConfig.PROP_SERVER_PORT, args[i + 1]);
                    i += 2;
                    continue;
                }
                break;
            }
            return i;
        }

        private static int parseClientId(String[] args, int index) {
            if (args.length <= index) {
                if (index >= args.length) {
                    return 1;
                }
                try {
                    return Integer.parseInt(args[index]);
                } catch (NumberFormatException e) {
                    System.out.println("ID i klientit duhet të jetë numër.");
                    return -1;
                }
            }

            private static void printUsage() {
                System.out.println("Përdorimi: java Main <server|admin|read> [clientId]");
                System.out.println("  server      - starton UDPServer");
                System.out.println("  admin [id]  - starton klientin admin me ID opsionale");
                System.out.println("  read [id]   - starton klientin read-only me ID opsionale");
                System.out.println("Përdorimi: java Main <server|admin|read> [opsione] [clientId]");
                System.out.println("Opsionet: --host <hostname> --port <numri_portit>");
                System.out.println("  server         - starton UDPServer (parazgjedhje)");
                System.out.println("  admin [id]     - starton klientin admin me ID opsionale");
                System.out.println("  read [id]      - starton klientin read-only me ID opsionale");
            }
        }