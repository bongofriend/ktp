package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"time"

	"github.com/gorilla/websocket"
)

var (
	username *string
	password *string
	token    string
)

const (
	baseUrl                    = "http://localhost:8080"
	loginEndpoint              = "/users"
	tokenEndpoint              = "/users/token"
	chatGroupEndpoint          = "/chatgroups"
	messageEndpoint            = "/messages"
	addToGroupEndpoint         = "/chatgroups/add"
	createNewChatGroupEndpoint = "/chatgroups/create"
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
	rsp, e := client.Do(request)

	if e != nil {
		return nil, e
	}

	if rsp.StatusCode > 226 {
		return nil, errors.New("Http Error" + string(rune(rsp.StatusCode)))
	}

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

func ShowCurrentMessages(groupname string) error {
	g, e := GetGroupId(groupname)
	if e != nil {
		return e
	}
	msgs, err := GetChatMessage(*g)
	if err != nil {
		return err
	}
	for _, m := range msgs {
		PrintChatMessage(m)
	}
	return nil
}

func Register() error {
	data := map[string]string{"username": *username, "password": *password}
	_, e := Request(loginEndpoint, "POST", data)
	return e
}

func AddUserToGroup(groupName string) error {
	data := map[string]string{"name": groupName}
	_, e := Request(addToGroupEndpoint, "POST", data)
	return e
}

func CreateNewGroup(groupName string) error {
	data := map[string]string{"name": groupName}
	_, e := Request(createNewChatGroupEndpoint, "POST", data)
	return e
}

func main() {
	var e error
	var isRegister bool
	var showGroups bool
	var addToGroup bool
	var createNewGroup bool

	username = flag.String("username", "", "Username for login")
	password = flag.String("password", "", "Password for login")
	group := flag.String("group", "", "Chat Group Name")
	message := flag.String("message", "", "Message to be send")
	flag.BoolVar(&isRegister, "register", false, "Should Register")
	flag.BoolVar(&showGroups, "groups", false, "Display Groups")
	flag.BoolVar(&addToGroup, "add", false, "Add user to group")
	flag.BoolVar(&createNewGroup, "create", false, "Create new group")
	flag.Parse()

	if len(*username) == 0 || len(*password) == 0 {
		fmt.Println("username or password missing")
		return
	}

	if isRegister {
		e = Register()
		if e != nil {
			fmt.Println("Could not create user")
		} else {
			fmt.Println("User created")
		}
		return
	}

	e = Login()
	if e != nil {
		fmt.Println("Could not login")
		return
	}

	if !addToGroup && !createNewGroup && showGroups {
		e = DisplayGroups()
		if e != nil {
			fmt.Println("Could not display groups")
		}
		return
	}

	if !createNewGroup && !showGroups && addToGroup {
		if len(*group) == 0 {
			fmt.Println("Group required")
			return
		}
		e = AddUserToGroup(*group)
		if e != nil {
			fmt.Println("Could not add user to group")
		} else {
			fmt.Println("User added to group")
		}
		return
	}

	if !addToGroup && !showGroups && createNewGroup {
		if len(*group) == 0 {
			fmt.Println("Group required")
			return
		}
		e = CreateNewGroup(*group)
		if e != nil {
			fmt.Println("Could not create group")
		} else {
			fmt.Println("Group created and user added")
		}
		return
	}

	if len(*group) > 0 && len(*message) > 0 {
		e = SendMessage(*group, *message)
		if e != nil {
			fmt.Println("Could not send message")
		}
		return
	}

	if len(*group) > 0 && len(*message) == 0 {
		e := ShowCurrentMessages(*group)
		if e != nil {
			fmt.Println("Could not access groups because not part of group or group not existing")
			return
		}
		StartGroupConnection(*group)
	}

}
