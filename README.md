# Modbus TCP Client (Java)

This project provides a simplified Modbus TCP client in Java, developed as a university project to explore advanced networking and industrial communication protocols beyond the core curriculum. I am proud to note that this project successfully met and exceeded the professor's expectations, and I was the only student to complete it entirely, receiving recognition for this achievement.

## Table of Contents

-   [Modbus TCP Client (Java)](https://www.google.com/search?q=%23modbus-tcp-client-java "null")
    
    -   [Introduction to Modbus](https://www.google.com/search?q=%23introduction-to-modbus "null")
        
    -   [Project Overview](https://www.google.com/search?q=%23project-overview "null")
        
        -   [Features](https://www.google.com/search?q=%23features "null")
            
        -   [Class Structure (`ModbusTCP` and `ModbusException`)](https://www.google.com/search?q=%23class-structure-modbustcp-and-modbusexception "null")
            
        -   [Interactive Test Client (`InteractiveTestModbusTCP`)](https://www.google.com/search?q=%23interactive-test-client-interactivetestmodbustcp "null")
            
    -   [Detailed Design and Documentation](https://www.google.com/search?q=%23detailed-design-and-documentation "null")
        
    -   [PLC Address Translation](https://www.google.com/search?q=%23plc-address-translation "null")
        
    -   [Getting Started](https://www.google.com/search?q=%23getting-started "null")
        
    -   [Error Handling](https://www.google.com/search?q=%23error-handling "null")
        

## Introduction to Modbus

Modbus is a widely adopted industrial communication protocol. This project focuses on **Modbus TCP/IP**, which encapsulates Modbus messages over Ethernet, commonly used for PLCs.

Key Modbus concepts implemented:

-   **Modbus TCP/IP Framing:** Uses a standard MBAP header (Transaction ID, Protocol ID, Length, Unit ID) followed by the Protocol Data Unit (PDU).
    
-   **Data Types:** Interacts with **Coils** (single-bit, ON/OFF) and **Holding Registers** (16-bit word, numerical values).
    
-   **Function Codes:** Supports reading Coils (0x01) and Holding Registers (0x03), and writing Single Coil (0x05) and Single Register (0x06).
    

## Project Overview

This project implements a robust Modbus TCP client in Java, designed for straightforward interaction with Modbus TCP servers like PLCs.

### Features

-   **Connection Management:** Connect and disconnect from Modbus TCP servers.
    
-   **Modbus Operations:** Read/write Coils and Holding Registers.
    
-   **PLC Address Translation:** Convert common PLC-style addresses (e.g., `M100`, `D500`) to Modbus numerical addresses.
    
-   **Error Handling:** Custom `ModbusException` for protocol errors and robust I/O error handling.
    
-   **Interactive Test Client:** A command-line tool for quick testing and demonstration.
    

### Class Structure (`ModbusTCP` and `ModbusException`)

The core of the project is the `ModbusTCP` class.

-   **`ModbusTCP`**: This class contains all the logic for Modbus TCP communication. It handles:
    
    -   **Connection Management**: Establishing and closing socket connections.
        
    -   **Frame Construction**: Building Modbus request ADUs (MBAP header + PDU) for various function codes.
        
    -   **Data Transmission**: Sending requests and receiving responses over TCP streams.
        
    -   **Response Parsing**: Interpreting received Modbus responses and handling exceptions.
        
    -   **PLC Address Translation**: Converting user-friendly PLC addresses (like "M0", "D100") into the numerical Modbus addresses.
        
    -   **High-Level Functions**: User-friendly methods for `readCoils`, `readHoldingRegisters`, `writeSingleCoil`, and `writeSingleRegister`.
        
-   **`ModbusException`**: A custom `IOException` subclass for Modbus-specific protocol errors, providing detailed messages based on the Modbus exception code.
    

### Interactive Test Client (`InteractiveTestModbusTCP`)

The `InteractiveTestModbusTCP` class provides a command-line interface to demonstrate and test the `ModbusTCP` client. It allows users to connect to a server, set timeouts, and perform read/write operations on various Modbus data points interactively.

## Detailed Design and Documentation

For a comprehensive understanding of the Modbus protocol, the class component tree, and detailed explanations of functions and properties, please refer to the accompanying Miro board:

[https://miro.com/app/board/uXjVIEsAN3Q=/?share_link_id=797776246622](https://miro.com/app/board/uXjVIEsAN3Q= "null")

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
