package main

import (
	"bufio"
	"bytes"
	"encoding/binary"
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"strings"
	"sync"
)

var mutex *sync.Mutex
var rooms map[RoomID]*Room

// Creates a room on this server. Returns the created room UUID
func createRoom(player *Player) (RoomID, error) {
	r := NewRoom()
	u := r.uuid
	mutex.Lock()
	rooms[u] = r
	mutex.Unlock()
	if err := joinRoom(player, u); err != nil {
		return u, err
	}
	return u, nil
}

// Joins a room on this server.
func joinRoom(player *Player, roomUUID RoomID) error {
	mutex.Lock()
	defer mutex.Unlock()

	if _, ok := rooms[roomUUID]; !ok {
		return fmt.Errorf("Room does not exist")
	}
	if err := rooms[roomUUID].AddMember(player); err != nil {
		return fmt.Errorf("%s", err)
	}
	fmt.Println(rooms[roomUUID].String())
	return nil
}

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

// Returns content and error, if any
func handlePacket(player *Player, b *bytes.Buffer) (*bytes.Buffer, error) {
	// TODO copy this logic from the Android project
	len := b.Len()
	headerBytes := b.Next(4)
	header := binary.BigEndian.Uint32(headerBytes)

	fmt.Println("Received packet of size", len, "with header", header)

	var contentBuf bytes.Buffer
	var err error

	switch header {
	case HeaderRoomCreate:
		fmt.Println("Creating a room")
		var u RoomID
		u, err = createRoom(player)
		if err == nil {
			contentBuf.Write(u.Bytes())
		}
		break
	case HeaderRoomJoin:
		roomUUID := RoomIdFromBytes(b.Next(RoomIdSize()))
		fmt.Println("Joining room", roomUUID.String())
		err = joinRoom(player, roomUUID)
		break
	case HeaderStartGame:
		if player.room != nil {
			err = player.room.StartGame()
		} else {
			err = fmt.Errorf("Player not in room")
		}
		break
	case HeaderSubmitTurn:
		if player.room != nil {
			// Extract the remainder of the bytes of the packet
			data := b.Next(b.Len())
			err = player.room.SubmitTurn(player, data)
		} else {
			err = fmt.Errorf("Player not in room")
		}
		break
	default:
		fmt.Println("Invalid header")
		err = fmt.Errorf("Invalid header")
		break
	}

	return &contentBuf, err
}

func disconnectPlayer(player *Player) {
	player.Close()
	if player.room != nil {
		player.room.RemoveMember(player)
		fmt.Println(player.room.String())

		// TODO potentially close up room if empty?

		player.room = nil
	}
}

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
				disconnectPlayer(player)
				return
			}
			buf.Write(b[:len])

			if len < 1024 {
				break
			}
		}

		// Packet has been fully read, dispatch
		// TODO collect any other packets that need to be sent separately and
		// send AFTER the success is sent
		contentBuf, err := handlePacket(player, &buf)
		var responseBuf bytes.Buffer

		responseCode := make([]byte, 4)
		binary.BigEndian.PutUint32(responseCode, ResponseSuccess)
		if err != nil {
			fmt.Println(err)
			binary.BigEndian.PutUint32(responseCode, ResponseError)
		}
		responseBuf.Write(responseCode)
		responseBuf.Write(contentBuf.Bytes())

		fmt.Println("Writing response of length", responseBuf.Len())
		player.WriteBytes(responseBuf.Bytes())
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
		// Register the connection
		player := &Player{}
		player.conn = conn
		player.room = nil

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
			for _, room := range rooms {
				fmt.Println(room.String())
			}
			break
		}
	}
}

func main() {
	mutex = &sync.Mutex{}
	rooms = make(map[RoomID]*Room)

	go handleUserCommands()
	spin()
}
