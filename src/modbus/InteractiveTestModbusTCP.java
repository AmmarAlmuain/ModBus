package modbus;

import java.io.IOException;
import java.net.SocketException;
import java.util.InputMismatchException;
import java.util.Scanner;

import modbus.ModbusException;
import modbus.ModbusTCP;

public class InteractiveTestModbusTCP {

    private static Scanner scanner = new Scanner(System.in);
    private static ModbusTCP client = new modbus.ModbusTCP();
    private static String lastIp = "127.0.0.1";
    private static int lastPort = 502;

    public static void main(String[] args) {
        System.out.println("Interactive Modbus TCP Client Test (PLC Addresses)");
        System.out.println("=================================================");

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Enter choice: ");
            String choice = scanner.nextLine();

            try {
                switch (choice.trim()) {
                    case "1":
                        connectToServer();
                        break;
                    case "2":
                        testReadCoils();
                        break;
                    case "3":
                        testReadHoldingRegisters();
                        break;
                    case "4":
                        testWriteSingleCoil();
                        break;
                    case "5":
                        testWriteSingleRegister();
                        break;
                    case "9":
                        disconnectFromServer();
                        break;
                    case "0":
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice, please try again.");
                        break;
                }
            } catch (ModbusException me) {
                handleModbusError(me);
            } catch (IOException ioe) {
                handleIoError(ioe);
            } catch (NumberFormatException nfe) {
                handleInputError("Invalid number entered.");
            } catch (IllegalArgumentException iae) {
                handleInputError(iae.getMessage());
            } catch (Exception e) {
                handleUnexpectedError(e);
            }

            if (running) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }

        disconnectFromServer();
        System.out.println("Exiting test application.");
        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\n--- Modbus Test Menu ---");
        System.out.println(
                "Status: " + (client.isConnected() ? "Connected to " + lastIp + ":" + lastPort : "Disconnected"));
        System.out.println("1. Connect");
        System.out.println("2. Read Coils (e.g., M0)");
        System.out.println("3. Read Holding Registers (e.g., D100)");
        System.out.println("4. Write Single Coil (e.g., M5)");
        System.out.println("5. Write Single Register (e.g., D200)");
        System.out.println("------------------------");
        System.out.println("9. Disconnect");
        System.out.println("0. Exit");
        System.out.println("------------------------");
    }

    private static boolean checkConnection() {
        if (!client.isConnected()) {
            System.out.println("Not connected. Please use option 1 to connect first.");
            return false;
        }
        return true;
    }

    // --- Menu Action Methods ---

    private static void connectToServer() {
        if (client.isConnected()) {
            System.out.println("Already connected. Disconnect first (option 9) to change server.");
            return;
        }
        try {
            System.out.print("Enter Server IP [" + lastIp + "]: ");
            String ip = scanner.nextLine();
            if (ip.isEmpty())
                ip = lastIp;

            System.out.print("Enter Port [" + lastPort + "]: ");
            String portStr = scanner.nextLine();
            int port = portStr.isEmpty() ? lastPort : Integer.parseInt(portStr);

            System.out.print("Enter Timeout (ms) [3000]: ");
            String timeoutStr = scanner.nextLine();
            int timeout = timeoutStr.isEmpty() ? 3000 : Integer.parseInt(timeoutStr);

            client.setTimeout(timeout);
            client.connect(ip, port);

            lastIp = ip;
            lastPort = port;
            System.out.println("Successfully connected!");

        } catch (NumberFormatException e) {
            System.err.println("Invalid number entered for port or timeout.");
        } catch (SocketException se) {
            System.err.println("Error setting timeout: " + se.getMessage());
        } catch (IOException e) {
            System.err.println("Connection failed: " + e.getMessage());
            client.disconnect();
        }
    }

    private static void disconnectFromServer() {
        if (client.isConnected()) {
            client.disconnect();
            System.out.println("Disconnected.");
        } else {
            System.out.println("Already disconnected.");
        }
    }

    private static void testReadCoils() throws IOException, ModbusException {
        if (!checkConnection())
            return;

        System.out.print("Enter Unit ID [1]: ");
        String unitIdStr = scanner.nextLine();
        int unitId = unitIdStr.isEmpty() ? 1 : Integer.parseInt(unitIdStr);

        System.out.print("Enter Start Address (e.g., M0) [M0]: ");
        String startAddrStr = scanner.nextLine();
        if (startAddrStr.isEmpty())
            startAddrStr = "M0";

        System.out.print("Enter Quantity [8]: ");
        String quantityStr = scanner.nextLine();
        int quantity = quantityStr.isEmpty() ? 8 : Integer.parseInt(quantityStr);

        boolean[] values = client.readCoils(unitId, startAddrStr, quantity);

        System.out.println("Read Coils Result (" + quantity + " coils starting at " + startAddrStr + "):");
        System.out.print("  [");
        for (int i = 0; i < values.length; i++) {
            System.out.print(values[i] ? "1" : "0");
            if (i < values.length - 1)
                System.out.print(", ");
        }
        System.out.println("]");
    }

    private static void testReadHoldingRegisters() throws IOException, ModbusException {
        if (!checkConnection())
            return;

        System.out.print("Enter Unit ID [1]: ");
        String unitIdStr = scanner.nextLine();
        int unitId = unitIdStr.isEmpty() ? 1 : Integer.parseInt(unitIdStr);

        System.out.print("Enter Start Address (e.g., D100) [D100]: ");
        String startAddrStr = scanner.nextLine();
        if (startAddrStr.isEmpty())
            startAddrStr = "D100";

        System.out.print("Enter Quantity [2]: ");
        String quantityStr = scanner.nextLine();
        int quantity = quantityStr.isEmpty() ? 2 : Integer.parseInt(quantityStr);

        int[] values = client.readHoldingRegisters(unitId, startAddrStr, quantity);

        System.out.println("Read Registers Result (" + quantity + " registers starting at " + startAddrStr + "):");
        for (int i = 0; i < values.length; i++) {
            System.out.printf("  Offset %d: %d (0x%04X)\n", i, values[i], values[i]);
        }
    }

    private static void testWriteSingleCoil() throws IOException, ModbusException {
        if (!checkConnection())
            return;

        System.out.print("Enter Unit ID [1]: ");
        String unitIdStr = scanner.nextLine();
        int unitId = unitIdStr.isEmpty() ? 1 : Integer.parseInt(unitIdStr);

        System.out.print("Enter Coil Address to Write (e.g., M5) [M5]: ");
        String addrStr = scanner.nextLine();
        if (addrStr.isEmpty())
            addrStr = "M5";

        System.out.print("Enter Value (1=ON, 0=OFF) [1]: ");
        String valueStr = scanner.nextLine();
        boolean value = valueStr.isEmpty() ? true : (Integer.parseInt(valueStr) != 0);

        client.writeSingleCoil(unitId, addrStr, value);
        System.out.println("Write Single Coil command sent successfully for address " + addrStr + " with value "
                + (value ? "ON" : "OFF") + ".");
    }

    private static void testWriteSingleRegister() throws IOException, ModbusException {
        if (!checkConnection())
            return;

        System.out.print("Enter Unit ID [1]: ");
        String unitIdStr = scanner.nextLine();
        int unitId = unitIdStr.isEmpty() ? 1 : Integer.parseInt(unitIdStr);

        System.out.print("Enter Register Address to Write (e.g., D200) [D200]: ");
        String addrStr = scanner.nextLine();
        if (addrStr.isEmpty())
            addrStr = "D200";

        System.out.print("Enter Value (0-65535) [12345]: ");
        String valueStr = scanner.nextLine();
        int value = valueStr.isEmpty() ? 12345 : Integer.parseInt(valueStr);

        client.writeSingleRegister(unitId, addrStr, value);
        System.out.println("Write Single Register command sent successfully for address " + addrStr + " with value "
                + value + ".");
    }

    // --- Error Handling Helper Methods ---

    private static void handleModbusError(ModbusException me) {
        System.err.println("\n### MODBUS PROTOCOL ERROR ###");
        System.err.println("  Message: " + me.getMessage());
        System.err.println("  Code: 0x" + String.format("%02X", me.getExceptionCode()));
        System.err.println("#############################");
    }

    private static void handleIoError(IOException ioe) {
        System.err.println("\n### NETWORK/IO ERROR ###");
        System.err.println("  Message: " + ioe.getMessage());
        System.err.println("########################");
    }

    private static void handleInputError(String message) {
        System.err.println("\n### INPUT ERROR ###");
        System.err.println("  Message: " + message);
        System.err.println("###################");
    }

    private static void handleUnexpectedError(Exception e) {
        System.err.println("\n### UNEXPECTED ERROR ###");
        System.err.println("  Message: " + e.getMessage());
        e.printStackTrace();
        System.err.println("########################");
    }
}