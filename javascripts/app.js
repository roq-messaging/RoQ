/* Foundation v2.1.4 http://foundation.zurb.com */
$(document).ready(function () {

	/* Use this js doc for all application specific JS */

	/* TABS --------------------------------- */
	/* Remove if you don't need :) */

	function activateTab($tab) {
		var $activeTab = $tab.closest('dl').find('a.active'),
				contentLocation = $tab.attr("href") + 'Tab';

		//Make Tab Active
		$activeTab.removeClass('active');
		$tab.addClass('active');

    	//Show Tab Content
		$(contentLocation).closest('.tabs-content').children('li').hide();
		$(contentLocation).show();
	}

	$('dl.tabs').each(function () {
		//Get all tabs
		var tabs = $(this).children('dd').children('a');
		tabs.click(function (e) {
			activateTab($(this));
		});
	});
  
  //Tab activation by links on the home tab
  $('a.call2tab').click(function () {
		//Get corresponding tab
    var target = $(this).attr("href");
    var tabTarget = $("dl.tabs > dd > a[href='"+target+"']");
		activateTab(tabTarget);
	});

	if (window.location.hash) {
		activateTab($('a[href="' + window.location.hash + '"]'));
	}

	/* ALERT BOXES ------------ */
	$(".alert-box").delegate("a.close", "click", function(event) {
    event.preventDefault();
	  $(this).closest(".alert-box").fadeOut(function(event){
	    $(this).remove();
	  });
	});


	/* PLACEHOLDER FOR FORMS ------------- */
	/* Remove this and jquery.placeholder.min.js if you don't need :) */

	$('input, textarea').placeholder();



	/* UNCOMMENT THE LINE YOU WANT BELOW IF YOU WANT IE6/7/8 SUPPORT AND ARE USING .block-grids */
//	$('.block-grid.two-up>li:nth-child(2n+1)').css({clear: 'left'});
//	$('.block-grid.three-up>li:nth-child(3n+1)').css({clear: 'left'});
//	$('.block-grid.four-up>li:nth-child(4n+1)').css({clear: 'left'});
//	$('.block-grid.five-up>li:nth-child(5n+1)').css({clear: 'left'});



	/* DROPDOWN NAV ------------- */

	var currentFoundationDropdown = null;
	$('.nav-bar li a, .nav-bar li a:after').each(function() {
		$(this).data('clicks', 0);
	});
	$('.nav-bar li a, .nav-bar li a:after').live('click', function(e) {
		e.preventDefault();
		if (currentFoundationDropdown !== $(this).index() || currentFoundationDropdown === null) {
			$(this).data('clicks', 0);
			currentFoundationDropdown = $(this).index();
		}
		$(this).data('clicks', ($(this).data('clicks') + 1));
		var f = $(this).siblings('.flyout');
		if (!f.is(':visible') && $(this).parent('.has-flyout').length > 1) {
			$('.nav-bar li .flyout').hide();
			f.show();
		} else if (($(this).data('clicks') > 1) || ($(this).parent('.has-flyout').length < 1)) {
			window.location = $(this).attr('href');
		}
	});
	$('.nav-bar').live('click', function(e) {
		e.stopPropagation();
		if ($(e.target).parents().is('.flyout') || $(e.target).is('.flyout')) {
			e.preventDefault();
		}
	});
	// $('body').bind('touchend', function(e) {
	// 	if (!$(e.target).parents().is('.nav-bar') || !$(e.target).is('.nav-bar')) {
	// 		$('.nav-bar li .flyout').is(':visible').hide();
	// 	}
	// });

	/* DISABLED BUTTONS ------------- */
	/* Gives elements with a class of 'disabled' a return: false; */

  // Orbit features
  $('#features').orbit({
     animation: 'horizontal-push',                  // fade, horizontal-slide, vertical-slide, horizontal-push
     animationSpeed: 800,                // how fast animtions are
     timer: true, 			 // true or false to have the timer
     advanceSpeed: 4000, 		 // if timer is enabled, time between transitions 
     pauseOnHover: true, 		 // if you hover pauses the slider
     startClockOnMouseOut: true, 	 // if clock should start on MouseOut
     startClockOnMouseOutAfter: 500, 	 // how long after MouseOut should the timer start again
     directionalNav: false, 		 // manual advancing directional navs
     captions: true, 			 // do you want captions?
     captionAnimation: 'none', 		 // fade, slideOpen, none
     captionAnimationSpeed: 800, 	 // if so how quickly should they animate in
     bullets: true,			 // true or false to activate the bullet navigation
     fluid: true                         // or set a aspect ratio for content slides (ex: '4x3') 
});

});
