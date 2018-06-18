package main

import (
	"container/ring"
	"errors"
	"fmt"
	"math/rand"
	"sync"
)

// State of this game
const (
	StateLobby     = iota
	StatePlacement = iota
	StateGame      = iota
	StateEndGame   = iota
)

// MinPlayers is the minimum number of players needed to play a game
// TODO should be 3
const MinPlayers = 2

// Room represents a room hosted on the server
type Room struct {
	members    map[*Player]bool
	mutex      *sync.Mutex
	papers     map[*Player][][]byte
	ring       *ring.Ring
	round      int
	state      uint
	uuid       RoomID
	waitingFor map[*Player]bool
}

// NewRoom creates a new Room struct
func NewRoom() *Room {
	r := &Room{}
	r.members = make(map[*Player]bool)
	r.mutex = &sync.Mutex{}
	r.papers = make(map[*Player][][]byte)
	r.ring = nil
	r.round = 0
	r.state = StateLobby
	r.uuid = NewRoomId()
	r.waitingFor = make(map[*Player]bool)

	return r
}

// AddMember adds a new player to this room
func (r *Room) AddMember(player *Player) error {
	r.mutex.Lock()
	defer r.mutex.Unlock()

	if r.state != StateLobby {
		return errors.New("Room must be in lobby to join")
	}
	if _, ok := r.members[player]; ok {
		return errors.New("User already member of room")
	}
	r.members[player] = true
	return nil
}

// RemoveMember removes a player from this room
func (r *Room) RemoveMember(player *Player) error {
	r.mutex.Lock()
	defer r.mutex.Unlock()

	delete(r.members, player)

	return nil
}

// StartGame starts the game for r room
func (r *Room) StartGame() error {
	r.mutex.Lock()
	defer r.mutex.Unlock()

	fmt.Println("Starting game")

	// Error checking
	if r.state != StateLobby {
		return errors.New("Game can only start from lobby state")
	}
	if len(r.members) < MinPlayers {
		return errors.New("Insufficient number of players")
	}

	// Set to StatePlacement
	r.state = StatePlacement

	// Place members in the ring
	// 1. First convert the Set into an array (for indexing purposes)
	keys := make([]*Player, len(r.members))
	i := 0
	for mem := range r.members {
		keys[i] = mem
		r.waitingFor[mem] = true
		i++
	}
	// 2. Create a Ring and randomly place members into the ring
	r.ring = ring.New(len(r.members))
	order := rand.Perm(len(r.members))
	for i = 0; i < len(r.members); i++ {
		r.ring.Value = keys[order[i]]
		r.ring = r.ring.Next()

		// Initialize r player's paper
		r.papers[keys[order[i]]] = make([][]byte, len(r.members))
	}

	// Set to StateGame
	r.state = StateGame

	fmt.Println("Game started!")

	return nil
}

// SubmitTurn submits a turn. Returns true if the overall turn is done
func (r *Room) SubmitTurn(player *Player, data []byte) error {
	r.mutex.Lock()
	defer r.mutex.Unlock()

	fmt.Println("Submitting turn:", string(data))
	paper := r.papers[player]
	// TODO check consistency of round number
	// if r.round != len(paper) {
	// 	return errors.New("Inconsistent round # and submission")
	// }
	paper[r.round] = data

	delete(r.waitingFor, player)
	if len(r.waitingFor) == 0 {
		// done waiting, advance to the next turn
		fmt.Println("next turn!!")
		return r.nextTurn()
	}

	return nil
}

// nextTurn advances this room's game to the next turn
// NOTE: mutex must be held on entry
func (r *Room) nextTurn() error {
	r.round++

	for member := range r.members {
		r.waitingFor[member] = true
	}

	// TODO broadcast next turn (including turn #) to all players

	return nil
}

// Returns a string representation of the room
func (r *Room) String() string {
	r.mutex.Lock()
	defer r.mutex.Unlock()

	mems := make([]string, len(r.members))
	i := 0
	for key := range r.members {
		mems[i] = key.GetAddr()

		i++
	}

	return fmt.Sprintf("%s: %v", r.uuid.String(), mems)
}
