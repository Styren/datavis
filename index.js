var width = 500,
    height = 500,
    focused = null,
    geoPath;

var svg = d3.select("#map")
	.append("svg")
	.attr("width", width)
	.attr("height", height);

svg.append("rect")
	.attr("class", "background")
	.attr("width", width)
	.attr("height", height);

var map = svg.append("g")
	.append("g")
	.attr("id", "states");

var NUM_STATES = 16

var carPerState = [];
var maxCarsInState = 0;

var calcCarPerState = () => {
	carPerState = Array.apply(null, Array(NUM_STATES + 1)).map(Number.prototype.valueOf,0);
	for (var i = 0; i < autos.length; ++i) {
		++carPerState[autos[i].state];
	}
	maxCarsInState = Math.max.apply(null, carPerState);
}

calcCarPerState();
console.log(maxCarsInState);
console.log(carPerState);

var renderMap = () => {
	var color = d3.scale.category10();
	d3.json("./dataBundesLander.json", function(collection) {
			var bounds = d3.geo.bounds(collection),
			bottomLeft = bounds[0],
			topRight = bounds[1],
			rotLong = -(topRight[0]+bottomLeft[0])/2;
			center = [(topRight[0]+bottomLeft[0])/2+rotLong, (topRight[1]+bottomLeft[1])/2];

			//default scale projection
			var projection = d3.geo.albers()
			.parallels([bottomLeft[1],topRight[1]])
			.rotate([rotLong,0,0])
			.translate([width/2,height/2])
			.center(center);

			var bottomLeftPx = projection(bottomLeft);
			var topRightPx = projection(topRight);
			var scaleFactor = 1.00*Math.min(width/(topRightPx[0]-bottomLeftPx[0]), height/(-topRightPx[1]+bottomLeftPx[1]));

			var projection = d3.geo.albers()
			.parallels([bottomLeft[1],topRight[1]])
			.rotate([rotLong,0,0])
			.translate([width/2,height/2])
			.scale(scaleFactor*0.975*1000)
			//.scale(4*1000)  //1000 is default for USA map
			.center(center);

	geoPath = d3.geo.path().projection(projection);

	map.selectAll("path.feature")
		.data(collection.features)
		.enter()
		.append("path")
		.attr("class", (x) => {return "states " + x.properties.ID_1;})
		.style("fill", (x) => {return d3.hsl(0, carPerState[x.properties.ID_1] / maxCarsInState, 0.7);})
		.attr("d", geoPath)
		.on("click", clickPath);
	});
}



function clickPath(d) {
	var x = width/2,
	    y = height/2,
	    k = 1,
	    name = d.properties.NAME_1;

	map.selectAll("text")
		.remove();
	if ((focused === null) || !(focused === d)) {
		var centroid = geoPath.centroid(d),
		    x = +centroid[0],
		    y = +centroid[1],
		    k = 1.75;
		focused = d;

		map.append("text")
			.text(name)
			.attr("x", x)
			.attr("y", y)
			.style("text-anchor","middle")
			.style("font-size","8px")
			.style("stroke-width","0px")
			.style("fill","black")
			.style("font-family","Times New Roman")
			.on("click", clickText);
	} else {
		focused = null;
	};

	map.selectAll("path")
		.classed("active", focused && function(d) { return d === focused; });
}


function clickText(d) {
	focused = null;
	map.selectAll("text")
		.remove();
	map.selectAll("path")
		.classed("active", 0);
	map.transition()
		.duration(1000)
		.attr("transform", "scale("+1+")translate("+0+","+0+")")
		.style("stroke-width", 1.00+"px");
}

renderMap();
