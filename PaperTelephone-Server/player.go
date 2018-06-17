package main

import (
	"net"
)

type Player struct {
	conn net.Conn
	room *Room
}

func (this *Player) Read(b []byte) (int, error) {
	return this.conn.Read(b)
}

func (this *Player) WriteError(s string) (int, error) {
	// TODO first write an error header
	return this.WriteString(s)
}

func (this *Player) WriteString(s string) (int, error) {
	return this.WriteBytes([]byte(s))
}

func (this *Player) WriteBytes(b []byte) (int, error) {
	return this.conn.Write(b)
}

func (this *Player) Close() error {
	return this.conn.Close()
}

func (this *Player) GetAddr() string {
	return this.conn.RemoteAddr().String()
}
