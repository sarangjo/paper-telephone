package main

import (
	"bufio"
	"bytes"
	"encoding/binary"
	"fmt"
	"net"
	"os"
	"strings"

	uuid "github.com/satori/go.uuid"
)

const debug = 1

const (
	HEADER_ROOM_CREATE = iota
	HEADER_ROOM_JOIN   = iota
	HEADER_START_GAME  = iota
	HEADER_SUBMIT_TURN = iota
)

const (
	ResponseError   = iota
	ResponseSuccess = iota
)

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
	response := make([]byte, 30)
	done := false

	for !done {
		fmt.Print("Enter command: ")
		text, err := reader.ReadString('\n')
		var buf bytes.Buffer
		if err != nil {
			fmt.Fprintf(os.Stderr, fmt.Sprint(err))
			return
		}

		command := strings.Split(strings.TrimSpace(text), " ")
		switch command[0] {
		case "c":
			// Create room
			binary.BigEndian.PutUint32(header, HEADER_ROOM_CREATE)
			len, _ := conn.Write(header)
			fmt.Println("Bytes written:", len)

			conn.Read(response)
			break
		case "j":
			// Join room
			binary.BigEndian.PutUint32(header, HEADER_ROOM_JOIN)
			buf.Write(header)

			buf.Write(uuid.FromStringOrNil(command[1]).Bytes())

			conn.Write(buf.Bytes())
			conn.Read(response)

			fmt.Println("Joined room")
		case "s":
			binary.BigEndian.PutUint32(header, HEADER_START_GAME)
			conn.Write(header)
			conn.Read(response)
			break
		case "t":
			binary.BigEndian.PutUint32(header, HEADER_SUBMIT_TURN)
			buf.Write(header)

			msg := strings.Join(command[1:], " ")
			buf.WriteString(msg)

			conn.Write(buf.Bytes())
			conn.Read(response)

			fmt.Println("submitted turn:", msg)
		case "q":
			done = true
			break
		}
	}

	conn.Close()
}
