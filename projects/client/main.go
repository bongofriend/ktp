package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"github.com/gorilla/websocket"
	"io"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"time"
)

var (
	username *string
	password *string
	token    string
)

const (
	baseUrl           = "http://localhost:8080"
	tokenEndpoint     = "/users/token"
	chatGroupEndpoint = "/chatgroups"
	messageEndpoint   = "/messages"
)

type ChatGroup struct {
	Id   string `json:"id"`
	Name string `json:"name"`
}

type ChatGroupMessage struct {
	Username string `json:"username"`
	Message  string `json:"message"`
}

func Request(endpoint string, method string, data interface{}) ([]byte, error) {
	client := http.Client{}
	var jsonValue []byte
	var request *http.Request
	var err error

	if data != nil {
		jsonValue, _ = json.Marshal(data)
	}
	if jsonValue != nil {
		request, err = http.NewRequest(method, baseUrl+endpoint, bytes.NewBuffer(jsonValue))
	} else {
		request, err = http.NewRequest(method, baseUrl+endpoint, nil)
	}
	if err != nil {
		return nil, err
	}
	request.Header = http.Header{}
	if method == "POST" {
		request.Header.Add("Content-Type", "application/json")
	}
	if len(token) > 0 {
		request.Header.Add("Authorization", fmt.Sprintf("Bearer %s", token))
	}
	rsp, _ := client.Do(request)
	if rsp.Body != nil {
		defer rsp.Body.Close()
	}
	return io.ReadAll(rsp.Body)
}

func Login() error {
	data := map[string]string{"username": *username, "password": *password}
	jsonValue, _ := json.Marshal(data)
	var rsp, err = http.Post(baseUrl+tokenEndpoint, "application/json", bytes.NewBuffer(jsonValue))
	if rsp.Body != nil {
		defer rsp.Body.Close()
	}
	if err != nil {
		return err
	}
	var tokenData map[string]string
	d, _ := io.ReadAll(rsp.Body)
	e := json.Unmarshal(d, &tokenData)
	if e != nil {
		return e
	}
	token = tokenData["token"]
	return nil
}

func GetChatGroups() ([]ChatGroup, error) {
	data, e := Request(chatGroupEndpoint, "GET", nil)
	if e != nil {
		return nil, e
	}
	var groups []ChatGroup
	err := json.Unmarshal(data, &groups)
	if err != nil {
		return nil, err
	}
	return groups, nil
}

func GetChatMessage(group ChatGroup) ([]ChatGroupMessage, error) {
	var e error
	url := fmt.Sprintf("%s/%s", messageEndpoint, group.Id)
	data, e := Request(url, "GET", nil)
	if e != nil {
		return nil, e
	}
	var messages map[string][]ChatGroupMessage
	e = json.Unmarshal(data, &messages)
	if e != nil {
		return nil, e
	}
	return messages["messages"], nil
}

func SendChatMessage(msg string, group ChatGroup) error {
	var e error
	url := fmt.Sprintf("%s/%s", messageEndpoint, group.Id)
	data := map[string]string{
		"message": msg,
	}
	_, e = Request(url, "POST", data)
	if e != nil {
		return e
	}
	return nil
}

func DisplayGroups() error {
	groups, e := GetChatGroups()
	if e != nil {
		return e
	}
	fmt.Println("Groups")
	for _, g := range groups {
		fmt.Println(g.Name)
	}
	return nil
}

func GetGroupId(name string) (*ChatGroup, error) {
	var g *ChatGroup
	groups, e := GetChatGroups()
	if e != nil {
		return nil, e
	}
	for _, gr := range groups {
		if gr.Name == name {
			g = &gr
			break
		}
	}
	if g == nil {
		return nil, errors.New("group not found")
	}
	return g, nil
}

func SendMessage(grp string, msg string) error {
	chatGroup, _ := GetGroupId(grp)
	err := SendChatMessage(msg, *chatGroup)
	if err != nil {
		return err
	}
	return nil
}

func StartGroupConnection(groupname string) {
	group, _ := GetGroupId(groupname)
	socketBaseUrl := strings.Replace(baseUrl, "http", "ws", 1)
	socketUrl := fmt.Sprintf("%s/socket?token=%s&groupId=%s", socketBaseUrl, token, group.Id)

	interrupt := make(chan os.Signal, 1)
	signal.Notify(interrupt, os.Interrupt)

	handler := func(connection *websocket.Conn) {
		for {
			_, data, err := connection.ReadMessage()
			if err != nil {
				fmt.Println(err)
				return
			}
			var msg ChatGroupMessage
			json.Unmarshal(data, &msg)
			PrintChatMessage(msg)
		}
	}

	connection, _, err := websocket.DefaultDialer.Dial(socketUrl, nil)
	if err != nil {
		return
	}
	go handler(connection)
	for {
		select {
		case <-time.After(time.Duration(1) * time.Millisecond * 1000):
			// Send an echo packet every second
			err := connection.WriteMessage(websocket.PingMessage, make([]byte, 1))
			if err != nil {
				log.Println("Error during writing to websocket:", err)
				return
			}
		}
	}
}

func PrintChatMessage(msg ChatGroupMessage) {
	fmt.Printf("%s: %s\n", msg.Username, msg.Message)
}

func ShowCurrentMessages(groupname string) {
	g, _ := GetGroupId(groupname)
	msgs, _ := GetChatMessage(*g)
	for _, m := range msgs {
		PrintChatMessage(m)
	}
}

func main() {
	var e error

	username = flag.String("username", "test", "Username for login")
	password = flag.String("password", "password", "Password for login")
	group := flag.String("group", "testgroup", "Chat Group Name")
	message := flag.String("message", "", "Message to be send")
	flag.Parse()

	if len(*username) == 0 || len(*password) == 0 {
		fmt.Println("username or password missing")
	}
	e = Login()
	if e != nil {
		fmt.Println(e)
	}

	if len(*group) == 0 && len(*message) == 0 {
		e = DisplayGroups()
		if e != nil {
			fmt.Println(e)
		}
	}
	if len(*group) > 0 && len(*message) > 0 {
		e = SendMessage(*group, *message)
		if e != nil {
			fmt.Println(e)
		}
	}
	if len(*group) > 0 && len(*message) == 0 {
		ShowCurrentMessages(*group)
		StartGroupConnection(*group)
	}

}
