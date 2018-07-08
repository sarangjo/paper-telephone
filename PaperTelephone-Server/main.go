package main

import (
	"bufio"
	"bytes"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"os"
	"strings"

	"github.com/gorilla/websocket"
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

// TCP

const serverPort = 8181

// A new player has joined the server
func handleConnectionTCP(player *PlayerTCP) {
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

		// Trigger broadcast, if any
		player.Room().broadcast <- true
	}
}

func spinTCP() {
	ln, err := net.Listen("tcp", fmt.Sprintf(":%d", serverPort))
	if err != nil {
		log.Fatal(err)
	}

	fmt.Println("Serving at 8181")
	for {
		conn, err := ln.Accept()
		if err != nil {
			fmt.Printf("Accept error: %v", err)
			return
		}

		player := NewPlayerTCP(conn)

		go handleConnectionTCP(player)
	}
}

// WebSocket

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

func handleConnectionWs(player Player) {
}

func spinWs() {
	http.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
		conn, err := upgrader.Upgrade(w, r, nil)
		if err != nil {
			log.Println(err)
			return
		}

		player := NewPlayerWs(conn)

		go handleConnectionWs(player)
	})
}

// For testing purposes
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
	spinTCP()
}
