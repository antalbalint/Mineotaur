define(['underscore', 'pako'], function (_, pako) {

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
    var types = {  'genewiseScatter': 0,

                                             'genewiseHistogram': 1,
                                             'genewiseMultiHistogram': 1,
                                             'genewiseKDE': 1,
                                             'cellwiseScatter': 2,
                                             'cellwiseHistogram': 3,
                                             'cellwiseKDE': 3,
                                             };
    var sizes;
    var bitsForData;
    var parameters;
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

    function parseURL(query) {
        var result = {};
                  query.split("&").forEach(function(part) {
                    var item = part.split("=");
                    console.log(typeof(result[item[0]]));
                    if (typeof(result[item[0]]) === 'undefined') {
                        result[item[0]] = [];
                    }
                    result[item[0]].push(decodeURIComponent(item[1]));
                  });
                  console.log(sizes);
                  return result;
    }

    function getNumberOfBits(array, json, multi) {
        if (array === null || typeof(array) === 'undefined')
            return 0;
        if (multi) {
            if (json) {
                return Object.keys(array).length;
            }
            else {
                return array.length;
            }

        }
        else {
            length = json ? Object.keys(array).length : array.length;
            var i;
            for (i = 0; Math.pow(2,i) < length; ++i);
            return i;
        }
    }


    function encodeURL(query) {
        var tags = parseURL(query);
        var bitsForProps = getNumberOfBits(parameters['props'], true, false);
        console.log(bitsForProps);
        console.log(type);
        typeCode = types[type];
        console.log(typeCode);
        var code = 0 | typeCode;

        code = code << bitsForProps;
        console.log(code.toString(2));
        var prop1, prop2, aggProp1, aggProp2, stage1, stage2, hits, geneList;
        var temp;
        switch (type) {
           case 'genewiseScatter':
                prop1 = tags['prop1'][0];
                prop2 = tags['prop2'][0];
                aggProp1 = tags['aggProp1'][0];
                aggProp2 = tags['aggProp2'][0];
                stage1 = tags['mapValuesProp1'];
                stage2 = tags['mapValuesProp2'];
                hits = tags['hitCheckbox'];
                geneList = tags['geneList'];
                break;
           case 'cellwiseScatter': prop1 = tags['cellwiseProp1'][0]; break;
           case 'genewiseHistogram':
           case 'genewiseMultiHistogram':
           case 'genewiseKDE': prop1 = tags['propGWDist'][0]; break;
           case 'cellwiseHistogram':
           case 'cellwiseKDE': prop1 = tags['propCWDist'][0]; break;
        }
        if (typeof(prop1) !== 'undefined') {
            console.log(prop1);
            console.log(parameters['props']);
            for (i=0; i < parameters['props'].length; ++i) {
                if (prop1 === parameters['props'][i].split("/")[0]) {
                    temp = i;
                    break;
                }
            }
            console.log(temp.toString(2));
            code = code | temp;
            temp = null;
        }
        console.log(code.toString(2));
        var bitsForAgg = getNumberOfBits(parameters['aggValues'], false, false);
        console.log(bitsForAgg);
        code = code << bitsForAgg;
        console.log(code.toString(2));
        if (typeof(aggProp1) !== 'undefined') {
            temp = parameters['aggValues'].indexOf(aggProp1);
            console.log(temp.toString(2));
            code = code | temp;
            temp = null;
        }
        console.log(code.toString(2));
        size = getNumberOfBits(parameters['stages'], true, true);
        code = code << size;
        if (typeof(stage1) !== 'undefined') {
            console.log(stage1);
                    temp = 0;
                    mask = 1;
                    keys = Object.keys(parameters['stages']);
                    for (i = 0; i < keys.length; ++i) {
                        stage = keys[i];
                        console.log(stage);
                        if (stage1.indexOf(stage) !== -1) {
                            temp = temp | mask;
                        }
                        mask = mask << 1;
                    }
                    console.log(temp.toString(2));
                    code = code | temp;
                    temp = null;
                }
        console.log(code.toString(2));
        code = code << bitsForProps;
        if (typeof(prop2) !== 'undefined') {
                    console.log(prop2);
                    console.log(parameters['props']);
                    for (i=0; i < parameters['props'].length; ++i) {
                        if (prop1 === parameters['props'][i].split("/")[0]) {
                            temp = i;
                            break;
                        }
                    }
                    console.log(temp.toString(2));
                    code = code | temp;
                    temp = null;
                }
                console.log(code.toString(2));
                var bitsForAgg = getNumberOfBits(parameters['aggValues'], false, false);
                console.log(bitsForAgg);
                code = code << bitsForAgg;
                console.log(code.toString(2));
                if (typeof(aggProp2) !== 'undefined') {
                    temp = parameters['aggValues'].indexOf(aggProp2);
                    console.log(temp.toString(2));
                    code = code | temp;
                    temp = null;
                }
                console.log(code.toString(2));
                size = getNumberOfBits(parameters['stages'], true, true);
                code = code << size;
                if (typeof(stage2) !== 'undefined') {
                    console.log(stage2);
                            temp = 0;
                            mask = 1;
                            keys = Object.keys(parameters['stages']);
                            for (i = 0; i < keys.length; ++i) {
                                stage = keys[i];
                                console.log(stage);
                                if (stage2.indexOf(stage) !== -1) {
                                    temp = temp | mask;
                                }
                                mask = mask << 1;
                            }
                            console.log(temp.toString(2));
                            code = code | temp;
                            temp = null;
                        }
                console.log(code.toString(2));
                size = getNumberOfBits(parameters['hits'], true, true);
                code = code << size;
                if (typeof(hits) !== 'undefined') {
                                    console.log(hits);
                                            temp = 0;
                                            mask = 1;
                                            keys = Object.keys(parameters['hits']);
                                            for (i = 0; i < keys.length; ++i) {
                                                hit = keys[i];
                                                console.log(hit);
                                                if (hits.indexOf(hit) !== -1) {
                                                    temp = temp | mask;
                                                }
                                                mask = mask << 1;
                                            }
                                            console.log(temp.toString(2));
                                            code = code | temp;
                                            temp = null;
                                        }
                                console.log(code.toString(2));
                 /*size = getNumberOfBits(parameters['gene'], false, true);
                                 code = code << size;*/
                var geneBits='';
                if (typeof(geneList) !== 'undefined') {
                                                    console.log(geneList);
                                                            temp = 0;
                                                            mask = 1;
                                                            keys = parameters['gene'];
                                                            for (i = 0; i < keys.length; ++i) {
                                                                gene = keys[i];
                                                                //console.log(hit);
                                                                if (geneList.indexOf(gene) !== -1) {
                                                                    /*temp = temp | mask;*/
                                                                    geneBits = geneBits + '1';
                                                                }
                                                                else {
                                                                    geneBits = geneBits + '0';
                                                                }
                                                                //mask = mask << 1;
                                                            }
                                                            //geneBits = geneBits.split("").reverse().join("");;
                                                            /*console.log(temp.toString(2));
                                                            code = code | temp;
                                                            temp = null;*/
                                                        }
                 console.log(code.toString(2));
                 console.log(code);
                 console.log(btoa(code));
                 console.log(geneBits);
                 console.log(btoa(geneBits));
                 var compressed = pako.deflate(geneBits, {level: 9 , to: 'string'});
                 console.log(btoa(compressed));
                  console.log(geneBits.length);
                                   console.log(btoa(geneBits).length);
                 console.log(compressed.length);
                 console.log(btoa(compressed).length);
    }
    return {
        setLabels: function(_labels) {
                console.log(_labels)
                labels = _labels;
            },
            getLabels: function() {
                return labels;
            },
        getCompressedGeneList: function(geneList) {
            var geneBits='';
                            if (typeof(geneList) !== 'undefined') {
                                                                console.log(geneList);
                                                                        /*temp = 0;
                                                                        mask = 1;*/
                                                                        keys = parameters['gene'];
                                                                        for (i = 0; i < keys.length; ++i) {
                                                                            gene = keys[i];
                                                                            //console.log(hit);
                                                                            if (geneList.indexOf(gene) !== -1) {
                                                                                /*temp = temp | mask;*/
                                                                                geneBits = geneBits + '1';
                                                                            }
                                                                            else {
                                                                                geneBits = geneBits + '0';
                                                                            }
                                                                            //mask = mask << 1;
                                                                        }
                                                                        //geneBits = geneBits.split("").reverse().join("");;
                                                                        /*console.log(temp.toString(2));
                                                                        code = code | temp;
                                                                        temp = null;*/
                                                                    }
            var compressed = pako.deflate(geneBits, {level: 9 , to: 'string'});
            console.log(btoa(compressed));
            return btoa(compressed);
        },
        setURL: function(_URL) {
//            encodeURL(_URL);
            //console.log(encodeURL(_URL));
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
       },
       setSizes: function(_sizes) {

        sizes = _sizes;

       },
       getSizes: function() {
        return sizes;
       },
       setParameters: function(_parameters) {
        parameters = _parameters;
       },
       getParameters: function() {
        return parameters;
       }
    }
});