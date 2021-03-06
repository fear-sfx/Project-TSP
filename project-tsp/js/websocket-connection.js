/**
	Web Socket Connection
*/ 
function WebSocketConnection() {
	this.onOpen = null;
	this.onClose = null;
	this.onMessage = null;
	this.onError = null;
	
	var self = this;
	var url = "undefined";
	var connection = null;
	
	this.createNewConnection = function (connectionUrl) {
		try {
			url = connectionUrl;
			connection = new WebSocket(connectionUrl);
			
			connection.onopen = function () {
				if (self.onOpen) {
					self.onOpen();
				}
			};

			connection.onerror = function (error) {
				if(self.onError) {
					self.onError(error)
				}
			};

			connection.onmessage = function (e) {
				if(self.onMessage) {
					self.onMessage(e)
				}
			};
		
			connection.onclose = function() {
				if(self.onClose) {
					self.onClose()
				}
			}		
		}
		catch(err) {
			var text = "ERROR: " + err.message;
			alert(text);
			this.close();
		}
	}
	
	this.send = function(message) {
		if (connection) {
			this.waitForConnection(function () {
				connection.send(message);
			}, 1000);
		} else {
			var error = "Connection is not established or already closed. Please use createNewConnection to create a new one";
			console.error(error);
			throw error;
		}
	}

	this.waitForConnection = function (callback, interval) {
	    if (connection.readyState === 1) {
		callback();
	    } else {
		var that = this;
		setTimeout(function () {
		    that.waitForConnection(callback);
		}, interval);
	    }
	};
	
	this.close = function(message) {
		if (connection) {
			connection.close();
			connection = null;
		}
	}
}

//Web Socket Connection End
