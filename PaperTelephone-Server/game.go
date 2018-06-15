package main

import (
	"container/ring"
	"errors"
	"math/rand"
)

type Game struct {
	papers     map[string][][]byte
	ring       *ring.Ring
	room       *Room
	round      int
	waitingFor map[string]bool
}

// Creates a new Game struct and sets up the game data structures
func NewGame(room *Room) *Game {
	g := &Game{}
	g.papers = make(map[string][][]byte)
	g.room = room
	g.round = 0
	g.waitingFor = make(map[string]bool)

	// Place members in the ring
	keys := make([]string, len(g.room.members))
	i := 0
	for mem := range g.room.members {
		keys[i] = mem
		g.waitingFor[mem] = true
		i++
	}
	g.ring = ring.New(len(g.room.members))
	order := rand.Perm(len(g.room.members))
	for i = 0; i < len(g.room.members); i++ {
		g.ring.Value = keys[order[i]]
		g.ring = g.ring.Next()

		// Initialize g player's paper
		g.papers[keys[order[i]]] = make([][]byte, len(g.room.members))
	}

	return g
}

func (this *Game) SubmitTurn(remoteAddr string, data []byte) error {
	paper := this.papers[remoteAddr]
	// TODO check consistency of #
	// if this.round != len(paper) {
	// 	return errors.New("Inconsistent round # and submission")
	// }
	paper[this.round] = data

	delete(this.waitingFor, remoteAddr)
	if len(this.waitingFor) == 0 {
		// done waiting, advance to the next turn
		this.NextTurn()
	}

	return nil
}

func (this *Game) NextTurn() {
	this.round++

	for member := range this.room.members {
		this.waitingFor[member] = true
	}

	// TODO broadcast next turn (including turn #) to all players
}
