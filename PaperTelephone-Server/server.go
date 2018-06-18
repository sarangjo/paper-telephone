package main

import (
	"bytes"
	"encoding/binary"
	"fmt"
	"github.com/satori/go.uuid"
	"io"
	"log"
	"net"
	"sync"
)

var mutex *sync.Mutex
var rooms map[string]*Room

// TODO general: writing back to the player is pretty shoddy; distinguish
// between errors and normal

// Create a room on this server.
func CreateRoom(player Player) {
	fmt.Println("Creating a room")

	r := NewRoom()
	u := r.uuid.String()
	mutex.Lock()
	rooms[u] = r
	mutex.Unlock()
	if JoinRoom(player, u) {
		player.WriteBytes(r.uuid.Bytes())
	}
}

// Join a room on this server.
func JoinRoom(player Player, roomUuid string) bool {
	mutex.Lock()
	defer mutex.Unlock()

	if _, ok := rooms[roomUuid]; !ok {
		player.WriteError("Room does not exist")
		return false
	}
	if err := rooms[roomUuid].AddMember(player); err != nil {
		player.WriteError(fmt.Sprintf("%s", err))
		return false
	}
	fmt.Println("Joining a room")
	player.room = rooms[roomUuid]

	fmt.Println(rooms[roomUuid].String())

	return true
}

const (
	HEADER_ROOM_CREATE = iota
	HEADER_ROOM_JOIN   = iota
	HEADER_START_GAME  = iota
)

func HandlePacket(player Player, b *bytes.Buffer) {
	fmt.Println("Buffer length:", b.Len())

	// TODO copy this logic from the Android project
	headerBytes := b.Next(4)
	header := binary.BigEndian.Uint32(headerBytes)

	switch header {
	case HEADER_ROOM_CREATE:
		CreateRoom(player)
		break
	case HEADER_ROOM_JOIN:
		roomUuid := uuid.FromBytesOrNil(b.Next(uuid.Size))
		JoinRoom(player, roomUuid.String())
		break
	default:
		// Dispatch this to the room-specific stuff
		if player.room != nil {
			player.room.HandlePacket(player, b)
		}
		break
	}
}

// A new player has joined the server
func HandleConnection(player Player) {
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
				player.Close()
				if player.room != nil {
					player.room.RemoveMember(player)
					fmt.Println(player.room.String())
					player.room = nil
				}
				return
			}
			fmt.Println("Received packet of length", len)
			buf.Write(b[:len])

			if len < 1024 {
				break
			}
		}

		// Packet has been fully read, dispatch
		HandlePacket(player, &buf)
	}
}

const PORT = 8080

func Spin() {
	ln, err := net.Listen("tcp", fmt.Sprintf(":%d", PORT))
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
		player := Player{}
		player.conn = conn
		player.room = nil

		go HandleConnection(player)
	}
}

func HandleUserCommands() {
	// TODO implement
}

func main() {
	mutex = &sync.Mutex{}
	rooms = make(map[string]*Room)

	go HandleUserCommands()
	Spin()
}
