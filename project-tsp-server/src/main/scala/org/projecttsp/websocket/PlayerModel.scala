package org.projecttsp.websocket



class PlayerModel(var name:String, var characterId:String, var x:String, var y:String, var movement:String) {
	def toList = List(name, characterId, x, y, movement)
}