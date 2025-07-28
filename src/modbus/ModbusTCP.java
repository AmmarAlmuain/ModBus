package modbus;

import java.io.*;
import java.net.*;

/**
 * ============================================================================
 * Modbus TCP Client - Single File Implementation
 * ============================================================================
 * Provides a simplified Modbus TCP client consolidated into a single file.
 * Handles connection management and provides methods for Modbus communication.
 * Refactored from a multi-class structure for simplicity.
 */
public class ModbusTCP {

    // --- Configuration ---
    private int timeoutMillis = 5000;

    // --- Connection State ---
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private boolean connected = false;

    // --- Request Building State ---
    private int transactionIdCounter = 0;

    // ========================================================================
    // Section: Connection Management
    // Purpose: Handles establishing, maintaining, and closing the socket
    // connection to the Modbus TCP server.
    // ========================================================================

    public void connect(String serverAddress, int serverPort) throws IOException {
        if (connected) {
            System.out.println("INFO: Already connected. Disconnect first to reconnect.");
            return;
        }
        System.out.println("INFO: Attempting to connect to " + serverAddress + ":" + serverPort + "...");
        try {
            socket = new Socket();
            InetSocketAddress endpoint = new InetSocketAddress(serverAddress, serverPort);
            socket.connect(endpoint, timeoutMillis);
            socket.setSoTimeout(timeoutMillis);

            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            connected = true;
            System.out.println("INFO: Successfully connected.");

        } catch (SocketTimeoutException e) {
            disconnect();
            throw new IOException("ERROR: Connection timed out after " + timeoutMillis + "ms: " + e.getMessage(), e);
        } catch (IOException e) {
            disconnect();
            throw new IOException("ERROR: Connection failed: " + e.getMessage(), e);
        }
    }

    public void disconnect() {
        if (!connected && socket == null && out == null && in == null) {
            return;
        }
        System.out.println("INFO: Disconnecting...");
        connected = false;

        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                System.err.println("ERROR: Error closing input stream: " + e.getMessage());
            } finally {
                in = null;
            }
        }

        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                System.err.println("ERROR: Error closing output stream: " + e.getMessage());
            } finally {
                out = null;
            }
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("ERROR: Error closing socket: " + e.getMessage());
            } finally {
                socket = null;
            }
        }
        System.out.println("INFO: Disconnected.");
    }

    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void setTimeout(int timeoutMillis) throws SocketException {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative");
        }
        this.timeoutMillis = timeoutMillis;
        if (socket != null && isConnected()) {
            socket.setSoTimeout(this.timeoutMillis);
            System.out.println("INFO: Socket read timeout updated to " + this.timeoutMillis + " ms.");
        } else {
            System.out.println(
                    "INFO: Socket read timeout set to " + this.timeoutMillis + " ms (will apply on next connection).");
        }
    }

    // --- End of Connection Management Section ---

    // ========================================================================
    // Section: Modbus Frame Construction
    // Purpose: Methods to build the byte arrays for Modbus requests (MBAP + PDU).
    // (Based directly on the provided RequestBuilder.java code)
    // ========================================================================

    /*
     * Note:
     * & 0xFFFF keeps the counter within the valid 0-65535 range needed for the
     * 16-bit Modbus Transaction ID.
     */
    private synchronized int getNextTransactionId() {
        transactionIdCounter = (transactionIdCounter + 1) & 0xFFFF;
        // or transactionIdCounter = (transactionIdCounter + 1) % 65536;
        return transactionIdCounter;
    }

    /*
     * General Notes:
     * (value >>> 8) This gets the HIGH byte
     * (value & 0xFF) This gets the LOW byte
     * pdu is the Protocol Data Unit (the actual Modbus command and data)
     * its involve function code and data (such as address, value, etc.)
     */

    public byte[] buildReadCoilsRequest(int unitId, int startAddress, int quantity) {
        validateReadQuantity(quantity, 2000);

        byte[] pdu = new byte[5];
        pdu[0] = 0x01;
        pdu[1] = (byte) (startAddress >>> 8);
        pdu[2] = (byte) (startAddress & 0xFF);
        pdu[3] = (byte) (quantity >>> 8);
        pdu[4] = (byte) (quantity & 0xFF);

        return buildMbapFrame(unitId, pdu);
    }

    public byte[] buildReadRegistersRequest(int unitId, int startAddress, int quantity) {
        validateReadQuantity(quantity, 125);

        byte[] pdu = new byte[5];
        pdu[0] = 0x03;
        pdu[1] = (byte) (startAddress >>> 8);
        pdu[2] = (byte) (startAddress & 0xFF);
        pdu[3] = (byte) (quantity >>> 8);
        pdu[4] = (byte) (quantity & 0xFF);

        return buildMbapFrame(unitId, pdu);
    }

    public byte[] buildWriteSingleCoilRequest(int unitId, int address, boolean value) {
        byte[] pdu = new byte[5];
        pdu[0] = 0x05;
        pdu[1] = (byte) (address >>> 8);
        pdu[2] = (byte) (address & 0xFF);
        pdu[3] = (byte) (value ? 0xFF : 0x00);
        pdu[4] = 0x00;

        return buildMbapFrame(unitId, pdu);
    }

    public byte[] buildWriteSingleRegisterRequest(int unitId, int address, int value) {
        byte[] pdu = new byte[5];
        pdu[0] = 0x06;
        pdu[1] = (byte) (address >>> 8);
        pdu[2] = (byte) (address & 0xFF);
        pdu[3] = (byte) (value >>> 8);
        pdu[4] = (byte) (value & 0xFF);

        return buildMbapFrame(unitId, pdu);
    }

    private byte[] buildMbapFrame(int unitId, byte[] pdu) {
        byte[] mbap = new byte[7];
        int transactionId = getNextTransactionId();

        mbap[0] = (byte) (transactionId >>> 8);
        mbap[1] = (byte) (transactionId & 0xFF);
        mbap[2] = 0x00;
        mbap[3] = 0x00;
        mbap[4] = (byte) ((pdu.length + 1) >>> 8);
        mbap[5] = (byte) ((pdu.length + 1) & 0xFF);
        mbap[6] = (byte) unitId;

        byte[] adu = new byte[mbap.length + pdu.length];
        System.arraycopy(mbap, 0, adu, 0, mbap.length);
        System.arraycopy(pdu, 0, adu, mbap.length, pdu.length);
        return adu;
    }

    private void validateReadQuantity(int quantity, int max) {
        if (quantity < 1 || quantity > max) {
            throw new IllegalArgumentException("Quantity must be between 1 and " + max + ", but was " + quantity);
        }
    }

    // --- End of Modbus Frame Construction Section ---

    // ========================================================================
    // Section: Modbus Frame Sending/Receiving
    // Purpose: Methods to send request bytes and receive response bytes via
    // streams.
    // ========================================================================

    private synchronized byte[] executeTransaction(byte[] requestAdu) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected. Cannot execute transaction.");
        }
        if (requestAdu == null || requestAdu.length < 8) {
            throw new IllegalArgumentException("Invalid request ADU provided.");
        }

        try {
            out.write(requestAdu);
            out.flush();

            byte[] responseMbap = new byte[7];
            in.readFully(responseMbap, 0, 7);

            int responseLength = ((responseMbap[4] & 0xFF) << 8) | (responseMbap[5] & 0xFF);

            if (responseLength < 1) {
                throw new IOException("Invalid response length in MBAP header: " + responseLength);
            }

            int pduLength = responseLength - 1;

            byte[] responsePdu = new byte[pduLength];
            if (pduLength > 0) {
                in.readFully(responsePdu, 0, pduLength);
            }

            byte[] responseAdu = new byte[responseMbap.length + responsePdu.length];
            System.arraycopy(responseMbap, 0, responseAdu, 0, responseMbap.length);
            System.arraycopy(responsePdu, 0, responseAdu, responseMbap.length, responsePdu.length);

            return responseAdu;

        } catch (SocketTimeoutException e) {
            System.err.println("ERROR: Modbus read timeout after " + this.timeoutMillis + " ms.");
            throw new IOException("Modbus read timed out", e);
        } catch (IOException e) {
            System.err.println("ERROR: Modbus communication error: " + e.getMessage());
            disconnect();
            throw e;
        }
    }

    // --- End of Modbus Frame Sending/Receiving Section ---

    // ========================================================================
    // Section: Modbus Response Parsing
    // Purpose: Methods to parse the byte arrays received from the server.
    // ========================================================================

    private boolean[] parseReadCoilsResponse(byte[] responseAdu, int expectedQuantity)
            throws ModbusException, IOException {
        byte[] pdu = extractPdu(responseAdu);
        validateFunctionCode(pdu, 0x01);

        if (pdu.length < 2) {
            throw new IOException("Read Coils PDU too short.");
        }

        int byteCount = pdu[1] & 0xFF;
        int expectedByteCount = (expectedQuantity + 7) / 8;

        if (byteCount != expectedByteCount) {
            throw new IOException("Read Coils response byte count mismatch.");
        }
        if (pdu.length != (2 + byteCount)) {
            throw new IOException("Read Coils response PDU length mismatch.");
        }

        byte[] coilDataBytes = new byte[byteCount];
        System.arraycopy(pdu, 2, coilDataBytes, 0, byteCount);

        java.util.BitSet bitSet = java.util.BitSet.valueOf(coilDataBytes);
        boolean[] coils = new boolean[expectedQuantity];
        for (int i = 0; i < expectedQuantity; i++) {
            coils[i] = bitSet.get(i);
        }

        return coils;
    }

    private int[] parseReadRegistersResponse(byte[] responseAdu, int expectedQuantity)
            throws ModbusException, IOException {
        byte[] pdu = extractPdu(responseAdu);
        validateFunctionCode(pdu, 0x03);

        if (pdu.length < 2) {
            throw new IOException("Read Registers PDU too short.");
        }

        int byteCount = pdu[1] & 0xFF;
        int expectedByteCount = expectedQuantity * 2;

        if (byteCount != expectedByteCount) {
            throw new IOException("Read Registers response byte count mismatch.");
        }
        if (pdu.length != (2 + byteCount)) {
            throw new IOException("Read Registers response PDU length mismatch.");
        }

        int[] registers = new int[expectedQuantity];
        ByteArrayInputStream bais = new ByteArrayInputStream(pdu, 2, byteCount);
        DataInputStream dis = new DataInputStream(bais);

        for (int i = 0; i < expectedQuantity; i++) {
            registers[i] = dis.readUnsignedShort();
        }

        return registers;
    }

    private void validateWriteResponse(byte[] responseAdu, int expectedFunctionCode)
            throws ModbusException, IOException {
        byte[] pdu = extractPdu(responseAdu);
        validateFunctionCode(pdu, expectedFunctionCode);
    }

    private byte[] extractPdu(byte[] responseAdu) throws ModbusException, IOException {
        if (responseAdu == null || responseAdu.length < 8) {
            throw new IOException("Response ADU too short or null.");
        }

        byte functionCode = responseAdu[7];
        if ((functionCode & 0x80) != 0) {
            if (responseAdu.length < 9) {
                throw new IOException("Malformed Modbus exception response.");
            }
            byte exceptionCode = responseAdu[8];
            throw new ModbusException(exceptionCode);
        }

        int pduLength = responseAdu.length - 7;
        if (pduLength < 1) {
            throw new IOException("Calculated PDU length is less than 1.");
        }
        byte[] pdu = new byte[pduLength];
        System.arraycopy(responseAdu, 7, pdu, 0, pduLength);

        return pdu;
    }

    private static void validateFunctionCode(byte[] pdu, int expectedCode) throws IOException {
        if (pdu == null || pdu.length == 0) {
            throw new IOException("Cannot validate function code on empty/null PDU.");
        }
        if ((pdu[0] & 0x7F) != (expectedCode & 0x7F)) {
            throw new IOException(String.format(
                    "Function code mismatch. Expected: %02X, Received: %02X",
                    expectedCode, pdu[0]));
        }
    }

    // --- End of Modbus Response Parsing Section ---

    // ========================================================================
    // Section: Address Translation
    // Purpose: Translates PLC-specific addresses to Modbus numerical addresses.
    // ========================================================================

    // Translates PLC-style address (e.g., "M1072", "D500") to Modbus address
    // IMPORTANT: Logic here is specific to PLC type (likely Delta).
    private int plcAddressToModbus(String plcAddress) throws IllegalArgumentException {
        if (plcAddress == null || plcAddress.length() < 2) {
            throw new IllegalArgumentException("Invalid PLC address format");
        }

        String area = plcAddress.substring(0, 1).toUpperCase();
        int offset;

        try {
            offset = Integer.parseInt(plcAddress.substring(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid address number: " + plcAddress);
        }

        switch (area) {
            case "M":
                if (offset >= 0 && offset < 1536) {
                    return 0x800 + offset;
                } else if (offset >= 1536 && offset <= 8191) {
                    return 0xB000 + (offset - 1536);
                } else {
                    throw new IllegalArgumentException("M address offset out of range: " + offset);
                }

            case "D":
                if (offset < 0 || offset > 9999) {
                    throw new IllegalArgumentException("D register address offset out of range (0-9999): " + offset);
                }
                return 0x1000 + offset;

            case "T":
                return 0x1C00 + offset;

            case "C":
                return 0x1E00 + offset;

            default:
                throw new IllegalArgumentException("Unsupported PLC address area: " + area);
        }
    }

    // --- End of Address Translation Section ---

    // ========================================================================
    // Section: High-Level Modbus Functions
    // Purpose: User-friendly methods like readHoldingRegisters, writeSingleCoil
    // etc.
    // ========================================================================

    public boolean[] readCoils(int unitId, String plcStartAddress, int quantity)
            throws IOException, ModbusException, IllegalArgumentException {
        int startAddress = plcAddressToModbus(plcStartAddress);
        return readCoilsInternal(unitId, startAddress, quantity);
    }

    private boolean[] readCoilsInternal(int unitId, int startAddress, int quantity)
            throws IOException, ModbusException {
        if (!isConnected()) {
            throw new IOException("Not connected.");
        }
        byte[] requestAdu = buildReadCoilsRequest(unitId, startAddress, quantity);
        byte[] responseAdu = executeTransaction(requestAdu);
        return parseReadCoilsResponse(responseAdu, quantity);
    }

    public int[] readHoldingRegisters(int unitId, String plcStartAddress, int quantity)
            throws IOException, ModbusException, IllegalArgumentException {
        int startAddress = plcAddressToModbus(plcStartAddress);
        return readHoldingRegistersInternal(unitId, startAddress, quantity);
    }

    private int[] readHoldingRegistersInternal(int unitId, int startAddress, int quantity)
            throws IOException, ModbusException {
        if (!isConnected()) {
            throw new IOException("Not connected.");
        }
        byte[] requestAdu = buildReadRegistersRequest(unitId, startAddress, quantity);
        byte[] responseAdu = executeTransaction(requestAdu);
        return parseReadRegistersResponse(responseAdu, quantity);
    }

    public void writeSingleCoil(int unitId, String plcAddress, boolean value)
            throws IOException, ModbusException, IllegalArgumentException {
        int address = plcAddressToModbus(plcAddress);
        writeSingleCoilInternal(unitId, address, value);
    }

    private void writeSingleCoilInternal(int unitId, int address, boolean value) throws IOException, ModbusException {
        if (!isConnected()) {
            throw new IOException("Not connected.");
        }
        byte[] requestAdu = buildWriteSingleCoilRequest(unitId, address, value);
        byte[] responseAdu = executeTransaction(requestAdu);
        validateWriteResponse(responseAdu, 0x05);
    }

    public void writeSingleRegister(int unitId, String plcAddress, int value)
            throws IOException, ModbusException, IllegalArgumentException {
        int address = plcAddressToModbus(plcAddress);
        writeSingleRegisterInternal(unitId, address, value);
    }

    private void writeSingleRegisterInternal(int unitId, int address, int value) throws IOException, ModbusException {
        if (!isConnected()) {
            throw new IOException("Not connected.");
        }
        byte[] requestAdu = buildWriteSingleRegisterRequest(unitId, address, value);
        byte[] responseAdu = executeTransaction(requestAdu);
        validateWriteResponse(responseAdu, 0x06);
    }

    // --- End of High-Level Modbus Functions Section ---

} // --- End of ModbusTCP class ---

// ========================================================================
// Section: Modbus Exception Class
// ========================================================================

class ModbusException extends IOException {
    private final byte exceptionCode;

    public ModbusException(byte exceptionCode) {
        super(getModbusExceptionMessage(exceptionCode) + " (Code: 0x" + String.format("%02X", exceptionCode & 0xFF)
                + ")");
        this.exceptionCode = exceptionCode;
    }

    public byte getExceptionCode() {
        return exceptionCode;
    }

    // Provides standard Modbus exception messages.
    public static String getModbusExceptionMessage(byte code) {
        switch (code) {
            case 0x01:
                return "Illegal Function";
            case 0x02:
                return "Illegal Data Address";
            case 0x03:
                return "Illegal Data Value";
            case 0x04:
                return "Slave Device Failure"; // Or Server Failure
            case 0x05:
                return "Acknowledge"; // Not always an error
            case 0x06:
                return "Slave Device Busy"; // Or Server Busy
            case 0x07:
                return "Negative Acknowledge";
            case 0x08:
                return "Memory Parity Error";
            case 0x0A:
                return "Gateway Path Unavailable";
            case 0x0B:
                return "Gateway Target Device Failed to Respond";
            default:
                return "Unknown or Unspecified Modbus Exception";
        }
    }
}
