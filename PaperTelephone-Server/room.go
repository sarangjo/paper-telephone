package main

import (
	"container/ring"
	"errors"
	"github.com/satori/go.uuid"
	"sync"
)

const (
	STATE_LOBBY     = iota
	STATE_PLACEMENT = iota
	STATE_GAME      = iota
	STATE_END_GAME  = iota
)

type Room struct {
	members map[string]bool
	mutex   *sync.Mutex
	ring	*ring.Ring
	state   int
	uuid    uuid.UUID
}

func newRoom() *Room {
	r := &Room{}
	r.members = make(map[string]bool)
	r.mutex = &sync.Mutex{}
	r.ring = nil
	r.state = STATE_LOBBY
	r.uuid = uuid.Must(uuid.NewV4())
	return r
}

func (this *Room) AddMember(remoteAddr string) error {
	this.mutex.Lock()
	defer this.mutex.Unlock()

	if this.state != STATE_LOBBY {
		return errors.New("Room must be in lobby to join")
	}
	if _, ok := this.members[remoteAddr]; ok {
		return errors.New("User already member of room")
	}
	this.members[remoteAddr] = true
	return nil
}

func (this *Room) StartGame() error {
	this.mutex.Lock()
	defer this.mutex.Unlock()

	if this.state != STATE_LOBBY {
		return errors.New("Game can only start from lobby state")
	}
	// Check # of people
	if len(this.members) < 3 {
		return errors.New("Insufficient number of players")
	}
	// Set to STATE_PLACEMENT
	this.state = STATE_PLACEMENT
	// TODO Place members in the ring
	this.ring = ring.New(len(this.members))
	// Set to STATE_GAME
	// Initialize any data fields for the game

	return nil
}
