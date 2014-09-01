define(['mineotaur/util', 'mineotaur/controller', 'mineotaur/context', 'regression', 'numbers', 'd3'], function (util, controller, context, regression, numbers, d3) {
	// Radius of the circles on the scatter plot.
	var radius = 40;
	// Maximum opacity value for the circles. 1 = not opaque, 0 = completely opaque.
	var maxOpacity = 0.8;
	// Colorblind safe palette for labels. From colorbrewer2.org.
	var colors = ["#377eb8", "#4daf4a", "#984ea3", "#ff7f00", "#a65628", "#f781bf", "#999999", "#e41a1c", "#ffff33"];
	// NOT USED. Old color palette, not colorblind safe.
	var oldColors = ["#1f77b4", "#ff7f0e", "#2ca02c", "#9467bd"];
	var allLabels = null/* = ['Wild type', 'Microtubule hit', 'Shape hit', 'Cell cycle hit', 'Control']*/;
	var brush = 0;
	var margin = {
		top: 20,
		right: 20,
		bottom: 30,
		left: 40
	};
	var selected = [];

	function getColor(label) {
	    if (allLabels === null) {
	        allLabels = context.getLabels();
	    }
			for (i = 0; i < allLabels.length; ++i) {
				if (label == allLabels[i]) {
					return colors[i];
				}
			}
		}
		/* Function to add legend on charts.
            id: id of the chart in jQuery-style. E.g. '#chart'.
        */

	function addLegend(id) {
			// Width of the chart.
			width = $(id).attr('width') - margin.left - margin.right;
			// Adds legend.
			var legend = svg.selectAll(".legend")
				.data(labels)
				.enter()
				.append("g")
				.attr("class", "legend")
				.attr("transform", function (d, i) {
					return "translate(0," + i * 20 + ")";
				});
			// Adds rectangles containing the color for each label.
			legend.append("rect")
				.attr("x", width - 18)
				.attr("width", 18)
				.attr("height", 18)
				.style("fill", function (label) {
					return getColor(label);
				});
			// Adds text containg the labels shown in the chart.
			legend.append("text")
				.attr("x", width - 24)
				.attr("y", 9)
				.attr("dy", ".35em")
				.style("text-anchor", "end")
				.text(function (label) {
					return label;
				});
		}
		/*
            Function to add axes to a graph.
            svg: svg element of the graph.
            xAxis, yAxis: axes to be added.
            xText, yText: axis titles.
            width, height: width and height of the graph.
        */

	function addAxes(svg, xAxis, yAxis, xText, yText, width, height) {
	 	console.log(xText);
	 	console.log(yText);
	    // Adds x axis. The title is set by the variable prop1.
		svg.append("g")
			.attr("class", "x axis")
			.attr("transform", "translate(0," + height + ")")
			.call(xAxis)
			.append("text")
			.attr("class", "axislabel")
			.attr("x", width)
			.attr("y", -6)
			.style("text-anchor", "end")
			.text(xText);
		// Adds y axis. The title is set by the variable prop2.
		svg.append("g")
			.attr("class", "y axis")
			.call(yAxis)
			.append("text")
			.attr("class", "axislabel")
			.attr("transform", "rotate(-90)")
			.attr("y", 6)
			.attr("dy", ".71em")
			.style("text-anchor", "end")
			.text(yText);
	}

	function addHexColor(c1, c2) {
		var hexStr = (parseInt(c1, 16) + parseInt(c2, 16)).toString(16);
		while (hexStr.length < 6) {
			hexStr = '0' + hexStr;
		} // Zero pad.
		return hexStr;
	}

	function divideHexColor(c1, length) {
		var hexStr = (Math.floor((parseInt(c1, 16)) / length)).toString(16);
		while (hexStr.length < 6) {
			hexStr = '0' + hexStr;
		} // Zero pad.
		return hexStr;
	}

	function getHistogramData(hist, input, l, x) {
		input = input.filter(function (d) {
			return d.labels.indexOf(l) > -1
		});
		var values = [];
		input.forEach(function (d) {
			values.push(d.x)
		});
		return hist(values);
	}

	function addDataToMultiHistogram(data, x, y, barWidth, height, labels) {
		//console.log(data.length);
		var bar = svg.selectAll(".bar").data(data).enter().append("g")
			.attr("class", "bar")
			//.attr("transform", function(d) { return "translate(" + (x(d.x) + translate) + "," + y(d.y) + ")"; })
		;
		//data.forEach(function(d) {console.log(height - y(d.y))});
		bar.append("rect")
			.attr("x", function (d) {
				return x(d.x) + getTranslationForLabel(d.label, barWidth, labels)
			})
			.attr("y", function (d) {
				return y(d.y)
			})
			.attr("width", barWidth)
			.attr("height", function (d) {
				return height - y(d.y);
			}).style("fill", function (d) {
				return getColor(d.label);
			}).style("opacity", 1);
	}

	function addDataToHistogram(data, x, y, barWidth, height) {
		var bar = svg.selectAll(".bar").data(data).enter().append("g")
			.attr("class", "bar")
			//.attr("transform", function(d) { return "translate(" + (x(d.x) + translate) + "," + y(d.y) + ")"; })
		;
		//data.forEach(function(d) {console.log(height - y(d.y))});
		bar.append("rect")
			.attr("x", function (d) {
				return x(d.x)
			})
			.attr("y", function (d) {
				return y(d.y)
			})
			.attr("width", barWidth)
			.attr("height", function (d) {
				return height - y(d.y);
			}).style("opacity", 1);
	}

	function getTranslationForLabel(d, barWidth, labels) {
		i = 0;
		translation = 0;
		labels.forEach(function (label, i) {
			if (d == label) {
				translation = barWidth * i;
				return;
			} else {
				i = i + 1;
			}
		});
		return translation;
	}
	return {
	    getRange: function(data) {
	        var xRange = d3.extent(data, function (d) {
            						return d.x;
            					});
            					var yRange = d3.extent(data, function (d) {
            						return d.y;
            					});
            var xLogRange = d3.extent(data, function (d) {
            						return Math.log(d.x);
            					});
            					var yLogRange = d3.extent(data, function (d) {
            						return Math.log(d.y);
            					});
            console.log(xRange);
            console.log(yRange);
            return {x: xRange, y: yRange, logX: xLogRange, logY: yLogRange};
	    },
		drawScatterPlot: function (data, target, id, type, prop1, prop2, geneName) {
                console.log(controller);
				if (typeof (data) === 'undefined') {
					var data = context.getData();
					var type = context.getType();
					var target = context.getTarget();
					var id = context.getId();
					var prop1 = context.getProperty(0);
					var prop2 = context.getProperty(1);
					var range = context.getRange();
					var xRange = range.x;
					var yRange = range.y;
					var geneName = context.getGenename();
				} else {
					var xRange = d3.extent(data, function (d) {
						return d.x;
					});
					var yRange = d3.extent(data, function (d) {
						return d.y;
					});
				}
				console.log(xRange);
			// Width of the target
		var graphWidth = $(target).width();
		// Width of the graph
		var width = graphWidth - margin.left - margin.right;
		// Height of the graph
		//console.log($(target).css('max-height'));
		var height;
		var maxHeight = parseInt($(target).css('max-height'));
		if (!isNaN(maxHeight)) {
			height = maxHeight - margin.top - margin.bottom - 40;
		} else {
			height = 550 - margin.top - margin.bottom - 40;
		}
		//console.log(height);
		// Adjust x and y scales according to their physical range.
		x = d3.scale.linear().range([0, width]);
		y = d3.scale.linear().range([height, 0]);
		// If the x or the y ranges of the plot are already set, use them, otherwise calculate them from the data.
		x.domain(xRange).nice();
		y.domain(yRange).nice();
		xAxis = d3.svg.axis().scale(x).orient("bottom");
		xAxis.outerTickSize(1);
		yAxis = d3.svg.axis().scale(y).orient("left");
		yAxis.outerTickSize(1);
		// Divide the maximum opacity among the labels.
		opacity = maxOpacity / labels.length;
		console.log(opacity);
		// Div for the tooltip on the circles. Contains coordinates and name.
		var tooltip = d3.select("body")
			.append("div")
			.attr("class", "tooltip")
			.style("opacity", 0);
		// Add chart to the target width the provided id.
		svg = d3.select(target)
			.append("svg")
			.attr("id", id)
			.attr("viewbox", "0 0 " + width + " " + height)
			.attr("width", width + margin.left + margin.right)
			.attr("height", height + margin.top + margin.bottom)
			.append("g")
			.attr("transform", "translate(" + margin.left + "," + margin.top + ")");
		// Add axes to the graph.
		addAxes(svg, xAxis, yAxis, prop1, prop2, width, height);
		// Set chart title. If the chart is cellwise, the title contains the name of the gene.
		console.log(prop1);
		console.log(prop2);
		var title = prop1 + " vs " + prop2;
		if (type == 'cellwiseScatter') {
			title = geneName + ": " + title;
		}
		// Adds title to the chart.
		svg.append("text")
			.attr("x", (width / 2))
			.attr("y", 5 - (margin.top / 2))
			.attr("text-anchor", "middle")
			.style("font-size", "16px")
			.text(title);
		// Adds circles to plot.
		var dots = svg.selectAll(".dot")
			.data(data)
			.enter()
			// Adds link to each circle to the corresponding sysgro page.
			.append("a")
			.attr("xlink:href", function (d) {
				if (d.name != "") {
					return "http://sysgro.org/omero/HTscape/geneFromScreen/" + d.name
				}
			})
			.attr("target", "_blank")
			.append("circle")
			// Adds context menu via the context_1 class attribute. See eventlistener.js for more.
			.attr("class", "dot context_1")
			.attr("r", 3.5)
			.attr("cx", function (d) {
				return x(d.x);
			})
			.attr("cy", function (d) {
				return y(d.y);
			})
			.style("opacity", /*opacity*/ function (d) {
				return opacity * d.labels.length;
			})
			// Fills each node with a color assigned to the label.
			.attr("fill", function (d) {
				var color = "0x000000";
				d.labels.forEach(function (l) {
					color = addHexColor(color, getColor(l).replace("#", "0x"));
				});
				color = divideHexColor(color, d.labels.length);
				//console.log(color);
				return '#'.concat(color); /*return getColor(d.label);*/
			})
			// Adds tooltip if the mouse is over the circle.
			.on("mouseover", function (d) {
				tooltip.transition()
					.duration(200)
					.style("opacity", 0.8);
				tooltip.html(d.name + ": (" + (d.x).toFixed(2) + ", " + (d.y).toFixed(2) + ")")
					.style("left", (d3.event.pageX + 5) + "px")
					.style("top", (d3.event.pageY - 28) + "px");
			})
			// Removes tooltip if the mouse is out of the circle.
			.on("mouseout", function (d) {
				tooltip.transition()
					.duration(500)
					.style("opacity", 0);
			})
			// Sets currentItem to the data assigned to the circle if the right button is clicked.
			.on("contextmenu", function (d) {
				context.setCurrentItem(d);
				//currentItem = d;
			});
	},
	drawKDEPlot: function (input, target, prop, geneName) {
			if (typeof (input) === 'undefined') {
				var input = context.getData();
				var target = context.getTarget();
				var id = context.getId();
				var prop = context.getProperty(0);
				var geneName = context.getGenename();
			}
			// Width of the target
			var graphWidth = $(target).width();
			// Dimensions of the graph.
			/*var margin = {top: 20, right: 30, bottom: 30, left: 40},*/
			width = graphWidth - margin.left - margin.right - 40;
			var maxHeight = parseInt($(target).css('max-height'));
			if (!isNaN(maxHeight)) {
				height = maxHeight - margin.top - margin.bottom - 40;
			} else {
				height = 550 - margin.top - margin.bottom - 40;
			}
			//height = 500 - margin.top - margin.bottom;
			// Map to store the data values per label.
			var map = new Object();
			// Array to store all data values.
			var values = [];
			// Initialize map with empty arrays.
			labels.forEach(function (label) {
				map[label] = [];
			});
			// Add data to map and values.
			input.forEach(function (d) {
				d.labels.forEach(function (l) {
					map[l].push(d.x);
				})
				values.push(d.x);
			});
			// Calculate optimal bandwidth with the Freedman-Diaconis rule.
			var bandWidth = util.freedmanDiaconisRule(values);
			// Calculate optimal number of bins based on optimal bandwidth.
			var numberOfBins = Math.ceil(d3.max(values) / bandWidth);
			// Scale for the x axis.
			var x = d3.scale.linear().domain(d3.extent(values)).range([0, width]);
			// Initialize the KDE function.
			var kde = util.kernelDensityEstimator(util.gaussianKernel(bandWidth), x.ticks(numberOfBins));
			// Map to store KDE arrays per label.
			var data = new Object();
			// Maximum KDE value.
			var max = 0;
			// Calculate and store KDE values and max.
			labels.forEach(function (d) {
				data[d] = kde(map[d]);
				m = d3.max(data[d], function (d) {
					return d[1];
				});
				if (m > max) {
					max = m;
				}
			});
			// Scale for the y axis.
			var y = d3.scale.linear().domain([0, max]).range([height, 0]);
			// Add chart to the target width the provided id.
			svg = d3.select(target)
				.append("svg")
				.attr("id", id)
				.attr("width", width + margin.left + margin.right)
				.attr("height", height + margin.top + margin.bottom)
				.append("g")
				.attr("transform", "translate(" + margin.left + "," + margin.top + ")");
			// Create axes.
			var xAxis = d3.svg.axis().scale(x).orient("bottom");
			var yAxis = d3.svg.axis().scale(y).orient("left").tickFormat(d3.format("%"));
			// Add axes to the graph.
			addAxes(svg, xAxis, yAxis, context.getLegends(0), "Percentage", width, height);
			// Create line drawer function.
			var line = d3.svg
				.line()
				.x(function (d) {
					return x(d[0]);
				})
				.y(function (d) {
					return y(d[1]);
				});
			// Draw KDE line for each label.
			labels.forEach(function (label) {
				svg.append("path")
					.datum(data[label])
					.attr("class", "line")
					.attr("d", line)
					.style("stroke", getColor(label));
			});
			var title = prop;
			if (context.getType() === 'cellwiseKDE') {
				title = geneName + ": " + title;
			}
			svg.append("text")
				.attr("x", (width / 2))
				.attr("y", 5 - (margin.top / 2))
				.attr("text-anchor", "middle")
				.style("font-size", "16px")
				.text(title);
		},
		drawHistogram: function (multi, input, prop, target, id, geneName) {
		    if (typeof (input) === 'undefined') {
		        var input = context.getData();
                var prop = context.getProperty(0);
            	var target = context.getTarget();
            	var id = context.getId();
            	var geneName = context.getGenename();
		    }
            console.log(prop);
            console.log(geneName);
			var graphWidth = $(target).width();
			var values = [];
			input.forEach(function (d) {
				values.push(d.x)
			});
			// A formatter for counts.
			var formatCount = d3.format(",.0f");
			/*var margin = {top: 10, right: 30, bottom: 30, left: 30},*/
			width = graphWidth - margin.left - margin.right;
			var maxHeight = parseInt($(target).css('max-height'));
			if (!isNaN(maxHeight)) {
				height = maxHeight - margin.top - margin.bottom - 40;
			} else {
				height = 550 - margin.top - margin.bottom - 40;
			}
			//height = 500 - margin.top - margin.bottom;
			var x = d3.scale.linear()
				.domain(d3.extent(values))
				.range([0, width]).nice();
			var lbs = [];
			input.forEach(function (d) {
				d.labels.forEach(function (l) {
					lbs.push(l)
				});
			});
			labels = _.uniq(lbs);
			console.log(labels);
			var numberOfBins, barWidth, hist;
			if (input.length == 1) {
				numberOfBins = 1;
				barWidth = width - 1;
				//values.push(0);
				hist = d3.layout.histogram().range(d3.extent(values)).frequency(false);
			} else {
				var bandWidth = util.freedmanDiaconisRule(values);
				numberOfBins = Math.ceil(d3.max(values) / bandWidth);
				if (multi) {
					numberOfBins = numberOfBins / labels.length;
				}
				//barWidth = width / (data.length - 1);
				hist = d3.layout.histogram().bins(x.ticks(numberOfBins)).frequency(false);
			}
			//var hist = d3.layout.histogram().bins(x.ticks(numberOfBins)).frequency(false);
			var data = hist(values);
			//var barWidth = bandWidth;
			if (data.length != 1) {
				barWidth = width / (data.length) - 1;
			}
			var y = d3.scale.linear()
				.domain([0, d3.max(data, function (d) {
					return d.y;
				})])
				.range([height, 0]).nice();
			var xAxis = d3.svg.axis()
				.scale(x)
				.orient("bottom");
			var yAxis = d3.svg.axis()
				.scale(y)
				.orient("left")
				.tickFormat(d3.format(".0%"));
			svg = d3.select(target).append("svg").attr("id", id)
				.attr("width", width + margin.left + margin.right)
				.attr("height", height + margin.top + margin.bottom)
				.append("g")
				.attr("transform", "translate(" + margin.left + "," + margin.top + ")");
			var bar = svg.selectAll(".bar").data(data).enter();
			var arr = [];
			labels.forEach(function (label) {
				var mt = getHistogramData(hist, input, label, x);
				mt.forEach(function (d) {
					var obj = new Object();
					obj.x = d.x;
					obj.y = d.y;
					obj.label = label;
					arr.push(obj);
				});
			});
			if (multi) {
				addDataToMultiHistogram(arr, x, y, barWidth / labels.length, height, labels)
			} else {
				addDataToHistogram(arr, x, y, barWidth, height)
			}
			svg.append("g")
				.attr("class", "x axis")
				.attr("transform", "translate(0," + height + ")")
				.call(xAxis)
				.append("text")
				.attr("class", "axislabel")
				.attr("x", width)
				.attr("y", -6)
				.style("text-anchor", "end")
				.text(context.getLegends(0));
			svg.append("g")
				.attr("class", "y axis")
				.call(yAxis)
				.append("text")
				.attr("class", "axislabel")
				.attr("transform", "rotate(-90)")
				.attr("y", 6)
				.attr("dy", ".71em")
				.style("text-anchor", "end")
				.text("Frequency");
			var title = prop;
			if (context.getType() == 'cellwiseHistogram') {
				title = geneName + ": " + title;
			}
			svg.append("text")
				.attr("x", (width / 2))
				.attr("y", 5 - (margin.top / 2))
				.attr("text-anchor", "middle")
				.style("font-size", "16px")
				.text(title);
		},
		addLegend: function (id) {
			// Width of the chart.
			width = $(id).attr('width') - margin.left - margin.right;
			// Adds legend.
			var legend = svg.selectAll(".legend")
				.data(labels)
				.enter()
				.append("g")
				.attr("class", "legend")
				.attr("transform", function (d, i) {
					return "translate(0," + i * 20 + ")";
				});
			// Adds rectangles containing the color for each label.
			legend.append("rect")
				.attr("x", width - 18)
				.attr("width", 18)
				.attr("height", 18)
				.style("fill", function (label) {
					return getColor(label);
				});
			// Adds text containg the labels shown in the chart.
			legend.append("text")
				.attr("x", width - 24)
				.attr("y", 9)
				.attr("dy", ".35em")
				.style("text-anchor", "end")
				.text(function (label) {
					return label;
				});
		},
		addSelectionTool: function (id) {
			if (typeof (id) == 'undefined') {
				id = 'selbrush';
			}
			var data = context.getData();
			svg = d3.select(context.getTarget()).select("svg");
			selected = context.getSelected();
			brush = svg.append("g")
				.attr("class", "brush")
				.attr("id", "selbrush")
				.call(d3.svg.brush()
					.x(x)
					.y(y)
					.on("brush", function () {
						extent = d3.event.target.extent();
						svg.selectAll(".dot").classed("selected", function (d) {
							return extent[0][0] <= d.x && d.x < extent[1][0] && extent[0][1] <= d.y && d.y < extent[1][1];
						});
					})
					.on("brushend", function () {
						//console.log(keyisdown);
						/*selected = [];*/
						data.forEach(function (d) {
							if (extent[0][0] <= d.x && d.x < extent[1][0] && extent[0][1] <= d.y && d.y < extent[1][1])
								selected.push(d);
						});
						svg.selectAll("rect.extent").attr("class", "extent context_2");
						console.log(controller);
						if (context.isKeyDown()) {
							brush = brush + 1;
							addSelectionTool(context.getData(), id + brush);
							//console.log(selected);
						}
						context.setSelected(selected);
					})
				);
		},
		removeSelectionTool: function (id) {
			if (typeof (id) == 'undefined') {
				id = 'selbrush';
			}
			context.setSelected([]);
			$(id).remove();
		},
		addRegressionLine: function () {
			var data = context.getData();
			//svg = d3.select("#graph").select("svg");
			tooltip = d3.select("body").append("div")
				.attr("class", "tooltip")
				.style("opacity", 0);
			data.sort(function (a, b) {
				return a.x - b.x
			});
			var values = [];
			data.forEach(function (d) {
				values.push([x(d.x), y(d.y)]);
			});
			//console.log(regression);
			var myRegression = regression($('#regressionType').val(), values);
			//console.log(data, function(d) {return d.y});
			//console.log(myRegression.points, function(d) {return d.y});
			var origValues = [],
				regressionValues = [];
			//console.log(data)
			data.forEach(function (d) {
				origValues.push(y(d.y));
			});
			//origValues.sort(function(a,b){return a-b;});
			//console.log(origValues);
			myRegression.points.forEach(function (d) {
				regressionValues.push(d[1]);
			});
			//var reg = numbers.statistic.linearRegression(xValues, origValues);
			//var reg_line = d3.svg.line().x(function(d,i){return x(i)}).y(function(d,i){return y(regression(i))});
			//console.log(reg_line);
			/*console.log(origValues.length);

                       console.log(regressionValues.length);
                       console.log(numbers.statistic.correlation(origValues, regressionValues));*/
			//var numbers = require('numbers');
			/*console.log(numbers);
                    console.log(regression);*/
			var corr = numbers.statistic.correlation(origValues, regressionValues);
			/*console.log(x);
                	console.log(y);
                	console.log(regressionValues);*/
			var lineFunction = d3.svg.line()
				.x(function (d) {
					return d[0];
				})
				.y(function (d) {
					return d[1];
				});
			//.interpolate("linear");
			svg.append("path").attr("id", "regressionline").attr("d", lineFunction(myRegression.points)).on("mouseover", function (d) {
					tooltip.transition()
						.duration(200)
						.style("opacity", .9);
					tooltip.html('Correlation: ' + corr.toFixed(2))
						.style("left", (d3.event.pageX) + "px")
						.style("top", (d3.event.pageY - 28) + "px");
				})
				.on("mouseout", function (d) {
					tooltip.transition()
						.duration(500)
						.style("opacity", 0);
				});
		},
		removeRegressionLine: function () {
			$('#regressionline').remove();
		},
		filterGenes: function() {
		    var checked = $("#geneFilt").multiselect("getChecked").map(function () {
                                        			return this.value;
                                        		}).get();
                                        		//checked.forEach(function(c) {
                                        		dots = d3.select("#graph").select("svg").selectAll("circle");
                                        		dots.attr("class", function (d) {
                                        			if (checked.indexOf(d.name) > -1) {
                                        				console.log(d.name);
                                        				return "dot context_1 selectedDot";
                                        			} else {
                                        				return "dot context_1 nonSelectedDot";
                                        			}
                                        		});
                                        		d3.select("#graph").select("svg").selectAll(".selectedDot").attr("r", 10);
                                        		d3.select("#graph").select("svg").selectAll(".nonSelectedDot").attr("r", 1);
                                        		/*.attr("class", function(d) { if (d.name==c) {console.log(d.name + " vs " + c); return "selectedDot"} else {return "";}});*/
                                        		//});
		},
		resetGeneFilter: function() {
		    d3.select("#graph").select("svg").selectAll("circle").attr("class", "dot context1").attr("r", 3.5);
		}
};
});