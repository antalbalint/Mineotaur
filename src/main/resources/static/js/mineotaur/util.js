define(['mineotaur/context', 'd3'], function (context, d3) {
	var openTR = '<tr>';
	var closeTR = '</tr>';
	var openTD = '<td>';
	var closeTD = '</td>';
	// Constant for the Gaussian kernel computation.
    var cnst = Math.sqrt(2 * Math.PI);
    // Constant for F-D rule computation.
	var third = 1.0 / 3.0;
	function downloadableSVG() {
    			var element = document.getElementById("chart");
    			element.setAttribute("version", "1.1");
    			element.setAttribute("baseProfile", "full");
    			element.setAttribute("xmlns", "http://www.w3.org/2000/svg");
    			element.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
    			element.setAttribute("xmlns:ev", "http://www.w3.org/2001/xml-events");
    			return (new XMLSerializer).serializeToString(element);
    		}
    function convertToCSV(data) {
    			var str = '';
    			var and = ' and ';
    			for (k in data[1]) {
    				str += k + ',';
    			}
    			str = str.substr(0, str.length - 1);
    			str += '\n';
    			/*var str=prop1 + "," + prop2 + ",Gene name,Label\n";*/
    			data.forEach(function (d) {
    				for (k in d) {
    					if ($.isArray(d[k])) {
    						innerStr = '';
    						d[k].forEach(function (l) {
    							innerStr += l + and
    						});
    						innerStr = innerStr.substr(0, innerStr.length - and.length);
    						innerStr += ',';
    						str += innerStr;
    					} else {
    						str += d[k] + ",";
    					}
    				}
    				str = str.substr(0, str.length - 1);
    				str += '\n';
    				//str += d.x + "," + d.y + "," +  d.name + "," + d.label + "\n";
    			});
    			return str;
    		}

	return {
		shuffle: function (o) {
			//+ Jonas Raoni Soares Silva
			//@ http://jsfromhell.com/array/shuffle [v1.0]//v1.0
			for (var j, x, i = o.length; i; j = Math.floor(Math.random() * i), x = o[--i], o[i] = o[j], o[j] = x);
			return o;
		},
		scrollToElement: function (ele) {
			$(window).scrollTop(ele.offset().top).scrollLeft(ele.offset().left);
		},
		getLabels: function (data) {
			labelCloud = [];
			data.forEach(function (d) {
				lbs = d.labels;
				lbs.forEach(function (l) {
					labelCloud.push(l)
				})
			});
			/*temp = _.uniq(data, function(d) {return d.labels});
            var labels = [];
            temp.forEach(function(d) {
                 labels.push(d.label);
            });*/
			labels = _.uniq(labelCloud);
			labels.sort();
			return labels;
		},


		// TODO: data as input
		downloadCSV: function (filename) {
		    data = context.getData();
			/*if ($('#selection').is(':checked')) {
                content = convertToCSV(data);
            }
            else {
                content = convertToCSV(data);
            }*/
            content = convertToCSV(data);
			var csvData = 'data:application/csv;charset=utf-8,' + encodeURIComponent(content);
			$(this).attr({
				'download': filename,
				'href': csvData,
				'target': '_blank'
			});
		},
		downloadSVG: function (filename) {
			content = downloadableSVG(context.getData());
			var svgData = 'data:application/svg;charset=utf-8,' + encodeURIComponent(content);
			$(this).attr({
				'download': filename,
				'href': svgData,
				'target': '_blank'
			});
		},
		createInfoRow: function (prop, arr) {
			var row = openTR + openTD;
			row += prop;
			row += closeTD;
			row += openTD;
			row += numbers.statistic.mean(arr);
			row += closeTD;
			row += openTD;
			row += numbers.statistic.standardDev(arr);
			row += closeTD;
			row += openTD;
			row += numbers.statistic.median(arr);
			row += closeTD;
			row += openTD;
			row += Math.min.apply(Math, arr);
			row += closeTD;
			row += openTD;
			row += Math.max.apply(Math, arr);
			row += closeTD + closeTR;
			return row;
		},
		kernelDensityEstimator: function(kernel, x) {
        		return function (sample) {
        			return x.map(function (x) {
        				return [x, d3.mean(sample, function (v) {
        					return kernel(x - v);
        				})];
        			});
        		};
        },
        	/*
            Function to generate Gaussian kernel.
            Scale: bandwith.
        */

        gaussianKernel: function(scale) {
        		return function (u) {
        			return Math.exp(-0.5 * u * u / scale) / cnst;
        		};
        },
        	/*
            Function to calculate the optimal bandwidth with Freedman-Diaconis rule.
            d: array containing values.
        */

        freedmanDiaconisRule: function(d) {
        	d = d.sort();
        	var scale = d3.scale.quantile().domain(d).range([0, 1, 2, 3]).quantiles();
        	var iqr = scale[2] - scale[0];
        	var n = d.length;
        	return (2 * iqr) / Math.pow(n, third);
        }
	};
});