package main

import (
	"net"
)

// Player represents a single player registered on the server
type Player struct {
	conn net.Conn
	room *Room
}

// nolint
func (p *Player) Read(b []byte) (int, error) {
	return p.conn.Read(b)
}

// nolint
func (p *Player) Write(b []byte) (int, error) {
	return p.conn.Write(b)
}

// nolint
func (p *Player) Disconnect() error {
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

// nolint
func (p *Player) GetAddr() string {
	return p.conn.RemoteAddr().String()
}

// NewPlayer creates a new player with the given connection
// TODO connection-agnostic?
func NewPlayer(conn net.Conn) *Player {
	player := &Player{}
	player.conn = conn
	player.room = nil

	return player
}
