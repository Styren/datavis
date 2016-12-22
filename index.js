var margin = {top: 30, right: 10, bottom: 10, left: 10},
    width = 500,
    height = 500,
    pwidth = 2000,
    pheight = 400,
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
var maxCarsWithConfig = autos.length;

var currState = null;
var currCarType = 'all';
var currAttrF = ['price', 'yearOfRegistration', 'powerPS', 'kilometer'];
var currAttr = [[0, 100000000],
	[0, 100000000],
	[0, 100000000],
	[0, 100000000]
	];
var pieData = null;

var calcCarPerState = () => {
	carPerState = Array.apply(null, Array(NUM_STATES + 1)).map(Number.prototype.valueOf,0);
    var res = {};
    pieData = [];
    maxCarsWithConfig = 0;
    if (currCarType == 'all') {
        for (var i = 0; i < autos.length; ++i) {
			var ok = true;
			for (var j = 0; j < currAttr.length; ++j) {
				if (autos[i][currAttrF[j]] < currAttr[j][0] || autos[i][currAttrF[j]] > currAttr[j][1])
					ok = false;
			}
			if (ok) {
				++carPerState[autos[i].state];
                if (!currState || currState == autos[i].state) {
                    if (res[autos[i].brand] == null)
                        res[autos[i].brand] = 0;
                    ++res[autos[i].brand];
                    ++maxCarsWithConfig;
                }
            }
        }
        maxCarsInState = Math.max.apply(null, carPerState);
    } else {
        for (var i = 0; i < autos.length; ++i) {
			if (autos[i].vehicleType == currCarType) {
				var ok = true;
				for (var j = 0; j < currAttr.length; ++j) {
					if (autos[i][currAttrF[j]] < currAttr[j][0] || autos[i][currAttrF[j]] > currAttr[j][1])
						ok = false;
				}
				if (ok) {
                    ++carPerState[autos[i].state];
                    if (!currState || currState == autos[i].state) {
                        if (res[autos[i].brand] == null)
                            res[autos[i].brand] = 0;
                        ++res[autos[i].brand];
                        ++maxCarsWithConfig;
                    }
                }
			}
        }
        maxCarsInState = Math.max.apply(null, carPerState);
    }
    var cutoff = maxCarsWithConfig / 100;
    var other = 0;
    for (var key in res) {
        if (res.hasOwnProperty(key)) {
            if (res[key] < cutoff)
                other += res[key];
            else
                pieData.push({brand: key, value: res[key]})
        }
    }
    pieData.push({brand: "other", value: other})
}

calcCarPerState();

var carType = null;
var renderAllCross = null;

d3.json("data.json", function(error, data) {
		var charts;
		var cars = [];
		var car;
		var filterCars = () => {
			cars = data.filter((d) => {
				if (!currState)
					return true;
				else
					return d.state == currState;
				});
		};
		filterCars();
		var color = d3.scale.category10();
		d3.json("./dataBundesLander.json", function(collection) {
			var bounds = d3.geo.bounds(collection),
			bottomLeft = bounds[0],
			topRight = bounds[1],
			rotLong = -(topRight[0]+bottomLeft[0])/2;
			center = [(topRight[0]+bottomLeft[0])/2+rotLong, (topRight[1]+bottomLeft[1])/2];
			currState = null;

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
				.style("fill", (x) => {return d3.rgb(0.0, 120 * carPerState[x.properties.ID_1] / maxCarsInState, 120 + 120 * carPerState[x.properties.ID_1] / maxCarsInState);})
				.attr("d", geoPath)
				.on("click", clickPath);
		});
		// Various formatters.
		var formatNumber = d3.format(",d"),
			formatChange = d3.format("+,d"),
			formatDate = d3.time.format("%B %d, %Y"),
			formatTime = d3.time.format("%I:%M %p");
		// A little coercion, since the CSV is untyped.
		cars.forEach(function(d, i) {
				d.index = i;
				d.price = +d.price;
				d.yearOfRegistration = +d.yearOfRegistration;
				d.powerPS = +d.powerPS;
				d.kilometer = +d.kilometer;
				d.state = +d.state;
				});
		// Create the crossfilter for the relevant dimensions and groups.
		car = crossfilter(cars);
		var all = car.groupAll(),
			price = car.dimension(function(d) { return d.price; }),
			prices = price.group(function(d) { return Math.floor(d / 100) * 100; }),
			year = car.dimension(function(d) { return d.yearOfRegistration; }),
			years = year.group(function(d) { return d; }),
			power = car.dimension(function(d) { return d.powerPS; }),
			powers = power.group(function(d) { return Math.floor(d / 10) * 10; }),
			kilometer = car.dimension(function(d) { return d.kilometer; }),
			kilometers = kilometer.group(function(d) { return Math.floor(d / 50) * 50; }),
			state = car.dimension(function(d) { return d.state; });
		carType = car.dimension(function(d) { return d.vehicleType; });
		charts = [
			barChart()
			.dimension(price)
			.group(prices)
			.x(d3.scale.linear()
					.domain([0, 100000])
					.rangeRound([0, 500])),
			barChart()
				.dimension(year)
				.group(years)
				.x(d3.scale.linear()
						.domain([1950, 2016])
						.range([0, 500])),
			barChart()
				.dimension(power)
				.group(powers)
				.x(d3.scale.linear()
						.domain([0, 800])
						.rangeRound([0, 500])),
			barChart()
				.dimension(kilometer)
				.group(kilometers)
				.x(d3.scale.linear()
						.domain([0, 200000])
						.rangeRound([0, 500])),
			];
		// Given our array of charts, which we assume are in the same order as the
		// .chart elements in the DOM, bind the charts to the DOM and render them.
		// We also listen to the chart's brush events to update the display.
		var chart = d3.selectAll(".chart")
			.data(charts)
			.each(function(chart) { chart.on("brush", renderAll).on("brushend", renderAll); });
		// Render the total.
		d3.selectAll("#total")
			.text(formatNumber(car.size()));
		// Renders the specified chart or list.
		renderAll();
		function render(method) {
			d3.select(this).call(method);
		}
		// Whenever the brush moves, re-rendering everything.
		function renderAll() {
			chart.each(render);
			d3.select("#active").text(formatNumber(all.value()));
		}
		renderAllCross = renderAll;
		// Like d3.time.format, but faster.
		function parseDate(d) {
			return new Date(2001,
					d.substring(0, 2) - 1,
					d.substring(2, 4),
					d.substring(4, 6),
					d.substring(6, 8));
		}
		window.filter = function(filters) {
			filters.forEach(function(d, i) { charts[i].filter(d); });
			renderAll();
		};
		window.reset = function(i) {
			charts[i].filter(null);
			currAttr = [[0, 100000000],
					 [0, 100000000],
					 [0, 100000000],
					 [0, 100000000]
						 ];
			redrawMap();
			renderAll();
		};
		function barChart() {
			if (!barChart.id) barChart.id = 0;
			var margin = {top: 10, right: 10, bottom: 20, left: 10},
				x,
				y = d3.scale.linear().range([200, 0]),
				id = barChart.id++,
				axis = d3.svg.axis().orient("bottom"),
				brush = d3.svg.brush(),
				brushDirty,
				dimension,
				group,
				round;
			function chart(div) {
				var width = x.range()[1],
					height = y.range()[0];
				y.domain([0, group.top(1)[0].value]);
				div.each(function() {
						var div = d3.select(this),
						g = div.select("g");
						// Create the skeletal chart.
						if (g.empty()) {
						div.select(".title").append("a")
						.attr("href", "javascript:reset(" + id + ")")
						.attr("class", "reset")
						.text("reset")
						.style("display", "none");
						g = div.append("svg")
						.attr("width", width + margin.left + margin.right)
						.attr("height", height + margin.top + margin.bottom)
						.append("g")
						.attr("transform", "translate(" + margin.left + "," + margin.top + ")");
						g.append("clipPath")
						.attr("id", "clip-" + id)
						.append("rect")
						.attr("width", width)
						.attr("height", height);
						g.selectAll(".bar")
							.data(["background", "foreground"])
							.enter().append("path")
							.attr("class", function(d) { return d + " bar"; })
							.datum(group.all());
						g.selectAll(".foreground.bar")
							.attr("clip-path", "url(#clip-" + id + ")");
						g.append("g")
							.attr("class", "axis")
							.attr("transform", "translate(0," + height + ")")
							.call(axis);
						// Initialize the brush component with pretty resize handles.
						var gBrush = g.append("g").attr("class", "brush").call(brush);
						gBrush.selectAll("rect").attr("height", height);
						gBrush.selectAll(".resize").append("path").attr("d", resizePath);
						}
						// Only redraw the brush if set externally.
						if (brushDirty) {
							brushDirty = false;
							g.selectAll(".brush").call(brush);
							div.select(".title a").style("display", brush.empty() ? "none" : null);
							if (brush.empty()) {
								g.selectAll("#clip-" + id + " rect")
									.attr("x", 0)
									.attr("width", width);
							} else {
								var extent = brush.extent();
								g.selectAll("#clip-" + id + " rect")
									.attr("x", x(extent[0]))
									.attr("width", x(extent[1]) - x(extent[0]));
							}
						}
						g.selectAll(".bar").attr("d", barPath);
				});
				function barPath(groups) {
					var path = [],
						i = -1,
						n = groups.length,
						d;
					while (++i < n) {
						d = groups[i];
						path.push("M", x(d.key), ",", height, "V", y(d.value), "h9V", height);
					}
					return path.join("");
				}
				function resizePath(d) {
					var e = +(d == "e"),
						x = e ? 1 : -1,
						y = height / 3;
					return "M" + (.5 * x) + "," + y
						+ "A6,6 0 0 " + e + " " + (6.5 * x) + "," + (y + 6)
						+ "V" + (2 * y - 6)
						+ "A6,6 0 0 " + e + " " + (.5 * x) + "," + (2 * y)
						+ "Z"
						+ "M" + (2.5 * x) + "," + (y + 8)
						+ "V" + (2 * y - 8)
						+ "M" + (4.5 * x) + "," + (y + 8)
						+ "V" + (2 * y - 8);
				}
			}
			brush.on("brushstart.chart", function() {
					var div = d3.select(this.parentNode.parentNode.parentNode);
					div.select(".title a").style("display", null);
					});
			brush.on("brush.chart", function() {
					var g = d3.select(this.parentNode),
					extent = brush.extent();
					if (round) g.select(".brush")
					.call(brush.extent(extent = extent.map(round)))
					.selectAll(".resize")
					.style("display", null);
					g.select("#clip-" + id + " rect")
					.attr("x", x(extent[0]))
					.attr("width", x(extent[1]) - x(extent[0]));
					dimension.filterRange(extent);
					currAttr[id][0] = extent[0];
					currAttr[id][1] = extent[1];
					redrawMap();
					redrawPie();
					});
			brush.on("brushend.chart", function() {
					if (brush.empty()) {
					var div = d3.select(this.parentNode.parentNode.parentNode);
					div.select(".title a").style("display", "none");
					div.select("#clip-" + id + " rect").attr("x", null).attr("width", "100%");
					dimension.filterAll();
					}
					});
			chart.margin = function(_) {
				if (!arguments.length) return margin;
				margin = _;
				return chart;
			};
			chart.x = function(_) {
				if (!arguments.length) return x;
				x = _;
				axis.scale(x);
				brush.x(x);
				return chart;
			};
			chart.y = function(_) {
				if (!arguments.length) return y;
				y = _;
				return chart;
			};
			chart.dimension = function(_) {
				if (!arguments.length) return dimension;
				dimension = _;
				return chart;
			};
			chart.filter = function(_) {
				if (_) {
					brush.extent(_);
					dimension.filterRange(_);
				} else {
					brush.clear();
					dimension.filterAll();
				}
				brushDirty = true;
				return chart;
			};
			chart.group = function(_) {
				if (!arguments.length) return group;
				group = _;
				return chart;
			};
			chart.round = function(_) {
				if (!arguments.length) return round;
				round = _;
				return chart;
			};
			return d3.rebind(chart, brush, "on");
		}

		function clickPath(d) {
			var x = width/2,
				y = height/2,
				k = 1,
				name = d.properties.NAME_1;

			map.selectAll("text")
				.remove();
			if ((focused === null) || !(focused === d)) {
				currState = d.properties.ID_1;
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
					.style("font-size","12px")
					.style("stroke-width","0px")
					.style("fill","white")
					.on("click", clickText);
			} else {
				currState = null;
				focused = null;
			};

			map.selectAll("path")
				.classed("active", focused && function(d) { return d === focused; });
			state.filter((d) => {
				if (!currState)
					return true;
				else
					return currState == d;
			})
			renderAll();
            calcCarPerState();
            redrawPie();
		}

		function clickText(d) {
			currState = null;
			focused = null;
			map.selectAll("text")
				.remove();
			map.selectAll("path")
				.classed("active", 0);
			map.transition()
				.duration(1000)
				.attr("transform", "scale("+1+")translate("+0+","+0+")")
				.style("stroke-width", 1.00+"px");
			state.filter((d) => {
				if (!currState)
					return true;
				else
					return currState == d;
					})
			renderAll();
            calcCarPerState();
            redrawPie();
		}
var bwidth = 900,
    bheight = 900,
    radius = Math.min(bwidth, bheight) / 2;

var color = d3.scale.category20c()
    //.range(["#98abc5", "#8a89a6", "#7b6888", "#6b486b", "#a05d56", "#d0743c", "#ff8c00"]);

var arc = d3.svg.arc()
    .outerRadius(radius - 10)
    .innerRadius(0);

var labelArc = d3.svg.arc()
    .outerRadius(radius - 40)
    .innerRadius(radius - 40);

var pie = d3.layout.pie()
    .sort(null)
    .value(function(d) { return d.value; });

var bsvg = d3.select("#pie").append("svg")
    .attr("width", bwidth)
    .attr("height", bheight)
  .append("g")
    .attr("transform", "translate(" + bwidth / 2 + "," + bheight / 2 + ")");

  if (error) throw error;

  var g = bsvg.selectAll(".arc")
      .data(pie(pieData))
    .enter().append("g")
      .attr("class", "arc");
  g.append("path")
      .attr("d", arc)
      .style("fill", function(d) {  return color(d.data.value); });

  g.append("text")
      .attr("transform", function(d) { return "translate(" + labelArc.centroid(d) + ")"; })
      .attr("dy", ".35em")
      .text(function(d) { return d.data.brand; });
  g.append("text")
      .attr("transform", function(d) { return "translate(" + labelArc.centroid(d) + ")"; })
      .attr("dy", "1.55em")
      .text(function(d) { return Math.round(100 * d.data.value / data.length) + "%"; });

  var redrawPie = () => {
      var bsvg = d3.select("#pie");
      bsvg.selectAll(".arc").remove();
      var g = bsvg.select("g").selectAll(".arc")
          .data(pie(pieData))
          .enter().append("g")
          .attr("class", "arc");
      g.append("path")
          .attr("d", arc)
          .style("fill", function(d) { return color(d.data.value); });

      g.append("text")
          .attr("transform", function(d) { return "translate(" + labelArc.centroid(d) + ")"; })
          .attr("dy", ".35em")
          .text(function(d) { return d.data.brand; });
      g.append("text")
          .attr("transform", function(d) { return "translate(" + labelArc.centroid(d) + ")"; })
          .attr("dy", "1.55em")
          .text(function(d) { return Math.round(100 * d.data.value / maxCarsWithConfig) + "%"; });
  }

var redrawMap = () => {
    calcCarPerState();
    d3.json("./dataBundesLander.json", function(collection) {
            map.selectAll("path")
		.style("fill", (x) => {return d3.rgb(0.0, 120 * carPerState[x.properties.ID_1] / maxCarsInState, 120 + 120 * carPerState[x.properties.ID_1] / maxCarsInState);})
    });
}

changeCarType = (s) => {
    currCarType = s;
    redrawMap();
    redrawPie();
	carType.filter((d) => {
			if (currCarType == 'all')
			return true;
			else
			return currCarType == d;
			})
	renderAllCross();
}

});

var changeCarType;


