define(['mineotaur/ui', 'mineotaur/util', 'mineotaur/context', 'd3', 'jquery', 'jquery.form', 'jquery-ui', 'jquery.blockUI'], function (ui, util, context, d3, $) {

    var range = new Object();
    var prop1, prop2, prop1Val, prop2Val, values, type;
        var currentData;
    var checkboxes = ['#wt', '#control', '#shape', '#mt', '#cc'];
    var filters = ['#wtFilt', '#controlFilt', '#shapeFilt', '#mtFilt', '#ccFilt'];
    var checkboxesGWDist = ['#wtGWDist', '#controlGWDist', '#shapeGWDist', '#mtGWDist', '#ccGWDist'];
    var mapValues = 'mapValues';
    var propName = '#prop';

    function handleResponse(data, type, target, id, prop1Pm, prop2Pm) {
        ui.hideSpinner();
        $.unblockUI();
        util.shuffle(data);
        ui.resetTools();
        currentData = JSON.parse(JSON.stringify(data));
        console.log(currentData)
        context.setData(currentData);
        xRange = d3.extent(data, function (d) {
                return d.x;
        });
        yRange = d3.extent(data, function (d) {
                return d.y;
        });
        xLogRange = d3.extent(data, function (d) {
                return d.logX;
        });
        yLogRange = d3.extent(data, function (d) {
                return d.logY;
        });


                	//range = {'x': xRange, 'y': yRange, 'logX': xLogRange, 'logY': yLogRange};
        context.setRange(xRange, yRange, xLogRange, yLogRange);
        var suffix = context.getSuffix(type);
        var mv = mapValues + suffix;

        if (type === 'groupwiseScatterplot' || type === 'cellwiseScatter') {
            var timePoints1 = ' (' + ui.convertCheckboxToTimepoints(mv + 'Prop1') + ')';
            console.log(mv + 'Prop1');
            console.log(timePoints1);
            var timePoints2 = ' (' + ui.convertCheckboxToTimepoints(mv + 'Prop2') + ')';
            if (typeof(prop1Pm) === 'undefined' || typeof(prop1Pm) === 'undefined') {
                //var p1 = suffix + propName+'1', p2 = suffix + propName+'2';
                if (type == cellwiseScatter) {
                    p1 = '#cellwiseProp1';
                    p2 = '#cellwiseProp2';
                }
                else {
                    p1 = '#prop1';
                    p2 = '#prop2';
                }
                console.log(p1);
                prop1 = $(p1 + ' option:selected').text() + timePoints1;
                console.log($(p1 + ' option:selected').text());
                prop1Val = $(p1).val();
                console.log(prop1Val);
                prop2 = $(p2 + ' option:selected').text() + timePoints2;
                prop2Val = $(p2).val()
            }
            else {
                prop1 = prop1Val = prop1Pm;
                prop2 = prop2Val = prop2Pm;
            }
            console.log(prop1);
            console.log(prop2);
            context.setProperties([prop1, prop2]);
            context.setLegends([prop1Val, prop2Val]);
            if (type == 'groupwiseScatterplot') {
                var values = $("#geneList").val();
                //console.log(values);
                $("#geneList").find(":checkbox").each(function (f) {

                    //console.log(f);
                    if (values.indexOf(f.val()) === -1) {
                        $(f).prop('disabled', true);
                    }
                    else {
                        $(f).prop('disabled', false);
                    }
                });

            }
        }
        else {
            var timePoint =' (' + ui.convertCheckboxToTimepoints(mv) + ')';
            if (typeof(prop1Pm) === 'undefined') {
                var p = propName+suffix;
                prop1 = $(p + ' option:selected').text() + timePoint;
                prop1Val = $(p).val();
            }
            else {
                prop1 = prop1Val = prop1Pm;
            }
            context.setProperty(0, prop1);
            context.setLegends([prop1Val]);
            if (type == 'genewiseHistogram' || type == 'genewiseMultihistogram' || type == 'genewiseKDE') {
                            var values = $("#geneDistList").val();
                            //console.log(values);
                            $("#geneDistList").find(":checkbox").each(function (f) {

                                //console.log(f);
                                if (values.indexOf(f.val()) === -1) {
                                    $(f).prop('disabled', true);
                                }
                                else {
                                    $(f).prop('disabled', false);
                                }
                            });

                        }
        }
        context.setType(type);
        context.setTarget(target);
        context.setId(id);
    }
    return {

        /*getContext: function() {
            return context;
        },*/
        groupwiseScatterplotSuccess: function(data, prop1Pm, prop2Pm) {
        	handleResponse(data, 'groupwiseScatterplot', '#graph', 'chart', prop1Pm, prop2Pm);
        	$('#groupwiseScatterPlotFormSubmit').removeAttr("disabled");
            $('#groupwiseScatterPlotFormReset').removeAttr("disabled");
            $('#transpose').prop('disabled', false);
                    	$('#regression').prop('disabled', false);
                    	$('#selection').prop('disabled', false);
                    	values = $("#geneList").val();
                    	$("#geneFilt").multiselect("widget").find(":checkbox").each(function () {
                    		$(this).removeAttr('selected');
                    		val = $(this).val();
                    		if (values.indexOf(val) < 0) {
                    			$(this).prop("hidden", true);
                    		}
            });
        	$("#geneFilt").multiselect("enable");
                                    filters.forEach(function(f) {
                                        $(f).prop('disabled', false);
                                    });
                                                $('#opacitySlider').show();

                                            	/*$("#geneFilt").prop('disabled', 'disabled');*/
                                            	$('#geneFiltLink').show();
                                            	$('#resetFiltLink').show();
                                            	$('#tools').find('#showGeneInput').show();
            ui.resetFilters(checkboxes, filters);
            $('#toolNav').removeClass('disabled');
            $('#navigation a[href="#tools"]').tab('show');
            ui.showGraph();
        	/*console.log(data);
        	console.log(d3.version);*/
        	/*ui.hideSpinner();
        	//if (sharedLink) $.unblockUI();
        	if (typeof (prop1Pm) != 'undefined') {
        		prop1 = prop1Pm;
        	}
        	if (typeof (prop2Pm) != 'undefined') {
        		prop2 = prop2Pm;
        	}*/

        	/*util.shuffle(data);
        	ui.resetTools();
        	currentData = JSON.parse(JSON.stringify(data));
        	context.setData(currentData);
        	xRange = d3.extent(data, function (d) {
        		return d.x;
        	});
        	yRange = d3.extent(data, function (d) {
        		return d.y;
        	});
        	xLogRange = d3.extent(data, function (d) {
        		return d.logX;
        	});
        	yLogRange = d3.extent(data, function (d) {
        		return d.logY;
        	});*/

        	//range = {'x': xRange, 'y': yRange, 'logX': xLogRange, 'logY': yLogRange};
        	/*context.setRange(xRange, yRange, xLogRange, yLogRange);
        	console.log(context.getRange());
        	prop1 = $('#prop1 option:selected').text() + ' (' + ui.convertCheckboxToTimepoints('mapValuesProp1') + ')';
        	prop2 = $('#prop2 option:selected').text() + ' (' + ui.convertCheckboxToTimepoints('mapValuesProp2') + ')';
        	context.setProperties([prop1, prop2]);
        	prop1Val = $('#prop1').val();
        	prop2Val = $('#prop2').val();
        	context.setLegends([prop1Val, prop2Val]);*/


        	/*type = 'groupwiseScatterplot';
        	context.setType('groupwiseScatterplot');
        	context.setTarget('#graph');
        	context.setId('chart');*/

        },

        cellwiseScatterSuccess: function(data, prop1Pm, prop2Pm) {
            //ui.showRandomGraph(300000);
            handleResponse(data, 'cellwiseScatter', '#graph', 'chart', prop1Pm, prop2Pm);
        	$('#cellwisegroupwiseScatterPlotFormSubmit').removeAttr("disabled");
        	$('#cellwisegroupwiseScatterPlotFormReset').removeAttr("disabled");
        	context.setGenename($('#geneCWProp1').val());
        	$("#geneFilt").multiselect("disable");
            filters.forEach(function(f) {
                $(f).prop('disabled', true);
            });
                    	/*$("#geneFilt").prop('disabled', 'disabled');*/
                    	$('#geneFiltLink').hide();
                    	$('#resetFiltLink').hide();
                    	$('#tools').find('#showGeneInput').hide();
                    	            $('#opacitySlider').show();

                    	$('#transpose').prop('disabled', false);
                    	$('#regression').prop('disabled', false);
                    	$('#selection').prop('disabled', false);
                    	$('#toolNav').removeClass('disabled');
                    	$('#navigation a[href="#tools"]').tab('show');
            ui.showGraph();

        },

        groupwiseDistributionFormSuccess: function(data, prop1Pm) {
            handleResponse(data, $('#genewisePlotType').val(), '#graph', 'chart', prop1Pm);
        	/*ui.hideSpinner();
        	if (sharedLink) $.unblockUI();*/
        	$('#groupwiseDistributionFormFormSubmit').removeAttr("disabled");
        	$('#groupwiseDistributionFormFormReset').removeAttr("disabled");
        	/*data = util.shuffle(data);
        	ui.resetTools();
        	*//*points = JSON.parse(JSON.stringify(data));*//*
        	currentData = JSON.parse(JSON.stringify(data));*/
        	/*wtCompl = currentData.filter(function(d) { return d.label=='Wild type' });
                    mtCompl = currentData.filter(function(d) { return d.label=='Microtubule hit' });
                    wtValues = getCoordinates(wtCompl);*/
        	/*xRange = d3.extent(data, function (d) {
        		return d.x;
        	});
        	yRange = d3.extent(data, function (d) {
        		return d.y;
        	});
        	xLogRange = d3.extent(data, function (d) {
        		return d.logX;
        	});
        	yLogRange = d3.extent(data, function (d) {
        		return d.logY;
        	});
        	prop1 = $('#propGWDist option:selected').text() + ' (' + convertCheckboxToTimepoints('mapValuesGWDist') + ')';*/
        	//prop2 = $('#prop2').val();
        	$("#geneFilt").multiselect("enable");
                                                filters.forEach(function(f) {
                                                    $(f).prop('disabled', false);
                                                });
                                                        	/*$("#geneFilt").prop('disabled', 'disabled');*/
                                                        	$('#geneFiltLink').show();
                                                        	$('#resetFiltLink').show();
                                                        	$('#tools').find('#showGeneInput').show();
            $('#opacitySlider').hide();
        	$('#transpose').prop('disabled', true);
        	$('#regression').prop('disabled', true);
        	$('#selection').prop('disabled', true);
        	$('#toolNav').removeClass('disabled');
        	$('#navigation a[href="#tools"]').tab('show');
        	//type = $('#genewisePlotType').val();
        	//drawHistogram(data, prop1);
        	ui.resetFilters(checkboxesGWDist, filters);
        	ui.showGraph();
        	//showGraph(data, prop1, prop2, true, xRange, yRange);
        	//drawLineChart(mtCompl, prop1, prop2, xRange, yRange);
        },

        cellwiseDistributionSuccess: function(data, prop1Pm) {
            handleResponse(data, $('#cellwisePlotType').val(), '#graph', 'chart', prop1Pm);
        	//allLabels = getLabels(data);
        	/*hideSpinner();
        	if (sharedLink) $.unblockUI();*/
        	$('#cellwiseDistributionFormSubmit').removeAttr("disabled");
        	$('#cellwiseDistributionFormReset').removeAttr("disabled");
        	/*data = data.sort(predicatBy('label'));
        	resetTools();
        	points = JSON.parse(JSON.stringify(data));
        	currentData = JSON.parse(JSON.stringify(data));
        	*//*wtCompl = currentData.filter(function(d) { return d.label=='Wild type' });
                        mtCompl = currentData.filter(function(d) { return d.label=='Microtubule hit' });
                        wtValues = getCoordinates(wtCompl);*//*
        	xRange = d3.extent(data, function (d) {
        		return d.x;
        	});
        	yRange = d3.extent(data, function (d) {
        		return d.y;
        	});
        	xLogRange = d3.extent(data, function (d) {
        		return d.logX;
        	});
        	yLogRange = d3.extent(data, function (d) {
        		return d.logY;
        	});
        	prop1 = $('#propCWDist option:selected').text() + ' (' + convertCheckboxToTimepoints('mapValuesCWDist') + ')';*/
        	genename = $('#geneCWDist').val();
        	context.setGenename($('#geneCWDist').val());
        	//prop2 = $('#prop2').val();
        	$("#geneFilt").multiselect("disable");
                                    filters.forEach(function(f) {
                                        $(f).prop('disabled', true);
                                    });
                                            	/*$("#geneFilt").prop('disabled', 'disabled');*/
                                            	$('#geneFiltLink').hide();
                                            	$('#resetFiltLink').hide();
                                            	$('#tools').find('#showGeneInput').hide();
        	$('#opacitySlider').hide();

        	$('#transpose').prop('disabled', true);
        	$('#regression').prop('disabled', true);
        	$('#selection').prop('disabled', true);
        	$('#toolNav').removeClass('disabled');
        	$('.filter').prop('disabled', true);
        	$('#navigation a[href="#tools"]').tab('show');
        	ui.showGraph();
        	//type = $('#cellwisePlotType').val();
        	//drawHistogram(data, prop1);
        	//showGraph(data, prop1, prop2, true, xRange, yRange);
        	//drawLineChart(mtCompl, prop1, prop2, xRange, yRange);
        },

    };




});