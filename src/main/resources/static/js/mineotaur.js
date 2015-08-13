require.config({
    baseUrl: 'js/lib',
    paths: {
        mineotaur: '../mineotaur'
    },
    shim: {
            'jquery.blockUI': ['jquery'],
            'jquery.form': ['jquery'],
            'jquery.history': ['jquery'],
            'jquery.jeegoocontext': ['jquery'],
            'jquery.magnific-popup': ['jquery'],
            'jquery.multiselect': ['jquery','jquery-ui'],
            'jquery.multiselect.filter': ['jquery','jquery-ui','jquery.multiselect'],
            'jquery-ui': ['jquery'],
            'bootstrap': ['jquery'],
            'regression' : {exports: 'regression'},
            'numbers': {exports: 'numbers'},
            'underscore': {
                  exports: '_'
                },
            
            'chardinjs.min': ['jquery']

    }
});

define(['mineotaur/controller' /*,'jquery', 'jquery.multiselect', 'jquery.multiselect.filter', ,'mineotaur/main',*/], function(controller /*,$, main*/) {
    //    urlArgs: "bust=" + (new Date()).getTime(),

    //require(['jquery', 'jquery-ui', 'jquery.multiselect', 'jquery.multiselect.filter']);
});