/**
	Server Protocol
*/
function ServerProtocol(serverConnection, messageBroker) {
	var self = this;
	var connection = serverConnection;
	var msgBroker = messageBroker;
	
	var protocolStates = {
		connectionEstablished:false,
		logged:false,
		gameMode: undefined
	}
	
	//Streams
	var generalMessageHandlers = {
		'identify':function(args ){
			if (self.onIdentify) {
				self.onIdentify();
			}
		},
		'successful_authentication':function(args ){
			if (self.onSuccessfulAuthentication) {
				self.onSuccessfulAuthentication(args[1]);
			}
		},
		'bad_credentials':function(args ) {
			if (self.onBadCredentials) {
				self.onBadCredentials();
			}
		},
		'player_logged_in':function(args ) {
			if (self.onPlayerLoggedIn) {
				self.onPlayerLoggedIn(args[1], args[2]);
			}
		},
		'player_started':function(args) {
			if (self.onPlayerStarted) {
				self.onPlayerStarted(args[1], args[2], args[3], args[4]);
			}
		},
		'player_move':function(args) {
			if (self.onPlayerMove) {
				self.onPlayerMove(args[1], args[2], args[3], args[4]);
			}
		},
		'exit_game':function(args) {
			if (self.onExitGame) {
				self.onExitGame(args[1]);
			}
		},

	};
	
	var construct = function() {
		
		for (propt in generalMessageHandlers) {
			console.debug('Registering general message handler:'+generalMessageHandlers[propt]);
			msgBroker.registerHandler(propt, generalMessageHandlers[propt]);
		}
		
		connection.onOpen = function () {
			console.log('Connection established');
		}
		
		connection.onMessage = function (message) {
			console.debug('Received:' + message.data);
			if (messageBroker.stream(message.data, " ")) {
				//console.debug(message.data +" is recognized and will be streamed");
			} else {
				console.warn("No handler found for message: "+message.data);
			}
		}
		
		connection.onClose = function() {
			console.log('Closing socket' );
		}
		
	}
	//Initialize object
	construct();
	
	//Public
	this.init = function(connectionUrl) {
		console.log('Initing Server Protocol');
		connection.createNewConnection(connectionUrl);
	}

	this.login = function(username, password) {
		connection.send("login " + username + " " + password);
	}

	this.playerStart = function (username, characterId, x, y, movement) {
		connection.send("player_started_game " + username + " " + characterId + " " + x + " " + y + " " + movement);
	}

	this.move = function(playerName, x, y, movement) {
		connection.send("move " + playerName + " " + x + " " + y + " " + movement);
	}
}