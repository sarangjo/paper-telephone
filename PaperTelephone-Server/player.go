package main

import (
	"net"

	"github.com/gorilla/websocket"
)

// Player represents a single player registered on the server
type Player interface {
	Disconnect() error
	GetAddr() string
	Room() *Room
	Read(b []byte) (int, error)
	SetRoom(r *Room)
	Write(b []byte) (int, error)
}

// PlayerTCP is the TCP implementation of Player
type PlayerTCP struct {
	conn net.Conn
	room *Room
}

// Disconnect disconnects the player
func (p *PlayerTCP) Disconnect() error {
	err := p.conn.Close()
	if err != nil {
		return err
	}

	if p.room != nil {
		p.room.RemoveMember(p)
		// TODO potentially close up room if empty?
		p.room = nil
	}
	return nil
}

// GetAddr returns a string representation of this player's address
func (p *PlayerTCP) GetAddr() string {
	return p.conn.RemoteAddr().String()
}

// Read reads bytes into the given buffer, returning the number of bytes read
func (p *PlayerTCP) Read(b []byte) (int, error) {
	return p.conn.Read(b)
}

// Room returns a pointer to this player's room
func (p *PlayerTCP) Room() *Room {
	return p.room
}

// SetRoom sets the player's room
func (p *PlayerTCP) SetRoom(r *Room) {
	p.room = r
}

// Write writes bytes from the given buffer to the connection
func (p *PlayerTCP) Write(b []byte) (int, error) {
	return p.conn.Write(b)
}

// NewPlayerTCP creates a new TCP player with the given connection
func NewPlayerTCP(conn net.Conn) *PlayerTCP {
	player := &PlayerTCP{}
	player.conn = conn
	player.room = nil
	return player
}

// TODO use PreparedMessage for efficient broadcast

// PlayerWs is the Websocket implementation of Player
type PlayerWs struct {
	conn *websocket.Conn
	room *Room
}

// Disconnect disconnects the player
func (p *PlayerWs) Disconnect() error {
	err := p.conn.Close()
	if err != nil {
		return err
	}

	if p.room != nil {
		p.room.RemoveMember(p)
		// TODO potentially close up room if empty?
		p.room = nil
	}
	return nil
}

// GetAddr returns a string representation of this player's address
func (p *PlayerWs) GetAddr() string {
	return p.conn.RemoteAddr().String()
}

// Read reads bytes into the given buffer, returning the number of bytes read
func (p *PlayerWs) Read(b []byte) (int, error) {
	// TODO use message type
	_, r, err := p.conn.NextReader()
	if err != nil {
		return 0, err
	}

	return r.Read(b)
}

// Room returns a pointer to this player's room
func (p *PlayerWs) Room() *Room {
	return p.room
}

// SetRoom sets the player's room
func (p *PlayerWs) SetRoom(r *Room) {
	p.room = r
}

// Write writes bytes from the given buffer to the connection
func (p *PlayerWs) Write(b []byte) (int, error) {
	w, err := p.conn.NextWriter(websocket.TextMessage)
	if err != nil {
		return 0, err
	}
	return w.Write(b)
}

// NewPlayerWs creates a new Websocket player with the given connection
func NewPlayerWs(conn *websocket.Conn) *PlayerWs {
	player := &PlayerWs{}
	player.conn = conn
	player.room = nil
	return player
}
