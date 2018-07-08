package main

import (
	"bufio"
	"bytes"
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"strings"
)

// Header constants
const (
	HeaderRoomCreate = iota
	HeaderRoomJoin   = iota
	HeaderStartGame  = iota
	HeaderSubmitTurn = iota
)

// Response codes
const (
	ResponseError       = iota
	ResponseSuccess     = iota
	ResponseStartedGame = iota
	ResponseNextTurn    = iota
	ResponseEndGame     = iota
)

var server Server

// A new player has joined the server
func handleConnection(player *Player) {
	fmt.Println("Handled new connection:", player.GetAddr())

	b := make([]byte, 1024)
	var buf bytes.Buffer

	for {
		// Handling a single packet
		for {
			len, err := player.Read(b)
			if err != nil {
				if err == io.EOF {
					fmt.Println("Connection closed!")
				} else {
					fmt.Println("Read error:", err)
				}

				// Disconnect logic
				player.Disconnect()
				return
			}
			buf.Write(b[:len])

			if len < 1024 {
				break
			}
		}

		// Packet has been fully read, dispatch
		response := server.HandlePacket(player, &buf)

		fmt.Println("Writing response of length", response.Len())
		player.Write(response.Bytes())

		// Trigger broadcast if any
		player.room.broadcast <- true
	}
}

const serverPort = 8080

func spin() {
	ln, err := net.Listen("tcp", fmt.Sprintf(":%d", serverPort))
	if err != nil {
		log.Fatal(err)
	}

	fmt.Println("Serving at 8080")
	for {
		conn, err := ln.Accept()
		if err != nil {
			fmt.Printf("Accept error: %v", err)
			return
		}

		player := NewPlayer(conn)

		go handleConnection(player)
	}
}

func handleUserCommands() {
	// TODO implement
	reader := bufio.NewReader(os.Stdin)
	for {
		text, err := reader.ReadString('\n')
		if err != nil {
			fmt.Fprintf(os.Stderr, fmt.Sprint(err))
			os.Exit(69)
			return
		}

		switch strings.TrimSpace(text) {
		case "r":
			for _, room := range server.Rooms {
				fmt.Println(room.String())
			}
			break
		}
	}
}

func main() {
	server = NewServer()

	go handleUserCommands()
	spin()
}
