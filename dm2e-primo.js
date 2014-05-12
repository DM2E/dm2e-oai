$(document).ready(function(){
    $(".EXLResultFourthLine").html(
        $("<a></a>")
            .attr("href", $(".EXLResultFourthLine").text())
            .append($("<img></img>").attr("src", "https://dl.dropboxusercontent.com/u/1415926/dm2e/annotate_me_small.png")));
    $(".EXLResultAuthor").each(function(idx, el){
        $(el).html($(el).text().replace(/(\d\d\d\d)-\d\d-\d\dT\d\d:\d\d:\d\dZ?/, "$1"));
    });
});
$(document).ajaxComplete(function(){
    $("li[id^=Creation] span").each(function(idx, el){
        $(el).html($(el).text().replace(/(\d\d\d\d)-\d\d-\d\dT\d\d:\d\d:\d\dZ?/, "$1"));
    });
    console.log("OI");
});
