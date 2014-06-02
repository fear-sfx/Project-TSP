function Level(context2D, width, height, levelModel) {
	var columnsCount = levelModel.columnsCount;
	var mapGrid = levelModel.tileGrid;

	var rowsCount = mapGrid.length / columnsCount;

	var textureWidth = 32;
	var textureHeight = 32;

	var rigidBodies = Array();
	var collidables = levelModel.collidables;

	var aspectRatio = ResourceManagerSingleton.AspectRatio;

	for (var i = 0; i < mapGrid.length; i++) {
		if (collidables.contains(mapGrid[i])) {
			var column = i % columnsCount;
			var row = Math.floor(i / columnsCount);

			rigidBodies.push(new RigidBody(column*textureWidth, row*textureHeight, textureWidth, textureHeight));
		}
	}

	var textures = levelModel.textures;
	this.getTextureWidth = function() {
		return textureWidth;
	} 

	this.getTextureHeight = function() {
		return textureHeight;
	}

	this.getRigidBodies = function() {
		return rigidBodies;
	}

	this.draw = function(deltaSeconds) {

		ctx.fillStyle = "yellow";
		ctx.fillRect(0,0, width, height);
		
		for (var i = 0; i < mapGrid.length; i++) {
			var textureImage = textures[mapGrid[i]];
			var column = i % columnsCount;
			var row = Math.floor(i / columnsCount);

			ctx.drawImage(textureImage, 
						  column*textureWidth*aspectRatio, row*textureHeight*aspectRatio, 
						  textureWidth*aspectRatio, textureHeight*aspectRatio);
		
		};
	}
}

var LoadLevelOne = function(context2D, width, height) {
	var levelOneModel = new LevelModel();
	levelOneModel.tileGrid =   [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
						   		1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1,
								1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1,
							    1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1,
							    1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1,
							    1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1,
						    	1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1,
							    1, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 0, 0, 0, 1, 0, 0, 1,
							    1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1,
							    1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 1, 1,
							    1, 0, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 1, 1,
							    1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1,
							    1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1,
							    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1];

	levelOneModel.columnsCount = 20;
	levelOneModel.collidables = [1];

	var columnsCount = levelOneModel.columnsCount;
	var mapGrid = levelOneModel.tileGrid;

	var rowsCount = mapGrid.length / columnsCount;

	var textureHeight = height / rowsCount;

	ResourceManagerSingleton.AspectRatio = textureHeight / 32;

	var textures = Array();
	textures[0] = ResourceManagerSingleton.getImage("floor.png");
	textures[1] = ResourceManagerSingleton.getImage("stone-wall.jpg");

	levelOneModel.textures = textures;

	return new Level(context2D, width, height, levelOneModel);
}
