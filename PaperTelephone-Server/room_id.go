package main

import (
	"github.com/satori/go.uuid"
)

// RoomID is the room id
type RoomID struct {
	uuid uuid.UUID
}

// nolint
func NewRoomId() RoomID {
	rid := RoomID{}
	rid.uuid = uuid.Must(uuid.NewV4())
	return rid
}

// nolint
func RoomIdFromBytes(bytes []byte) RoomID {
	rid := RoomID{}
	rid.uuid = uuid.FromBytesOrNil(bytes)
	return rid
}

// nolint
func RoomIdSize() int {
	return uuid.Size
}

// nolint
func (r *RoomID) String() string {
	return r.uuid.String()
}

// nolint
func (r *RoomID) Bytes() []byte {
	return r.uuid.Bytes()
}
