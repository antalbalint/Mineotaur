define(['mineotaur/util','mineotaur/plots','mineotaur/context','spin', 'underscore', 'jquery'], function (util, plots, context, Spinner, _, $, ls) {
    //Do setup work here

    var spinner;
    var tools = ['#regression', '#log', '#selection', '#transpose'];


    return {
        /*spinner:  new Spinner(),*/

        resetTools: function() {
            tools.forEach(function(tool) {
                $(tool).removeAttr("checked");
            });
        },

    /*function resetTools() {
        $('#regression').removeAttr("checked");
        $('#log').removeAttr("checked");
        $('#transpose').removeAttr("checked");
        $('#selection').removeAttr("checked");
    }*/

        resetFilters: function(checkboxes, filters) {
            for (i = 0; i < checkboxes.length; ++i) {
                console.log(checkboxes[i]);
                if ($(checkboxes[i]).is(":checked")) {

                        $(filters[i]).prop('disabled', false);
                        $(filters[i]).prop("checked", true);
                    }
                    else {
                        $(filters[i]).prop('disabled', true);
                        $(filters[i]).prop("checked", false);
                    }
            }
        },

        showRandomGraph: function(numberOfNodes) {
            var data = [];
            for (i = 0; i < numberOfNodes; ++i) {
                //console.log(i);
                var node = {};
                node.x = Math.random();
                node.y = Math.random();
                node.logX = Math.log(node.x);
                node.logY = Math.log(node.y);
                node.labels = ['Wild type'];
                data.push(node);
            }
            labels = util.getLabels(data);
            data = JSON.parse(JSON.stringify(data));

            $('#graph').html(plots.drawScatterPlot(data, '#graph', 'chart', 'cellwiseScatter','x', 'y', 'random'));
            spinner.stop();
        },


        showGraph: function(data, prop1, prop2, overwrite, xRange, yRange, target, id, type, geneName) {
            var toComparisonSheet = true;
            if (typeof(data) === 'undefined') {
                var data = context.getData();
                console.log(context.getProperties());
                            var prop1 = context.getProperty(0);
                            console.log(prop1);
                            var prop2 = context.getProperty(1);
                            console.log(prop2);
                            var range = context.getRange();
                            var xRange = range.x;
                            var yRange = range.y;
                            var target = context.getTarget();
                            var type = context.getType();
                            var id = context.getId();
                            var overwrite = true;
                            var geneName = context.getGenename();
                            toComparisonSheet = false;
            }
            else {
                var xRange = null;
                var yRange = null;
            }

            var idJQ = '#'+id;
            if (data.length==0)  {
                $('#noDataModal').modal('show');
                return;
            }
            if (overwrite) {
                $(idJQ).remove();
            }
            /*if (typeof(target) == 'undefined') {
                target = '#graph';
            }
            if (typeof(id) == 'undefined') {
                    id = 'chart';
            }
            if (typeof(plottype) != 'undefined') {
                        type = plottype;
            }*/
            data = util.shuffle(data);
            labels = util.getLabels(data);
            if (type == 'groupwiseScatterplot' || type == 'cellwiseScatter') {
                /*var adjust = xRange === null && yRange === null;
                console.log(adjust);*/
                var content;
                                if (toComparisonSheet) {
                                    content = plots.drawScatterPlot(data, target, id, type, prop1, prop2, geneName);
                                }
                                else {
                                    content = plots.drawScatterPlot();
                               }
                                    $(target).html(content);
                //$(target).html();
                if (!overwrite) {
                    if ($('#regression').is(':checked')) {
                                    plots.addRegressionLine();
                                }
                            if ($('#selection').is(':checked')) {
                                    plots.addSelectionTool();
                                }
                }
                plots.addLegend(idJQ);
            }
            else if (type === 'genewiseKDE' || type === 'cellwiseKDE') {
                if (type === 'cellwiseKDE') {
                    $('.filter').prop('disabled', true);
                }
                var content;
                if (toComparisonSheet) {
                    content = plots.drawKDEPlot(data, target, prop1, geneName);
                }
                else {
                    content = plots.drawKDEPlot();
               }
               $(target).html(content);
                if (type === 'genewiseKDE') {
                                    plots.addLegend(idJQ);
                                }

            }

            else if (type == 'genewiseMultiHistogram' || type == 'genewiseHistogram' || type == 'cellwiseHistogram') {
                if (type == 'cellwiseHistogram') {
                   $('.filter').prop('disabled', true);
                }
                  var multi = type === 'genewiseMultiHistogram';
                  var content;
                  if (toComparisonSheet) {
                    console.log()
                    content = plots.drawHistogram(multi, data, prop1, target, id, geneName);
                  }
                  else {
                    content = plots.drawHistogram(multi);
                  }
                $(target).html(content);
                if (multi) {
                    plots.addLegend(idJQ);
                }

            }
            /*else if (type == 'genewiseHistogram') {
                $(target).html(plots.drawHistogram(false));

            }
            else if (type == 'cellwiseHistogram') {
                $('.filter').prop('disabled', true);
                $(target).html(plots.drawHistogram(false));
             }*/
            History.pushState(/*graph:$(target).html()*/context.export(),'',History.getHash());
            console.log(History.savedStates.length);
            if (target == '#graph') {
                $('html, body').animate({
                        scrollTop: $(target).offset().top
                      });
            }
              $('#infoBody').empty();
              $('#info').attr("hidden","hidden");
        },
        convertCheckboxToTimepoints: function(checkbox) {
            var time = '';
            $('input[name=' + checkbox + ']').each(function(d) {if ($(this).is(':checked')) time = time + $(this).next('label').text() + ', ';});
            time = time.substring(0,time.length-2);
            return time;
        },
        convertArrayToTimepoints: function(checkbox, array) {
                    var time = '';
                    $('input[name=' + checkbox + ']').each(function(d) {if ($(this).val() in array) time = time + $(this).next('label').text() + ', ';});
                    time = time.substring(0,time.length-2);
                    return time;
                },
        showSpinner: function(element) {
        	var target = document.getElementById(element);
        	spinner = new Spinner().spin(target);
        	return spinner;
        },
        hideSpinner: function() {

        	spinner.stop();
        },

        formError: function(element) {
        	spinner.stop();
        	$('#noDataModal').modal('show');
        },
        removeChart: function(element, useParent, hide) {
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
        },
        showGallery: function(name, ids) {


        }
    }
});