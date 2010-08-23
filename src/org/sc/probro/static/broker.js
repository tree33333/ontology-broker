
Broker = function() { 
	
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
		date_string: date_string

    };
}();

