# Paper Telephone

The final project for CSE 461. Siddhartha Gorti, Sarang Joshi, Jakob Sunde.

## Game flow

#### Output

- Hits the "Start Game" button:
    - Check to see that no other device has sent out a start signal
    - Broadcast START to all connected devices, and wait for START\_ACK's from all

#### Input

- Receives START from device D
    - If no START device has been saved:
        - Save D as the start and immediately respond with a START\_ACK
    - Else:
        - Respond with a START\_ACK if D has a **lower** address than the saved START device
    - After this, if the START device has changed, then set up all the unplaced devices

- Receives START\_ACK
    - If all devices are done ACK'ing, start the successor placement process.

## Packet Headers

### Lobby

- CONNECTED: Sent when a connection has initially been established
- DISCONNECTED: 

### Game

- START
- START\_ACK
