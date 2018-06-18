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
func (p *Player) WriteBytes(b []byte) (int, error) {
	return p.conn.Write(b)
}

// nolint
func (p *Player) Close() error {
	return p.conn.Close()
}

// nolint
func (p *Player) GetAddr() string {
	return p.conn.RemoteAddr().String()
}
