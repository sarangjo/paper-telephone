package main

import (
	"bytes"
	"encoding/binary"
	"fmt"
	"sync"
)

// Server represents a connection type agnostic Paper Telephone server.
type Server struct {
	mutex *sync.Mutex
	Rooms map[RoomID]*Room
}

// createRoom creates a room on this server. Returns the created room UUID
func (s *Server) createRoom(player *Player) (RoomID, error) {
	r := NewRoom()
	go r.Broadcaster()
	u := r.uuid
	s.mutex.Lock()
	s.Rooms[u] = r
	s.mutex.Unlock()
	if err := s.joinRoom(player, u); err != nil {
		return u, err
	}
	return u, nil
}

// joinRoom joins a room on this server.
func (s *Server) joinRoom(player *Player, roomUUID RoomID) error {
	s.mutex.Lock()
	defer s.mutex.Unlock()

	if _, ok := s.Rooms[roomUUID]; !ok {
		return fmt.Errorf("Room does not exist")
	}
	if err := s.Rooms[roomUUID].AddMember(player); err != nil {
		return fmt.Errorf("%s", err)
	}
	fmt.Println(s.Rooms[roomUUID].String())
	return nil
}

// HandlePacket handles a single packet sent from a player.
// Returns content and error, if any
func (s *Server) HandlePacket(player *Player, b *bytes.Buffer) *bytes.Buffer {
	// TODO copy this logic from the Android project
	header := binary.BigEndian.Uint32(b.Next(4))

	fmt.Println("Received packet of size", b.Len()+4, "with header", header)

	var contentBuf bytes.Buffer
	var err error

	switch header {
	case HeaderRoomCreate:
		fmt.Println("Creating a room")
		var u RoomID
		u, err = s.createRoom(player)
		if err == nil {
			contentBuf.Write(u.Bytes())
		}
		break
	case HeaderRoomJoin:
		roomUUID := RoomIdFromBytes(b.Next(RoomIdSize()))
		fmt.Println("Joining room", roomUUID.String())
		err = s.joinRoom(player, roomUUID)
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
		err = fmt.Errorf("Invalid header")
		break
	}

	responseCode := make([]byte, 4)
	if err == nil {
		binary.BigEndian.PutUint32(responseCode, ResponseSuccess)
	} else {
		fmt.Println(err)
		binary.BigEndian.PutUint32(responseCode, ResponseError)
	}
	var responseBuf bytes.Buffer
	responseBuf.Write(responseCode)
	responseBuf.Write(contentBuf.Bytes())

	return &responseBuf
}

// NewServer creates a new server
func NewServer() Server {
	s := Server{}
	s.mutex = &sync.Mutex{}
	s.Rooms = make(map[RoomID]*Room)

	return s
}
