The `InteractiveTestModbusTCP` class provides a command-line interface to demonstrate and test the `ModbusTCP` client. It allows users to connect to a server, set timeouts, and perform read/write operations on various Modbus data points interactively.

## Detailed Design and Documentation

For a comprehensive understanding of the Modbus protocol, the class component tree, and detailed explanations of functions and properties, please refer to the accompanying Miro board:

[https://miro.com/app/board/uXjVIEsAN3Q=/?share_link_id=797776246622](https://miro.com/app/board/uXjVIEsAN3Q=/?share_link_id=797776246622 "null")

## PLC Address Translation

The client includes a `plcAddressToModbus()` method to translate PLC-style addresses (e.g., `M1072`, `D500`) into their numerical Modbus equivalents. This is crucial for interfacing with PLCs that use specific internal addressing schemes (e.g., certain Delta PLCs).

-   `M` addresses (Coils): `M0`-`M1535` map to `0x800` + offset; `M1536`-`M8191` map to `0xB000` + (offset - 1536).
    
-   `D` addresses (Holding Registers): `D0`-`D9999` map to `0x1000` + offset.
    
-   `T` addresses (Timers): Map to `0x1C00` + offset.
    
-   `C` addresses (Counters): Map to `0x1E00` + offset.
    

## Getting Started

**Prerequisites:** Java Development Kit (JDK) 8 or higher.

1.  **Clone the repository:**
    
    ```
    git clone https://github.com/AmmarAlmuain/ModBus.git
    cd ModBus
    
    ```
    
2.  **Compile and Run the Interactive Client:**
    
    ```
    javac modbus/*.java
    java modbus.InteractiveTestModbusTCP
    
    ```
    
    Follow the menu prompts to interact with a Modbus TCP server.
    

For integration into your own Java project, simply include `ModbusTCP.java` and `ModbusException.java` in your source path and instantiate `ModbusTCP`.

## Error Handling

The client manages communication stability with:

-   **`ModbusException`**: For protocol-level errors (e.g., illegal data address, slave device failure).
    
-   **`IOException`**: For underlying network issues like connection timeouts or failures.
    
-   **`IllegalArgumentException`**: For invalid input parameters like incorrect addresses or quantities.
