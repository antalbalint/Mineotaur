(function(b){var f=/[\-\[\]{}()*+?.,\\\^$|#\s]/g;b.widget("ech.multiselectfilter",{options:{label:"Filter:",width:null,placeholder:"Enter keywords",autoReset:!1},_create:function(){var a=this.options,c=b(this.element),d=this.instance=c.data("echMultiselect")||c.data("multiselect")||c.data("ech-multiselect");this.header=d.menu.find(".ui-multiselect-header").addClass("ui-multiselect-hasfilter");a=this.wrapper=b('<div class="ui-multiselect-filter">'+(a.label.length?a.label:"")+'<input placeholder="'+
a.placeholder+'" type="search"'+(/\d/.test(a.width)?'style="width:'+a.width+'px"':"")+" /></div>").prependTo(this.header);this.inputs=d.menu.find('input[type="checkbox"], input[type="radio"]');this.input=a.find("input").bind({keydown:function(b){13===b.which&&b.preventDefault()},keyup:b.proxy(this._handler,this),click:b.proxy(this._handler,this)});this.updateCache();d._toggleChecked=function(a,c){var e=c&&c.length?c:this.labels.find("input"),k=this,e=e.not(d._isOpen?":disabled, :hidden":":disabled").each(this._toggleState("checked",
a));this.update();var l=e.map(function(){return this.value}).get();this.element.find("option").filter(function(){!this.disabled&&-1<b.inArray(this.value,l)&&k._toggleState("selected",a).call(this)});e.length&&this.element.trigger("change")};a=b(document).bind("multiselectrefresh",b.proxy(function(){this.updateCache();this._handler()},this));this.options.autoReset&&a.bind("multiselectclose",b.proxy(this._reset,this))},_handler:function(a){var c=b.trim(this.input[0].value.toLowerCase()),d=this.rows,
g=this.inputs,h=this.cache;if(c){d.hide();var e=new RegExp(c.replace(f,"\\$&"),"gi");this._trigger("filter",a,b.map(h,function(b,a){return-1!==b.search(e)?(d.eq(a).show(),g.get(a)):null}))}else d.show();this.instance.menu.find(".ui-multiselect-optgroup-label").each(function(){var a=b(this),c=a.nextUntil(".ui-multiselect-optgroup-label").filter(function(){return"none"!==b.css(this,"display")}).length;a[c?"show":"hide"]()})},_reset:function(){this.input.val("").trigger("keyup")},updateCache:function(){this.rows=
this.instance.menu.find(".ui-multiselect-checkboxes li:not(.ui-multiselect-optgroup-label)");this.cache=this.element.children().map(function(){var a=b(this);"optgroup"===this.tagName.toLowerCase()&&(a=a.children());return a.map(function(){return this.innerHTML.toLowerCase()}).get()}).get()},widget:function(){return this.wrapper},destroy:function(){b.Widget.prototype.destroy.call(this);this.input.val("").trigger("keyup");this.wrapper.remove()}})})(jQuery);