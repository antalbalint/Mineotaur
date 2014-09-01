define(['underscore'], function (_) {

    var map = {};

    map['noDataModal'] = '#noDataModal';
    map['mainGraph'] = '#graph';
    map['tools'] = ['#regression', '#log', '#transpose', '#selection'];

    var props=[];
    var legends=[];
    var formData={};
    var range={};
    var data;
    var filtered=[];
    var type;
    var target;
    var id;
    var currentItem;
    var selected=[];
    var genename;
    var suffix = {  'genewiseScatter': '',
                            'cellwiseScatter': 'Cellwise',
                            'genewiseHistogram': 'GWDist',
                            'genewiseMultiHistogram': 'GWDist',
                            'genewiseKDE': 'GWDist',
                            'cellwiseHistogram': 'CWDist',
                            'cellwiseKDE': 'CWDist',
                            };
    var URL;
    var include;
    var keyisdown = false;
    var labels;


    function doLog(input, adjustRange) {
        input.forEach(function(d) {
                        tmp = d.x;
                        d.x = d.logX;
                        d.logX = tmp;
                        tmp = d.y;
                        d.y = d.logY;
                        d.logY = tmp;

                    });
        if (adjustRange) {
            tmp = range.logX;
                                                range.logX = range.x;
                                                range.x = tmp;
                                tmp = range.logY;
                                                            range.logY = range.y;
                                                            range.y = tmp;
        }

    }

    function doTranspose(input, adjustRange) {
    if (adjustRange) {
        tmp = props[0];
                                props[0] = props[1];
                                props[1] = tmp;
                                tmp = range['x'];
                                range['x'] = range['y'];
                                range['y'] = tmp;
                                tmp = range['logX'];
                                range['logX'] = range['logY'];
                                range['logY'] = tmp;
    }

                input.forEach(function(d) {
                        v = d.x;
                        d.x = d.y;
                        d.y = v;
                    });
    }
    return {
        setLabels: function(_labels) {
            labels = _labels;
        },
        getLabels: function() {
            return labels;
        },
        setURL: function(_URL) {
            URL=_URL;
        },

        getURL: function() {
            return URL;
        },

        setInclude: function(val) {
            include = val;
        },

        getInclude: function() {
                    return include;
                },

       getSuffix: function(type) {
        return suffix[type];
       },

       setProperty: function(id, prop) {
        props[id] = prop;
       },

       getProperty: function(id) {
        return props[id];
       },

       setCurrentItem: function(obj) {
                               currentItem = obj;
                              },

                              getCurrentItem: function() {
                               return currentItem;
                              },

                              setSelected: function(obj) {
                                             selected = obj;
                                            },

                                            getSelected: function() {
                                             return selected;
                                            },

       setProperties: function(_props) {
        props = _props;
       },

       getProperties: function() {
        return props;
       },

       setLegends: function(_legends) {
               legends = _legends;
              },

              getLegends: function() {
               return legends;
              },


       setRange: function(xRange, yRange, logXRange, logYRange) {
        range.x = xRange;
        range.y = yRange;
        range.logX = logXRange;
        range.logY = logYRange;
       },
       setRangeBulk: function(_range) {
               range = _range;
              },
       getRange: function() {
        return range;
       },

       getData: function() {
        return data;
       },

       setData: function(_data) {
        data=_data;
       },

       getGenename: function() {
               return genename;
              },

              setGenename: function(_genename) {
               genename=_genename;
              },

       getType: function() {
               return type;
              },

              setType: function(_type) {
               type=_type;
              },
       getTarget: function() {
                      return target;
                     },

                     setTarget: function(_target) {
                      target=_target;
                     },

        getId: function() {
                              return id;
                             },

                             setId: function(_id) {
                              id = _id;
                             },
       transpose: function() {
            doTranspose(data, true);
            doTranspose(filtered, false);
       },

       log: function () {
            doLog(data, true);
            doLog(filtered, false);
       },

       filter: function(label) {
        var inner = [];
        data.forEach(function(d) {
            if (d.labels.indexOf(label) > -1) {
                filtered.push(d);
            }
            else {
                inner.push(d);
            }
        });
        data = inner;
       },

       filterDec: function(label) {
         var inner = [];
         data.forEach(function(d) {
            toBeFiltered = false;
            if (d.labels.indexOf(label) > -1) {
                if (include === false) {
                    if (d.labels.length===1) {
                        toBeFiltered = true;
                    }
                }
                else {
                    toBeFiltered = true;
                }
            }
            if (toBeFiltered) {
                filtered.push(d);
            }
            else {
                inner.push(d);
            }
         });
         data = inner;
       },

        putback: function(label) {
            var inner = [];
                    filtered.forEach(function(d) {
                        if (d.labels.indexOf(label) > -1) {
                            data.push(d);
                        }
                        else {
                            inner.push(d);
                        }
                    });
                    filtered = inner;
        },

       setFormData: function(prefix) {
        formData = {};
        formData['submit'] = prefix.concat('FormSubmit');
        formData['reset'] = prefix.concat('FormReset');
       },

       getFormData: function() {
        return formData;
       },
       isKeyDown: function() {
                               return keyisdown;
                           },
       setKeyDown: function(_bl) {
        isKeyDown = _bl;
       },
       export: function() {
        var obj = {data: data, props: props, type: type, target: target, id: id, range: range, genename: genename};
        return obj;
       },
       import: function(obj) {
        data = obj.data;
        props = obj.props;
        type = obj.type;
        target = obj.target;
        id = obj.id;
        range = obj.range;
        genename = obj.genename;
       }
    }
});