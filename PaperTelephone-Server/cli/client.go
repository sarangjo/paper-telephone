package main

import (
	"bufio"
	"bytes"
	"encoding/binary"
	"fmt"
	"io"
	"net"
	"os"
	"strings"

	uuid "github.com/satori/go.uuid"
)

const debug = 1

// nolint
const (
	HEADER_ROOM_CREATE = iota
	HEADER_ROOM_JOIN   = iota
	HEADER_START_GAME  = iota
	HEADER_SUBMIT_TURN = iota
)

// nolint
const (
	ResponseError       = iota
	ResponseSuccess     = iota
	ResponseStartedGame = iota
	ResponseNextTurn    = iota
)

var conn net.Conn
var response chan bool

func listen() {
	b := make([]byte, 1024)
	for {
		_, err := conn.Read(b)
		if err != nil {
			if err == io.EOF {
				fmt.Println("Connection closed")
			}
		}

		code := binary.BigEndian.Uint32(b[:4])

		switch code {
		case ResponseError:
			fmt.Println("Error")
			response <- false
			break
		case ResponseSuccess:
			fmt.Println("Success!")
			response <- true
			break
		case ResponseStartedGame:
			fmt.Println("Game started")
			break
		case ResponseNextTurn:
			fmt.Println("next turn")
			break
		}
	}
}

func main() {
	response = make(chan bool)
	tcpPort := 8080
	serverAddr, err := net.ResolveTCPAddr("tcp", fmt.Sprintf("127.0.0.1:%d", tcpPort))
	if err != nil {
		fmt.Println(err)
		return
	}

	// connect remote and local tcp ports
	conn, err = net.DialTCP("tcp", nil, serverAddr)
	if err != nil {
		fmt.Println(err)
		return
	}

	// Start listening
	go listen()

	// Start taking commands from user
	reader := bufio.NewReader(os.Stdin)

	header := make([]byte, 4)

Loop:
	for {
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
			break
		case "j":
			// Join room
			binary.BigEndian.PutUint32(header, HEADER_ROOM_JOIN)
			buf.Write(header)

			buf.Write(uuid.FromStringOrNil(command[1]).Bytes())

			conn.Write(buf.Bytes())
			fmt.Println("Joined room")
		case "s":
			binary.BigEndian.PutUint32(header, HEADER_START_GAME)
			conn.Write(header)
			break
		case "t":
			binary.BigEndian.PutUint32(header, HEADER_SUBMIT_TURN)
			buf.Write(header)

			msg := strings.Join(command[1:], " ")
			buf.WriteString(msg)

			conn.Write(buf.Bytes())
			fmt.Println("submitted turn:", msg)
		case "q":
			break Loop
		}

		success := <-response
		if !success {
			break
		}
	}

	conn.Close()
}
