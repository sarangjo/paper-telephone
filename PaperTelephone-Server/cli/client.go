package main

import (
	"bufio"
	"bytes"
	"encoding/binary"
	"fmt"
	"github.com/satori/go.uuid"
	"net"
	"os"
	"strings"
)

const debug = 1

const (
	HEADER_ROOM_CREATE = iota
	HEADER_ROOM_JOIN   = iota
	HEADER_START_GAME  = iota
)

// The current room
var currentRoom uuid.UUID

func main() {
	tcp_port := 8080
	server_addr, err := net.ResolveTCPAddr("tcp", fmt.Sprintf("127.0.0.1:%d", tcp_port))
	if err != nil {
		fmt.Println(err)
		return
	}

	// connect remote and local tcp ports
	conn, err := net.DialTCP("tcp", nil, server_addr)
	if err != nil {
		fmt.Println(err)
		return
	}

	// Start taking commands from user
	reader := bufio.NewReader(os.Stdin)

	header := make([]byte, 4)
	var buf bytes.Buffer
	done := false

	for !done {
		fmt.Print("Enter command: ")
		text, err := reader.ReadString('\n')
		if err != nil {
			fmt.Fprintf(os.Stderr, fmt.Sprint(err))
			return
		}

		command := strings.Split(strings.TrimSpace(text), " ")
		switch command[0] {
		case "c":
			// Create room
			binary.BigEndian.PutUint32(header, HEADER_ROOM_CREATE)
			len,_ := conn.Write(header)
			fmt.Println("Bytes written:", len)

			uuidBytes := make([]byte, uuid.Size)
			conn.Read(uuidBytes)
			currentRoom = uuid.FromBytesOrNil(uuidBytes)

			fmt.Println("Created and joined room:", currentRoom.String())
			break
		case "j":
			// Join room
			binary.BigEndian.PutUint32(header, HEADER_ROOM_JOIN)
			buf.Write(header)

			currentRoom = uuid.FromStringOrNil(command[1])
			buf.Write(currentRoom.Bytes())

			conn.Write(buf.Bytes())

			fmt.Println("Joined room:", currentRoom.String())
		case "s":
			binary.BigEndian.PutUint32(header, HEADER_START_GAME)
			conn.Write(header)
			break
		case "q":
			done = true
			break
		}
	}

	conn.Close()
}
