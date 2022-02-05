# KTP-Project: Chat-Server with mutltiple clients

## Starting server

1. Open terminal
2. Navigate to `projects/chat-server` of this repo
3. Execute `mvn clean compile exec:java` for a clean launch

## Chat Client
1. Open terminal
2. Navigate to `projects/client` of this repo
3. Execute `go build client` to compile the binary

### Support Commands

The client supports the following commands:

**NOTE:** If the string passed to an argument contains whitespaces, enclose the string in double quotation marks
(e.g "test test" instead of test test). 

**Create A New User:**

<code>./client -register -username *username* -password *password*</code>

**Create A New Chat Group**

<code>./client -create -username *username* -password *passsword* -group *name of new group*</code>

**Add User to Existing Chat Group**

<code>./client -add -username *username* -password *passsword* -group *name of group*</code>

**Show Chat Groups user is part of**

<code>./client -groups -username *username* -password *passsword*</code>

**Send a message to a Chat Group**

<code>./client -username *username* -password *passsword* -group *name of group* -message="*chat message*"</code>

**Show current messages in group and listen to new ones**

<code>./client -add -username *username* -password *passsword* -group *name of group*</code>
