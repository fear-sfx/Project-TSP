/**
	Resource Manager Singleton
*/
function ResourceManagerClass() {
	var self = this;
	var imagePath = "img/"
	this.images = Array();

	this.AspectRatio = 0;

	var loadImage = function(name) {
		var image = new Image();
		image.src = imagePath+name;
		self.images[name] = image;
	}

	var loadImages = function() {
		loadImage("heroes.png");
		loadImage("stone-wall.jpg");
		loadImage("floor.png");
	}

	this.loadResources = function() {
		loadImages();
	}

	this.getImage = function(name) {
		return self.images[name];
	}
}

var ResourceManagerSingleton = new ResourceManagerClass();
