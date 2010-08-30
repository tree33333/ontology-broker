
Broker = function() { 

	var get_params = function(uri) {
		var paramIndex = uri.indexOf('?');
		var dict = {};
		if(paramIndex !== -1) { 
			var paramArray = uri.slice(paramIndex + 1, uri.length).split('&');
			var i; 
			for(i = 0; i < paramArray.length; i++) { 
				var pair = paramArray[i].split('=', 2);
				var key = decodeURI(pair[0]);
				if(pair.length > 1) { 
					var value = decodeURI(pair[1]);
					dict[key] = value;
				} else { 
					dict[key] = true;
				}
			}
		}
		return dict;
	};
	
	var find_username = function(user_id, callback) { 
		$.postJSON('/user/' + user_id + '/',
			function(user_data) { 
				callback(user_data.user_id);	
			});
	};

	var date_string = function() { 
		var now = new Date();
		return (now.getMonth() + 1) + '-' + now.getDate() + '-' + now.getFullYear();
	};

	var debug = function(msg) { 
		if($('#debug').length > 0) { 
			$('#debug').append(msg + '<br>'); 
		} else { 
			alert(msg);
		}
	};

    var create_request = function(req, callback) {
        $.post('/requests', req, function(response) { callback(response); }, 'html');
    };
    
    var list_requests = function(callback) { 
        $.get('/requests?format=html', {}, callback, 'html');
    };
        
    return { 
		find_username: find_username,
		debug: debug,
        create_request: create_request,
        list_requests: list_requests,
		date_string: date_string,
		get_params: get_params

    };
}();

