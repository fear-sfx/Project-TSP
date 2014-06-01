package org.projecttsp.websocket

object Keeper {
  private var playerModels = Map[String, PlayerModel]()
  def setModels(models: Map[String, PlayerModel]) = {
    playerModels = models;
  }
  def getModels(): Map[String, PlayerModel] = {
    playerModels;
  }
}

class PlayerModel(var name:String, var characterId:String, var x:String, var y:String, var movement:String) {
	def toList = List(name, characterId, x, y, movement)
}