package main

import (
	"encoding/binary"
	"bytes"
	"container/ring"
	"errors"
	"fmt"
	"github.com/satori/go.uuid"
	"math/rand"
	"sync"
)

// State of this game
const (
	STATE_LOBBY     = iota
	STATE_PLACEMENT = iota
	STATE_GAME      = iota
	STATE_END_GAME  = iota
)

const MIN_PLAYERS = 2 // TODO 3

type Room struct {
	members    map[Player]bool
	mutex      *sync.Mutex
	papers     map[Player][][]byte
	ring       *ring.Ring
	round      int
	state      uint
	uuid       uuid.UUID
	waitingFor map[Player]bool
}

// Create a new Room struct
func NewRoom() *Room {
	r := &Room{}
	r.members = make(map[Player]bool)
	r.mutex = &sync.Mutex{}
	r.papers = make(map[Player][][]byte)
	r.ring = nil
	r.round = 0
	r.state = STATE_LOBBY
	r.uuid = uuid.Must(uuid.NewV4())
	r.waitingFor = make(map[Player]bool)

	return r
}

func (this *Room) HandlePacket(player Player, b *bytes.Buffer) {
	this.mutex.Lock()
	defer this.mutex.Unlock()
	// TODO copy this logic from the Android project
	headerBytes := b.Next(4)
	header := binary.BigEndian.Uint32(headerBytes)

	switch header {
	case HEADER_START_GAME:
		this.StartGame()
		break
	}
}

// Add a new player to this room
func (this *Room) AddMember(player Player) error {
	this.mutex.Lock()
	defer this.mutex.Unlock()

	if this.state != STATE_LOBBY {
		return errors.New("Room must be in lobby to join")
	}
	if _, ok := this.members[player]; ok {
		return errors.New("User already member of room")
	}
	this.members[player] = true
	return nil
}

// TODO implement
func (this *Room) RemoveMember(player Player) error {
	this.mutex.Lock()
	defer this.mutex.Unlock()

	return nil
}

// Start the game for this room
func (this *Room) StartGame() error {
	this.mutex.Lock()
	defer this.mutex.Unlock()

	// Error checking
	if this.state != STATE_LOBBY {
		return errors.New("Game can only start from lobby state")
	}
	if len(this.members) < MIN_PLAYERS {
		return errors.New("Insufficient number of players")
	}

	// Set to STATE_PLACEMENT
	this.state = STATE_PLACEMENT

	// Place members in the ring
	// 1. First convert the Set into an array (for indexing purposes)
	keys := make([]Player, len(this.members))
	i := 0
	for mem := range this.members {
		keys[i] = mem
		this.waitingFor[mem] = true
		i++
	}
	// 2. Create a Ring and randomly place members into the ring
	this.ring = ring.New(len(this.members))
	order := rand.Perm(len(this.members))
	for i = 0; i < len(this.members); i++ {
		this.ring.Value = keys[order[i]]
		this.ring = this.ring.Next()

		// Initialize this player's paper
		this.papers[keys[order[i]]] = make([][]byte, len(this.members))
	}

	// Set to STATE_GAME
	this.state = STATE_GAME

	return nil
}

// Creates a new Game struct and sets up the game data structures
// Returns true if the overall turn is done
func (this *Room) SubmitTurn(player Player, data []byte) bool {
	this.mutex.Lock()
	defer this.mutex.Unlock()

	paper := this.papers[player]
	// TODO check consistency of round number
	// if this.round != len(paper) {
	// 	return errors.New("Inconsistent round # and submission")
	// }
	paper[this.round] = data

	delete(this.waitingFor, player)
	if len(this.waitingFor) == 0 {
		// done waiting, advance to the next turn
		return true
	}

	return false
}

func (this *Room) NextTurn() {
	this.mutex.Lock()
	defer this.mutex.Unlock()

	this.round++

	for member := range this.members {
		this.waitingFor[member] = true
	}

	// TODO broadcast next turn (including turn #) to all players
}

// Returns a string representation of the room
func (this *Room) String() string {
	this.mutex.Lock()
	defer this.mutex.Unlock()

	mems := make([]string, len(this.members))
	i := 0
	for key, _ := range this.members {
		mems[i] = key.GetAddr()

		i++
	}

	return fmt.Sprintf("%s\n%v", this.uuid.String(), mems)
}
