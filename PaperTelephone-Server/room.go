package main

import (
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

const MIN_PLAYERS = 3

type Room struct {
	game	*Game
	members map[string]bool
	mutex   *sync.Mutex
	state   uint
	uuid    uuid.UUID
}

func NewRoom() *Room {
	r := &Room{}
	r.game = nil
	r.members = make(map[string]bool)
	r.mutex = &sync.Mutex{}
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
	if len(this.members) < MIN_PLAYERS {
		return errors.New("Insufficient number of players")
	}
	// Set to STATE_PLACEMENT
	this.state = STATE_PLACEMENT

	this.game = NewGame()

	// Set to STATE_GAME
	this.state = STATE_GAME

	return nil
}

func (this *Room) Submit(remoteAddr string, data []byte) {
	this.mutex.Lock()
	defer this.mutex.Unlock()

	this.game.SubmitTurn(remoteAddr, data)
}
