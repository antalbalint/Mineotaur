define(['mineotaur/events', 'mineotaur/ui', 'mineotaur/context', 'mineotaur/plots', 'mineotaur/util', 'pako',    'jquery', 'jquery.form', 'jquery-ui', 'jquery.magnific-popup','jquery.multiselect', 'jquery.multiselect.filter', 'jquery.jeegoocontext',   'jquery.history', 'bootstrap', 'lightslider.min'],
function (events, ui, context, plots, util, pako, $) {

    /*console.log(util);
    console.log(ui);
    console.log(context);
    console.log($);    */

    var compareMap = new Object();
    var chartBase = 'chart';
    var graphBase = '#graph';
    var compareMax = 4;
    var nextChart = 0;
    var compareCharts = [];
    var keyisdown = false;

    /*function removeChart(element, useParent, hide) {
    		if (useParent) {
    			parent = $(element).parent();
    		} else {
    			parent = $(element);
    		}
    		id = parent.attr("id").substring(5, 6);
    		parent.empty();
    		if (hide) {
    			delete compareMap[compareCharts[id]];
    			console.log("before: " + compareCharts);
    			compareCharts.splice(id, 1);
    			console.log("after: " + compareCharts);
    			nextChart = parseInt(id);
    			console.log(nextChart);
    			parent.attr("hidden", "hidden");
    			if (Object.keys(compareCharts).length == 0) {
    				$.magnificPopup.close();
    			}
    		}
    	}*/

    $.blockUI.defaults.message = null;
    $(document).ajaxStart($.blockUI).ajaxStop($.unblockUI);
    var keyisdown = false;
        var sep = '|';

        function processArray(array) {
            var string = array[0];
            for (i=1; i < array.length; ++i) {
                string = string + sep + array[i];
            }
            return string;
        }

        function transformFormData(form, type) {
            var formData = $(form).serializeArray();
                                    	    var result = {};
                                    	    for (i = 0; i < formData.length; ++i) {

                                    	        if (typeof(result[formData[i].name]) === 'undefined') {

                                    	            result[formData[i].name] = [];
                                    	        }
                                    	        result[formData[i].name].push(formData[i].value);
                                    	    }
                                    	    console.log(result);
                                    	    var data = {};
                                    	    for (key in result) {
                                    	        var array = result[key];
                                    	        if (key === 'geneList' || key === 'geneListDist') {
                                    	            console.log(array);
                                    	            data[key] = context.getCompressedGeneList(array);
                                    	        }
                                    	        //console.log(key);

                                    	        //console.log(array);
                                    	        else if (array.length === 1) {
                                    	            data[key] = array[0];
                                    	        }
                                    	        else {
                                    	            data[key] = processArray(array);
                                    	        }
                                    	    }
            data['type'] = type;
            console.log(data);
            var compressed = pako.deflate(JSON.stringify(data), {level: 9 , to: 'string'});
            /*var compressed = pako.deflate(JSON.stringify(data), {level: 9 , to: 'string'});
            console.log(JSON.stringify(data).length)
            console.log(compressed.length);
            console.log(btoa(compressed).length);*/
            return btoa(compressed);
        }
    $(document).ready(function () {


                /*ZeroClipboard.config({
                		moviePath: 'js/lib/ZeroClipboard.swf'
                	});
                	var client = new ZeroClipboard($("#copy-button"));
                	client.on('load', function (client) {
                		client.on('dataRequested', function (client, args) {
                			client.setText(window.location.href + "query?type=" + type + "&" + queryString);
                		});
                		client.on('complete', function (client, args) {
                			$("#shareAlert").show();
                			$("#shareAlert").fadeOut(5000);
                		});
                	});
                	$('.alert .close').on("click", function (e) {
                		$(this).parent().hide();
                	});*/

                	$('body').bind('keydown', function (e) {
                		var keycode = 16;
                		if (e.which === keycode) {
                			context.setKeyDown(true);
                		}
                	}).bind('keyup', function () {
                		context.setKeyDown(false);
                	});
                	$('.context_1').jeegoocontext('menu_1', {
                    		widthOverflowOffset: 0,
                    		heightOverflowOffset: 3,
                    		submenuLeftOffset: -4,
                    		submenuTopOffset: -5,
                    		onSelect: function (e, c) {
                    			var key = $(this).attr('id');
                    			switch (key) {
                    			case 'cellwiseScatter':
                    			    $('#cellwisePlotType').val(key);
                    			    $('#cellwiseProp1').val($('#prop1').val());
                                                        					$('#cellwiseProp2').val($('#prop2').val());
                                                        					$('#interphaseCellwiseProp1').val($('#interphaseProp1').val());
                                                        					$('#spindleCellwiseProp1').val($('#spindleProp1').val());
                                                        					$('#paaCellwiseProp1').val($('#paaProp1').val());
                                                        					$('#interphase2CellwiseProp1').val($('#interphaseProp1').val());
                                                        					$('#interphaseCellwiseProp2').val($('#interphaseProp2').val());
                                                        					$('#spindleCellwiseProp2').val($('#spindleProp2').val());
                                                        					$('#paaCellwiseProp2').val($('#paaProp2').val());
                                                        					$('#interphase2CellwiseProp2').val($('#interphaseProp2').val());
                                                        					console.log(context);
                                                        					console.log(context.getCurrentItem());
                                                        					$('#geneCWProp1').val(context.getCurrentItem().name);
                                                        					$('#cellwiseGraphSubmit').trigger('click');
                                                        					break;
                    			case 'cellwiseHistogramX':
                    			case 'cellwiseKDEX':

                    			case 'cellwiseHistogramY':
                    			case 'cellwiseKDEY':
                    				$('#cellwisePlotType').val(key);
                    					$('#cellwisePlotType').val(key.substring(0, key.length - 1));
                    					console.log(key);
                    					console.log(key.substring(0, key.length - 1));
                    					console.log(context);
                    					console.log(context.getCurrentItem());
                    					$('#geneCWDist').val(context.getCurrentItem().name);
                    					console.log(context.getLegends());
                    					if (key == 'cellwiseHistogramX' || key == 'cellwiseKDEX') {
                    						$('#propCWDist').val(context.getLegends()[0]);
                    					} else {
                    						$('#propCWDist').val(context.getLegends()[1]);
                    					}
                    					/*$('#propCWDist').val(prop);*/
                    					console.log($('#propCWDist').val());
                    					$('#cellwiseDistributionSubmit').trigger('click');

                    				break;
                    			default:
                    				return false;
                    			}
                    		}
                    	});
                    	// TODO
                    	$('.context_2').jeegoocontext('menu_2', {
                    		submenuLeftOffset: -1,
                    		event: 'mouseover',
                    		autoHide: true,
                    		onSelect: function (e, c) {
                    			switch ($(this).attr('id')) {
                    			case 'analyze':
                    				context.setData(context.getSelected());
                    				console.log(context.getSelected());
                    				console.log(context.getData());
                                    context.setRangeBulk(plots.getRange(context.getData()));
                                    console.log(context.getRange());
                    				ui.showGraph();
                    				break;
                    			default:
                    				return false;
                    			}
                    		}
                    	});
                    	History.Adapter.bind(window, 'statechange', function () {
                    		//console.log(History.savedStates);
                    		//console.log(History.savedStates[History.savedStates.length - 2]);
                    		//console.log(History.state);
                    		currentState = History.getState();
                    		/*if (History.savedStates.length > 2) {
                    			console.log(History.savedStates[History.savedStates.length - 3].id);
                    			console.log(currentState.id);
                    		}*/
                    		if (History.savedStates.length > 2 && History.savedStates[History.savedStates.length - 3].id == currentState.id) {
                    			/*$('#graph').html(currentState.data.graph);*/
                    			/*currentData = currentState.data.graph;
                    			prop1 = currentState.data.prop1;
                    			prop2 = currentState.data.prop2;
                    			type = currentState.data.type;*/
                    			/*context.setData(currentData);
                    			context.setProperties([prop1, prop2]);
                    			context.setType(type);*/
                                context.import(currentState.data);
                    			ui.showGraph();
                    			/*$('#graph').html(currentState.data.graph);
                    			$('#tools').html(currentState.data.tools);*/
                    			ui.resetTools();
                    			//console.log(currentState);
                    		}
                    		//console.log(History.getState().title); // do something on statechange
                    	});
                    	//- See more at: http://blog.teamextension.com/onhashchange-jquery-hashchange-pushstate-and-history-js-1012#sthash.RLmY4eXl.dpuf

                    	$("#geneList").multiselect({
                    		noneSelectedText: "Genes"
                    	}).multiselectfilter();
                    	$("#geneListDist").multiselect({
                    		noneSelectedText: "Genes"
                    	}).multiselectfilter();
                    	$("#geneFilt").multiselect({
                    		noneSelectedText: "Genes"
                    	}).multiselectfilter();
//                        $('#graphForm').ajaxForm({
//                        		// dataType identifies the expected content type of the server response
//                        		dataType: 'json',
//                        		beforeSend: function (arr, $form, options) {
//                        		console.log("button hit");
//                        		ui.showSpinner('spin');
//                                    //ui.spinner.spin(document.getElementById('spin'));
//                        			$('#graphFormSubmit').attr("disabled", "disabled");
//                        			$('#graphFormReset').attr("disabled", "disabled");
//                        			queryString = $('#graphForm').formSerialize();
//                                    context.setURL(queryString);
//                        		},
//                        		// success identifies the function to invoke when the server response
//                        		// has been received
//                        		success: function(data) {events.genewiseScatterSuccess(data)},
//                        		error: function () {
//
//                        				ui.formError('spin');
//                        			}
//
//                        	});
//                        	$('#cellwiseGraphForm').ajaxForm({
//                        		// dataType identifies the expected content type of the server response
//                        		dataType: 'json',
//                        		beforeSend: function () {
//                        			ui.showSpinner('spin');
//                        			$('#cellwiseGraphFormSubmit').attr("disabled", "disabled");
//                        			$('#cellwiseGraphFormReset').attr("disabled", "disabled");
//                        			queryString = $('#cellwiseGraphForm').formSerialize();
//                        			context.setURL(queryString);
//                        		},
//                        		// success identifies the function to invoke when the server response
//                        		// has been received
//                        		success: function(data) {events.cellwiseScatterSuccess(data)},
//                        		error: function () {
//                        			ui.formError('spin');
//                        		}
//                        	});
//                        	$('#genewiseDistributionForm').ajaxForm({
//                        		// dataType identifies the expected content type of the server response
//                        		dataType: 'json',
//                        		beforeSend: function () {
//                        			ui.showSpinner('spin');
//                        			$('#genewiseDistributionFormSubmit').attr("disabled", "disabled");
//                        			$('#genewiseDistributionFormReset').attr("disabled", "disabled");
//                        			queryString = $('#genewiseDistributionForm').formSerialize();
//                        			context.setURL(queryString);
//                        		},
//                        		// success identifies the function to invoke when the server response
//                        		// has been received
//                        		success: function(data) {events.genewiseDistributionSuccess(data)},
//                        		error: function () {
//                        			ui.formError('spin');
//                        		}
//                        	});
//                        	$('#cellwiseDistributionForm').ajaxForm({
//                        		// dataType identifies the expected content type of the server response
//                        		dataType: 'json',
//                        		beforeSend: function () {
//                        			ui.showSpinner('spin');
//                        			$('#cellwiseDistributionFormSubmit').attr("disabled", "disabled");
//                        			$('#cellwiseDistributionFormReset').attr("disabled", "disabled");
//                        			queryString = $('#cellwiseDistributionForm').formSerialize();
//                        			context.setURL(queryString);
//                        		},
//                        		// success identifies the function to invoke when the server response
//                        		// has been received
//                        		success: function(data) {events.cellwiseDistributionSuccess(data)},
//                        		error: function () {
//                        			ui.formError('spin');
//                        		}
//                        	});
$('#graphForm').submit(function(event){
                        	    data = {};
                                                        	    data['content'] = transformFormData(this,'genewiseScatter');
                                                                context.setURL("content="+data['content']);
                                                        	    /*context.setType('genewiseScatter');
                                                        	    queryString = $(this).formSerialize();
                                                                context.setURL(queryString);
                                                        	    console.log(data);*/
                                                        	    $.ajax({
                                                                                                    url: "/decode",
                                                                                                    data: data,
                                                                                                    dataType: "json",
                                                                                                    type:"GET",
                                                                                                    beforeSend: function () {
                                                                                                                            			ui.showSpinner('spin');
                                                                                                                            			$('#graphFormSubmit').attr("disabled", "disabled");
                                                                                                                            			$('#graphFormReset').attr("disabled", "disabled");

                                                                                                                            		},
                                                                                                                            		// success identifies the function to invoke when the server response
                                                                                                                            		// has been received
                                                                                                                            		success: function(data) {events.genewiseScatterSuccess(data)},
                                                                                                                            		error: function () {
                                                                                                                            			ui.formError('spin');
                                                                                                                            		}
                                                                                                    }
                                                                                                )

                                                                event.preventDefault();
                        	});
                        	$('#cellwiseGraphForm').submit(function(event){
                        	    data = {};
                        	    data['content'] = transformFormData(this, 'cellwiseScatter');
                        	    context.setURL("content="+data['content']);
                        	    console.log(data);
                        	    $.ajax({
                                                                    url: "/decode",
                                                                    data: data,
                                                                    dataType: "json",
                                                                    type:"POST",
                                                                    beforeSend: function () {
                                                                                            			ui.showSpinner('spin');
                                                                                            			$('#cellwiseGraphFormSubmit').attr("disabled", "disabled");
                                                                                            			$('#cellwiseGraphFormReset').attr("disabled", "disabled");

                                                                                            		},
                                                                                            		// success identifies the function to invoke when the server response
                                                                                            		// has been received
                                                                                            		success: function(data) {
                                                                                            		/*queryString = this.url;
                                                                                                    context.setURL(queryString);*/

                                                                                            		events.cellwiseScatterSuccess(data)},

                                                                                            		error: function () {
                                                                                            			ui.formError('spin');
                                                                                            		}
                                                                    }

                                                                )
                        	    /*console.log($('#mapValuesCellwiseProp1 option:selected').val());
                        	    var filterString = processArray($('#mapValuesCellwiseProp1 option:selected').val());

                        	    console.log(filterString);
                                */
                                event.preventDefault();
                        	});
                        	$('#genewiseDistributionForm').submit(function(event){
                                                    	    data = {};
                                                    	    data['content'] = transformFormData(this, 'genewiseDistribution');
                                                    	    context.setURL("content="+data['content']);
                                                    	    console.log(data);
                                                    	    $.ajax({
                                                                                                url: "/decode",
                                                                                                data: data,
                                                                                                dataType: "json",
                                                                                                type:"POST",
                                                                                                beforeSend: function () {
                                                                                                                        			ui.showSpinner('spin');
                                                                                                                        			$('#genewiseDistributionFormSubmit').attr("disabled", "disabled");
                                                                                                                        			$('#genewiseDistributionhFormReset').attr("disabled", "disabled");

                                                                                                                        		},
                                                                                                                        		// success identifies the function to invoke when the server response
                                                                                                                        		// has been received
                                                                                                                        		success: function(data) {
                                                                                                                        		/*queryString = this.url;
                                                                                                                                context.setURL(queryString);*/

                                                                                                                        		events.genewiseDistributionSuccess(data)},

                                                                                                                        		error: function () {
                                                                                                                        			ui.formError('spin');
                                                                                                                        		}
                                                                                                }

                                                                                            )
                                                    	    /*console.log($('#mapValuesCellwiseProp1 option:selected').val());
                                                    	    var filterString = processArray($('#mapValuesCellwiseProp1 option:selected').val());

                                                    	    console.log(filterString);
                                                            */
                                                            event.preventDefault();
                                                    	});
                        	/*$('#cellwiseGraphForm').ajaxForm({
                        		// dataType identifies the expected content type of the server response
                        		dataType: 'json',
                        		beforeSend: function () {
                        			ui.showSpinner('spin');
                        			$('#cellwiseGraphFormSubmit').attr("disabled", "disabled");
                        			$('#cellwiseGraphFormReset').attr("disabled", "disabled");
                        			queryString = $('#cellwiseGraphForm').formSerialize();
                        			context.setURL(queryString);
                        		},
                        		// success identifies the function to invoke when the server response
                        		// has been received
                        		success: function(data) {events.cellwiseScatterSuccess(data)},
                        		error: function () {
                        			ui.formError('spin');
                        		}
                        	});*/
                        	$('#cellwiseDistributionForm').submit(function(event){
                                                    	    data = {};
                                                    	    data['content'] = transformFormData(this, 'cellwiseDistribution');
                                                    	    context.setURL("content="+data['content']);
                                                    	    console.log(data);
                                                    	    $.ajax({
                                                                                                url: "/decode",
                                                                                                data: data,
                                                                                                dataType: "json",
                                                                                                type:"POST",
                                                                                                beforeSend: function () {
                                                                                                                        			ui.showSpinner('spin');
                                                                                                                        			$('#cellwiseDistributionFormSubmit').attr("disabled", "disabled");
                                                                                                                        			$('#cellwiseDistributionFormReset').attr("disabled", "disabled");

                                                                                                                        		},
                                                                                                                        		// success identifies the function to invoke when the server response
                                                                                                                        		// has been received
                                                                                                                        		success: function(data) {
                                                                                                                        		/*queryString = this.url;
                                                                                                                                context.setURL(queryString);*/

                                                                                                                        		events.cellwiseDistributionSuccess(data)},

                                                                                                                        		error: function () {
                                                                                                                        			ui.formError('spin');
                                                                                                                        		}
                                                                                                }

                                                                                            )
                                                    	    /*console.log($('#mapValuesCellwiseProp1 option:selected').val());
                                                    	    var filterString = processArray($('#mapValuesCellwiseProp1 option:selected').val());

                                                    	    console.log(filterString);
                                                            */
                                                            event.preventDefault();
                                                    	});
                        	$('#log').bind('change', function () {
                            		if ($(this).is(':checked')) {
                            		    context.log();
                            			ui.showGraph();
                            		} else {
                            		    context.log();
                                        ui.showGraph();
                            		}
                            	});

                        	$('#transpose').bind('change', function () {
                        		if ($(this).is(':checked')) {
                        		    context.transpose();
                                    ui.showGraph();

                        		} else {
                        			context.transpose();
                                    ui.showGraph();
                        		}
                        	});
                        	$('#include').bind('change', function () {
                                                                 		if ($(this).is(':checked')) {
                                                                 		    context.setInclude(true);
                                                                 		} else {
                                                                 		    context.setInclude(false);
                                                                 		}
                                                                 	});
                        	$('#mtFilt').bind('change', function () {
                        		if ($(this).is(':checked')) {
                        		    context.putback('Microtubule hit');
                        		    ui.showGraph();
                        		} else {
                        		    //TODO assign with some #mtFilt field
                        		    context.filter('Microtubule hit');
                        		    ui.showGraph();
                        		}
                        	});
                        	$('#ccFilt').bind('change', function () {
                                                    		if ($(this).is(':checked')) {
                                                    		    context.putback('Cell cycle hit');
                                                    		    ui.showGraph();
                                                    		} else {
                                                    		    //TODO assign with some #mtFilt field
                                                    		    context.filter('Cell cycle hit');
                                                    		    ui.showGraph();
                                                    		}
                                                    	});
                            $('#shapeFilt').bind('change', function () {
                                                                                		if ($(this).is(':checked')) {
                                                                                		    context.putback('Shape hit');
                                                                                		    ui.showGraph();
                                                                                		} else {
                                                                                		    //TODO assign with some #mtFilt field
                                                                                		    context.filter('Shape hit');
                                                                                		    ui.showGraph();
                                                                                		}
                                                                                	});
                            $('#wtFilt').bind('change', function () {
                                                                                		if ($(this).is(':checked')) {
                                                                                		    context.putback('Wild type');
                                                                                		    ui.showGraph();
                                                                                		} else {
                                                                                		    //TODO assign with some #mtFilt field
                                                                                		    context.filter('Wild type');
                                                                                		    ui.showGraph();
                                                                                		}
                                                                                	});
                            $('#controlFilt').bind('change', function () {
                                                                                                            		if ($(this).is(':checked')) {
                                                                                                            		    context.putback('Control');
                                                                                                            		    ui.showGraph();
                                                                                                            		} else {
                                                                                                            		    //TODO assign with some #mtFilt field
                                                                                                            		    context.filter('Control');
                                                                                                            		    ui.showGraph();
                                                                                                            		}
                                                                                                            	});
                        	$('#regression').bind('change', function () {
                        		if ($(this).is(':checked')) {
                        			plots.addRegressionLine();
                        		} else {
                        			plots.removeRegressionLine();
                        		}
                        	});
                        	$('#regressionType').bind('change', function () {
                        		if ($('#regression').is(':checked')) {
                        			plots.removeRegressionLine();
                        			plots.addRegressionLine();
                        		}
                        	});
                        	$('#selection').bind('change', function () {
                        		if ($(this).is(':checked')) {
                        			plots.addSelectionTool();
                        		} else {
                        			plots.removeSelectionTool();
                        			$('#selectionLink').attr('disabled', true);
                        		}
                        	});

                        	$('#csvDownload').on('click', function (event) {
                        		util.downloadCSV.call(this, 'data.csv');
                        	});
                        	$('#share').on('click', function (event) {
                        	    var string = window.location.href + "share?type=" + context.getType() + "&" + context.getURL();
                                $('#shareTextArea').val(string);
                                $('#shareTextArea').attr('readonly', true);
                                $('#shareModal').modal('show');
                                $('textArea#shareTextArea').select();
                            });
                            $('#embed').on('click', function (event) {
                                                    	    var string = '<iframe src=' + window.location.href + 'embed?type=' + context.getType() + "&" + context.getURL() + ' width="800" height="500"/>';
                                                            $('#embedTextArea').val(string);
                                                            $('#embedTextArea').attr('readonly', true);
                                                            $('#embedModal').modal('show');
                                                            $('textArea#embedTextArea').select();
                                                        });
                        	$('#infoLink').on('click', function (event) {
                        	    var currentData = context.getData();
                            		if ($('#info').attr("hidden") == "hidden") {
                            			$('#num').text(currentData.length);
                            			arr = [];
                            			var select = $('#selection').is(':checked');

                            			//console.log(select)
                            			if (!select) {
                            				currentData.forEach(function (d) {
                            					arr.push(d.x)
                            				});
                            			} else {
                            				selected.forEach(function (d) {
                            					arr.push(d.x)
                            				});
                            			}
                            			var row = util.createInfoRow(context.getProperty(0), arr);
                            			var type = context.getType();
                            			if (type == 'genewiseScatter' || type == 'cellwiseScatter') {
                            				arr = [];
                            				if (!select) {
                            					currentData.forEach(function (d) {
                            						arr.push(d.y)
                            					});
                            				} else {
                            					selected.forEach(function (d) {
                            						arr.push(d.y)
                            					});
                            				}
                            				row += util.createInfoRow(context.getProperty(1), arr);
                            			}
                            			$('#infoBody').html(row);
                            			/*$('#average').text(numbers.statistic.mean(arr));
                                                    $('#stdev').text(numbers.statistic.standardDev(arr));
                                                    $('#median').text(numbers.statistic.median(arr));
                                                    $('#min').text(Math.min.apply(Math,arr));
                                                    $('#max').text(Math.max.apply(Math,arr));*/
                            			$('#info').removeAttr("hidden");
                            		} else {
                            			$('#info').attr("hidden", "hidden");
                            		}
                            	});
                            	$('#svgDownload').on('click', function (event) {
                            		util.downloadSVG.call(this, 'chart.svg');
                            	});
                            	$('#showGeneList').on('click', function (event) {
                            		$('#geneListModal').modal('show');
                            	});
                            	$('#showGeneListDist').on('click', function (event) {
                                                            		$('#geneListDistModal').modal('show');
                                                            	});
                            	$('#submitGeneList').on('click', function (event) {

                            		var lines = $('#geneListTextArea').val().split('\n');
                            		var actual = [],
                            			wrong = [];
                            		$("#geneList").multiselect("uncheckAll");
                            		for (var i = 0; i < lines.length; i++) {
                            			console.log(lines[i]);
                            			if (geneNames.indexOf(lines[i]) > -1) {
                            				actual.push(lines[i]);
                            			} else {
                            				wrong.push(lines[i]);
                            			}
                            			//code here using lines[i] which will give you each line
                            		}
                            		console.log(actual);
                            		console.log(wrong);
                            		//console.log($("#geneList").multiselect("widget"));
                            		$("#geneList").multiselect("widget").find(":checkbox").each(function () {
                            			if (actual.indexOf($(this).val()) > -1) {
                            				this.click();
                            			}
                            			//console.log();
                            		});
                            		//$('#submitGeneList').remove();
                            		$('#geneListModal').modal('hide');
                            		if (wrong.length !== 0) {
                            			var text = "The system can not recognize the following genes: ";
                            			wrong.forEach(function (d) {
                            				text = text.concat(d);
                            				text = text.concat(", ");
                            			});
                            			text = text.substring(0, text.length - 2);
                            			console.log(text);
                            			alert(text);
                            		}
                            		if (actual.length === 0) {
                            			$("#geneList").multiselect("checkAll");
                            		}
                            		/*if (wrong.length===0) {
                                                    $('#geneListModal .modal-body').html("The genes have been succesfully loaded.");
                                                  }
                                                  else {
                                                    $('#geneListModal .modal-body').html("The system can not recognize the following genes: ".concat(wrong, function(d) {return d.concat(" ")}));
                                                  }*/
                            		/*$('#geneListModal').modal('hide');*/
                            	});
                            	$('#submitGeneListDist').on('click', function (event) {

                                                            		var lines = $('#geneListDistTextArea').val().split('\n');
                                                            		var actual = [],
                                                            			wrong = [];
                                                            		$("#geneListDist").multiselect("uncheckAll");
                                                            		for (var i = 0; i < lines.length; i++) {
                                                            			console.log(lines[i]);
                                                            			if (geneNames.indexOf(lines[i]) > -1) {
                                                            				actual.push(lines[i]);
                                                            			} else {
                                                            				wrong.push(lines[i]);
                                                            			}
                                                            			//code here using lines[i] which will give you each line
                                                            		}
                                                            		console.log(actual);
                                                            		console.log(wrong);
                                                            		//console.log($("#geneList").multiselect("widget"));
                                                            		$("#geneListDist").multiselect("widget").find(":checkbox").each(function () {
                                                            			if (actual.indexOf($(this).val()) > -1) {
                                                            				this.click();
                                                            			}
                                                            			//console.log();
                                                            		});
                                                            		//$('#submitGeneList').remove();
                                                            		$('#geneListDistModal').modal('hide');
                                                            		if (wrong.length !== 0) {
                                                            			var text = "The system can not recognize the following genes: ";
                                                            			wrong.forEach(function (d) {
                                                            				text = text.concat(d);
                                                            				text = text.concat(", ");
                                                            			});
                                                            			text = text.substring(0, text.length - 2);
                                                            			console.log(text);
                                                            			alert(text);
                                                            		}
                                                            		if (actual.length === 0) {
                                                            			$("#geneListDist").multiselect("checkAll");
                                                            		}
                                                            		/*if (wrong.length===0) {
                                                                                    $('#geneListModal .modal-body').html("The genes have been succesfully loaded.");
                                                                                  }
                                                                                  else {
                                                                                    $('#geneListModal .modal-body').html("The system can not recognize the following genes: ".concat(wrong, function(d) {return d.concat(" ")}));
                                                                                  }*/
                                                            		/*$('#geneListModal').modal('hide');*/
                                                            	});
                            	$('#shareLink').on('click', function (event) {
                            		//$("#queryLink").attr("data-text", window.location.href + "query?type=" + type + "&" + queryString);
                            		//$("#queryLink").attr("data-text", encodeURIComponent(window.location.href) + "query?type=" + encodeURIComponent(type) + "&" + encodeURIComponent(queryString));
                            		//$('.clippy').clippy({clippy_path:'js/clippy.swf'});
                            		var jsData = "<script language='text/javascript'>$('#copy-button').zclip({path:'js/ZeroClipboard.swf',beforeCopy:function() {console.log('valami');}, copy:function(){return window.location.href + 'query?type=' + type + '&' + queryString;}});</script>";
                            		$("#queryLink").html(jsData);
                            		$("#shareModal").modal('show');
                            	});
                            	$("#addGridMenuOpts li a").click(function () {
                            		//$('#addGridMenu').change(function () {
                            		var currentChart = $('#graph').html();
                            		console.log(currentChart);
                            		$('#chart').remove();
                            		var gridElement = function (name) {
                            			return "<div id='" + name + "' class='col-md-6 column' />"
                            		};
                            		var value = parseInt($(this).text());
                            		console.log(value);
                            		var str = '';
                            		for (i = 0; i < value; ++i) {
                            			var tab = gridElement('chart' + i);
                            			console.log(tab);
                            			str += tab;
                            			if (i % 1 == 0) {
                            				str += "<br/>";
                            			}
                            		}
                            		$('#graph').html(str);
                            		$('#chart0').html(currentChart);
                            		$('#chart1').html(currentChart);
                            		$('#chart2').html(currentChart);
                            		$('#chart3').html(currentChart);
                            		chart = $("#chart");
                            		var targetWidth = chart.parent().width();
                            		chart.attr("width", targetWidth);
                            		//svg.attr("width", $('#chart0').width());
                            	});
                            	$('.compare-popup').magnificPopup({
                            		type: 'inline',
                            		preloader: false,
                            		callbacks: {
                            			open: function () {
                            				console.log(compareCharts);
                            				for (j in compareCharts) {
                            					i = compareCharts[j];
                            					//console.log(i);
                            					id = chartBase + i;
                            					//console.log(id);
                            					target = graphBase + i;
                            					if ($(target).html()) {
                            						continue;
                            					}
                            					//console.log(target);
                            					obj = compareMap[i];
                            					//console.log(obj.prop1);
                            					$(target).removeAttr("hidden");
                            					ui.showGraph(obj.data, obj.prop1, obj.prop2, false, null, null, target, id, obj.plottype, obj.geneName);
                            					$(target).append('<a href="#" class="removeChart" style="padding:5px">Remove</a> <input class="selectChart" type="checkbox" style="padding:3px"/> <label>Select chart</label>');
                            				}
                            				/*showGraph(currentData, prop1, prop2, false, null, null, '#graph0');
                                            showGraph(currentData, prop1, prop2, false, null, null, '#graph1');
                                            showGraph(currentData, prop1, prop2, false, null, null, '#graph2');
                                            showGraph(currentData, prop1, prop2, false, null, null, '#graph3');*/
                            			}
                            		}
                            	});
                            	$('#comparison').on('click', '.removeChart', function (event) {
                            		ui.removeChart(this, true, true);
                            	});
                            	$('#sendToCompare').on('click', function (event) {
                            		if (nextChart == compareMax) {
                            			$('#sheetFullModal').modal('show');
                            		} else {
                            			compareMap[nextChart] = {
                            				data: context.getData(),
                            				prop1: context.getProperty(0),
                            				prop2: context.getProperty(1),
                            				plottype: context.getType(),
                            				geneName: context.getGenename()
                            			};
                            			console.log(compareMap[nextChart].prop1);
                            			console.log(compareMap[nextChart].prop2);
                            			compareCharts.push(nextChart);
                            			console.log("compareCharts:" + compareCharts);
                            			console.log("compareCharts:" + compareMap);
                            			while (nextChart in compareCharts) nextChart++;
                            			console.log(nextChart);
                            		}
                            		$('#comparisonAlert').show();
                            		$('#comparisonAlert').fadeOut(5000);
                            	});
                            	$('#mergeChartsLink').on('click', function (event) {
                            		ids = [];
                            		$('input:checkbox.selectChart').each(function () {
                            			console.log($(this).parent());
                            			count++;
                            			ids.push(parent.attr("id").substring(5, 6));
                            		});
                            		lastType = null;
                            		lastProp1 = null;
                            		lastProp2 = null;
                            		data = {};
                            		for (id in ids) {
                            			i = compareCharts[id];
                            			obj = compareMap[i];
                            			if (lastType == null || lastType == obj.type) {
                            				data = $.extend(data, obj.data);
                            				ui.removeChart('graph' + id, false);
                            			} else {
                            				console.log(lastType + " vs " + obj.type);
                            			}
                            		}
                            	});
                            	$('#synchronizeChartsLink').on('click', function (event) {
                            		ids = [];
                            		count = 0;
                            		$('input:checkbox.selectChart').each(function () {
                            			if ($(this).is(':checked')) {
                            				console.log($(this).parent());
                            				count++;
                            				ids.push($(this).parent().attr("id").substring(5, 6));
                            			}
                            		});
                            		lastType = null;
                            		lastProp1 = null;
                            		lastProp2 = null;
                            		data = {};
                            		maxX = -Infinity, minX = Infinity;
                            		maxY = -Infinity, minY = Infinity;
                            		error = false;
                            		console.log(ids);
                            		for (id in ids) {
                            			id = parseInt(id);
                            			console.log("compareCharts:" + compareCharts);
                            			console.log("compareCharts:" + compareMap);
                            			console.log(id);
                            			i = compareCharts[id];
                            			console.log(compareCharts);
                            			obj = compareMap[i];
                            			console.log(obj);
                            			if (lastType == null || lastType == obj.type) {
                            				extentX = d3.extent(obj.data, function (d) {
                            					return d.x
                            				});
                            				extentY = d3.extent(obj.data, function (d) {
                            					return d.y
                            				});
                            				//console.log(extentX);
                            				//console.log(extentY);
                            				if (minX > extentX[0]) {
                            					minX = extentX[0];
                            				}
                            				if (maxX < extentX[1]) {
                            					maxX = extentX[1];
                            				}
                            				if (minY > extentY[0]) {
                            					minY = extentY[0];
                            				}
                            				if (maxY < extentY[1]) {
                            					maxY = extentY[1];
                            				}
                            				//removeChart('graph' + id, false);
                            			} else {
                            				console.log(lastType + " vs " + obj.type);
                            				error = true;
                            				break;
                            			}
                            		}
                            		if (!error) {
                            			xRange = [minX, maxX];
                            			yRange = [minY, maxY];
                            			xLogRange = [Math.log(minX), Math.log(maxX)];
                            			yLogRange = [Math.log(minY), Math.log(maxY)];
                            		}
                            		for (id in ids) {
                            			id = parseInt(id);
                            			console.log("compareCharts:" + compareCharts);
                            			console.log("compareCharts:" + compareMap);
                            			console.log(id);
                            			i = compareCharts[id];
                            			console.log(compareCharts);
                            			obj = compareMap[i];
                            			console.log(obj);
                            			var target = graphBase + id;
                            			ui.removeChart(target, false, false);
                            			ui.showGraph(obj.data, obj.prop1, obj.prop2, false, xRange, yRange, target, chartBase + id, obj.plottype);
                            			$(target).append('<a href="#" class="removeChart" style="padding:5px">Remove</a> <input class="selectChart" type="checkbox" style="padding:3px"/> <label>Select chart</label>');
                            		}
                            	});
                            	$("#geneFiltLink").on("click", function() {
                            	    plots.filterGenes()
                            	});
                            	$("#resetFiltLink").on("click", function () {
                            		plots.resetGeneFilter();
                            	});
                    });


                return {
                    isKeyDown: function() {
                        return keyisdown;
                    },

					sendAjaxRequest: function(data) {
						context.setURL("content=" + data['content']);
						console.log(data);
						$.ajax({
							url: "/decode",
							data: data,
							dataType: "json",
							type: "GET",
							beforeSend: function() {
								ui.showSpinner('spin');
								//                                                                                                                                            			$('#' + prefix + 'FormSubmit').attr("disabled", "disabled");
								//                                                                                                                                            			$('#' + prefix + 'FormReset').attr("disabled", "disabled");

							},
							// success identifies the function to invoke when the server response
							// has been received
							success: function(data) {
								switch (type) {
									case 'genewiseScatter':
										events.genewiseScatterSuccess(data);
										break;
									case 'cellwiseScatter':
										events.cellwiseScatterSuccess(data);
										break;
									case 'genewiseHistogram':
									case 'genewiseKDE':
									case 'genewiseMultihistogram':
										events.genewiseDistributionSuccess(data);
										break;

									case 'cellwiseHistogram':
									case 'cellwiseKDE':
										events.cellwiseDistributionSuccess(data);
										break;
								}
							},
							error: function() {
								ui.formError('spin');
							}
						})

						event.preventDefault();
					}
}




});