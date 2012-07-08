alert("hello");
//function render(value) {
//	var html = $("<div class='intent'><div class='details'></div></div>");
//	
//	var el = html.find(".details");
//	$("<div>").addClass("property").append("<span class='key'>Action</span>: ").append(value.action).appendTo(el);
//	$("<div>").addClass("property").append("<span class='key'>Type</span>: ").append($("<span>").addClass("value").append(value.type)).appendTo(el);
//	var data = value.data;
//	var dataSpan;
//	if (data == null) {
//	  dataSpan = "<em>none</em>";
//	} else if (typeof(data) == "string") {
//	  if (data.indexOf("http:") == 0 || data.indexOf("https:") == 0) {
//	    dataSpan = $("<a>").attr("href", data).append(data);
//	  } else if (data.indexOf("data:image/") == 0) {
//	    dataSpan = $("<img>").attr("src", data);
//	  } else {
//	    dataSpan = $("<span>").addClass("value").append(data);
//	  }
//	} else {
//	  dataSpan = "<em>JS type " + typeof(data) + "</em>";
//	}
//	$("<div>").addClass("property").append("<span class='key'>Data</span>: ").append(dataSpan).appendTo(el);
//	
//	$("#intents").append(html);
//}
//var Intent = function(action, type, data) {
//    this.action = action;
//    this.type = type;
//    this.data = data;
//};  
//   
//window.intent = new Intent("http://webintents.org/share",
//          "text/uri-list", "http://www.163.com");
//render(window.intent);