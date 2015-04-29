({
    baseUrl: 'lib',
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
    },
    name: "mineotaur",
    out: "mineotaur-built.js"
})