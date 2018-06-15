package main

import (
	"fmt"
	"log"
	"net"
	"net/http"
	"strings"
)

type Server struct {
	rooms map[string]*Room
    users map[string]*Room
}

// Create a room on this server.
func (this *Server) CreateRoom(w http.ResponseWriter, remoteAddr string) {
    r := NewRoom()
    u := r.uuid.String()
    this.rooms[u] = r
    if this.JoinRoom(w, u, remoteAddr) {
        fmt.Fprintf(w, "%s", r.uuid)
    }
}

// Join a room on this server.
func (this *Server) JoinRoom(w http.ResponseWriter, roomUuid string, remoteAddr string) bool {
    if _, ok := this.rooms[roomUuid]; !ok {
        fmt.Fprintf(w, "Room does not exist")
        return false
    }
    if err := this.rooms[roomUuid].AddMember(remoteAddr); err != nil {
        fmt.Fprintf(w, "%s", err)
        return false
    }
    this.users[remoteAddr] = this.rooms[roomUuid]
    return true
}

// Start a game for a room.
func (this *Server) StartGame(w http.ResponseWriter, remoteAddr string) {
    room, ok := this.users[remoteAddr]
    if !ok {
        fmt.Fprintf(w, "User has not joined a room")
        return
    }

	room.StartGame()
}

// Handle room requests
func (this *Server) RoomHandler(w http.ResponseWriter, r *http.Request) {
    req := strings.Split(r.URL.Path, "/")

	// TODO use only the IP address, not the port, of this remote address
    fmt.Fprintf(w, "Request: %s", r.RemoteAddr)

    switch req[2] {
    case "create":
        this.CreateRoom(w, r.RemoteAddr)
        break
    case "join":
        this.JoinRoom(w, r.Header[http.CanonicalHeaderKey("room")][0], r.RemoteAddr)
        break
    case "start":
		this.StartGame(w, r.RemoteAddr)
		break
    default:
        fmt.Fprintf(w, "unknown request")
        break
    }

    fmt.Printf("%v", this.rooms)
}

// A new remote has joined the game
func (this *Server) HandleConnection(conn net.Conn) {
	buf := make([]byte, 1024)

	for {
		len, err := conn.Read(buf)
		if err != nil {
			fmt.Printf("Read error: %v", err)
			return
		}

	}
}

func (this *Server) Start() {
    // http.HandleFunc("/room/", this.RoomHandler)
	ln, err := net.Listen("tcp", ":8080")
	if err != nil {
		log.Fatal(err)
	}

    fmt.Println("Serving at 8080")
	for {
		conn, err := ln.Accept()
		if err != nil {
			fmt.Printf("Accept error: %v", err)
			return
		}

		go this.HandleConnection(conn)
	}
    // log.Fatal(http.ListenAndServe(":8080", nil))
}

func main() {
    s := Server{}
    s.rooms = make(map[string]*Room)
    s.Start()
}
